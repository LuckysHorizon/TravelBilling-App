import React, { useEffect, useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { Zap } from 'lucide-react';
import { RootState } from '../../store';
import { clearRateLimited } from '../../store/slices/agentSlice';

export const RateLimitNotice: React.FC = () => {
  const dispatch = useDispatch();
  const { rateLimited, rateLimitRetryAfter } = useSelector(
    (state: RootState) => state.agent,
  );
  const [countdown, setCountdown] = useState(0);

  useEffect(() => {
    if (rateLimited) {
      setCountdown(rateLimitRetryAfter);
    }
  }, [rateLimited, rateLimitRetryAfter]);

  useEffect(() => {
    if (countdown <= 0) {
      if (rateLimited) dispatch(clearRateLimited());
      return;
    }
    const timer = setInterval(() => setCountdown((c) => c - 1), 1000);
    return () => clearInterval(timer);
  }, [countdown, rateLimited, dispatch]);

  if (!rateLimited || countdown <= 0) return null;

  return (
    <div className="rate-limit-notice">
      <Zap size={14} style={{ flexShrink: 0 }} />
      <span>Rate limited. Please wait {countdown}s before sending another message.</span>
    </div>
  );
};
