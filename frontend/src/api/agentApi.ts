import config from '../config';
import api from './axiosInstance';

// ── Types ────────────────────────────────────────────────────────────────────

export interface AgentSession {
  sessionId: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  messageCount: number;
  totalTokens?: number;
  providerUsed?: string;
  /** Convenience alias for sessionId, used by UI components */
  id?: string;
}

export interface AgentMessage {
  messageId: string;
  sessionId: string;
  role: 'user' | 'assistant' | 'tool' | 'system';
  content: string;
  toolCalls?: Record<string, any> | null;
  toolResults?: Record<string, any> | null;
  tokenCount?: number;
  provider?: string;
  model?: string;
  latencyMs?: number;
  createdAt: string;
  /** Convenience alias for messageId, used by UI components */
  id?: string;
  /** Local-only fields used by the hook for in-memory messages */
  toolName?: string;
  toolParams?: Record<string, any>;
  toolResult?: string;
  timestamp?: string;
  type?: 'text' | 'tool_call' | 'tool_result' | 'approval' | 'error';
  resolved?: boolean;
}

export interface ToolApproval {
  toolName: string;
  description: string;
  params: Record<string, any>;
  sessionId: string;
}

type TextCallback = (text: string) => void;
type ToolCallCallback = (tool: { name: string; params: Record<string, any> }) => void;
type ApprovalCallback = (approval: ToolApproval) => void;
type DoneCallback = (data: { sessionId: string; messageId: string }) => void;
type ErrorCallback = (error: string) => void;
type ProviderCallback = (provider: string) => void;
type SessionCallback = (sessionId: string) => void;
type NavigateCallback = (route: string) => void;

export interface StreamHandle {
  onText(cb: TextCallback): StreamHandle;
  onToolCall(cb: ToolCallCallback): StreamHandle;
  onApproval(cb: ApprovalCallback): StreamHandle;
  onDone(cb: DoneCallback): StreamHandle;
  onError(cb: ErrorCallback): StreamHandle;
  onProvider(cb: ProviderCallback): StreamHandle;
  onSession(cb: SessionCallback): StreamHandle;
  onNavigate(cb: NavigateCallback): StreamHandle;
  abort(): void;
}

// ── SSE Stream ───────────────────────────────────────────────────────────────

