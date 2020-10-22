package codedriver.module.knowledge.constvalue;

public enum KnowledgeType {
    ALL("all", "所有知识", "/knowledge-all"),
    WAITINGFORREVIEW("waitingforreview", "待我审批的知识", "/knowledge-review"),
    SHARE("share", "我共享的知识", "/knowledge-share"),
    FAVORITES("favorites", "我收藏的知识", "/knowledge-favorites"),
    DRAFT("draft", "草稿", "/knowledge-draft");
    private String value;
    private String text;
    private String route;
    private KnowledgeType(String value, String text, String route) {
        this.value = value;
        this.text = text;
        this.route = route;
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
    public String getRoute() {
        return route;
    }
    public void setRoute(String route) {
        this.route = route;
    }
}
