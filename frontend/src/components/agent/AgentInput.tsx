import React, { useState, useRef } from 'react';
import { Send, StopCircle, BarChart2, Ticket, FileText, Building2 } from 'lucide-react';

const SUGGESTIONS = [
  { icon: <BarChart2 size={13} />, label: 'Dashboard stats', prompt: "Show me today's dashboard stats" },
  { icon: <Ticket size={13} />,    label: 'Pending tickets', prompt: 'List all pending tickets' },
  { icon: <FileText size={13} />,  label: 'Recent invoices', prompt: 'Show my recent invoices' },
  { icon: <Building2 size={13} />, label: 'All companies',   prompt: 'List all companies' },
];

interface SuggestionChipsProps {
  onSelect: (v: string) => void;
}

function SuggestionChips({ onSelect }: SuggestionChipsProps) {
  return (
    <div className="suggestion-chips">
      {SUGGESTIONS.map((s) => (
        <button
          key={s.label}
          className="suggestion-chip"
          onClick={() => onSelect(s.prompt)}
          type="button"
        >
          <span className="suggestion-chip__icon" style={{ display: 'inline-flex', marginRight: '6px', opacity: 0.8, verticalAlign: 'middle' }}>
            {s.icon}
          </span>
          {s.label}
        </button>
      ))}
    </div>
  );
}

interface AgentInputProps {
  onSend: (message: string) => void;
  onStop?: () => void;
  isStreaming: boolean;
  disabled?: boolean;
}

export const AgentInput: React.FC<AgentInputProps> = ({
  onSend,
  onStop,
  isStreaming,
  disabled,
}) => {
  const [inputValue, setInputValue] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const handleSend = () => {
    const trimmed = inputValue.trim();
    if (!trimmed || isStreaming || disabled) return;
    onSend(trimmed);
    setInputValue('');
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
  };

  const handleStop = () => {
    if (onStop) {
      onStop();
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setInputValue(e.target.value);
    e.target.style.height = 'auto';
    e.target.style.height = Math.min(e.target.scrollHeight, 200) + 'px';
  };

  const handleSelectSuggestion = (prompt: string) => {
    setInputValue(prompt);
    if (textareaRef.current) {
      textareaRef.current.focus();
      // Schedule height recalculation after DOM update
      setTimeout(() => {
        if (textareaRef.current) {
          textareaRef.current.style.height = 'auto';
          textareaRef.current.style.height = Math.min(textareaRef.current.scrollHeight, 200) + 'px';
        }
      }, 0);
    }
  };

  return (
    <div className="agent-input-wrap">
      {/* Suggestion chips — show when input is empty */}
      {!inputValue.trim() && <SuggestionChips onSelect={handleSelectSuggestion} />}

      <div className="agent-input-box">
        <textarea
          ref={textareaRef}
          className="agent-input"
          placeholder={isStreaming ? 'Thinking...' : 'Ask anything about your tickets, invoices, or companies...'}
          value={inputValue}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          rows={1}
          disabled={disabled}
        />
        <button
          className={`agent-send ${isStreaming ? 'agent-send--stop' : ''}`}
          onClick={isStreaming ? handleStop : handleSend}
          disabled={!inputValue.trim() && !isStreaming}
          type="button"
          title={isStreaming ? 'Stop generation' : 'Send message'}
        >
          {isStreaming ? <StopCircle size={18} /> : <Send size={18} />}
        </button>
      </div>

      <p className="agent-input-hint">
        Enter to send · Shift+Enter for new line
      </p>
    </div>
  );
};
