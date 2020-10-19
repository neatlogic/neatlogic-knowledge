package codedriver.module.knowledge.exception;

import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;

public class KnowledgeDocumentDraftStatusException extends ApiRuntimeException {

    private static final long serialVersionUID = 6911137500865613699L;
    
    public KnowledgeDocumentDraftStatusException(Long id, KnowledgeDocumentVersionStatus status, String msg) {
        super("文档草稿：'" + id + "',状态为" +status.getText()+ "，" + msg);
    }
}
