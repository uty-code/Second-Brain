/**
 * API Service for AIMS-Graph Frontend
 * Includes SSE (Server-Sent Events) stub for MCP client integration.
 */

// Base URL for the backend API
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';
export const SSE_URL = `${API_BASE_URL}/v1/mcp/sse`;

import { useAppStore } from '../store/useAppStore';

function getAuthHeader() {
  const token = useAppStore.getState().jwtToken;
  return `Bearer ${token || 'MVP_DUMMY_TOKEN'}`;
}

export class SSEClient {
  private eventSource: EventSource | null = null;
  private url: string;
  private onMessageCallback: ((data: any) => void) | null = null;
  private onErrorCallback: ((error: Event) => void) | null = null;

  constructor(endpoint: string) {
    this.url = `${API_BASE_URL}${endpoint}`;
  }

  public connect() {
    if (this.eventSource) {
      this.disconnect();
    }

    console.log(`Connecting to SSE endpoint: ${this.url}`);
    this.eventSource = new EventSource(this.url);

    this.eventSource.onmessage = (event) => {
      try {
        const parsedData = JSON.parse(event.data);
        if (this.onMessageCallback) {
          this.onMessageCallback(parsedData);
        }
      } catch (e) {
        console.error('Error parsing SSE data:', e);
      }
    };

    this.eventSource.onerror = (error) => {
      console.error('SSE connection error:', error);
      if (this.onErrorCallback) {
        this.onErrorCallback(error);
      }
      // Reconnection logic can be added here
    };
    
    // Custom events
    this.eventSource.addEventListener('mcp-event', (event: MessageEvent) => {
      console.log('Received MCP Event:', event.data);
    });
  }

  public disconnect() {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
      console.log('SSE connection closed.');
    }
  }

  public onMessage(callback: (data: any) => void) {
    this.onMessageCallback = callback;
  }

  public onError(callback: (error: Event) => void) {
    this.onErrorCallback = callback;
  }
}

// Example usage hook stub
export function useMCPConnection() {
  // This would typically be a React hook that initializes SSEClient
  // and manages connection state (connecting, connected, error).
}

export async function fetchWorkspaces(): Promise<string[]> {
  try {
    const response = await fetch(`${API_BASE_URL}/v1/workspaces/list`, {
      method: "GET",
      headers: {
        "Authorization": getAuthHeader()
      }
    });
    
    if (!response.ok) {
      throw new Error(`API error: ${response.status} ${response.statusText}`);
    }
    
    return await response.json();
  } catch (error) {
    console.error("Failed to fetch workspaces:", error);
    return ["default-workspace"];
  }
}

export async function fetchWorkspaceGraph(workspaceId: string): Promise<{ nodes: any[]; links: any[] }> {
  try {
    const response = await fetch(`${API_BASE_URL}/v1/workspaces/${workspaceId}/graph`, {
      method: "GET",
      headers: {
        "Authorization": getAuthHeader(),
        "X-Workspace-ID": workspaceId
      }
    });
    
    if (!response.ok) {
      throw new Error(`API error: ${response.status} ${response.statusText}`);
    }
    
    return await response.json();
  } catch (error) {
    console.error("Failed to fetch workspace graph:", error);
    return { nodes: [], links: [] };
  }
}

export async function sendQuery(query: string, workspaceId: string = "default-workspace", file_back: boolean = false, model: string = "gpt-4o-mini", useNotion: boolean = false): Promise<{ answer: string; insightFile?: string; graphUpdated?: boolean }> {
  try {
    const response = await fetch(`${API_BASE_URL}/v1/query`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": getAuthHeader(),
        "X-Workspace-ID": workspaceId,
        "X-AI-Model": model
      },
      body: JSON.stringify({ query, file_back, useNotion }),
    });
    
    if (!response.ok) {
      throw new Error(`API error: ${response.status} ${response.statusText}`);
    }
    
    return await response.json();
  } catch (error) {
    console.error("Failed to send query:", error);
    throw error;
  }
}

export async function analyzeFiles(files: File[], workspaceId: string = "default-workspace", model: string = "gpt-4o-mini"): Promise<{ nodes: any[]; links: any[] }> {
  const formData = new FormData();
  files.forEach((file) => {
    formData.append("files", file);
  });

  const response = await fetch(`${API_BASE_URL}/v1/analyze`, {
    method: "POST",
    headers: {
      "Authorization": getAuthHeader(),
      "X-Workspace-ID": workspaceId,
      "X-AI-Model": model
    },
    body: formData,
  });

  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(`Analyze API error: ${response.status} - ${errorBody}`);
  }

  return await response.json();
}

