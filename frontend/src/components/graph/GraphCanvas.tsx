"use client";

import React, { useRef, useState, useCallback, useMemo, useEffect } from "react";
import dynamic from "next/dynamic";
import { GraphData, GraphNode, GraphLink } from "@/types/graph";
import { EmptyState } from "./EmptyState";
import { UploadCloud, Loader2, Server, CheckCircle2, X, Settings2, GitPullRequest, HardDrive, ChevronDown, ChevronUp, BrainCircuit } from "lucide-react";
import { useAppStore } from "@/store/useAppStore";
import { fetchCredentialsStatus, updateCredentials } from "@/services/api";

const NotionIcon = ({ className }: { className?: string }) => (
  <svg 
    className={className}
    xmlns="http://www.w3.org/2000/svg" 
    viewBox="0 0 640 640"
    fill="currentColor"
  >
    <path d="M158.9 164.2C173.8 176.3 179.4 175.4 207.5 173.5L471.8 157.6C477.4 157.6 472.7 152 470.9 151.1L426.9 119.4C418.5 112.9 407.3 105.4 385.8 107.3L129.9 125.9C120.6 126.8 118.7 131.5 122.4 135.2L158.8 164.1zM174.8 225.8L174.8 503.9C174.8 518.8 182.3 524.4 199.1 523.5L489.6 506.7C506.4 505.8 508.3 495.5 508.3 483.4L508.3 207.2C508.3 195.1 503.6 188.5 493.3 189.5L189.7 207.1C178.5 208 174.8 213.6 174.8 225.8zM461.5 240.7C463.4 249.1 461.5 257.5 453.1 258.5L439.1 261.3L439.1 466.6C426.9 473.1 415.7 476.9 406.4 476.9C391.4 476.9 387.7 472.2 376.5 458.2L285 314.5L285 453.5L314 460C314 460 314 476.8 290.6 476.8L226.2 480.5C224.3 476.8 226.2 467.4 232.7 465.6L249.5 460.9L249.5 277.1L226.2 275.2C224.3 266.8 229 254.7 242.1 253.7L311.2 249L406.5 394.6L406.5 265.8L382.2 263C380.3 252.7 387.8 245.3 397.1 244.3L461.6 240.5zM108.4 100.7L374.6 81.1C407.3 78.3 415.7 80.2 436.2 95.1L521.2 154.8C535.2 165.1 539.9 167.9 539.9 179.1L539.9 506.7C539.9 527.2 532.4 539.4 506.3 541.2L197.2 559.8C177.6 560.7 168.2 557.9 158 544.9L95.4 463.7C84.2 448.8 79.5 437.6 79.5 424.5L79.5 133.3C79.5 116.5 87 102.5 108.4 100.6z" />
  </svg>
);

