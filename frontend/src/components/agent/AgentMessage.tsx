import React, { useMemo } from 'react';
import { Terminal, AlertCircle } from 'lucide-react';
import type { AgentMessage as AgentMessageType } from '../../api/agentApi';

interface AgentMessageProps {
  message: AgentMessageType;
  isStreaming?: boolean;
}

// ── Relative time formatter ──────────────────────────────────────────────

function formatRelativeTime(timestamp: string): string {
  const diff = Date.now() - new Date(timestamp).getTime();
  const seconds = Math.floor(diff / 1000);
  if (seconds < 60) return 'just now';
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

// ── Simple markdown renderer (code blocks + inline code) ─────────────────

function renderContent(content: string): React.ReactNode {
  const parts = content.split(/(```[\s\S]*?```)/g);

  return parts.map((part, i) => {
    // Fenced code block
    if (part.startsWith('```') && part.endsWith('```')) {
      const inner = part.slice(3, -3);
      const newlineIndex = inner.indexOf('\n');
      const code = newlineIndex > -1 ? inner.slice(newlineIndex + 1) : inner;
      return (
        <pre key={i}>
          <code>{code}</code>
        </pre>
      );
    }

    // Inline code
    const inlineParts = part.split(/(`[^`]+`)/g);
    return inlineParts.map((ip, j) => {
      if (ip.startsWith('`') && ip.endsWith('`')) {
        return <code key={`${i}-${j}`}>{ip.slice(1, -1)}</code>;
      }
      // Preserve line breaks
      return ip.split('\n').map((line, k, arr) => (
        <React.Fragment key={`${i}-${j}-${k}`}>
          {line}
          {k < arr.length - 1 && <br />}
        </React.Fragment>
      ));
    });
  });
}

// ── Component ────────────────────────────────────────────────────────────

export const AgentMessage: React.FC<AgentMessageProps> = ({
  message,
  isStreaming,
}) => {
  const timeStr = useMemo(
    () => formatRelativeTime(message.createdAt || message.timestamp || new Date().toISOString()),
    [message.createdAt, message.timestamp],
  );

  // Tool call
  if (message.type === 'tool_call') {
    return (
      <div className="agent-message agent-message--assistant">
        <div className="agent-tool-card">
          <div className="agent-tool-card-header">
            <Terminal size={14} />
            {message.toolName || 'Tool Call'}
          </div>
          {message.toolParams && (
            <div className="agent-tool-card-params">
              {JSON.stringify(message.toolParams, null, 2)}
            </div>
          )}
        </div>
        <span className="agent-message-time">{timeStr}</span>
      </div>
    );
  }

  // Tool result
  if (message.type === 'tool_result') {
    return (
      <div className="agent-message agent-message--assistant">
        <div
          className="agent-tool-card"
          style={{
            borderColor: 'rgba(34, 197, 94, 0.2)',
            background: 'rgba(34, 197, 94, 0.05)',
          }}
        >
          <div
            className="agent-tool-card-header"
            style={{ color: 'var(--agent-success)' }}
          >
            <Terminal size={14} />
            Result: {message.toolName || 'Tool'}
          </div>
          <div className="agent-tool-card-params">{message.content}</div>
        </div>
        <span className="agent-message-time">{timeStr}</span>
      </div>
    );
  }

  // Error
  if (message.type === 'error') {
    return (
      <div className="agent-message agent-message--assistant">
        <div className="agent-error-card">
          <AlertCircle
            size={14}
            style={{ marginRight: 6, flexShrink: 0, marginTop: 2 }}
          />
          {message.content}
        </div>
        <span className="agent-message-time">{timeStr}</span>
      </div>
    );
  }

  // User message
  if (message.role === 'user') {
    return (
      <div className="agent-message agent-message--user">
        <div className="agent-message-bubble agent-message-bubble--user">
          {message.content}
        </div>
        <span className="agent-message-time">{timeStr}</span>
      </div>
    );
  }

  // Assistant message (default)
  return (
    <div className="agent-message agent-message--assistant">
      <div className="agent-message-bubble agent-message-bubble--assistant">
        {renderContent(message.content)}
        {isStreaming && <span className="agent-typing-cursor" />}
      </div>
      <span className="agent-message-time">{timeStr}</span>
    </div>
  );
};