export async function fetchWikiPageContent(conceptName: string, workspaceId: string = "default-workspace"): Promise<{ content: string }> {
  try {
    const response = await fetch(`${API_BASE_URL}/v1/wiki/${conceptName}`, {
      method: "GET",
      headers: {
        "Authorization": getAuthHeader(),
        "X-Workspace-ID": workspaceId
      }
    });

    if (!response.ok) {
      if (response.status === 404) {
        return { content: `## ${conceptName}\n\n해당 노드의 마크다운 문서를 찾을 수 없습니다.` };
      }
      throw new Error(`API error: ${response.status} ${response.statusText}`);
    }

    return await response.json();
  } catch (error) {
    console.error("Failed to fetch wiki content:", error);
    return { content: `## 에러 발생\n\n문서 내용을 불러오는 중 에러가 발생했습니다:\n\n${(error as Error).message}` };
  }
}

export async function deleteWorkspaceData(workspaceId: string): Promise<{ status: string; deletedNodes: number; deletedFiles: number }> {
  const response = await fetch(`${API_BASE_URL}/v1/workspaces/${workspaceId}/data`, {
    method: "DELETE",
    headers: {
      "Authorization": getAuthHeader(),
      "X-Workspace-ID": workspaceId,
    },
  });

  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(`Delete API error: ${response.status} - ${errorBody}`);
  }

  return await response.json();
}

export async function createWorkspace(name: string): Promise<{ status: string; workspaceId: string }> {
  const response = await fetch(`${API_BASE_URL}/v1/workspaces`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": getAuthHeader(),
    },
    body: JSON.stringify({ name }),
  });

  if (!response.ok) {
    const errorBody = await response.json().catch(() => ({ message: response.statusText }));
    throw new Error(errorBody.message || `API error: ${response.status}`);
  }

  return await response.json();
}

export async function ingestNotionPage(pageId: string, workspaceId: string = "default-workspace", model: string = "gpt-4o-mini"): Promise<{ nodes: any[]; links: any[] }> {
  const response = await fetch(`${API_BASE_URL}/v1/analyze/notion`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": getAuthHeader(),
      "X-Workspace-ID": workspaceId,
      "X-AI-Model": model
    },
    body: JSON.stringify({ pageId }),
  });

  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(`Notion ingest API error: ${response.status} - ${errorBody}`);
  }

  return await response.json();
}

export async function verifyNotionToken(workspaceId: string): Promise<boolean> {
  const response = await fetch(`${API_BASE_URL}/v1/analyze/notion/verify`, {
    method: "POST",
    headers: {
      "Authorization": getAuthHeader(),
      "X-Workspace-ID": workspaceId
    }
  });

  if (!response.ok) {
    return false;
  }
  
  const data = await response.json();
  return data.valid === true;
}

export async function verifyGithubToken(token: string): Promise<boolean> {
  try {
    const response = await fetch("https://api.github.com/user", {
      headers: {
        "Authorization": `token ${token}`
      }
    });
    return response.ok;
  } catch (error) {
    return false;
  }
}

export async function fetchCredentialsStatus(workspaceId: string): Promise<{ hasNotionKey: boolean; hasGithubKey: boolean; hasDeepseekKey: boolean }> {
  try {
    const response = await fetch(`${API_BASE_URL}/v1/workspaces/${workspaceId}/credentials/status`, {
      method: "GET",
      headers: {
        "Authorization": getAuthHeader(),
        "X-Workspace-ID": workspaceId
      }
    });
    if (!response.ok) {
      return { hasNotionKey: false, hasGithubKey: false, hasDeepseekKey: false };
    }
    return await response.json();
  } catch (e) {
    return { hasNotionKey: false, hasGithubKey: false, hasDeepseekKey: false };
  }
}

export async function updateCredentials(workspaceId: string, keys: { notionApiKey?: string; githubApiKey?: string; deepseekApiKey?: string }): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/v1/workspaces/${workspaceId}/credentials`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      "Authorization": getAuthHeader(),
      "X-Workspace-ID": workspaceId
    },
    body: JSON.stringify(keys),
  });
  if (!response.ok) {
    throw new Error("Failed to update credentials");
  }
}
export async function login(username: string, password: string) {
  const response = await fetch(`${API_BASE_URL}/v1/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password })
  });
  if (!response.ok) {
    const err = await response.json().catch(() => ({ message: "Login failed" }));
    throw new Error(err.message);
  }
  return response.json();
}

export async function registerUser(username: string, password: string) {
  const response = await fetch(`${API_BASE_URL}/v1/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password })
  });
  if (!response.ok) {
    const err = await response.json().catch(() => ({ message: "Register failed" }));
    throw new Error(err.message);
  }
  return response.json();
}
