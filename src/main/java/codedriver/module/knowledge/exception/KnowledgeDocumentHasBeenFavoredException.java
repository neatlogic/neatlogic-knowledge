package codedriver.module.knowledge.exception;

import codedriver.framework.exception.core.ApiRuntimeException;

public class KnowledgeDocumentHasBeenFavoredException extends ApiRuntimeException {

    private static final long serialVersionUID = 4725067136465559549L;

    public KnowledgeDocumentHasBeenFavoredException(Long id){
        super("您已点赞文档：'" + id);
    }
}
