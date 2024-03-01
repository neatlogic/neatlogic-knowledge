package neatlogic.module.knowledge.approve.constvalue;

import neatlogic.framework.process.approve.core.IApproveHandlerType;

public enum KnowledgeApproveHandlerType implements IApproveHandlerType {
    KNOWLEDGE_DOCUMENT("knowledgeDocument", "知识库文档");

    private String value;
    private String text;

    KnowledgeApproveHandlerType(String value, String text) {
        this.value = value;
        this.text = text;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public String getText() {
        return this.text;
    }
}