const GithubIcon = ({ className }: { className?: string }) => (
  <svg 
    className={className}
    xmlns="http://www.w3.org/2000/svg" 
    viewBox="0 0 640 640"
    fill="currentColor"
  >
    <path d="M237.9 461.4C237.9 463.4 235.6 465 232.7 465C229.4 465.3 227.1 463.7 227.1 461.4C227.1 459.4 229.4 457.8 232.3 457.8C235.3 457.5 237.9 459.1 237.9 461.4zM206.8 456.9C206.1 458.9 208.1 461.2 211.1 461.8C213.7 462.8 216.7 461.8 217.3 459.8C217.9 457.8 216 455.5 213 454.6C210.4 453.9 207.5 454.9 206.8 456.9zM251 455.2C248.1 455.9 246.1 457.8 246.4 460.1C246.7 462.1 249.3 463.4 252.3 462.7C255.2 462 257.2 460.1 256.9 458.1C256.6 456.2 253.9 454.9 251 455.2zM316.8 72C178.1 72 72 177.3 72 316C72 426.9 141.8 521.8 241.5 555.2C254.3 557.5 258.8 549.6 258.8 543.1C258.8 536.9 258.5 502.7 258.5 481.7C258.5 481.7 188.5 496.7 173.8 451.9C173.8 451.9 162.4 422.8 146 415.3C146 415.3 123.1 399.6 147.6 399.9C147.6 399.9 172.5 401.9 186.2 425.7C208.1 464.3 244.8 453.2 259.1 446.6C261.4 430.6 267.9 419.5 275.1 412.9C219.2 406.7 162.8 398.6 162.8 302.4C162.8 274.9 170.4 261.1 186.4 243.5C183.8 237 175.3 210.2 189 175.6C209.9 169.1 258 202.6 258 202.6C278 197 299.5 194.1 320.8 194.1C342.1 194.1 363.6 197 383.6 202.6C383.6 202.6 431.7 169 452.6 175.6C466.3 210.3 457.8 237 455.2 243.5C471.2 261.2 481 275 481 302.4C481 398.9 422.1 406.6 366.2 412.9C375.4 420.8 383.2 435.8 383.2 459.3C383.2 493 382.9 534.7 382.9 542.9C382.9 549.4 387.5 557.3 400.2 555C500.2 521.8 568 426.9 568 316C568 177.3 455.5 72 316.8 72zM169.2 416.9C167.9 417.9 168.2 420.2 169.9 422.1C171.5 423.7 173.8 424.4 175.1 423.1C176.4 422.1 176.1 419.8 174.4 417.9C172.8 416.3 170.5 415.6 169.2 416.9zM158.4 408.8C157.7 410.1 158.7 411.7 160.7 412.7C162.3 413.7 164.3 413.4 165 412C165.7 410.7 164.7 409.1 162.7 408.1C160.7 407.5 159.1 407.8 158.4 408.8zM190.8 444.4C189.2 445.7 189.8 448.7 192.1 450.6C194.4 452.9 197.3 453.2 198.6 451.6C199.9 450.3 199.3 447.3 197.3 445.4C195.1 443.1 192.1 442.8 190.8 444.4zM179.4 429.7C177.8 430.7 177.8 433.3 179.4 435.6C181 437.9 183.7 438.9 185 437.9C186.6 436.6 186.6 434 185 431.7C183.6 429.4 181 428.4 179.4 429.7z"/>
  </svg>
);

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
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [showMcpModal, setShowMcpModal] = useState(false);
  const [showModelModal, setShowModelModal] = useState(false);
  const { setGraphData, currentWorkspaceId, selectedModel, setSelectedModel, isGraphLoading, credentialsStatus, setCredentialsStatus, activeAiNodes, addActiveAiNode, removeActiveAiNode, jwtToken } = useAppStore();
  const [activeTab, setActiveTab] = useState<'connected' | 'available'>('available');
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [expandedCard, setExpandedCard] = useState<string | null>(null);
  const [isVerifying, setIsVerifying] = useState(false);
  const [tempApiKey, setTempApiKey] = useState("");
  const [tempGithubKey, setTempGithubKey] = useState("");
  const [tempDeepseekKey, setTempDeepseekKey] = useState("");

  const handleVerifyAndSaveToken = async (tokenInput: string, onSuccess: () => void) => {
    if (!tokenInput) return;
    setIsVerifying(true);
    try {
      await updateCredentials(currentWorkspaceId, { notionApiKey: tokenInput });
      const status = await fetchCredentialsStatus(currentWorkspaceId);
      setCredentialsStatus(status);
      onSuccess();
    } catch (err) {
      console.error(err);
      alert("토큰 업데이트 중 서버 오류가 발생했습니다.");
    } finally {
      setIsVerifying(false);
    }
  };

  const handleVerifyAndSaveGithubToken = async (tokenInput: string, onSuccess: () => void) => {
    if (!tokenInput) return;
    setIsVerifying(true);
    try {
      await updateCredentials(currentWorkspaceId, { githubApiKey: tokenInput });
      const status = await fetchCredentialsStatus(currentWorkspaceId);
      setCredentialsStatus(status);
      onSuccess();
    } catch (err) {
      alert("토큰 업데이트 중 서버 오류가 발생했습니다.");
    } finally {
      setIsVerifying(false);
    }
  };

  useEffect(() => {
    fetchCredentialsStatus(currentWorkspaceId).then(status => {
      setCredentialsStatus(status);
      if (status.hasNotionKey || status.hasGithubKey) {
        setActiveTab('connected');
      }
    });
  }, [currentWorkspaceId, setCredentialsStatus]);

  useEffect(() => {
    if (showMcpModal) {
      setActiveTab(credentialsStatus?.hasNotionKey || credentialsStatus?.hasGithubKey ? 'connected' : 'available');
    }
  }, [showMcpModal, credentialsStatus]);

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

  const dataRef = useRef(data);
  useEffect(() => {
    dataRef.current = data;
  }, [data]);

  useEffect(() => {
    if (!currentWorkspaceId) return;

    // JWT 토큰이 필요한 경우 쿠키나 로컬스토리지 처리가 복잡할 수 있으나, 현재 백엔드는 workspaceId 기반 SSE입니다.
    // 백엔드의 NotificationController에 맞게 EventSource 연결
    const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';
    const url = jwtToken 
      ? `${API_BASE_URL}/v1/notifications/sse?token=${jwtToken}&workspaceId=${currentWorkspaceId}` 
      : `${API_BASE_URL}/v1/notifications/sse?token=MVP_DUMMY_TOKEN&workspaceId=${currentWorkspaceId}`;
    const eventSource = new EventSource(url);

    let lastCameraMoveTime = 0;
    const CAMERA_THROTTLE_MS = 1500;

    eventSource.addEventListener('ai_reading', (event) => {
      try {
        const parsedData = JSON.parse(event.data);
        if (parsedData && parsedData.nodeId) {
          addActiveAiNode(parsedData.nodeId);
          
          if (fgRef.current && dataRef.current) {
            const node = dataRef.current.nodes.find(n => n.id === parsedData.nodeId);
            if (node && node.x !== undefined && node.y !== undefined) {
              const now = Date.now();
              if (now - lastCameraMoveTime > CAMERA_THROTTLE_MS) {
                lastCameraMoveTime = now;
                fgRef.current.centerAt(node.x, node.y, 1000);
                fgRef.current.zoom(5, 1000);
              }
            }
          }
        }
      } catch (e) {
        console.error("Failed to parse SSE ai_reading event", e);
      }
    });

    return () => {
      eventSource.close();
    };
  }, [currentWorkspaceId, addActiveAiNode, jwtToken]);

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
      const isActiveAiNode = activeAiNodes.includes(node.id);
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
      
      const isFocused = !hoverNode || isHovered || isConnected || isActiveAiNode;
      const opacity = isFocused ? 1 : 0.2;

      const size = node.val ? Math.min(8, Math.max(4, node.val)) : 4;

      ctx.beginPath();
      ctx.arc(node.x, node.y, size, 0, 2 * Math.PI, false);
      ctx.fillStyle = isActiveAiNode 
        ? `rgba(16, 185, 129, ${opacity})` // Emerald 500 for AI active node
        : `rgba(228, 228, 231, ${opacity})`; // Default zinc-200
      ctx.fill();

      // 글로우 효과 (AI 읽는 중)
      if (isActiveAiNode) {
        ctx.beginPath();
        ctx.arc(node.x, node.y, size + 4, 0, 2 * Math.PI, false);
        ctx.strokeStyle = `rgba(16, 185, 129, 0.6)`; // 에메랄드 글로우
        ctx.lineWidth = 2;
        ctx.stroke();
      }

      if (globalScale >= 2 || isFocused) {
        const fontSize = 10 / globalScale;
        ctx.font = `${fontSize}px Inter, sans-serif`;
        ctx.textAlign = "center";
        ctx.textBaseline = "middle";
        
        ctx.fillStyle = isActiveAiNode
          ? `rgba(16, 185, 129, 1)` // Emerald 500 for AI text
          : isHovered 
            ? `rgba(255, 255, 255, ${opacity})` // #FFFFFF on hover
            : `rgba(161, 161, 170, ${opacity})`; // #A1A1AA (zinc-400)
          
        ctx.fillText(node.name || node.id, node.x, node.y + size + fontSize + 2);
      }
    },
    [hoverNode, links, activeAiNodes]
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
  }, [data, links, activeAiNodes]);

  if (!data || data.nodes.length === 0) {
    if (isGraphLoading) {
      return (
        <div id="graph-container" className="w-full h-full bg-zinc-900 overflow-hidden relative">
          <div className="absolute inset-0 z-40 flex items-center justify-center bg-zinc-900/80 backdrop-blur-sm">
            <div className="flex flex-col items-center justify-center p-10 w-full max-w-lg border border-dashed border-zinc-700 bg-zinc-800/20 rounded-md shadow-2xl animate-in fade-in zoom-in-95 duration-200">
              <Loader2 className="w-10 h-10 text-zinc-400 mb-3 animate-spin" />
              <p className="text-zinc-200 text-center text-sm font-medium mb-1">
                지식 그래프를 불러오는 중...
              </p>
            </div>
          </div>
        </div>
      );
    }
    return (
      <div id="graph-container" className="w-full h-full bg-zinc-900 overflow-hidden">
        <EmptyState onSubmit={onFileDrop || (() => {})} isUploading={isUploading} />
      </div>
    );
  }

  return (
    <div id="graph-container" className="w-full h-full bg-zinc-900 overflow-hidden relative">
      {(isGraphLoading || isUploading) && (
        <div className="absolute inset-0 z-40 flex items-center justify-center bg-zinc-900/80 backdrop-blur-sm">
          <div className="flex flex-col items-center justify-center p-10 w-full max-w-lg border border-dashed border-zinc-700 bg-zinc-800/20 rounded-md shadow-2xl animate-in fade-in zoom-in-95 duration-200">
            <Loader2 className="w-10 h-10 text-zinc-400 mb-3 animate-spin" />
            <p className="text-zinc-200 text-center text-sm font-medium mb-1">
              지식 그래프를 생성하는 중...
            </p>
            <p className="text-zinc-500 text-center text-xs animate-pulse">
              AI가 문서를 분석하고 있습니다.
            </p>
          </div>
        </div>
      )}
      {showMcpModal && (
        <div className="absolute inset-0 z-50 flex items-center justify-center">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setShowMcpModal(false)} />
          <div className="relative bg-zinc-900 border border-zinc-700 rounded-xl shadow-2xl w-[460px] p-6 animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between mb-4 border-b border-zinc-800 pb-4">
              <h3 className="text-zinc-100 font-semibold text-lg flex items-center gap-2">
                <Server className="w-5 h-5 text-zinc-300" /> 앱 연결 관리 (Integrations)
              </h3>
              <button onClick={() => setShowMcpModal(false)} className="text-zinc-500 hover:text-zinc-300 transition-colors"><X className="w-5 h-5" /></button>
            </div>

            {/* Tabs */}
            <div className="flex bg-zinc-800/50 p-1 rounded-lg mb-4">
              <button 
                onClick={() => setActiveTab('connected')}
                className={`flex-1 py-1.5 text-sm font-medium rounded-md transition-colors ${activeTab === 'connected' ? 'bg-zinc-700 text-white shadow' : 'text-zinc-500 hover:text-zinc-300'}`}
              >
                내 연결
              </button>
              <button 
                onClick={() => setActiveTab('available')}
                className={`flex-1 py-1.5 text-sm font-medium rounded-md transition-colors ${activeTab === 'available' ? 'bg-zinc-700 text-white shadow' : 'text-zinc-500 hover:text-zinc-300'}`}
              >
                지원하는 앱
              </button>
            </div>



            <div className="space-y-3 min-h-[160px]">
              
              {/* === CONNECTED TAB === */}
              {activeTab === 'connected' && (
                <div className="space-y-3">
                  {!credentialsStatus?.hasNotionKey && !credentialsStatus?.hasGithubKey ? (
                    <div className="flex flex-col items-center justify-center py-10 text-zinc-500">
                      <Server className="w-8 h-8 mb-2 opacity-20" />
                      <p className="text-sm">현재 연결된 서비스가 없습니다.</p>
                    </div>
                  ) : (
                    <>
                      {credentialsStatus?.hasNotionKey && (
                        <div className="bg-zinc-800/40 border border-zinc-700/50 rounded-lg overflow-hidden transition-all">
                          <div className="p-4 flex items-center justify-between hover:bg-zinc-800/60 cursor-pointer" onClick={() => setExpandedCard(expandedCard === 'notion_connected' ? null : 'notion_connected')}>
                            <div className="flex items-center gap-3">
                              <div className="w-10 h-10 rounded-md bg-zinc-900 flex items-center justify-center border border-zinc-700">
                                 <NotionIcon className="w-5 h-5 text-zinc-300" />
                              </div>
                              <div>
                                <h4 className="text-sm font-medium text-zinc-200 flex items-center gap-2">
                                  Notion
                                  <span className="px-1.5 py-0.5 rounded-full bg-zinc-800 text-zinc-300 text-[10px] border border-zinc-700">Connected</span>
                                </h4>
                                <p className="text-xs text-zinc-500">노션 페이지와 데이터베이스 연동</p>
                              </div>
                            </div>
                            <button className="text-zinc-400 hover:text-white">
                              {expandedCard === 'notion_connected' ? <ChevronUp className="w-5 h-5" /> : <Settings2 className="w-5 h-5" />}
                            </button>
                          </div>
                          
                          {expandedCard === 'notion_connected' && (
                            <div className="p-4 pt-0 border-t border-zinc-800/50 bg-zinc-800/20 animate-in slide-in-from-top-2 duration-200">
                               <div className="mt-3">
                                 <label className="block text-xs text-zinc-400 mb-1">Notion API Token (수정 가능)</label>
                                 <input 
                                   type="password" 
                                   value={tempApiKey} 
                                   onChange={e => setTempApiKey(e.target.value)} 
                                   className="w-full bg-zinc-900 border border-zinc-700 rounded p-2 text-sm text-zinc-200 outline-none focus:border-zinc-500" 
                                   placeholder="새로운 토큰을 입력하면 업데이트됩니다." 
                                 />
                                 <button 
                                   onClick={() => { 
                                     handleVerifyAndSaveToken(tempApiKey, () => {
                                       setExpandedCard(null); 
                                       setTempApiKey("");
                                       alert("Notion 토큰이 성공적으로 갱신되었습니다."); 
                                     });
                                   }} 
                                   disabled={isVerifying || !tempApiKey}
                                   className="mt-3 w-full py-2 flex items-center justify-center bg-zinc-200 hover:bg-white disabled:opacity-50 text-zinc-900 text-sm font-semibold rounded-md transition-colors"
                                 >
                                   {isVerifying ? <Loader2 className="w-4 h-4 animate-spin mr-2" /> : null}
                                   업데이트
                                 </button>
                               </div>
                            </div>
                          )}
                        </div>
                      )}

                      {credentialsStatus?.hasGithubKey && (
                        <div className="bg-zinc-800/40 border border-zinc-700/50 rounded-lg overflow-hidden transition-all">
                          <div className="p-4 flex items-center justify-between hover:bg-zinc-800/60 cursor-pointer" onClick={() => setExpandedCard(expandedCard === 'github_connected' ? null : 'github_connected')}>
                            <div className="flex items-center gap-3">
                              <div className="w-10 h-10 rounded-md bg-zinc-900 flex items-center justify-center border border-zinc-700">
                                 <GithubIcon className="w-5 h-5 text-zinc-300" />
                              </div>
                              <div>
                                <h4 className="text-sm font-medium text-zinc-200 flex items-center gap-2">
                                  GitHub
                                  <span className="px-1.5 py-0.5 rounded-full bg-zinc-800 text-zinc-300 text-[10px] border border-zinc-700">Connected</span>
                                </h4>
                                <p className="text-xs text-zinc-500">리포지토리 이슈 및 문서 연동</p>
                              </div>
                            </div>
                            <button className="text-zinc-400 hover:text-white">
                              {expandedCard === 'github_connected' ? <ChevronUp className="w-5 h-5" /> : <Settings2 className="w-5 h-5" />}
                            </button>
                          </div>
                          
                          {expandedCard === 'github_connected' && (
                            <div className="p-4 pt-0 border-t border-zinc-800/50 bg-zinc-800/20 animate-in slide-in-from-top-2 duration-200">
                               <div className="mt-3">
                                 <label className="block text-xs text-zinc-400 mb-1">GitHub PAT Token (수정 가능)</label>
                                 <input 
                                   type="password" 
                                   value={tempGithubKey} 
                                   onChange={e => setTempGithubKey(e.target.value)} 
                                   className="w-full bg-zinc-900 border border-zinc-700 rounded p-2 text-sm text-zinc-200 outline-none focus:border-zinc-500" 
                                   placeholder="새로운 토큰을 입력하면 업데이트됩니다." 
                                 />
                                 <button 
                                   onClick={() => { 
                                     handleVerifyAndSaveGithubToken(tempGithubKey, () => {
                                       setExpandedCard(null); 
                                       setTempGithubKey("");
                                       alert("GitHub 토큰이 성공적으로 갱신되었습니다."); 
                                     });
                                   }} 
                                   disabled={isVerifying || !tempGithubKey}
                                   className="mt-3 w-full py-2 flex items-center justify-center bg-zinc-200 hover:bg-white disabled:opacity-50 text-zinc-900 text-sm font-semibold rounded-md transition-colors"
                                 >
                                   {isVerifying ? <Loader2 className="w-4 h-4 animate-spin mr-2" /> : null}
                                   업데이트
                                 </button>
                               </div>
                            </div>
                          )}
                        </div>
                      )}
                    </>
                  )}
                </div>
              )}

              {/* === AVAILABLE TAB === */}
              {activeTab === 'available' && (
                <>
                  {/* Notion Available Card (Always show) */}
                  <div className="bg-zinc-800/40 border border-zinc-700/50 rounded-lg overflow-hidden transition-all">
                    <div 
                      className={`p-4 flex items-center justify-between ${credentialsStatus?.hasNotionKey ? 'cursor-default' : 'hover:bg-zinc-800/60 cursor-pointer'}`} 
                      onClick={() => !credentialsStatus?.hasNotionKey && setExpandedCard(expandedCard === 'notion_available' ? null : 'notion_available')}
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-md bg-zinc-900 flex items-center justify-center border border-zinc-700">
                           <NotionIcon className="w-5 h-5 text-zinc-300" />
                        </div>
                        <div>
                          <h4 className="text-sm font-medium text-zinc-200 flex items-center gap-2">
                            Notion
                            {credentialsStatus?.hasNotionKey && <span className="px-1.5 py-0.5 rounded-full bg-zinc-800 text-emerald-400 text-[10px] border border-zinc-700">Connected</span>}
                          </h4>
                          <p className="text-xs text-zinc-500">노션 페이지와 데이터베이스 연동</p>
                        </div>
                      </div>
                      {!credentialsStatus?.hasNotionKey && (
                        <button className="text-zinc-400 hover:text-white">
                          {expandedCard === 'notion_available' ? <ChevronUp className="w-5 h-5" /> : <Settings2 className="w-5 h-5" />}
                        </button>
                      )}
                    </div>
                    
                    {expandedCard === 'notion_available' && !credentialsStatus?.hasNotionKey && (
                      <div className="p-4 pt-0 border-t border-zinc-800/50 bg-zinc-800/20 animate-in slide-in-from-top-2 duration-200">
                         <div className="mt-3">
                           <label className="block text-xs text-zinc-400 mb-1">Notion API Token 추가</label>
                           <input 
                             type="password" 
                             value={tempApiKey} 
                             onChange={e => setTempApiKey(e.target.value)} 
                             className="w-full bg-zinc-900 border border-zinc-700 rounded p-2 text-sm text-zinc-200 outline-none focus:border-zinc-500" 
                             placeholder="secret_..." 
                           />
                           <button 
                             onClick={() => { 
                               handleVerifyAndSaveToken(tempApiKey, () => {
                                 setExpandedCard(null);
                                 setActiveTab('connected'); 
                                 setTempApiKey("");
                                 alert("Notion이 성공적으로 연결되었습니다."); 
                               });
                             }} 
                             disabled={isVerifying || !tempApiKey}
                             className="mt-3 w-full flex items-center justify-center py-2 bg-zinc-200 hover:bg-white disabled:opacity-50 text-zinc-900 text-sm font-semibold rounded-md transition-colors"
                           >
                             {isVerifying ? <Loader2 className="w-4 h-4 animate-spin mr-2" /> : null}
                             새로 연결하기
                           </button>
                         </div>
                      </div>
                    )}
                  </div>

                  {/* Github Available Card */}
                  <div className="bg-zinc-800/40 border border-zinc-700/50 rounded-lg overflow-hidden transition-all">
                    <div 
                      className={`p-4 flex items-center justify-between ${credentialsStatus?.hasGithubKey ? 'cursor-default' : 'hover:bg-zinc-800/60 cursor-pointer'}`} 
                      onClick={() => !credentialsStatus?.hasGithubKey && setExpandedCard(expandedCard === 'github_available' ? null : 'github_available')}
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-md bg-zinc-900 flex items-center justify-center border border-zinc-700">
                           <GithubIcon className="w-5 h-5 text-zinc-300" />
                        </div>
                        <div>
                          <h4 className="text-sm font-medium text-zinc-200 flex items-center gap-2">
                            GitHub
                            {credentialsStatus?.hasGithubKey && <span className="px-1.5 py-0.5 rounded-full bg-zinc-800 text-emerald-400 text-[10px] border border-zinc-700">Connected</span>}
                          </h4>
                          <p className="text-xs text-zinc-500">리포지토리 이슈 및 문서 연동</p>
                        </div>
                      </div>
                      {!credentialsStatus?.hasGithubKey && (
                        <button className="text-zinc-400 hover:text-white">
                          {expandedCard === 'github_available' ? <ChevronUp className="w-5 h-5" /> : <Settings2 className="w-5 h-5" />}
                        </button>
                      )}
                    </div>
                    
                    {expandedCard === 'github_available' && !credentialsStatus?.hasGithubKey && (
                      <div className="p-4 pt-0 border-t border-zinc-800/50 bg-zinc-800/20 animate-in slide-in-from-top-2 duration-200">
                         <div className="mt-3">
                           <label className="block text-xs text-zinc-400 mb-1">GitHub PAT Token 추가</label>
                           <input 
                             type="password" 
                             value={tempGithubKey} 
                             onChange={e => setTempGithubKey(e.target.value)} 
                             className="w-full bg-zinc-900 border border-zinc-700 rounded p-2 text-sm text-zinc-200 outline-none focus:border-zinc-500" 
                             placeholder="ghp_..." 
                           />
                           <button 
                             onClick={() => { 
                               handleVerifyAndSaveGithubToken(tempGithubKey, () => {
                                 setExpandedCard(null);
                                 setActiveTab('connected'); 
                                 setTempGithubKey("");
                                 alert("GitHub이 성공적으로 연결되었습니다."); 
                               });
                             }} 
                             disabled={isVerifying || !tempGithubKey}
                             className="mt-3 w-full flex items-center justify-center py-2 bg-zinc-200 hover:bg-white disabled:opacity-50 text-zinc-900 text-sm font-semibold rounded-md transition-colors"
                           >
                             {isVerifying ? <Loader2 className="w-4 h-4 animate-spin mr-2" /> : null}
                             새로 연결하기
                           </button>
                         </div>
                      </div>
                    )}
                  </div>

                  {/* Google Drive Card (Soon) */}
                  <div className="bg-zinc-800/20 border border-zinc-800 rounded-lg overflow-hidden opacity-60">
                    <div className="p-4 flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-md bg-zinc-900 flex items-center justify-center border border-zinc-800">
                           <HardDrive className="w-5 h-5 text-zinc-500" />
                        </div>
                        <div>
                          <h4 className="text-sm font-medium text-zinc-400 flex items-center gap-2">
                            Google Drive
                            <span className="px-1.5 py-0.5 rounded-full bg-zinc-700 text-zinc-400 text-[10px]">Soon</span>
                          </h4>
                          <p className="text-xs text-zinc-600">독스 및 스프레드시트 연동</p>
                        </div>
                      </div>
                    </div>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      )}
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
      <input
        type="file"
        multiple
        className="hidden"
        ref={fileInputRef}
        onChange={(e) => {
          if (e.target.files && e.target.files.length > 0 && onFileDrop) {
            onFileDrop(Array.from(e.target.files));
            e.target.value = "";
          }
        }}
      />
      <div className="absolute bottom-8 left-8 z-50 flex items-center gap-4">
        <button onClick={() => setShowModelModal(true)} className="relative flex items-center justify-center p-4 rounded-full bg-zinc-800/80 backdrop-blur-md border border-zinc-700/50 shadow-2xl text-white hover:bg-zinc-700 hover:scale-105 transition-all duration-300" title="AI 모델 설정">
           <BrainCircuit className={`w-6 h-6 ${selectedModel === 'deepseek-v4' ? 'text-indigo-400' : 'text-emerald-400'}`} />
        </button>
      </div>

      <div className="absolute bottom-8 right-8 z-50 flex items-center gap-4">
        <button
          onClick={() => setShowUploadModal(true)}
          disabled={isUploading}
          className="relative flex items-center justify-center p-4 rounded-full bg-zinc-800/80 backdrop-blur-md border border-zinc-700/50 shadow-2xl text-white hover:bg-zinc-700 hover:scale-105 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed"
          title="파일 업로드"
        >
          {isUploading ? (
            <Loader2 className="w-6 h-6 animate-spin text-zinc-400" />
          ) : (
            <UploadCloud className="w-6 h-6" />
          )}
        </button>

        <button onClick={() => setShowMcpModal(prev => !prev)} className="relative flex items-center justify-center p-4 rounded-full bg-zinc-800/80 backdrop-blur-md border border-zinc-700/50 shadow-2xl text-white hover:bg-zinc-700 hover:scale-105 transition-all duration-300" title="MCP 연결 설정">
           <Server className="w-6 h-6 text-zinc-300" />
           <span className="absolute top-1 right-1 w-2.5 h-2.5 bg-emerald-500 rounded-full border-2 border-zinc-900 animate-pulse"></span>
        </button>
      </div>

      {/* Upload Modal */}
      {showUploadModal && (
        <div className="absolute inset-0 z-[60] flex items-center justify-center">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => !isUploading && setShowUploadModal(false)} />
          <div className="relative bg-zinc-900 border border-zinc-700 rounded-xl shadow-2xl w-[600px] max-w-[90vw] overflow-hidden animate-in fade-in zoom-in-95 duration-200 flex flex-col">
            <div className="flex items-center justify-between p-4 border-b border-zinc-800 bg-zinc-900/50">
              <h3 className="text-zinc-100 font-semibold flex items-center gap-2">
                <UploadCloud className="w-5 h-5 text-indigo-400" /> 파일 업로드 및 분석
              </h3>
              {!isUploading && (
                <button onClick={() => setShowUploadModal(false)} className="text-zinc-500 hover:text-zinc-300 transition-colors">
                  <X className="w-5 h-5" />
                </button>
              )}
            </div>
            <div className="flex-1 w-full relative h-[400px]">
              <EmptyState 
                onSubmit={(files) => { 
                  setShowUploadModal(false); 
                  if(onFileDrop) onFileDrop(files); 
                }} 
                isUploading={isUploading} 
              />
            </div>
          </div>
        </div>
      )}

      {/* Model Selection Modal */}
      {showModelModal && (
        <div className="absolute inset-0 z-50 flex items-center justify-center">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setShowModelModal(false)} />
          <div className="relative bg-zinc-900 border border-zinc-700 rounded-xl shadow-2xl w-[400px] p-6 animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between mb-4 border-b border-zinc-800 pb-4">
              <h3 className="text-zinc-100 font-semibold text-base flex items-center gap-2">
                <BrainCircuit className="w-5 h-5 text-indigo-400" /> AI 추론 엔진 선택
              </h3>
              <button onClick={() => setShowModelModal(false)} className="text-zinc-500 hover:text-zinc-300 transition-colors"><X className="w-5 h-5" /></button>
            </div>
            
            <div className="space-y-3">
              <label className={`flex flex-col p-4 rounded-lg border cursor-pointer transition-all ${selectedModel === 'gpt-4o-mini' ? 'bg-emerald-500/10 border-emerald-500/50' : 'bg-zinc-800/50 border-zinc-700 hover:border-zinc-500'}`}>
                <div className="flex items-center gap-3">
                  <input type="radio" name="model_modal" checked={selectedModel === 'gpt-4o-mini'} onChange={() => setSelectedModel('gpt-4o-mini')} className="accent-emerald-500 w-4 h-4" />
                  <span className={`text-base font-semibold ${selectedModel === 'gpt-4o-mini' ? 'text-emerald-400' : 'text-zinc-300'}`}>GPT-4o Mini</span>
                </div>
              </label>
              
              <label className={`flex flex-col p-4 rounded-lg border cursor-pointer transition-all ${selectedModel === 'deepseek-v4' ? 'bg-indigo-500/10 border-indigo-500/50' : 'bg-zinc-800/50 border-zinc-700 hover:border-zinc-500'}`}>
                <div className="flex items-center gap-3">
                  <input type="radio" name="model_modal" checked={selectedModel === 'deepseek-v4'} onChange={() => setSelectedModel('deepseek-v4')} className="accent-indigo-500 w-4 h-4" />
                  <span className={`text-base font-semibold ${selectedModel === 'deepseek-v4' ? 'text-indigo-400' : 'text-zinc-300'}`}>DeepSeek v4</span>
                </div>
              </label>
            </div>
            
            <div className="mt-6">
              <button 
                onClick={() => setShowModelModal(false)}
                className="w-full py-2.5 flex items-center justify-center bg-zinc-200 hover:bg-white text-zinc-900 text-sm font-semibold rounded-lg transition-colors"
              >
                적용하기
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
