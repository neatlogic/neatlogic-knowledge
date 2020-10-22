package codedriver.module.knowledge.dto;

import codedriver.module.knowledge.constvalue.KnowledgeType;

public class KnowledgeTypeVo {

    private String value;
    private String text;
    private int count;
    public KnowledgeTypeVo(KnowledgeType type) {
        this.value = type.getValue();
        this.text = type.getText();
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
    public int getCount() {
        return count;
    }
    public void setCount(int count) {
        this.count = count;
    }
}
