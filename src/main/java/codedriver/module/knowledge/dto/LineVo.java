package codedriver.module.knowledge.dto;

import com.alibaba.fastjson.JSONObject;

public class LineVo {

    private Integer lineNumber;
    private String type;
    private String uuid;
    private String content;
    private String changeType;
    private JSONObject config;
    public LineVo() {
    }
    public LineVo(Integer lineNumber, String type, String content) {
        this.lineNumber = lineNumber;
        this.type = type;
        this.content = content;
    }
    public Integer getLineNumber() {
        return lineNumber;
    }
    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public String getChangeType() {
        return changeType;
    }
    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }
    public JSONObject getConfig() {
        return config;
    }
    public void setConfig(JSONObject config) {
        this.config = config;
    }
    
//    @Override
//    public String toString() {
//        return "oldLineList.add(new LineVo(" + lineNumber + ", \""+ type +"\", \"" + content + "\"));";
//    }
}
