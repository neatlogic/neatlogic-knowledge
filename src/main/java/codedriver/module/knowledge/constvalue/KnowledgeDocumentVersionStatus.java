package codedriver.module.knowledge.constvalue;

public enum KnowledgeDocumentVersionStatus {
    ALL("all","全部"),
    DRAFT("draft", "未提交"),
    SUBMITTED("submitted", "待审批"),
    PASSED("passed", "已通过"),
//    EXPIRED("expired", "已失效"),
    REJECTED("rejected", "不通过");
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
    
    public static String getText(String _value) {
        for(KnowledgeDocumentVersionStatus status : values()) {
            if(status.value.equals(_value)) {
                return status.text;
            }
        }
        return "";
    }
}
