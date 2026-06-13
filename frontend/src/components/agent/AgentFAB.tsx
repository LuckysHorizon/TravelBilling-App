import React, { useEffect, useRef, useState, useCallback, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { togglePanel } from '../../store/slices/agentSlice';
import type { RootState } from '../../store';
import { Sparkles } from 'lucide-react';
import { AgentPanel } from './AgentPanel';

const FAB_STORAGE_KEY = 'agent-fab-position-v2';
const DEFAULT_POSITION = { right: 28, bottom: 28 };

function loadPosition(): { right: number; bottom: number } {
  try {
    const saved = localStorage.getItem(FAB_STORAGE_KEY);
    if (saved) {
      const parsed = JSON.parse(saved);
      if (typeof parsed.right === 'number' && typeof parsed.bottom === 'number') {
        return parsed;
      }
    }
  } catch {
    /* ignore */
  }
  // Try to migrate from the old key
  try {
    const oldSaved = localStorage.getItem('agent-fab-position');
    if (oldSaved) {
      const parsed = JSON.parse(oldSaved);
      if (typeof parsed.x === 'number' && typeof parsed.y === 'number' && parsed.x >= 0 && parsed.y >= 0) {
        const right = Math.max(28, window.innerWidth - parsed.x - 54);
        const bottom = Math.max(28, window.innerHeight - parsed.y - 54);
        return { right, bottom };
      }
    }
  } catch {
    /* ignore */
  }
  return DEFAULT_POSITION;
}

function savePosition(pos: { right: number; bottom: number }) {
  try {
    localStorage.setItem(FAB_STORAGE_KEY, JSON.stringify(pos));
  } catch {
    /* ignore */
  }
}

export function AgentFAB() {
  const dispatch = useDispatch();
  const panelOpen = useSelector((s: RootState) => s.agent.panelOpen);
  const isStreaming = useSelector((s: RootState) => s.agent.isStreaming);
  const unreadCount = useSelector((s: RootState) => s.agent.unreadCount);

  const [position, setPosition] = useState(DEFAULT_POSITION);
  const [isDragging, setIsDragging] = useState(false);

  const dragRef = useRef<{
    startX: number;
    startY: number;
    startRight: number;
    startBottom: number;
    moved: boolean;
  } | null>(null);

  const fabRootRef = useRef<HTMLDivElement>(null);
  const btnRef = useRef<HTMLButtonElement>(null);
  const positionRef = useRef(position);

  // Sync positionRef
  useEffect(() => {
    positionRef.current = position;
  }, [position]);

  const clampPosition = useCallback((pos: { right: number; bottom: number }) => {
    const viewWidth = document.documentElement.clientWidth || window.innerWidth;
    const viewHeight = document.documentElement.clientHeight || window.innerHeight;
    const maxRight = Math.max(0, viewWidth - 54);
    const maxBottom = Math.max(0, viewHeight - 54);
    return {
      right: Math.max(0, Math.min(maxRight, pos.right)),
      bottom: Math.max(0, Math.min(maxBottom, pos.bottom)),
    };
  }, []);

  // Load and validate/clamp position on mount
  useEffect(() => {
    const pos = loadPosition();
    setPosition(clampPosition(pos));
  }, [clampPosition]);

  // Listen to window resize to keep the button inside the viewport
  useEffect(() => {
    const handleResize = () => {
      setPosition((curr) => {
        const clamped = clampPosition(curr);
        if (clamped.right !== curr.right || clamped.bottom !== curr.bottom) {
          savePosition(clamped);
          return clamped;
        }
        return curr;
      });
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [clampPosition]);

  // ⌘K / Ctrl+K shortcut
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        dispatch(togglePanel());
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [dispatch]);

  // Drag handlers
  const handlePointerDown = useCallback((e: React.PointerEvent) => {
    if (!fabRootRef.current) return;
    const rect = fabRootRef.current.getBoundingClientRect();
    
    // We calculate offsets relative to the bottom-right corner of the viewport
    const viewWidth = document.documentElement.clientWidth || window.innerWidth;
    const viewHeight = document.documentElement.clientHeight || window.innerHeight;
    const rightOffset = viewWidth - rect.right;
    const bottomOffset = viewHeight - rect.bottom;

    dragRef.current = {
      startX: e.clientX,
      startY: e.clientY,
      startRight: rightOffset,
      startBottom: bottomOffset,
      moved: false,
    };
    fabRootRef.current.setPointerCapture(e.pointerId);
    setIsDragging(true);
  }, []);

  const handlePointerMove = useCallback((e: React.PointerEvent) => {
    if (!dragRef.current) return;
    const dx = e.clientX - dragRef.current.startX;
    const dy = e.clientY - dragRef.current.startY;

    if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
      dragRef.current.moved = true;
    }

    if (dragRef.current.moved) {
      const newRight = dragRef.current.startRight - dx;
      const newBottom = dragRef.current.startBottom - dy;
      setPosition(clampPosition({ right: newRight, bottom: newBottom }));
    }
  }, [clampPosition]);

  const handlePointerUp = useCallback(() => {
    if (!dragRef.current) return;
    if (!dragRef.current.moved) {
      dispatch(togglePanel());
    } else {
      savePosition(positionRef.current);
    }
    dragRef.current = null;
    setIsDragging(false);
  }, [dispatch]);

  const fabStyle: React.CSSProperties = useMemo(() => {
    return {
      right: `${position.right}px`,
      bottom: `${position.bottom}px`,
      left: 'auto',
      top: 'auto',
      cursor: isDragging ? 'grabbing' : 'grab',
      touchAction: 'none',
    };
  }, [position, isDragging]);

  return (
    <>
      <div
        ref={fabRootRef}
        className={[
          'agent-fab-root',
          isStreaming ? 'agent-fab-root--streaming' : '',
          panelOpen  ? 'agent-fab-root--open' : '',
        ].join(' ')}
        style={fabStyle}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        aria-label="Open TravelBill AI"
      >
        {/* The button itself */}
        <button
          ref={btnRef}
          className="fab-btn"
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault();
              dispatch(togglePanel());
            }
          }}
          aria-label={panelOpen ? 'Close TravelBill AI' : 'Open TravelBill AI'}
        >
          {/* Inner recess layer */}
          <span className="fab-inner">
            <Sparkles size={20} strokeWidth={1.8} className="fab-icon" />
          </span>
        </button>

        {/* Unread badge */}
        {unreadCount > 0 && !panelOpen && (
          <span className="fab-badge">{unreadCount > 9 ? '9+' : unreadCount}</span>
        )}
      </div>
      <AgentPanel />
    </>
  );
}
