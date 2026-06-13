import { useCallback, useRef } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useLocation, useNavigate } from 'react-router-dom';
import type { RootState, AppDispatch } from '../store';
import {
  addMessage,
  setStreaming,
  appendStreamContent,
  clearStream,
  setSession,
  setSessions,
  setMessages,
  setPendingApproval,
  clearApproval,
  setError,
  clearError,
  setProvider,
  setNavigateToast,
  resolveLastToolCall,
  resolveMessage,
} from '../store/slices/agentSlice';
import {
  streamChat,
  getSessions,
  getMessages,
  deleteSession as apiDeleteSession,
  executeTool,
} from '../api/agentApi';

export function useAgentChat() {
  const dispatch = useDispatch<AppDispatch>();
  const location = useLocation();
  const navigate = useNavigate();

  const {
    currentSessionId,
    isStreaming,
    error,
    pendingApproval,
    messages,
    sessions,
  } = useSelector((state: RootState) => state.agent);

  const abortRef = useRef<(() => void) | null>(null);
  const contentRef = useRef('');
  // Track the session ID received from the server during streaming
  const streamSessionRef = useRef<string | null>(null);
  const lastToolCallIdRef = useRef<string | null>(null);

  // ── Send a message and handle the SSE stream ──────────────────────────────
  const sendMessage = useCallback(
    (message: string) => {
      if (isStreaming) return;

      // Add user message immediately
      dispatch(
        addMessage({
          messageId: `user-${Date.now()}`,
          sessionId: currentSessionId || '',
          role: 'user',
          content: message,
          createdAt: new Date().toISOString(),
          type: 'text',
        }),
      );

      dispatch(setStreaming(true));
      dispatch(clearError());
      contentRef.current = '';
      streamSessionRef.current = null;
      lastToolCallIdRef.current = null;

      const context = {
        currentPage: location.pathname,
        currentSearch: location.search,
      };

      const stream = streamChat(currentSessionId, message, context);
      abortRef.current = stream.abort;

      stream
        .onSession((sessionId) => {
          // Capture the session ID from the server without triggering loadMessages
          streamSessionRef.current = sessionId;
          // Only set if we didn't have one before (new session)
          if (!currentSessionId) {
            dispatch(setSession(sessionId));
          }
        })
        .onText((text) => {
          if (lastToolCallIdRef.current) {
            dispatch(resolveMessage(lastToolCallIdRef.current));
            lastToolCallIdRef.current = null;
          }
          if (text.includes('✅ Tool result received.')) {
            dispatch(resolveLastToolCall());
            return;
          }
          contentRef.current += text;
          dispatch(appendStreamContent(text));
        })
        .onToolCall((_tool) => {
          const chipId = `tool-${Date.now()}-${Math.random()}`;
          dispatch(
            addMessage({
              messageId: chipId,
              sessionId: streamSessionRef.current || currentSessionId || '',
              role: 'assistant',
              content: '',
              createdAt: new Date().toISOString(),
              type: 'tool_call',
              toolName: _tool.name,
              toolParams: _tool.params,
              resolved: false,
            })
          );
          lastToolCallIdRef.current = chipId;
        })
        .onApproval((approval) => {
          dispatch(setPendingApproval(approval));
        })
        .onProvider((provider) => {
          dispatch(setProvider(provider));
        })
        .onNavigate((route) => {
          try {
            // Map common synonyms to actual routes
            let targetRoute = route;
            if (route === '/billing') {
              targetRoute = '/billing-panels';
            }
            dispatch(setNavigateToast(targetRoute));
            setTimeout(() => {
              navigate(targetRoute);
              dispatch(setNavigateToast(null));
            }, 800);
          } catch (e) {
            console.warn('[Agent] Navigation failed:', route, e);
          }
        })
        .onDone((data) => {
          if (lastToolCallIdRef.current) {
            dispatch(resolveMessage(lastToolCallIdRef.current));
            lastToolCallIdRef.current = null;
          }
          dispatch(setStreaming(false));

          // Add the final assistant message from accumulated content
          const finalContent = contentRef.current.trim();
          if (finalContent) {
            dispatch(
              addMessage({
                messageId: data.messageId || `ast-${Date.now()}`,
                sessionId: data.sessionId || streamSessionRef.current || currentSessionId || '',
                role: 'assistant',
                content: finalContent,
                createdAt: new Date().toISOString(),
                type: 'text',
              }),
            );
          }

          dispatch(clearStream());
          contentRef.current = '';

          // Update session ID if it's a new session — but DON'T clear messages
          const newSessionId = data.sessionId || streamSessionRef.current;
          if (newSessionId && newSessionId !== currentSessionId) {
            // Directly update session ID without clearing messages
            dispatch(setSession(newSessionId));
          }

          // Refresh sessions list in the sidebar (non-blocking)
          loadSessionsInternal();
        })
        .onError((errMsg) => {
          if (lastToolCallIdRef.current) {
            dispatch(resolveMessage(lastToolCallIdRef.current));
            lastToolCallIdRef.current = null;
          }
          dispatch(setError(errMsg));
          dispatch(setStreaming(false));
          dispatch(clearStream());
          contentRef.current = '';
        });
    },
    [dispatch, currentSessionId, isStreaming, location.pathname, location.search],
  );

  // ── Session management ────────────────────────────────────────────────────

  const loadSessionsInternal = async () => {
    try {
      const result = await getSessions(0);
      dispatch(setSessions(result.content || []));
    } catch (err) {
      console.error('[Agent] Failed to load sessions:', err);
    }
  };

  const loadSessions = useCallback(() => {
    loadSessionsInternal();
  }, [dispatch]);

  const loadMessages = useCallback(
    async (sessionId: string) => {
      try {
        const result = await getMessages(sessionId, 0);
        const msgs = (result.content || []).map((m: any) => ({
          ...m,
          type: m.type || 'text',
        }));
        // Reverse because backend returns DESC order
        dispatch(setMessages(msgs.reverse()));
      } catch (err: any) {
        dispatch(setError(err.message || 'Failed to load messages'));
      }
    },
    [dispatch],
  );

  const deleteSessionHandler = useCallback(
    async (sessionId: string) => {
      try {
        await apiDeleteSession(sessionId);
        dispatch(
          setSessions(sessions.filter((s) => s.sessionId !== sessionId)),
        );
        if (currentSessionId === sessionId) {
          dispatch(setSession(null));
        }
      } catch (err: any) {
        dispatch(setError(err.message || 'Failed to delete session'));
      }
    },
    [dispatch, sessions, currentSessionId],
  );

  // ── Tool approval ─────────────────────────────────────────────────────────

  const approveAction = useCallback(
    async (toolName: string, params: Record<string, any>) => {
      if (!currentSessionId) return;
      try {
        const result = await executeTool(currentSessionId, toolName, params);
        dispatch(
          addMessage({
            messageId: `result-${Date.now()}`,
            sessionId: currentSessionId,
            role: 'tool',
            content: typeof result === 'string' ? result : JSON.stringify(result, null, 2),
            toolName,
            createdAt: new Date().toISOString(),
            type: 'tool_result',
          }),
        );
        dispatch(clearApproval());
      } catch (err: any) {
        dispatch(setError(err.message || 'Tool execution failed'));
      }
    },
    [dispatch, currentSessionId],
  );

  const cancelAction = useCallback(() => {
    dispatch(clearApproval());
  }, [dispatch]);

  // ── Abort stream ──────────────────────────────────────────────────────────

  const abortStream = useCallback(() => {
    abortRef.current?.();
    dispatch(setStreaming(false));
    dispatch(clearStream());
    contentRef.current = '';
  }, [dispatch]);

  return {
    sendMessage,
    approveAction,
    cancelAction,
    loadSessions,
    loadMessages,
    deleteSession: deleteSessionHandler,
    abortStream,
    isStreaming,
    error,
    pendingApproval,
    messages,
    sessions,
    currentSessionId,
  };
}