export function streamChat(
  sessionId: string | null,
  message: string,
  context: object,
): StreamHandle {
  const controller = new AbortController();

  let textCb: TextCallback | null = null;
  let toolCallCb: ToolCallCallback | null = null;
  let approvalCb: ApprovalCallback | null = null;
  let doneCb: DoneCallback | null = null;
  let errorCb: ErrorCallback | null = null;
  let providerCb: ProviderCallback | null = null;
  let sessionCb: SessionCallback | null = null;
  let navigateCb: NavigateCallback | null = null;

  // Track the session ID received from the server
  let receivedSessionId = sessionId || '';

  function fireText(v: string) { if (textCb) textCb(v); }
  function fireToolCall(v: any) { if (toolCallCb) toolCallCb(v); }
  function fireApproval(v: any) { if (approvalCb) approvalCb(v); }
  function fireDone(v: any) { if (doneCb) doneCb(v); }
  function fireError(v: string) { if (errorCb) errorCb(v); }
  function fireProvider(v: string) { if (providerCb) providerCb(v); }
  function fireSession(v: string) { if (sessionCb) sessionCb(v); }
  function fireNavigate(v: string) { if (navigateCb) navigateCb(v); }

  const handle: StreamHandle = {
    onText(cb) { textCb = cb; return handle; },
    onToolCall(cb) { toolCallCb = cb; return handle; },
    onApproval(cb) { approvalCb = cb; return handle; },
    onDone(cb) { doneCb = cb; return handle; },
    onError(cb) { errorCb = cb; return handle; },
    onProvider(cb) { providerCb = cb; return handle; },
    onSession(cb) { sessionCb = cb; return handle; },
    onNavigate(cb) { navigateCb = cb; return handle; },
    abort() { controller.abort(); },
  };

  // Parse a single SSE event JSON
  function processEvent(event: any) {
    const eventType = event.type || '';
    const content = event.content ?? '';

    switch (eventType) {
      case 'session':
        // Server tells us the session ID
        receivedSessionId = content;
        fireSession(content);
        break;

      case 'text':
        if (typeof content === 'string' && content.length > 0) {
          fireText(content);
        }
        break;

      case 'tool_call': {
        // Extract tool info from toolCall field
        const tc = event.toolCall || {};
        let name = tc.name || tc.function?.name;
        if (!name && typeof content === 'string') {
          const match = content.match(/Using:\s*(\w+)/);
          if (match) {
            name = match[1];
          }
        }
        if (!name) name = 'Unknown Tool';
        const params = tc.params || tc.arguments || tc.function?.arguments || {};
        fireToolCall({ name, params: typeof params === 'string' ? JSON.parse(params) : params });
        break;
      }

      case 'tool_result':
        // Tool result is informational — append to streaming text
        fireText(`\n✅ Tool result received.\n`);
        break;

      case 'approval_required':
      case 'approval': {
        const tc = event.toolCall || {};
        fireApproval({
          toolName: tc.name || content,
          description: content,
          params: tc.params || tc.arguments || {},
          sessionId: receivedSessionId,
        });
        break;
      }

      case 'done': {
        const usageInfo = event.usage || {};
        fireDone({
          sessionId: usageInfo.sessionId || receivedSessionId || content,
          messageId: usageInfo.messageId || `msg-${Date.now()}`,
        });
        break;
      }

      case 'error':
        fireError(typeof content === 'string' ? content : (content?.message || 'Unknown error'));
        break;

      case 'provider':
        fireProvider(typeof content === 'string' ? content : (content?.name || 'unknown'));
        break;

      case 'navigate':
        // Backend tells us to navigate to a route
        if (typeof content === 'string' && content.length > 0) {
          fireNavigate(content);
        }
        break;

      case 'usage':
        // Usage stats — ignore in UI
        break;

      default:
        console.debug('[Agent SSE] Unknown event type:', eventType, event);
    }
  }

  // Start fetch in background
  const run = async () => {
    try {
      const baseUrl = config.apiBaseUrl;
      const response = await fetch(`${baseUrl}/api/agent/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ sessionId, message, context }),
        signal: controller.signal,
      });

      if (!response.ok) {
        const errText = await response.text().catch(() => 'Unknown error');
        fireError(errText || `HTTP ${response.status}`);
        return;
      }

      const reader = response.body?.getReader();
      if (!reader) {
        fireError('No response body');
        return;
      }

      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });

        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
          const trimmed = line.trim();
          if (!trimmed || trimmed.startsWith(':')) continue;

          // Handle both "event: xxx\ndata: {...}" and plain "data: {...}" formats
          if (trimmed.startsWith('data:')) {
            const jsonStr = trimmed.slice(5).trim();
            if (!jsonStr || jsonStr === '[DONE]') continue;
            try {
              const event = JSON.parse(jsonStr);
              processEvent(event);
            } catch {
              // Ignore malformed JSON
            }
          }
          // Skip "event:" lines — we read the type from the JSON data field
        }
      }

      // Process remaining buffer
      if (buffer.trim().startsWith('data:')) {
        const jsonStr = buffer.trim().slice(5).trim();
        if (jsonStr && jsonStr !== '[DONE]') {
          try {
            const event = JSON.parse(jsonStr);
            processEvent(event);
          } catch {
            // ignore
          }
        }
      }
    } catch (err: any) {
      if (err.name !== 'AbortError') {
        fireError(err.message || 'Stream connection failed');
      }
    }
  };

  run();
  return handle;
}

// ── REST Endpoints ───────────────────────────────────────────────────────────

export async function getSessions(
  page: number = 0,
  size: number = 20,
): Promise<{ content: AgentSession[]; totalElements: number }> {
  const response = await api.get('/agent/sessions', { params: { page, size } });
  return response.data;
}

export async function getMessages(
  sessionId: string,
  page: number = 0,
  size: number = 50,
): Promise<{ content: AgentMessage[]; totalElements: number }> {
  const response = await api.get(`/agent/sessions/${sessionId}/messages`, { params: { page, size } });
  return response.data;
}

export async function deleteSession(sessionId: string): Promise<void> {
  await api.delete(`/agent/sessions/${sessionId}`);
}

export async function executeTool(
  sessionId: string,
  toolName: string,
  params: Record<string, any>,
): Promise<{ success: boolean; data: any; message: string }> {
  const response = await api.post('/agent/tool/execute', {
    sessionId,
    toolName,
    parameters: params,
    approved: true,
  });
  return response.data;
}
