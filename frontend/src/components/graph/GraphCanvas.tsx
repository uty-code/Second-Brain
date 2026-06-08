"use client";

import React, { useRef, useState, useCallback, useMemo, useEffect } from "react";
import dynamic from "next/dynamic";
import { GraphData, GraphNode, GraphLink } from "@/types/graph";
import { EmptyState } from "./EmptyState";

// Dynamically import react-force-graph-2d with SSR disabled
const ForceGraph2D = dynamic(() => import("react-force-graph-2d"), {
  ssr: false,
});

// ForceGraph instance type placeholder
type ForceGraphInstance = {
  centerAt: (x: number, y: number, durationMs: number) => void;
  zoom: (scale: number, durationMs: number) => void;
  d3ReheatSimulation: () => void;
};

interface GraphCanvasProps {
  data: GraphData | null;
  onNodeClick?: (node: GraphNode) => void;
  onFileDrop?: (files: File[]) => void;
  isUploading?: boolean;
}

export function GraphCanvas({ data, onNodeClick, onFileDrop, isUploading }: GraphCanvasProps) {
  const fgRef = useRef<ForceGraphInstance>(null);
  const [hoverNode, setHoverNode] = useState<GraphNode | null>(null);
  const [windowSize, setWindowSize] = useState({ width: 0, height: 0 });

  useEffect(() => {
    const handleResize = () => {
      const container = document.getElementById("graph-container");
      if (container) {
        setWindowSize({
          width: container.clientWidth,
          height: container.clientHeight,
        });
      }
    };
    window.addEventListener("resize", handleResize);
    setTimeout(handleResize, 100);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  const handleNodeHover = useCallback((node: GraphNode | null) => {
    setHoverNode(node || null);
    if (document.body) {
      document.body.style.cursor = node ? "pointer" : "default";
    }
  }, []);

  const handleNodeClick = useCallback(
    (node: GraphNode) => {
      if (onNodeClick) {
        onNodeClick(node);
      }
      if (fgRef.current && node.x !== undefined && node.y !== undefined) {
        fgRef.current.centerAt(node.x, node.y, 1000);
        fgRef.current.zoom(8, 2000);
      }
    },
    [onNodeClick]
  );

  const links = useMemo(() => {
    if (!data) return [];
    const nodeIds = new Set(data.nodes.map((n) => n.id));
    return data.links
      .map((link) => ({
        ...link,
        // d3-force가 객체로 변환한 source/target을 문자열 ID로 정규화
        source: typeof link.source === "object" ? (link.source as any).id : link.source,
        target: typeof link.target === "object" ? (link.target as any).id : link.target,
      }))
      .filter((link) => nodeIds.has(link.source) && nodeIds.has(link.target));
  }, [data]);

  const paintNode = useCallback(
    (node: GraphNode, ctx: CanvasRenderingContext2D, globalScale: number) => {
      if (node.x === undefined || node.y === undefined) return;
      
      const isHovered = hoverNode?.id === node.id;
      const isConnected =
        hoverNode &&
        links.some((link) => {
          const sourceId = typeof link.source === "object" ? link.source.id : link.source;
          const targetId = typeof link.target === "object" ? link.target.id : link.target;
          return (
            (sourceId === hoverNode.id && targetId === node.id) ||
            (targetId === hoverNode.id && sourceId === node.id)
          );
        });
      
      const isFocused = !hoverNode || isHovered || isConnected;
      const opacity = isFocused ? 1 : 0.2;

      const size = node.val ? Math.min(8, Math.max(4, node.val)) : 4;

      ctx.beginPath();
      ctx.arc(node.x, node.y, size, 0, 2 * Math.PI, false);
      ctx.fillStyle = `rgba(228, 228, 231, ${opacity})`; // #E4E4E7 (zinc-200)
      ctx.fill();

      if (globalScale >= 2 || isFocused) {
        const fontSize = 10 / globalScale;
        ctx.font = `${fontSize}px Inter, sans-serif`;
        ctx.textAlign = "center";
        ctx.textBaseline = "middle";
        
        ctx.fillStyle = isHovered 
          ? `rgba(255, 255, 255, ${opacity})` // #FFFFFF on hover
          : `rgba(161, 161, 170, ${opacity})`; // #A1A1AA (zinc-400)
          
        ctx.fillText(node.name || node.id, node.x, node.y + size + fontSize + 2);
      }
    },
    [hoverNode, links]
  );

  const linkColor = useCallback(
    (link: GraphLink) => {
      const sourceId = typeof link.source === "object" ? link.source.id : link.source;
      const targetId = typeof link.target === "object" ? link.target.id : link.target;
      
      const isConnectedToHover =
        hoverNode && (sourceId === hoverNode.id || targetId === hoverNode.id);
      
      const isFocused = !hoverNode || isConnectedToHover;
      const opacity = isFocused ? 1 : 0.2;
      
      return `rgba(82, 82, 91, ${opacity})`; // #52525B (zinc-600)
    },
    [hoverNode]
  );

  const sanitizedData = useMemo(() => {
    if (!data) return null;
    return { nodes: data.nodes, links };
  }, [data, links]);

  if (!data || data.nodes.length === 0) {
    return (
      <div id="graph-container" className="w-full h-full bg-zinc-900 overflow-hidden">
        <EmptyState onSubmit={onFileDrop || (() => {})} isUploading={isUploading} />
      </div>
    );
  }

  return (
    <div id="graph-container" className="w-full h-full bg-zinc-900 overflow-hidden relative">
      <ForceGraph2D
        ref={fgRef}
        width={windowSize.width || 800}
        height={windowSize.height || 600}
        graphData={sanitizedData!}
        backgroundColor="#18181B"
        nodeCanvasObject={paintNode}
        nodeRelSize={4}
        linkColor={linkColor}
        linkWidth={1}
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        onNodeHover={handleNodeHover as any}
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        onNodeClick={handleNodeClick as any}
      />
    </div>
  );
}
