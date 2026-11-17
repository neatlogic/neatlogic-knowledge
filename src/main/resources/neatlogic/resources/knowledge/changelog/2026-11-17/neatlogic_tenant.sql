ALTER TABLE knowledge_document_invoke MODIFY invoke_id BIGINT NOT NULL COMMENT '创建来源记录 ID';
ALTER TABLE knowledge_document_invoke MODIFY source VARCHAR(64) NOT NULL COMMENT '创建来源类型';
ALTER TABLE knowledge_document_invoke ADD knowledge_document_version_id BIGINT NULL COMMENT '创建的版本 ID，历史记录可为空';
ALTER TABLE knowledge_document_invoke ADD UNIQUE KEY uk_created_version(knowledge_document_version_id);
ALTER TABLE knowledge_document_invoke ADD KEY idx_source(source,invoke_id);
ALTER TABLE knowledge_document_invoke ADD KEY idx_document(knowledge_document_id);
ALTER TABLE knowledge_document_invoke DROP INDEX idx_invoke_id;
ALTER TABLE knowledge_document_invoke DROP PRIMARY KEY;
