package codedriver.module.knowledge.constvalue;

public enum KnowledgeDocumentLineHandler {

    P("p", "段落"),
    H1("h1", "一级标题"),
    H2("h2", "二级标题"),
    IMG("img", "图片"),
    TABLE("table", "表格"),
    CODE("code", "代码块"),
    UL("ul", "无序列表"),
    OL("ol", "有序列表")
    ;
    private String value;
    private String text;
    private KnowledgeDocumentLineHandler(String value, String text) {
        this.value = value;
        this.text = text;
    }
    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
   
}
