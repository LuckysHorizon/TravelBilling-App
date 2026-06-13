import React, { useEffect, useRef, useState, useCallback } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { RootState } from '../../store';
import { togglePanel } from '../../store/slices/agentSlice';
import { AgentPanel } from './AgentPanel';

// ── Position persistence ─────────────────────────────────────────────────

const FAB_STORAGE_KEY = 'agent-fab-position';
const DEFAULT_POSITION = { x: -1, y: -1 }; // -1 = use CSS default (bottom-right)

function loadPosition(): { x: number; y: number } {
  try {
    const saved = localStorage.getItem(FAB_STORAGE_KEY);
    if (saved) return JSON.parse(saved);
  } catch {
    /* ignore */
  }
  return DEFAULT_POSITION;
}

function savePosition(pos: { x: number; y: number }) {
  try {
    localStorage.setItem(FAB_STORAGE_KEY, JSON.stringify(pos));
  } catch {
    /* ignore */
  }
}

// ── Component ────────────────────────────────────────────────────────────

export const AgentFAB: React.FC = () => {
  const dispatch = useDispatch();
  const { panelOpen, isStreaming, unreadCount } = useSelector(
    (state: RootState) => state.agent,
  );

  const [position, setPosition] = useState(loadPosition);
  const [isDragging, setIsDragging] = useState(false);

  const dragRef = useRef<{
    startX: number;
    startY: number;
    startPosX: number;
    startPosY: number;
    moved: boolean;
  } | null>(null);
  const fabRef = useRef<HTMLButtonElement>(null);
  const positionRef = useRef(position);

  // Keep positionRef in sync
  useEffect(() => {
    positionRef.current = position;
  }, [position]);

  // ── Keyboard shortcut: Ctrl+Shift+A ────────────────────────────────

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.ctrlKey && e.shiftKey && e.key === 'A') {
        e.preventDefault();
        dispatch(togglePanel());
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [dispatch]);

  // ── Drag handlers ──────────────────────────────────────────────────

  const handlePointerDown = useCallback((e: React.PointerEvent) => {
    if (!fabRef.current) return;
    const rect = fabRef.current.getBoundingClientRect();
    dragRef.current = {
      startX: e.clientX,
      startY: e.clientY,
      startPosX: rect.left,
      startPosY: rect.top,
      moved: false,
    };
    fabRef.current.setPointerCapture(e.pointerId);
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
      const newX = dragRef.current.startPosX + dx;
      const newY = dragRef.current.startPosY + dy;
      const clampedX = Math.max(0, Math.min(window.innerWidth - 56, newX));
      const clampedY = Math.max(0, Math.min(window.innerHeight - 56, newY));
      setPosition({ x: clampedX, y: clampedY });
    }
  }, []);

  const handlePointerUp = useCallback(() => {
    if (!dragRef.current) return;
    if (!dragRef.current.moved) {
      // Click — toggle panel
      dispatch(togglePanel());
    } else {
      // Drag ended — save position
      savePosition(positionRef.current);
    }
    dragRef.current = null;
    setIsDragging(false);
  }, [dispatch]);

  // ── Visual state ───────────────────────────────────────────────────

  const stateClass = isStreaming ? 'agent-fab--thinking' : 'agent-fab--idle';

  const fabStyle: React.CSSProperties =
    position.x >= 0
      ? {
          left: position.x,
          top: position.y,
          right: 'auto',
          bottom: 'auto',
          cursor: isDragging ? 'grabbing' : 'grab',
          touchAction: 'none',
        }
      : {
          cursor: isDragging ? 'grabbing' : 'grab',
          touchAction: 'none',
        };

  return (
    <>
      <button
        ref={fabRef}
        className={`agent-trigger ${unreadCount > 0 ? 'agent-trigger--notify' : ''}`}
        style={fabStyle}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        title="Open TravelBill AI (Ctrl+Shift+A)"
        aria-label="AI Assistant"
      >
        <span
          className="agent-trigger__icon"
          style={{
            animation: isStreaming ? 'spin 2s linear infinite' : 'none',
            display: 'inline-block',
          }}
        >
          ✦
        </span>
        {unreadCount > 0 && (
          <span className="agent-trigger__badge">{unreadCount}</span>
        )}
      </button>
      <AgentPanel />
    </>
  );
};
