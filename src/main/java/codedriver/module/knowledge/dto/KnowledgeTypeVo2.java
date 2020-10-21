package codedriver.module.knowledge.dto;

import codedriver.module.knowledge.constvalue.KnowledgeType;

public class KnowledgeTypeVo2 {

    private String value;
    private String text;
    private String route;
    private int count;
    public KnowledgeTypeVo2(KnowledgeType type) {
        this.value = type.getValue();
        this.text = type.getText();
        this.route = type.getRoute();
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
    public int getCount() {
        return count;
    }
    public void setCount(int count) {
        this.count = count;
    }
}
