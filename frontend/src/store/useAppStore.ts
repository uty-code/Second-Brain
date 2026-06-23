import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export type RightPanelTab = "viewer" | "chat";

export type Message = {
  id: string;
  role: "user" | "assistant";
  content: string;
  isLoading?: boolean;
};

export interface GraphNode {
  id: string;
  name: string;
  type?: string;
  summary?: string;
  val: number;
}

export interface GraphLink {
  source: string;
  target: string;
  label?: string;
}

export interface GraphData {
  nodes: GraphNode[];
  links: GraphLink[];
}

interface AppState {
  // Workspaces
  workspaces: string[];
  setWorkspaces: (workspaces: string[]) => void;
  currentWorkspaceId: string;
  setCurrentWorkspaceId: (id: string) => void;

  // Selection
  selectedNodeId: string | null;
  setSelectedNodeId: (id: string | null) => void;

  // AI Gaze Tracking
  activeAiNodes: string[];
  addActiveAiNode: (id: string) => void;
  removeActiveAiNode: (id: string) => void;
  clearActiveAiNodes: () => void;

  // Layout
  isSidebarOpen: boolean;
  toggleSidebar: () => void;
  isRightPanelOpen: boolean;
  toggleRightPanel: () => void;
  rightPanelTab: RightPanelTab;
  setRightPanelTab: (tab: RightPanelTab) => void;

  // Graph data (persisted)
  graphData: GraphData | null;
  setGraphData: (data: GraphData | null) => void;
  clearGraphData: () => void;

  // Chat
  chatMessages: Message[];
  setChatMessages: (updater: Message[] | ((prev: Message[]) => Message[])) => void;

  // Global Loading State for Graph Generation
  isGraphLoading: boolean;
  setIsGraphLoading: (loading: boolean) => void;

  credentialsStatus: { hasNotionKey: boolean, hasGithubKey: boolean, hasDeepseekKey: boolean } | null;
  setCredentialsStatus: (status: { hasNotionKey: boolean, hasGithubKey: boolean, hasDeepseekKey: boolean } | null) => void;

  // Auth
  jwtToken: string | null;
  setJwtToken: (token: string | null) => void;
  isLoggedIn: boolean;
  setIsLoggedIn: (isLoggedIn: boolean) => void;
  currentUser: string | null;
  setCurrentUser: (user: string | null) => void;

  // AI Model Settings
  selectedModel: 'gpt-4o-mini' | 'deepseek-v4';
  setSelectedModel: (model: 'gpt-4o-mini' | 'deepseek-v4') => void;
}

export const useAppStore = create<AppState>()(
  persist(
    (set) => ({
      // Workspaces
      workspaces: [],
      setWorkspaces: (workspaces) => set({ workspaces }),
      currentWorkspaceId: "",
      setCurrentWorkspaceId: (id) => set({ currentWorkspaceId: id, graphData: null, selectedNodeId: null, chatMessages: [{ id: "1", role: "assistant", content: "무엇을 도와드릴까요? 지식창고를 기반으로 답변해 드립니다." }] }),

      // Selection
      selectedNodeId: null,
      setSelectedNodeId: (id) => set({ selectedNodeId: id, rightPanelTab: "viewer", isRightPanelOpen: true }),

      // AI Gaze Tracking
      activeAiNodes: [],
      addActiveAiNode: (id) => set((state) => ({ 
        activeAiNodes: state.activeAiNodes.includes(id) ? state.activeAiNodes : [...state.activeAiNodes, id] 
      })),
      removeActiveAiNode: (id) => set((state) => ({ 
        activeAiNodes: state.activeAiNodes.filter(n => n !== id) 
      })),
      clearActiveAiNodes: () => set({ activeAiNodes: [] }),

      // Layout
      isSidebarOpen: true,
      toggleSidebar: () => set((state) => ({ isSidebarOpen: !state.isSidebarOpen })),
      isRightPanelOpen: true,
      toggleRightPanel: () => set((state) => ({ isRightPanelOpen: !state.isRightPanelOpen })),
      rightPanelTab: "chat",
      setRightPanelTab: (tab) => set({ rightPanelTab: tab }),

      // Graph
      graphData: null,
      setGraphData: (data) => set({ graphData: data }),
      clearGraphData: () => set({ graphData: null, selectedNodeId: null }),

      // Chat
      chatMessages: [{ id: "1", role: "assistant", content: "무엇을 도와드릴까요? 지식창고를 기반으로 답변해 드립니다." }],
      setChatMessages: (updater) => set((state) => ({
        chatMessages: typeof updater === "function" ? updater(state.chatMessages) : updater
      })),

      // Global Loading State for Graph Generation
      isGraphLoading: false,
      setIsGraphLoading: (loading) => set({ isGraphLoading: loading }),

      credentialsStatus: null,
      setCredentialsStatus: (status) => set({ credentialsStatus: status }),

      // Auth
      jwtToken: null,
      setJwtToken: (token) => set({ jwtToken: token }),
      isLoggedIn: false,
      setIsLoggedIn: (isLoggedIn) => set({ isLoggedIn }),
      currentUser: null,
      setCurrentUser: (user) => set({ currentUser: user }),

      // AI Model Settings
      selectedModel: 'gpt-4o-mini',
      setSelectedModel: (model) => set({ selectedModel: model }),
    }),
    {
      name: 'aims-graph-storage',
      partialize: (state) => ({ 
        graphData: state.graphData, 
        chatMessages: state.chatMessages, 
        currentWorkspaceId: state.currentWorkspaceId,
        selectedModel: state.selectedModel,
        jwtToken: state.jwtToken,
        isLoggedIn: state.isLoggedIn,
        currentUser: state.currentUser
      }), // Persist graphData, chatMessages, workspace, AI settings, and auth
    }
  )
);
