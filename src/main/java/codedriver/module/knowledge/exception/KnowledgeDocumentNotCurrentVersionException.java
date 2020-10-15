package codedriver.module.knowledge.exception;

import codedriver.framework.exception.core.ApiRuntimeException;

public class KnowledgeDocumentNotCurrentVersionException extends ApiRuntimeException {

    private static final long serialVersionUID = -8423316707875372218L;

    public KnowledgeDocumentNotCurrentVersionException(Long id) {
        super("版本号：'" + id + "'不是文档的当前版本，不能保存草稿");
    }
}
