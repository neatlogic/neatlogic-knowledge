package codedriver.module.knowledge.constvalue;

public enum KnowledgeType {
    ALL("all", "所有知识", ""),
    WAITINGFORREVIEW("waitingforreview", "待我审批的知识", "knowledge/document/waitingforreview/list"),
    SHARE("share", "我共享的知识", "knowledge/document/share/list"),
    FAVORITES("favorites", "我收藏的知识", "knowledge/document/favorites/list"),
    DRAFT("draft", "草稿", "knowledge/document/draft/list");
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
