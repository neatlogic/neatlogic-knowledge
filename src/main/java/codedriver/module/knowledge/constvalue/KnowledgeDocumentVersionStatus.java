package codedriver.module.knowledge.constvalue;

public enum KnowledgeDocumentVersionStatus {
    DRAFT("draft", "草稿"),
    SUBMITED("submitted", "已提交"),
    PASSED("passed", "已通过"),
    EXPIRED("expired", "已过期"),
    REJECTED("rejected", "已拒绝");
    private String value;
    private String text;
    private KnowledgeDocumentVersionStatus(String value, String text) {
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
