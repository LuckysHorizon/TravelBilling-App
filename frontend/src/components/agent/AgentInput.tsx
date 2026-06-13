import React, { useState, useRef } from 'react';
import { Input } from 'antd';
import { Send, Loader2 } from 'lucide-react';

const { TextArea } = Input;

interface AgentInputProps {
  onSend: (message: string) => void;
  isStreaming: boolean;
  disabled?: boolean;
}

export const AgentInput: React.FC<AgentInputProps> = ({
  onSend,
  isStreaming,
  disabled,
}) => {
  const [value, setValue] = useState('');
  const textAreaRef = useRef<any>(null);

  const handleSend = () => {
    const trimmed = value.trim();
    if (!trimmed || isStreaming || disabled) return;
    onSend(trimmed);
    setValue('');
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="agent-input-wrapper">
      <div style={{ flex: 1 }}>
        <TextArea
          ref={textAreaRef}
          value={value}
          onChange={(e) => setValue(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={isStreaming ? 'Thinking...' : 'Ask TravelBill AI...'}
          disabled={isStreaming || disabled}
          autoSize={{ minRows: 1, maxRows: 4 }}
          style={{ width: '100%' }}
        />
        {value.length > 500 && (
          <div className="agent-char-count">{value.length} / 2000</div>
        )}
      </div>
      <button
        className="agent-send-btn"
        onClick={handleSend}
        disabled={!value.trim() || isStreaming || disabled}
        title="Send message"
      >
        {isStreaming ? (
          <Loader2 size={18} style={{ animation: 'agent-spin 1s linear infinite' }} />
        ) : (
          <Send size={18} />
        )}
      </button>
    </div>
  );
};
