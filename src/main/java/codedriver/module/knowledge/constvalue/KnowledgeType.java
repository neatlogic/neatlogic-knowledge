package codedriver.module.knowledge.constvalue;

public enum KnowledgeType {
    ALL("all", "所有知识"),
    WAITINGFORREVIEW("waitingforreview", "待我审批的知识"),
    SHARE("share", "我共享的知识"),
    COLLECT("collect", "我收藏的知识"),
    DRAFT("draft", "草稿");
    private String value;
    private String text;
    private KnowledgeType(String value, String text) {
        this.value = value;
        this.text = text;
    }
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
}
