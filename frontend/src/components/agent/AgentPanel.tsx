import React, { useEffect, useRef, useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { X, History, Bot, Sparkles, Navigation } from 'lucide-react';
import { RootState } from '../../store';
import { closePanel, togglePanel, setSession } from '../../store/slices/agentSlice';
import { useAgentChat } from '../../hooks/useAgentChat';
import { AgentMessage } from './AgentMessage';
import { AgentInput } from './AgentInput';
import { ActionCard } from './ActionCard';
import { ProviderBadge } from './ProviderBadge';
import { RateLimitNotice } from './RateLimitNotice';

function AgentEmptyState() {
  return (
    <div className="agent-empty">
      <div className="agent-empty__icon">
        <svg width="56" height="56" viewBox="0 0 56 56" fill="none">
          <circle cx="28" cy="28" r="27" stroke="url(#grad)" strokeWidth="1.5" />
          <defs>
            <linearGradient id="grad" x1="0" y1="0" x2="56" y2="56" gradientUnits="userSpaceOnUse">
              <stop offset="0%" stopColor="#6C63FF" />
              <stop offset="50%" stopColor="#A78BFA" />
              <stop offset="100%" stopColor="#38BDF8" />
            </linearGradient>
          </defs>
        </svg>
        <Bot
          size={24}
          style={{ position: 'absolute', color: '#A78BFA' }}
          strokeWidth={1.5}
        />
      </div>
      <h3 className="agent-empty__title">TravelBill AI</h3>
      <p className="agent-empty__subtitle">
        Search tickets, pull invoices, check companies, and navigate the app — all from one conversation.
      </p>
    </div>
  );
}

export const AgentPanel: React.FC = () => {
  const dispatch = useDispatch();
  const [showSessions, setShowSessions] = useState(false);

  const {
    panelOpen,
    currentSessionId,
    messages,
    isStreaming,
    streamingContent,
    pendingApproval,
    navigateToast,
  } = useSelector((state: RootState) => state.agent);

  const {
    sendMessage,
    approveAction,
    cancelAction,
    loadSessions,
    loadMessages,
    deleteSession,
    abortStream,
    sessions,
  } = useAgentChat();

  const bodyRef = useRef<HTMLDivElement>(null);

  // Load sessions when panel opens
  useEffect(() => {
    if (panelOpen) {
      loadSessions();
    }
  }, [panelOpen, loadSessions]);

  // Load messages when session changes
  useEffect(() => {
    if (currentSessionId) {
      loadMessages(currentSessionId);
    }
  }, [currentSessionId, loadMessages]);

  // Auto-scroll to bottom on new messages / streaming
  useEffect(() => {
    if (bodyRef.current) {
      bodyRef.current.scrollTop = bodyRef.current.scrollHeight;
    }
  }, [messages, streamingContent]);

  // Keyboard shortcut to open/toggle panel: Ctrl+K / Cmd+K
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        dispatch(togglePanel());
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [dispatch]);

  // Escape key to close panel
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && panelOpen) {
        dispatch(closePanel());
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [panelOpen, dispatch]);

  const toggleSessionMenu = () => {
    setShowSessions(!showSessions);
  };

  const showEmptyState = messages.length === 0 && !isStreaming;

  return (
    <div
      data-agent-panel
      style={{
        transform: panelOpen ? 'translateX(0)' : 'translateX(100%)',
        opacity: panelOpen ? 1 : 0,
        transition: 'transform 0.35s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.25s ease',
      }}
    >
      {/* ── Header ────────────────────────────────────────────────────── */}
      <header className="agent-header">
        <div className="agent-avatar">
          {isStreaming && <span className="agent-avatar__pulse" />}
          <span className="agent-avatar__inner">
            <Sparkles size={16} strokeWidth={1.5} />
          </span>
        </div>

        <div className="agent-identity">
          <span className="agent-name">TravelBill AI</span>
          <ProviderBadge />
        </div>

        <div className="agent-header-actions">
          <button title="Sessions" onClick={toggleSessionMenu}>
            <History size={16} />
          </button>
          <button title="Close" onClick={() => dispatch(closePanel())}>
            <X size={16} />
          </button>
        </div>
      </header>

      {/* ── Collapsible Session Bar ───────────────────────────────────── */}
      {showSessions && sessions.length > 0 && (
        <div className="session-list">
          <button className="session-new" onClick={() => dispatch(setSession(null))}>
            + New Chat
          </button>
          {sessions.slice(0, 5).map((s) => (
            <button
              key={s.sessionId}
              className={`session-chip ${s.sessionId === currentSessionId ? 'active' : ''}`}
              onClick={() => dispatch(setSession(s.sessionId))}
              title={s.title || 'Untitled session'}
            >
              {(s.title || 'Chat').slice(0, 20)}
              <span
                className="session-chip__delete"
                onClick={(e) => {
                  e.stopPropagation();
                  e.preventDefault();
                  deleteSession(s.sessionId);
                }}
                title="Delete session"
              >
                ×
              </span>
            </button>
          ))}
        </div>
      )}

      {/* ── Rate Limit Notice ─────────────────────────────────────────── */}
      <RateLimitNotice />

      {/* ── Messages Area ─────────────────────────────────────────────── */}
      <div className="agent-messages" ref={bodyRef}>
        {showEmptyState ? (
          <AgentEmptyState />
        ) : (
          <>
            {messages.map((msg) => (
              <AgentMessage
                key={msg.messageId || msg.id}
                message={msg}
                activeStreaming={isStreaming}
              />
            ))}

            {/* Streaming message */}
            {isStreaming && streamingContent && (
              <AgentMessage
                message={{
                  messageId: '__streaming__',
                  sessionId: currentSessionId || '',
                  role: 'assistant',
                  content: streamingContent,
                  createdAt: new Date().toISOString(),
                  type: 'text',
                }}
                isStreaming
                activeStreaming={isStreaming}
              />
            )}

            {/* Pending approval */}
            {pendingApproval && (
              <ActionCard
                approval={pendingApproval}
                onApprove={approveAction}
                onCancel={cancelAction}
              />
            )}
          </>
        )}
      </div>

      {/* ── Footer / Input ────────────────────────────────────────────── */}
      <AgentInput
        onSend={sendMessage}
        onStop={abortStream}
        isStreaming={isStreaming}
      />
      <div className="agent-kbd-hint">
        ⌘K to open · Esc to close
      </div>

      {/* ── Navigation Toast ─────────────────────────────────────────── */}
      {navigateToast && (
        <div className="agent-toast">
          <Navigation size={14} style={{ marginRight: 6, display: 'inline-block', verticalAlign: 'middle' }} />
          Navigating to {navigateToast.replace(/^\//, '')}...
        </div>
      )}
    </div>
  );
};
