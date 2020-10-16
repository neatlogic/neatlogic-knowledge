package codedriver.module.knowledge.exception;

import codedriver.framework.exception.core.ApiRuntimeException;

public class KnowledgeDocumentDraftExpiredException extends ApiRuntimeException {

    private static final long serialVersionUID = -7906343592706619028L;

    public KnowledgeDocumentDraftExpiredException(Long id) {
        super("文档草稿：'" + id + "'已过期，不能再修改");
    }
}
