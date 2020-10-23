package codedriver.module.knowledge.dto;

public class KnowledgeDocumentVersionStatusVo {
    private final String value;
    private final String text;
 
    public KnowledgeDocumentVersionStatusVo(String value, String text) {
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
