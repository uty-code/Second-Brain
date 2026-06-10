"use client";

import React, { useState } from "react";
import { UploadCloud, Loader2, X, FileText, BrainCircuit } from "lucide-react";
import { useDropzone } from "react-dropzone";
import { useAppStore } from "@/store/useAppStore";

interface EmptyStateProps {
  onSubmit: (files: File[]) => void;
  isUploading?: boolean;
}

export function EmptyState({ onSubmit, isUploading }: EmptyStateProps) {
  const [stagedFiles, setStagedFiles] = useState<File[]>([]);
  const { selectedModel, setSelectedModel } = useAppStore();

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop: (accepted) => {
      setStagedFiles((prev) => [...prev, ...accepted]);
    },
    noClick: isUploading,
    noKeyboard: isUploading,
  });

  const removeFile = (index: number) => {
    setStagedFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const formatSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div className="flex w-full h-full items-center justify-center bg-zinc-900 p-8">
      <div className="flex flex-col items-center w-full max-w-lg gap-4">
        {/* Drop zone */}
        <div
          {...getRootProps()}
          className={`flex flex-col items-center justify-center p-10 w-full border border-dashed rounded-md transition-colors ${
            isUploading
              ? "cursor-default border-zinc-700 bg-zinc-800/20"
              : "cursor-pointer"
          } ${
            isDragActive && !isUploading
              ? "border-zinc-400 bg-zinc-800/50"
              : !isUploading
                ? "border-zinc-700 bg-zinc-900 hover:border-zinc-500 hover:bg-zinc-800/30"
                : ""
          }`}
        >
          <input {...getInputProps()} disabled={isUploading} />

          {isUploading ? (
            <>
              <Loader2 className="w-10 h-10 text-zinc-400 mb-3 animate-spin" />
              <p className="text-zinc-200 text-center text-sm font-medium mb-1">
                지식 그래프를 생성하는 중...
              </p>
              <p className="text-zinc-500 text-center text-xs animate-pulse">
                AI가 문서를 분석하고 있습니다.
              </p>
            </>
          ) : (
            <>
              <UploadCloud className="w-10 h-10 text-zinc-400 mb-3" />
              <p className="text-zinc-200 text-center text-sm font-medium mb-1">
                {stagedFiles.length > 0
                  ? "파일을 더 추가하려면 드래그하세요"
                  : "지식의 우주가 비어있습니다."}
              </p>
              <p className="text-zinc-500 text-center text-xs">
                문서를 드래그 앤 드롭하거나 클릭하여 선택하세요.
              </p>
            </>
          )}
        </div>

        {/* Staged files list */}
        {stagedFiles.length > 0 && !isUploading && (
          <div className="w-full flex flex-col gap-1.5">
            {stagedFiles.map((file, i) => (
              <div
                key={`${file.name}-${i}`}
                className="flex items-center gap-2.5 px-3 py-2 bg-zinc-800/60 border border-zinc-700/50 rounded text-sm group"
              >
                <FileText className="w-4 h-4 text-zinc-400 shrink-0" />
                <span className="text-zinc-300 truncate flex-1">{file.name}</span>
                <span className="text-zinc-500 text-xs shrink-0">{formatSize(file.size)}</span>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    removeFile(i);
                  }}
                  className="text-zinc-500 hover:text-zinc-200 transition-colors shrink-0"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              </div>
            ))}
          </div>
        )}

        {/* AI Model Selection */}
        {!isUploading && (
          <div className="w-full flex gap-3 mt-2">
            <label className={`flex-1 flex items-center justify-center gap-2 p-3 rounded-md border cursor-pointer transition-all ${selectedModel === 'gpt-4o-mini' ? 'bg-emerald-500/10 border-emerald-500/50 text-emerald-400' : 'bg-zinc-800 border-zinc-700 text-zinc-400 hover:border-zinc-500'}`}>
              <input type="radio" name="empty_model" checked={selectedModel === 'gpt-4o-mini'} onChange={() => setSelectedModel('gpt-4o-mini')} className="hidden" />
              <BrainCircuit className="w-4 h-4" />
              <span className="text-xs font-semibold">GPT-4o Mini</span>
            </label>
            <label className={`flex-1 flex items-center justify-center gap-2 p-3 rounded-md border cursor-pointer transition-all ${selectedModel === 'deepseek-v4' ? 'bg-indigo-500/10 border-indigo-500/50 text-indigo-400' : 'bg-zinc-800 border-zinc-700 text-zinc-400 hover:border-zinc-500'}`}>
              <input type="radio" name="empty_model" checked={selectedModel === 'deepseek-v4'} onChange={() => setSelectedModel('deepseek-v4')} className="hidden" />
              <BrainCircuit className="w-4 h-4" />
              <span className="text-xs font-semibold">DeepSeek v4</span>
            </label>
          </div>
        )}

        {/* Submit button */}
        {stagedFiles.length > 0 && !isUploading && (
          <button
            onClick={() => onSubmit(stagedFiles)}
            className="w-full py-2.5 bg-zinc-100 text-zinc-900 text-sm font-medium rounded hover:bg-white transition-colors mt-2"
          >
            {stagedFiles.length}개 파일 분석 시작
          </button>
        )}
      </div>
    </div>
  );
}
