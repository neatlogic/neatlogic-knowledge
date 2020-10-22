package codedriver.module.knowledge.exception;

import codedriver.framework.exception.core.ApiRuntimeException;

public class KnowledgeDocumentNotCurrentVersionException extends ApiRuntimeException {

    private static final long serialVersionUID = -8423316707875372218L;

    public KnowledgeDocumentNotCurrentVersionException(Integer version) {
        super("版本：'" + version + "'不是文档的当前版本，不能在此版本上修改后保存草稿");
    }
}
