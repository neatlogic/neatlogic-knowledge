package codedriver.module.knowledge.exception;

import codedriver.framework.exception.core.ApiRuntimeException;

public class KnowledgeDocumentDraftSubmittedCannotBeDeletedException extends ApiRuntimeException {

    private static final long serialVersionUID = -3200607738061745327L;
    public KnowledgeDocumentDraftSubmittedCannotBeDeletedException(){
        super("该文档草稿已提交，不能删除");
    }
}
