import React from 'react';
import { Button } from 'antd';
import { AlertTriangle, Terminal } from 'lucide-react';
import type { ToolApproval } from '../../api/agentApi';

interface ActionCardProps {
  approval: ToolApproval;
  onApprove: (toolName: string, params: Record<string, any>) => void;
  onCancel: () => void;
}

export const ActionCard: React.FC<ActionCardProps> = ({
  approval,
  onApprove,
  onCancel,
}) => {
  return (
    <div className="agent-approval-card">
      <div className="agent-approval-card-header">
        <AlertTriangle size={16} />
        This action requires your approval
      </div>

      <div className="agent-tool-card-header">
        <Terminal size={14} />
        {approval.toolName}
      </div>

      {approval.description && (
        <p
          style={{
            fontSize: 13,
            color: 'var(--agent-text-muted)',
            margin: '4px 0 8px',
            lineHeight: 1.5,
          }}
        >
          {approval.description}
        </p>
      )}

      <div className="agent-tool-card-params">
        <pre style={{ margin: 0 }}>
          {JSON.stringify(approval.params, null, 2)}
        </pre>
      </div>

      <div className="agent-approval-card-actions">
        <Button
          type="primary"
          size="small"
          style={{
            background: 'var(--agent-success)',
            borderColor: 'var(--agent-success)',
          }}
          onClick={() => onApprove(approval.toolName, approval.params)}
        >
          Approve
        </Button>
        <Button size="small" onClick={onCancel}>
          Cancel
        </Button>
      </div>
    </div>
  );
};
