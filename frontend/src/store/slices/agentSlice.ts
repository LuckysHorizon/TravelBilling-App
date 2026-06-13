import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import type { AgentSession, AgentMessage, ToolApproval } from '../../api/agentApi';

// ── State ────────────────────────────────────────────────────────────────────

interface AgentState {
  panelOpen: boolean;
  currentSessionId: string | null;
  sessions: AgentSession[];
  messages: AgentMessage[];
  isStreaming: boolean;
  streamingContent: string;
  pendingApproval: ToolApproval | null;
  rateLimited: boolean;
  rateLimitRetryAfter: number;
  error: string | null;
  provider: string;
  unreadCount: number;
}

const initialState: AgentState = {
  panelOpen: false,
  currentSessionId: null,
  sessions: [],
  messages: [],
  isStreaming: false,
  streamingContent: '',
  pendingApproval: null,
  rateLimited: false,
  rateLimitRetryAfter: 0,
  error: null,
  provider: 'Groq',
  unreadCount: 0,
};

// ── Slice ────────────────────────────────────────────────────────────────────

const agentSlice = createSlice({
  name: 'agent',
  initialState,
  reducers: {
    togglePanel(state) {
      state.panelOpen = !state.panelOpen;
      if (state.panelOpen) state.unreadCount = 0;
    },
    openPanel(state) {
      state.panelOpen = true;
      state.unreadCount = 0;
    },
    closePanel(state) {
      state.panelOpen = false;
    },
    setSession(state, action: PayloadAction<string | null>) {
      const newId = action.payload;
      // Only clear messages when actually switching sessions, not when confirming current
      if (newId !== state.currentSessionId) {
        if (newId === null || (state.currentSessionId !== null && newId !== state.currentSessionId)) {
          state.messages = [];
          state.streamingContent = '';
        }
      }
      state.currentSessionId = newId;
    },
    setSessions(state, action: PayloadAction<AgentSession[]>) {
      state.sessions = action.payload;
    },
    setMessages(state, action: PayloadAction<AgentMessage[]>) {
      state.messages = action.payload;
    },
    addMessage(state, action: PayloadAction<AgentMessage>) {
      state.messages.push(action.payload);
      if (!state.panelOpen && action.payload.role === 'assistant') {
        state.unreadCount += 1;
      }
    },
    setStreaming(state, action: PayloadAction<boolean>) {
      state.isStreaming = action.payload;
    },
    appendStreamContent(state, action: PayloadAction<string>) {
      state.streamingContent += action.payload;
    },
    clearStream(state) {
      state.streamingContent = '';
    },
    setPendingApproval(state, action: PayloadAction<ToolApproval>) {
      state.pendingApproval = action.payload;
    },
    clearApproval(state) {
      state.pendingApproval = null;
    },
    setRateLimited(state, action: PayloadAction<{ limited: boolean; retryAfter: number }>) {
      state.rateLimited = action.payload.limited;
      state.rateLimitRetryAfter = action.payload.retryAfter;
    },
    clearRateLimited(state) {
      state.rateLimited = false;
      state.rateLimitRetryAfter = 0;
    },
    setError(state, action: PayloadAction<string>) {
      state.error = action.payload;
    },
    clearError(state) {
      state.error = null;
    },
    setProvider(state, action: PayloadAction<string>) {
      state.provider = action.payload;
    },
    resetUnread(state) {
      state.unreadCount = 0;
    },
  },
});

export const {
  togglePanel,
  openPanel,
  closePanel,
  setSession,
  setSessions,
  setMessages,
  addMessage,
  setStreaming,
  appendStreamContent,
  clearStream,
  setPendingApproval,
  clearApproval,
  setRateLimited,
  clearRateLimited,
  setError,
  clearError,
  setProvider,
  resetUnread,
} = agentSlice.actions;

export default agentSlice.reducer;
