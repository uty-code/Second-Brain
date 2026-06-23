"use client";

import { useState, useRef, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAppStore } from "@/store/useAppStore";
import { fetchWorkspaces, deleteWorkspaceData, createWorkspace, logoutUser, deleteAccount } from "@/services/api";
import { FileText, Brain, User, Wrench, BookOpen, Lightbulb, Trash2, AlertTriangle, Plus, X, LogOut, MoreVertical } from "lucide-react";

const typeIcons: Record<string, React.ReactNode> = {
  concept: <Lightbulb className="w-3.5 h-3.5 text-zinc-400" />,
  entity: <Brain className="w-3.5 h-3.5 text-zinc-400" />,
  tool: <Wrench className="w-3.5 h-3.5 text-zinc-400" />,
  theory: <BookOpen className="w-3.5 h-3.5 text-zinc-400" />,
  person: <User className="w-3.5 h-3.5 text-zinc-400" />,
};

export function Sidebar() {
  const { graphData, selectedNodeId, setSelectedNodeId, clearGraphData, workspaces, setWorkspaces, currentWorkspaceId, setCurrentWorkspaceId, currentUser } = useAppStore();
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newVaultName, setNewVaultName] = useState("");
  const [isCreating, setIsCreating] = useState(false);
  const [createError, setCreateError] = useState("");
  
  // User Menu State
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [showAccountDeleteConfirm, setShowAccountDeleteConfirm] = useState(false);
  const [isDeletingAccount, setIsDeletingAccount] = useState(false);

  const createInputRef = useRef<HTMLInputElement>(null);
  const router = useRouter();

  const handleLogout = async () => {
    try {
      await logoutUser();
    } catch (e) {
      console.error(e);
    }
    useAppStore.getState().setWorkspaces([]);
    useAppStore.getState().setCurrentWorkspaceId("");
    useAppStore.getState().setJwtToken(null);
    useAppStore.getState().setIsLoggedIn(false);
    useAppStore.getState().setCurrentUser(null);
    window.location.href = "/login";
  };

  const handleAccountDelete = async () => {
    setIsDeletingAccount(true);
    try {
      await deleteAccount();
      // On success, clear store and redirect to login
      useAppStore.getState().setWorkspaces([]);
      useAppStore.getState().setCurrentWorkspaceId("");
      useAppStore.getState().setJwtToken(null);
      useAppStore.getState().setIsLoggedIn(false);
      useAppStore.getState().setCurrentUser(null);
      window.location.href = "/login";
    } catch (error) {
      console.error("Failed to delete account:", error);
      alert("계정 삭제에 실패했습니다.");
    } finally {
      setIsDeletingAccount(false);
      setShowAccountDeleteConfirm(false);
    }
  };

  useEffect(() => {
    fetchWorkspaces().then((list) => {
      if (list) {
        setWorkspaces(list);
        if (list.length > 0) {
          const currentId = useAppStore.getState().currentWorkspaceId;
          if (!list.includes(currentId)) {
            setCurrentWorkspaceId(list[0]);
          }
        } else {
          setCurrentWorkspaceId("");
        }
      }
    });
  }, []);

  // Auto-focus input when create modal opens
  useEffect(() => {
    if (showCreateModal && createInputRef.current) {
      createInputRef.current.focus();
    }
  }, [showCreateModal]);

  const handleDelete = async () => {
    setIsDeleting(true);
    try {
      await deleteWorkspaceData(currentWorkspaceId);
      clearGraphData();
      // 워크스페이스 목록을 서버에서 다시 가져와서 삭제된 항목 반영
      const updatedList = await fetchWorkspaces();
      setWorkspaces(updatedList);
      // 삭제된 워크스페이스가 현재 선택된 것이면 다른 워크스페이스로 전환
      if (!updatedList.includes(currentWorkspaceId) && updatedList.length > 0) {
        setCurrentWorkspaceId(updatedList[0]);
      } else if (updatedList.length === 0) {
        setCurrentWorkspaceId("");
      }
    } catch (error) {
      console.error("Failed to delete workspace data:", error);
    } finally {
      setIsDeleting(false);
      setShowDeleteConfirm(false);
    }
  };

  const handleCreate = async () => {
    const trimmed = newVaultName.trim();
    if (!trimmed) return;

    setIsCreating(true);
    setCreateError("");
    try {
      const result = await createWorkspace(trimmed);
      // Refresh workspace list
      const updatedList = await fetchWorkspaces();
      setWorkspaces(updatedList);
      // Switch to the newly created workspace
      setCurrentWorkspaceId(result.workspaceId);
      setShowCreateModal(false);
      setNewVaultName("");
    } catch (error) {
      setCreateError((error as Error).message);
    } finally {
      setIsCreating(false);
    }
  };

  const handleCreateKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !isCreating && newVaultName.trim()) {
      handleCreate();
    }
    if (e.key === "Escape") {
      setShowCreateModal(false);
      setNewVaultName("");
      setCreateError("");
    }
  };

  const nodes = graphData?.nodes ?? [];
  const sortedNodes = [...nodes].sort((a, b) => (b.val ?? 0) - (a.val ?? 0));

  return (
    <>
      <aside className="w-64 h-full border-r border-zinc-800 flex flex-col bg-zinc-900 shrink-0">
        {/* Header */}
        <div className="p-4 border-b border-zinc-800 flex items-center gap-2">
          {workspaces.length > 0 ? (
            <select
              value={currentWorkspaceId}
              onChange={(e) => setCurrentWorkspaceId(e.target.value)}
              className="flex-1 min-w-0 bg-zinc-800 text-zinc-200 text-sm font-semibold rounded px-2 py-1 outline-none border border-zinc-700"
            >
              {workspaces.map((ws) => {
                let displayName = ws;
                if (currentUser && ws.startsWith(`${currentUser}_`)) {
                  displayName = ws.substring(currentUser.length + 1);
                }
                return <option key={ws} value={ws}>{displayName}</option>;
              })}
            </select>
          ) : (
            <span className="flex-1 text-zinc-500 text-sm">창고를 생성해 주세요</span>
          )}
          <button
            onClick={() => {
              setShowCreateModal(true);
              setCreateError("");
              setNewVaultName("");
            }}
            className="text-zinc-500 hover:text-zinc-200 transition-colors shrink-0"
            title="새 볼트 생성"
          >
            <Plus className="w-4 h-4" />
          </button>
          {currentWorkspaceId && (
            <button
              onClick={() => setShowDeleteConfirm(true)}
              className="text-zinc-500 hover:text-zinc-200 transition-colors shrink-0 ml-1"
              title="데이터 전체 삭제"
            >
              <Trash2 className="w-3.5 h-3.5" />
            </button>
          )}
        </div>

        {/* Node count */}
        {nodes.length > 0 && (
          <div className="px-4 py-2 border-b border-zinc-800/50">
            <p className="text-[11px] text-zinc-500">
              {nodes.length}개 노드 · {graphData?.links?.length ?? 0}개 연결
            </p>
          </div>
        )}

        {/* Node list */}
        <div className="flex-1 overflow-y-auto">
          {nodes.length === 0 ? (
            <div className="p-4">
              <p className="text-xs text-zinc-500">
                문서를 업로드하면 지식 노드가 여기에 표시됩니다.
              </p>
            </div>
          ) : (
            <ul className="py-1">
              {sortedNodes.map((node) => (
                <li key={node.id}>
                  <button
                    onClick={() => setSelectedNodeId(node.id)}
                    className={`w-full text-left px-4 py-2 flex items-start gap-2.5 transition-colors text-sm ${
                      selectedNodeId === node.id
                        ? "bg-zinc-800 text-zinc-100"
                        : "text-zinc-400 hover:bg-zinc-800/50 hover:text-zinc-200"
                    }`}
                  >
                    <span className="mt-0.5 shrink-0">
                      {typeIcons[node.type ?? "concept"] ?? <FileText className="w-3.5 h-3.5 text-zinc-400" />}
                    </span>
                    <span className="truncate">{node.name}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-zinc-800 mt-auto flex items-center justify-between relative">
          <div className="flex items-center gap-2.5 overflow-hidden">
            <div className="w-7 h-7 rounded-full bg-zinc-800/80 border border-zinc-700/50 flex items-center justify-center shrink-0">
              <User className="w-3.5 h-3.5 text-zinc-400" />
            </div>
            <span className="text-zinc-300 text-sm font-medium truncate">
              {currentUser || "Guest"}
            </span>
          </div>
          
          <div className="relative">
            <button
              onClick={() => setShowUserMenu(!showUserMenu)}
              className="text-zinc-500 hover:text-zinc-200 transition-colors shrink-0 p-1.5 rounded-md hover:bg-zinc-800/80"
              title="사용자 메뉴"
            >
              <MoreVertical className="w-4 h-4" />
            </button>

            {/* User Dropdown Menu */}
            {showUserMenu && (
              <>
                <div className="fixed inset-0 z-40" onClick={() => setShowUserMenu(false)} />
                <div className="absolute bottom-full right-0 mb-2 w-40 bg-zinc-900 border border-zinc-700 rounded-lg shadow-xl z-50 overflow-hidden animate-in fade-in slide-in-from-bottom-2 duration-200">
                  <div className="flex flex-col py-1">
                    <button
                      onClick={() => {
                        setShowUserMenu(false);
                        handleLogout();
                      }}
                      className="flex items-center gap-2 px-3 py-2 text-sm text-zinc-300 hover:bg-zinc-800 hover:text-zinc-100 transition-colors"
                    >
                      <LogOut className="w-4 h-4" />
                      <span>로그아웃</span>
                    </button>
                    <div className="h-px bg-zinc-800 my-1" />
                    <button
                      onClick={() => {
                        setShowUserMenu(false);
                        setShowAccountDeleteConfirm(true);
                      }}
                      className="flex items-center gap-2 px-3 py-2 text-sm text-red-400 hover:bg-red-500/10 hover:text-red-300 transition-colors"
                    >
                      <Trash2 className="w-4 h-4" />
                      <span>계정 탈퇴</span>
                    </button>
                  </div>
                </div>
              </>
            )}
          </div>
        </div>
      </aside>

      {/* Create Vault Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div
            className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            onClick={() => !isCreating && setShowCreateModal(false)}
          />
          <div className="relative bg-zinc-900 border border-zinc-700 rounded-xl shadow-2xl w-[380px] p-6 animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-zinc-100 font-semibold text-sm">새 볼트 생성</h3>
              <button
                onClick={() => setShowCreateModal(false)}
                className="text-zinc-500 hover:text-zinc-300 transition-colors"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-3">
              <div>
                <label className="text-zinc-400 text-xs block mb-1.5">볼트 이름</label>
                <input
                  ref={createInputRef}
                  type="text"
                  value={newVaultName}
                  onChange={(e) => {
                    setNewVaultName(e.target.value);
                    setCreateError("");
                  }}
                  onKeyDown={handleCreateKeyDown}
                  placeholder="예: my-notes"
                  className="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-3 py-2 text-sm text-zinc-200 placeholder-zinc-600 outline-none focus:border-zinc-500 transition-colors"
                  disabled={isCreating}
                />
              </div>

              {/* Removed sanitized name preview UI as per user request */}

              {/* Error message */}
              {createError && (
                <p className="text-zinc-400 text-xs">{createError}</p>
              )}
            </div>

            <div className="flex gap-3 mt-5">
              <button
                onClick={() => {
                  setShowCreateModal(false);
                  setNewVaultName("");
                  setCreateError("");
                }}
                disabled={isCreating}
                className="flex-1 px-4 py-2 rounded-lg border border-zinc-700 text-zinc-300 text-sm font-medium hover:bg-zinc-800 transition-colors disabled:opacity-50"
              >
                취소
              </button>
              <button
                onClick={handleCreate}
                disabled={isCreating || !newVaultName.trim()}
                className="flex-1 px-4 py-2 rounded-lg bg-zinc-800 hover:bg-zinc-700 text-zinc-100 text-sm font-medium transition-colors disabled:opacity-50 disabled:hover:bg-zinc-800 flex items-center justify-center gap-2"
              >
                {isCreating ? (
                  <>
                    <span className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    생성 중...
                  </>
                ) : (
                  "생성"
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div
            className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            onClick={() => !isDeleting && setShowDeleteConfirm(false)}
          />
          <div className="relative bg-zinc-900 border border-zinc-700 rounded-xl shadow-2xl w-[380px] p-6 animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 rounded-full bg-zinc-500/10 flex items-center justify-center shrink-0">
                <AlertTriangle className="w-5 h-5 text-zinc-400" />
              </div>
              <div>
                <h3 className="text-zinc-100 font-semibold text-sm">데이터 전체 삭제</h3>
                <p className="text-zinc-500 text-xs mt-0.5">{currentWorkspaceId}</p>
              </div>
            </div>

            <p className="text-zinc-400 text-sm leading-relaxed mb-6">
              이 워크스페이스의 <span className="text-zinc-200 font-medium">모든 그래프 노드({nodes.length}개)</span>와
              <span className="text-zinc-200 font-medium"> 위키 파일</span>이 영구적으로 삭제됩니다.
              <br />
              <span className="text-zinc-400 text-xs mt-2 block">이 작업은 되돌릴 수 없습니다.</span>
            </p>

            <div className="flex gap-3">
              <button
                onClick={() => setShowDeleteConfirm(false)}
                disabled={isDeleting}
                className="flex-1 px-4 py-2 rounded-lg border border-zinc-700 text-zinc-300 text-sm font-medium hover:bg-zinc-800 transition-colors disabled:opacity-50"
              >
                취소
              </button>
              <button
                onClick={handleDelete}
                disabled={isDeleting}
                className="flex-1 px-4 py-2 rounded-lg bg-zinc-800 hover:bg-zinc-700 text-zinc-100 text-sm font-medium transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
              >
                {isDeleting ? (
                  <>
                    <span className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    삭제 중...
                  </>
                ) : (
                  "삭제"
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Account Delete Confirmation Modal */}
      {showAccountDeleteConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div
            className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            onClick={() => !isDeletingAccount && setShowAccountDeleteConfirm(false)}
          />
          <div className="relative bg-zinc-900 border border-red-900/50 rounded-xl shadow-2xl w-[420px] p-6 animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-12 h-12 rounded-full bg-red-500/10 flex items-center justify-center shrink-0">
                <AlertTriangle className="w-6 h-6 text-red-500" />
              </div>
              <div>
                <h3 className="text-zinc-100 font-semibold text-base">계정 영구 탈퇴</h3>
                <p className="text-red-400 text-xs mt-0.5 font-medium">경고: 파괴적인 작업입니다</p>
              </div>
            </div>

            <p className="text-zinc-400 text-sm leading-relaxed mb-6">
              회원님의 <span className="text-zinc-200 font-medium">모든 지식창고 데이터</span>가 서버에서 
              <span className="text-red-400 font-bold"> 완전히 삭제</span>됩니다. (그래프, 마크다운 위키 파일 포함)
              <br /><br />
              한 번 삭제된 데이터는 절대로 복구할 수 없습니다. 계속하시겠습니까?
            </p>

            <div className="flex gap-3">
              <button
                onClick={() => setShowAccountDeleteConfirm(false)}
                disabled={isDeletingAccount}
                className="flex-1 px-4 py-2 rounded-lg border border-zinc-700 text-zinc-300 text-sm font-medium hover:bg-zinc-800 transition-colors disabled:opacity-50"
              >
                취소
              </button>
              <button
                onClick={handleAccountDelete}
                disabled={isDeletingAccount}
                className="flex-1 px-4 py-2 rounded-lg bg-red-600 hover:bg-red-500 text-white text-sm font-medium transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
              >
                {isDeletingAccount ? (
                  <>
                    <span className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    탈퇴 처리 중...
                  </>
                ) : (
                  "네, 모두 삭제하고 탈퇴합니다"
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
