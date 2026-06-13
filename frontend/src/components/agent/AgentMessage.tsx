import React, { useMemo } from 'react';
import {
  Copy,
  CheckCircle,
  Ticket,
  Building2,
  FileText,
  BarChart2,
  Navigation,
  User,
  Settings,
} from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import type { AgentMessage as AgentMessageType } from '../../api/agentApi';

interface AgentMessageProps {
  message: AgentMessageType;
  isStreaming?: boolean;
  activeStreaming?: boolean; // indicates if the agent is actively streaming text
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

// ── Lucide Icon Mapping for Tools ────────────────────────────────────────

const toolIconMap: Record<string, React.ReactNode> = {
  get_tickets:          <Ticket size={12} />,
  get_companies:        <Building2 size={12} />,
  get_invoices:         <FileText size={12} />,
  get_dashboard_stats:  <BarChart2 size={12} />,
  navigate_to:          <Navigation size={12} />,
  get_current_context:  <User size={12} />,
};

// ── Tool Call Chip Component ─────────────────────────────────────────────

interface ToolCallChipProps {
  toolName: string;
  resolved: boolean;
}

function ToolCallChip({ toolName, resolved }: ToolCallChipProps) {
  const label = toolName.replace(/_/g, ' ');

  return (
    <div className={`tool-chip ${resolved ? 'tool-chip--done' : ''}`}>
      <span className="tool-chip__dot" />
      <span className="tool-chip__icon">
        {toolIconMap[toolName] ?? <Settings size={12} />}
      </span>
      <span className="tool-chip__label">
        {resolved ? 'Used' : 'Running'} <code>{label}</code>
      </span>
      {resolved ? (
        <CheckCircle size={12} className="tool-chip__check" />
      ) : (
        <span className="tool-chip__spinner" />
      )}
    </div>
  );
}

// ── Stats Detection & Rendering ──────────────────────────────────────────

function isStatMessage(content: string): boolean {
  if (content.includes('|') && content.includes('Metric')) {
    // If it's already a Markdown table, let ReactMarkdown render it
    return false;
  }
  const lines = content.split('\n');
  let statLines = 0;
  for (const line of lines) {
    const trimmed = line.trim();
    if (/^[\*\-\s]*[\w\s]{2,30}:\s*\d+/.test(trimmed)) {
      statLines++;
    }
  }
  return statLines >= 3;
}

function parseStats(content: string): { label: string; value: string }[] {
  const lines = content.split('\n');
  const stats: { label: string; value: string }[] = [];
  for (const line of lines) {
    const trimmed = line.trim();
    if (/^[\*\-\s]*[\w\s]{2,30}:\s*\d+/.test(trimmed)) {
      const match = trimmed.replace(/^[\*\-\s]*/, '');
      const colonIndex = match.indexOf(':');
      if (colonIndex > -1) {
        const label = match.slice(0, colonIndex).trim();
        const value = match.slice(colonIndex + 1).trim();
        stats.push({ label, value });
      }
    }
  }
  return stats;
}

function StatGrid({ stats }: { stats: { label: string; value: string }[] }) {
  return (
    <div className="stat-grid">
      {stats.map((s) => (
        <div key={s.label} className="stat-card">
          <span className="stat-card__value">{s.value}</span>
          <span className="stat-card__label">{s.label}</span>
        </div>
      ))}
    </div>
  );
}

// ── Component ────────────────────────────────────────────────────────────

export const AgentMessage: React.FC<AgentMessageProps> = ({
  message,
  isStreaming = false,
  activeStreaming = false,
}) => {
  const timeStr = useMemo(
    () => formatRelativeTime(message.createdAt || message.timestamp || new Date().toISOString()),
    [message.createdAt, message.timestamp],
  );

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
  };

  // 1. Skip rendering raw tool result messages
  if (message.type === 'tool_result') {
    return null;
  }

  // 2. Skip rendering intermediate text messages containing tool result keywords
  if (
    message.content?.includes('✅ Tool result received.') ||
    message.content?.includes('🔧 Using tool:') ||
    message.content?.includes('🔧 Using: ')
  ) {
    return null;
  }

  // 3. Render Tool Call Chip
  if (message.type === 'tool_call') {
    const isCompleted = message.resolved || message.toolResult === 'completed' || !activeStreaming;
    return (
      <div className="agent-message">
        <ToolCallChip toolName={message.toolName || 'tool'} resolved={isCompleted} />
      </div>
    );
  }

  // 4. Skip empty text messages
  if (message.type === 'text' && !message.content?.trim() && !isStreaming) {
    return null;
  }

  // 5. User Message
  if (message.role === 'user') {
    return (
      <div className="agent-message" style={{ alignSelf: 'flex-end' }}>
        <div className="msg-user">
          {message.content}
        </div>
        <div className="msg-actions" style={{ justifyContent: 'flex-end' }}>
          <span className="msg-time">{timeStr}</span>
        </div>
      </div>
    );
  }

  // 6. Assistant Message
  const stats = isStatMessage(message.content) ? parseStats(message.content) : null;

  return (
    <div className="agent-message" style={{ alignSelf: 'flex-start' }}>
      <div className={`msg-assistant ${isStreaming ? 'msg-streaming' : ''}`}>
        {stats ? (
          <>
            <p style={{ marginBottom: 8, color: 'var(--agent-text-secondary)', fontSize: 13 }}>
              Here are the statistics:
            </p>
            <StatGrid stats={stats} />
          </>
        ) : (
          <ReactMarkdown remarkPlugins={[remarkGfm]}>
            {message.content}
          </ReactMarkdown>
        )}
        {isStreaming && <span className="streaming-cursor" />}
      </div>
      <div className="msg-actions" style={{ justifyContent: 'flex-start' }}>
        <span className="msg-time">{timeStr}</span>
        <button className="msg-copy" onClick={() => copyToClipboard(message.content)} title="Copy message">
          <Copy size={12} />
        </button>
      </div>
    </div>
  );
};
