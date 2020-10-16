package codedriver.module.knowledge.exception;

import codedriver.framework.exception.core.ApiRuntimeException;

public class KnowledgeDocumentDraftSubmitedException extends ApiRuntimeException {

    private static final long serialVersionUID = -8423316707875372218L;

    public KnowledgeDocumentDraftSubmitedException(Long id) {
        super("文档草稿：'" + id + "'已提交，不能再修改");
    }
}
