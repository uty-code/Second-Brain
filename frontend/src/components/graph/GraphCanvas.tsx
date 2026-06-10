"use client";

import React, { useRef, useState, useCallback, useMemo, useEffect } from "react";
import dynamic from "next/dynamic";
import { GraphData, GraphNode, GraphLink } from "@/types/graph";
import { EmptyState } from "./EmptyState";
import { UploadCloud, Loader2, Server, CheckCircle2, X, Settings2, GitPullRequest, HardDrive, ChevronDown, ChevronUp, BrainCircuit } from "lucide-react";
import { useAppStore } from "@/store/useAppStore";
import { ingestNotionPage, verifyNotionToken } from "@/services/api";

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
  const { setGraphData, currentWorkspaceId, notionApiKey, setNotionApiKey, selectedModel, setSelectedModel, deepseekApiKey, setDeepseekApiKey, isGraphLoading } = useAppStore();
  const [activeTab, setActiveTab] = useState<'connected' | 'available'>(notionApiKey ? 'connected' : 'available');
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [expandedCard, setExpandedCard] = useState<string | null>(null);
  const [isVerifying, setIsVerifying] = useState(false);
  const [tempApiKey, setTempApiKey] = useState("");

  const [isNotionLoading, setIsNotionLoading] = useState(false);
  const [tempDeepseekKey, setTempDeepseekKey] = useState(deepseekApiKey || "");

  const handleVerifyAndSaveToken = async (tokenInput: string, onSuccess: () => void) => {
    if (!tokenInput) return;
    setIsVerifying(true);
    try {
      const isValid = await verifyNotionToken(tokenInput);
      if (isValid) {
        setNotionApiKey(tokenInput);
        onSuccess();
      } else {
        alert("유효하지 않은 토큰입니다. 다시 확인해주세요.");
      }
    } catch (err) {
      console.error(err);
      alert("토큰 검증 중 서버 오류가 발생했습니다.");
    } finally {
      setIsVerifying(false);
    }
  };

  // 모달을 열 때마다 토큰 유무에 따라 똑똑하게 첫 탭을 결정합니다.
  useEffect(() => {
    if (showMcpModal) {
      setActiveTab(notionApiKey ? 'connected' : 'available');
    }
  }, [showMcpModal, notionApiKey]);

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
                <>
                  {!notionApiKey ? (
                    <div className="flex flex-col items-center justify-center py-10 text-zinc-500">
                      <Server className="w-8 h-8 mb-2 opacity-20" />
                      <p className="text-sm">현재 연결된 서비스가 없습니다.</p>
                    </div>
                  ) : (
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
                               value={tempApiKey || notionApiKey} 
                               onChange={e => setTempApiKey(e.target.value)} 
                               className="w-full bg-zinc-900 border border-zinc-700 rounded p-2 text-sm text-zinc-200 outline-none focus:border-zinc-500" 
                               placeholder="secret_..." 
                             />
                             <button 
                               onClick={() => { 
                                 const targetToken = tempApiKey || notionApiKey;
                                 handleVerifyAndSaveToken(targetToken, () => {
                                   setExpandedCard(null); 
                                   setTempApiKey("");
                                   alert("Notion 토큰이 성공적으로 갱신되었습니다."); 
                                 });
                               }} 
                               disabled={isVerifying || (!tempApiKey && !notionApiKey)}
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

              {/* === AVAILABLE TAB === */}
              {activeTab === 'available' && (
                <>
                  {!notionApiKey && (
                    <div className="bg-zinc-800/40 border border-zinc-700/50 rounded-lg overflow-hidden transition-all">
                      <div className="p-4 flex items-center justify-between hover:bg-zinc-800/60 cursor-pointer" onClick={() => setExpandedCard(expandedCard === 'notion_available' ? null : 'notion_available')}>
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 rounded-md bg-zinc-900 flex items-center justify-center border border-zinc-700">
                             <NotionIcon className="w-5 h-5 text-zinc-300" />
                          </div>
                          <div>
                            <h4 className="text-sm font-medium text-zinc-200">Notion</h4>
                            <p className="text-xs text-zinc-500">노션 페이지와 데이터베이스 연동</p>
                          </div>
                        </div>
                        <button className="text-zinc-400 hover:text-white">
                          {expandedCard === 'notion_available' ? <ChevronUp className="w-5 h-5" /> : <Settings2 className="w-5 h-5" />}
                        </button>
                      </div>
                      
                      {expandedCard === 'notion_available' && (
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
                  )}

                  {/* Github Card (Soon) */}
                  <div className="bg-zinc-800/20 border border-zinc-800 rounded-lg overflow-hidden opacity-60">
                    <div className="p-4 flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-md bg-zinc-900 flex items-center justify-center border border-zinc-800">
                           <GitPullRequest className="w-5 h-5 text-zinc-500" />
                        </div>
                        <div>
                          <h4 className="text-sm font-medium text-zinc-400 flex items-center gap-2">
                            GitHub
                            <span className="px-1.5 py-0.5 rounded-full bg-zinc-700 text-zinc-400 text-[10px]">Soon</span>
                          </h4>
                          <p className="text-xs text-zinc-600">리포지토리 이슈 및 문서 연동</p>
                        </div>
                      </div>
                    </div>
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
