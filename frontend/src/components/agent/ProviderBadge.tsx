import React from 'react';
import { useSelector } from 'react-redux';
import { RootState } from '../../store';
import { Zap } from 'lucide-react';

export const ProviderBadge: React.FC = () => {
  const provider = useSelector((state: RootState) => state.agent.provider);

  return (
    <span className="agent-provider-badge">
      <Zap size={12} />
      {provider}
    </span>
  );
};
