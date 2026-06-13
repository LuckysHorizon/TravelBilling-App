import React, { useEffect, useRef } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { Select } from 'antd';
import { X, Sparkles, MessageSquare, Trash2 } from 'lucide-react';
import { RootState } from '../../store';
import { closePanel, setSession } from '../../store/slices/agentSlice';
import { useAgentChat } from '../../hooks/useAgentChat';
import { AgentMessage } from './AgentMessage';
import { AgentInput } from './AgentInput';
import { ActionCard } from './ActionCard';
import { ProviderBadge } from './ProviderBadge';
import { RateLimitNotice } from './RateLimitNotice';

const SUGGESTIONS = [
  'Show me all pending tickets',
  'Generate billing report for this month',
  'How many invoices are due this week?',
  'Navigate to ticket upload',
  'Show dashboard statistics',
  'List all companies',
];

export const AgentPanel: React.FC = () => {
  const dispatch = useDispatch();
  const {
    panelOpen,
    currentSessionId,
    messages,
    isStreaming,
    streamingContent,
    pendingApproval,
  } = useSelector((state: RootState) => state.agent);

  const {
    sendMessage,
    approveAction,
    cancelAction,
    loadSessions,
    loadMessages,
    deleteSession,
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

  const handleSessionChange = (value: string) => {
    if (value === '__new__') {
      dispatch(setSession(null));
    } else {
      dispatch(setSession(value));
    }
  };

  const handleDeleteSession = (sessionId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    e.preventDefault();
    deleteSession(sessionId);
  };

  const showEmptyState = messages.length === 0 && !isStreaming;

  return (
    <div className={`agent-panel ${panelOpen ? 'agent-panel--open' : ''}`}>
      {/* ── Header ────────────────────────────────────────────────────── */}
      <div className="agent-panel-header">
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 12,
            flex: 1,
            minWidth: 0,
          }}
        >
          <div className="agent-panel-title">
            <div className="agent-panel-title-icon">
              <Sparkles size={14} color="white" />
            </div>
            TravelBill AI
          </div>
          <ProviderBadge />
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          {sessions.length > 0 && (
            <Select
              className="agent-session-select"
              value={currentSessionId || '__new__'}
              onChange={handleSessionChange}
              size="small"
              style={{ width: 140 }}
              popupMatchSelectWidth={false}
              options={[
                { label: '+ New Chat', value: '__new__' },
                ...sessions.map((s) => ({
                  label: (
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        gap: 8,
                      }}
                    >
                      <span
                        style={{
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                        }}
                      >
                        {s.title || 'Untitled'}
                      </span>
                      <Trash2
                        size={12}
                        style={{ flexShrink: 0, opacity: 0.5, cursor: 'pointer' }}
                        onClick={(e: any) => handleDeleteSession(s.sessionId, e)}
                      />
                    </div>
                  ),
                  value: s.sessionId,
                })),
              ]}
            />
          )}
          <button
            className="agent-panel-close"
            onClick={() => dispatch(closePanel())}
            aria-label="Close AI panel"
          >
            <X size={18} />
          </button>
        </div>
      </div>

      {/* ── Rate Limit Notice ─────────────────────────────────────────── */}
      <RateLimitNotice />

      {/* ── Body ──────────────────────────────────────────────────────── */}
      <div className="agent-panel-body" ref={bodyRef}>
        {showEmptyState ? (
          <div className="agent-empty-state">
            <MessageSquare size={64} className="agent-empty-state-icon" />
            <h3 className="agent-empty-state-title">TravelBill AI Assistant</h3>
            <p className="agent-empty-state-subtitle">
              Ask me anything about your travel billing data, generate reports,
              or navigate the app.
            </p>
            <div className="agent-suggestions">
              {SUGGESTIONS.map((suggestion) => (
                <button
                  key={suggestion}
                  className="agent-suggestion-chip"
                  onClick={() => sendMessage(suggestion)}
                >
                  {suggestion}
                </button>
              ))}
            </div>
          </div>
        ) : (
          <>
            {messages.map((msg) => (
              <AgentMessage key={msg.messageId || msg.id} message={msg} />
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

      {/* ── Footer ────────────────────────────────────────────────────── */}
      <div className="agent-panel-footer">
        <AgentInput onSend={sendMessage} isStreaming={isStreaming} />
      </div>
    </div>
  );
};
