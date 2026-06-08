"use client";

import { useEffect, useState } from "react";
import { useAppStore } from "@/store/useAppStore";
import { MarkdownViewer } from "@/components/viewer/MarkdownViewer";
import { fetchWikiPageContent } from "@/services/api";
import { Skeleton } from "@/components/ui/Skeleton";
import { ChatPanel } from "@/components/chat/ChatPanel";

export function RightPanel() {
  const { selectedNodeId, setSelectedNodeId, isRightPanelOpen, rightPanelTab, setRightPanelTab, currentWorkspaceId } = useAppStore();
  const [loading, setLoading] = useState(false);
  const [content, setContent] = useState<string>("");

  useEffect(() => {
    let isMounted = true;
    if (selectedNodeId) {
      setLoading(true);
      fetchWikiPageContent(selectedNodeId, currentWorkspaceId)
        .then((res) => {
          if (isMounted) {
            setContent(res.content);
            setLoading(false);
          }
        })
        .catch((err) => {
          if (isMounted) {
            setContent(`## 에러\n\n내용을 불러오지 못했습니다: ${err.message}`);
            setLoading(false);
          }
        });
    } else {
      setContent("");
    }
    return () => {
      isMounted = false;
    };
  }, [selectedNodeId, currentWorkspaceId]);

  if (!isRightPanelOpen) return null;

  return (
    <aside className="w-80 h-full border-l border-zinc-800 flex flex-col bg-zinc-900 shrink-0">
      <div className="flex border-b border-zinc-800">
        <button
          onClick={() => setRightPanelTab("chat")}
          className={`flex-1 py-3 text-sm font-semibold transition-colors ${
            rightPanelTab === "chat" ? "text-zinc-100 border-b-2 border-zinc-500" : "text-zinc-500 hover:text-zinc-300"
          }`}
        >
          Chat
        </button>
        <button
          onClick={() => setRightPanelTab("viewer")}
          className={`flex-1 py-3 text-sm font-semibold transition-colors ${
            rightPanelTab === "viewer" ? "text-zinc-100 border-b-2 border-zinc-500" : "text-zinc-500 hover:text-zinc-300"
          }`}
        >
          Viewer
        </button>
      </div>
      <div className="flex-1 overflow-y-auto">
        {rightPanelTab === "chat" ? (
          <ChatPanel />
        ) : (
          <div className="p-4 h-full">
            {!selectedNodeId ? (
              <div className="h-full flex flex-col items-center justify-center text-zinc-500 text-sm">
                <p>Select a node to view</p>
              </div>
            ) : loading ? (
              <div className="space-y-4">
                <Skeleton className="h-6 w-3/4" />
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-5/6" />
                <Skeleton className="h-4 w-full" />
                <div className="pt-4">
                  <Skeleton className="h-32 w-full" />
                </div>
              </div>
            ) : (
              <MarkdownViewer content={content} onLinkClick={(target) => setSelectedNodeId(target)} />
            )}
          </div>
        )}
      </div>
    </aside>
  );
}
