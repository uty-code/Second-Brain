CREATE TABLE Workspace (
    id VARCHAR(50) PRIMARY KEY,
    name NVARCHAR(100),
    created_at DATETIME2 DEFAULT GETUTCDATE()
);

CREATE TABLE RawSource (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    workspace_id VARCHAR(50),
    source_uri VARCHAR(500),
    title VARCHAR(500),
    content_hash VARCHAR(64),
    source_type VARCHAR(20),
    status VARCHAR(20),
    created_at DATETIME2 DEFAULT GETUTCDATE(),
    updated_at DATETIME2
);
CREATE INDEX IDX_RawSource_WorkspaceId ON RawSource(workspace_id);

CREATE TABLE WikiPage (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    workspace_id VARCHAR(50),
    page_path VARCHAR(500),
    title NVARCHAR(200),
    page_type VARCHAR(20),
    content_hash VARCHAR(64),
    created_at DATETIME2 DEFAULT GETUTCDATE(),
    updated_at DATETIME2,
    CONSTRAINT UQ_WikiPage_Workspace_Path UNIQUE (workspace_id, page_path)
);
CREATE INDEX IDX_WikiPage_WorkspaceId ON WikiPage(workspace_id);

CREATE TABLE OutboxEvent (
    id UNIQUEIDENTIFIER PRIMARY KEY,
    workspace_id VARCHAR(50),
    aggregate_type VARCHAR(50),
    aggregate_id VARCHAR(255),
    event_type VARCHAR(50),
    payload NVARCHAR(MAX),
    status VARCHAR(20),
    created_at DATETIME2 DEFAULT GETUTCDATE()
);
CREATE INDEX IDX_OutboxEvent_WorkspaceId ON OutboxEvent(workspace_id);
