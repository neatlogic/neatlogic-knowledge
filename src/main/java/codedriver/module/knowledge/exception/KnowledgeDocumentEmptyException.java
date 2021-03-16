package codedriver.module.knowledge.exception;

import codedriver.framework.exception.core.ApiRuntimeException;

public class KnowledgeDocumentEmptyException extends ApiRuntimeException {

    private static final long serialVersionUID = -8205734237715203764L;

    public KnowledgeDocumentEmptyException(Long id) {
        super("知识库文档：'" + id+ "'内容为空");
    }
}
