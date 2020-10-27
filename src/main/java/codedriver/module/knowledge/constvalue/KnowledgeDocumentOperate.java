package codedriver.module.knowledge.constvalue;

public enum KnowledgeDocumentOperate {
    SUBMIT("submit", "提交", "提交了审核"),
    PASS("pass", "通过", "通过了审核"),
    REJECT("reject", "拒绝", "退回了审核"),
    SWITCHVERSION("switchversion", "切换版本", "切换【${oldVersion}】至【${newVersion}】");
    private String value;
    private String text;
    private String title;
    private KnowledgeDocumentOperate(String value, String text, String title) {
        this.value = value;
        this.text = text;
        this.title = title;
    }
    public String getValue() {
        return value;
    }
    public String getText() {
        return text;
    }
    public String getTitle() {
        return title;
    }
}
