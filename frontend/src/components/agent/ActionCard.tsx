import React from 'react';
import { AlertTriangle } from 'lucide-react';
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
    <div className="action-card">
      <div className="action-card__header">
        <span className="action-card__icon" style={{ display: 'inline-flex', alignItems: 'center' }}>
          <AlertTriangle size={16} />
        </span>
        <div>
          <p className="action-card__title">Approval Required</p>
          <p className="action-card__subtitle">
            {approval.description || 'The agent wants to perform an action'}
          </p>
        </div>
      </div>

      <div className="action-card__body">
        <div className="action-card__tool">
          <code>{approval.toolName}</code>
        </div>
        {approval.params && (
          <pre className="action-card__params">
            {JSON.stringify(approval.params, null, 2)}
          </pre>
        )}
      </div>

      <div className="action-card__actions">
        <button className="action-btn action-btn--deny" onClick={onCancel}>
          Deny
        </button>
        <button
          className="action-btn action-btn--approve"
          onClick={() => onApprove(approval.toolName, approval.params)}
        >
          Approve & Run
        </button>
      </div>
    </div>
  );
};
