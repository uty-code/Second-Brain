"use client";

import React, { useState, useRef, useEffect } from "react";
import { Send, FileText } from "lucide-react";
import { sendQuery } from "@/services/api";
import { useAppStore, Message } from "@/store/useAppStore";
import { MarkdownViewer } from "@/components/viewer/MarkdownViewer";

export function ChatPanel() {
  const { chatMessages: messages, setChatMessages: setMessages, currentWorkspaceId, selectedModel, setSelectedNodeId, notionApiKey, setIsGraphLoading } = useAppStore();
  const [input, setInput] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [useNotion, setUseNotion] = useState(false);
  const endOfMessagesRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    endOfMessagesRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() || isSubmitting) return;

    const userMessage: Message = { id: Date.now().toString(), role: "user", content: input };
    const tempAssistantMessageId = (Date.now() + 1).toString();
    
    setMessages((prev) => [
      ...prev,
      userMessage,
      { id: tempAssistantMessageId, role: "assistant", content: "", isLoading: true }
    ]);
    const isSaveRequest = /저장|생성|만들|추가/.test(input);
    if (isSaveRequest) {
      setIsGraphLoading(true);
    }
    
    setInput("");
    setIsSubmitting(true);

    try {
      const response = await sendQuery(userMessage.content, currentWorkspaceId, false, selectedModel, useNotion, notionApiKey);
      
      setMessages((prev) => 
        prev.map((msg) => 
          msg.id === tempAssistantMessageId 
            ? { ...msg, content: response.answer, isLoading: false }
            : msg
        )
      );

      if (response.graphUpdated) {
        import("@/services/api").then(({ fetchWorkspaceGraph }) => {
          fetchWorkspaceGraph(currentWorkspaceId).then((newGraph) => {
            useAppStore.getState().setGraphData(newGraph);
          });
        });
      }
    } catch (error) {
      setMessages((prev) => 
        prev.map((msg) => 
          msg.id === tempAssistantMessageId 
            ? { ...msg, content: "오류가 발생했습니다. 잠시 후 다시 시도해주세요.", isLoading: false }
            : msg
        )
      );
    } finally {
      setIsSubmitting(false);
      setIsGraphLoading(false);
    }
  };

  return (
    <div className="flex flex-col h-full bg-zinc-900">
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {messages.map((msg) => (
          <div key={msg.id} className={`flex ${msg.role === "user" ? "justify-end" : "justify-start"}`}>
            <div className={`max-w-[85%] rounded-lg px-4 py-2 text-sm ${msg.role === "user" ? "bg-zinc-800 text-zinc-100" : "bg-transparent text-zinc-300 border border-zinc-800"}`}>
              {msg.isLoading ? (
                <div className="flex space-x-1 items-center h-5">
                  <div className="w-1.5 h-1.5 bg-zinc-500 rounded-full animate-bounce" />
                  <div className="w-1.5 h-1.5 bg-zinc-500 rounded-full animate-bounce [animation-delay:0.2s]" />
                  <div className="w-1.5 h-1.5 bg-zinc-500 rounded-full animate-bounce [animation-delay:0.4s]" />
                </div>
              ) : msg.role === "assistant" ? (
                <MarkdownViewer content={msg.content} onLinkClick={(target) => setSelectedNodeId(target)} />
              ) : (
                <div className="whitespace-pre-wrap">{msg.content}</div>
              )}
            </div>
          </div>
        ))}
        <div ref={endOfMessagesRef} />
      </div>
      
      <div className="p-4 border-t border-zinc-800 bg-zinc-900 flex flex-col gap-3">
        {notionApiKey && (
          <div className="flex items-center gap-4 px-1">
            <button
              type="button"
              onClick={() => setUseNotion(!useNotion)}
              className={`flex items-center justify-center p-2 rounded-full transition-colors border ${
                useNotion 
                  ? "bg-zinc-100 text-zinc-900 border-zinc-200 shadow-sm" 
                  : "bg-zinc-800 text-zinc-400 border-zinc-700 hover:text-zinc-300 hover:bg-zinc-700"
              }`}
              title="Toggle Notion Search"
            >
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 640" className="w-5 h-5 fill-current">
                <path d="M158.9 164.2C173.8 176.3 179.4 175.4 207.5 173.5L471.8 157.6C477.4 157.6 472.7 152 470.9 151.1L426.9 119.4C418.5 112.9 407.3 105.4 385.8 107.3L129.9 125.9C120.6 126.8 118.7 131.5 122.4 135.2L158.8 164.1zM174.8 225.8L174.8 503.9C174.8 518.8 182.3 524.4 199.1 523.5L489.6 506.7C506.4 505.8 508.3 495.5 508.3 483.4L508.3 207.2C508.3 195.1 503.6 188.5 493.3 189.5L189.7 207.1C178.5 208 174.8 213.6 174.8 225.8zM461.5 240.7C463.4 249.1 461.5 257.5 453.1 258.5L439.1 261.3L439.1 466.6C426.9 473.1 415.7 476.9 406.4 476.9C391.4 476.9 387.7 472.2 376.5 458.2L285 314.5L285 453.5L314 460C314 460 314 476.8 290.6 476.8L226.2 480.5C224.3 476.8 226.2 467.4 232.7 465.6L249.5 460.9L249.5 277.1L226.2 275.2C224.3 266.8 229 254.7 242.1 253.7L311.2 249L406.5 394.6L406.5 265.8L382.2 263C380.3 252.7 387.8 245.3 397.1 244.3L461.6 240.5zM108.4 100.7L374.6 81.1C407.3 78.3 415.7 80.2 436.2 95.1L521.2 154.8C535.2 165.1 539.9 167.9 539.9 179.1L539.9 506.7C539.9 527.2 532.4 539.4 506.3 541.2L197.2 559.8C177.6 560.7 168.2 557.9 158 544.9L95.4 463.7C84.2 448.8 79.5 437.6 79.5 424.5L79.5 133.3C79.5 116.5 87 102.5 108.4 100.6z"/>
              </svg>
            </button>
          </div>
        )}
        <form onSubmit={handleSubmit} className="flex gap-2">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="AI에게 질문하기..."
            className="flex-1 bg-zinc-800 border border-zinc-700 rounded-md px-3 py-2 text-sm text-zinc-100 placeholder-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
            disabled={isSubmitting}
          />
          <button
            type="submit"
            disabled={!input.trim() || isSubmitting}
            className="bg-zinc-800 text-zinc-300 p-2 rounded-md hover:bg-zinc-700 disabled:opacity-50 transition-colors border border-zinc-700"
          >
            <Send className="w-4 h-4" />
          </button>
        </form>
      </div>
    </div>
  );
}
