"use client";

import { useState, useEffect } from "react";
import { Sidebar } from "@/components/layout/Sidebar";
import { RightPanel } from "@/components/layout/RightPanel";
import { GraphCanvas } from "@/components/graph/GraphCanvas";
import { useAppStore } from "@/store/useAppStore";
import { analyzeFiles, fetchWorkspaceGraph } from "@/services/api";

export default function Home() {
  const { setSelectedNodeId, graphData, setGraphData, currentWorkspaceId } = useAppStore();
  const [isUploading, setIsUploading] = useState(false);

  useEffect(() => {
    if (currentWorkspaceId) {
      fetchWorkspaceGraph(currentWorkspaceId).then((result) => {
        setGraphData(result);
      });
    }
  }, [currentWorkspaceId, setGraphData]);

  const handleFileDrop = async (files: File[]) => {
    if (files.length === 0) return;
    setIsUploading(true);
    try {
      const result = await analyzeFiles(files, currentWorkspaceId);
      setGraphData(result);
    } catch (error) {
      console.error("Analysis failed", error);
      alert("파일 분석에 실패했습니다: " + (error as Error).message);
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <main className="flex h-screen w-full bg-zinc-900">
      <Sidebar />
      <section className="flex-1 relative flex items-center justify-center bg-zinc-900 overflow-hidden">
        <GraphCanvas 
          data={graphData} 
          onNodeClick={(node) => setSelectedNodeId(node.id as string)} 
          onFileDrop={handleFileDrop} 
          isUploading={isUploading}
        />
      </section>
      <RightPanel />
    </main>
  );
}
