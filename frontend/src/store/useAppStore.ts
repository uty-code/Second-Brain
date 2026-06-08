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
}

export const useAppStore = create<AppState>()(
  persist(
    (set) => ({
      // Workspaces
      workspaces: ["default-workspace"],
      setWorkspaces: (workspaces) => set({ workspaces }),
      currentWorkspaceId: "default-workspace",
      setCurrentWorkspaceId: (id) => set({ currentWorkspaceId: id, graphData: null, selectedNodeId: null, chatMessages: [{ id: "1", role: "assistant", content: "무엇을 도와드릴까요? 지식창고를 기반으로 답변해 드립니다." }] }),

      // Selection
      selectedNodeId: null,
      setSelectedNodeId: (id) => set({ selectedNodeId: id, rightPanelTab: "viewer", isRightPanelOpen: true }),

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
    }),
    {
      name: 'aims-graph-storage',
      partialize: (state) => ({ graphData: state.graphData, chatMessages: state.chatMessages, currentWorkspaceId: state.currentWorkspaceId }), // Persist graphData, chatMessages and currentWorkspaceId
    }
  )
);
