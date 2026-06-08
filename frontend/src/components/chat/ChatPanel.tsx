"use client";

import React, { useState, useRef, useEffect } from "react";
import { Send } from "lucide-react";
import { sendQuery } from "@/services/api";
import { useAppStore, Message } from "@/store/useAppStore";
import { MarkdownViewer } from "@/components/viewer/MarkdownViewer";

export function ChatPanel() {
  const { chatMessages: messages, setChatMessages: setMessages, currentWorkspaceId, setSelectedNodeId } = useAppStore();
  const [input, setInput] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
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
    setInput("");
    setIsSubmitting(true);

    try {
      const response = await sendQuery(userMessage.content, currentWorkspaceId);
      
      setMessages((prev) => 
        prev.map((msg) => 
          msg.id === tempAssistantMessageId 
            ? { ...msg, content: response.answer, isLoading: false }
            : msg
        )
      );
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
      
      <div className="p-4 border-t border-zinc-800 bg-zinc-900">
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
