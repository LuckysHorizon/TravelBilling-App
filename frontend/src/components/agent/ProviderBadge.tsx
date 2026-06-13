import React from 'react';
import { useSelector } from 'react-redux';
import { RootState } from '../../store';

export const ProviderBadge: React.FC = () => {
  const provider = useSelector((state: RootState) => state.agent.provider);

  return (
    <span className="provider-badge">
      <span className="provider-badge__dot" />
      {provider}
    </span>
  );
};
