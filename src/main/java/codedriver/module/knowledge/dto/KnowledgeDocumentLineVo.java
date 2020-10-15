package codedriver.module.knowledge.dto;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.EntityField;

public class KnowledgeDocumentLineVo {

    @EntityField(name = "标题", type = ApiParamType.STRING)
    private String uuid;
    private String handler;
    private String content;
    private String changeType;
    private JSONObject config;
    private Integer lineNumber;
    private transient Long knowledgeDocumentId;
    private transient Long knowledgeDocumentVersionId;
    private transient String contentHash;
    private transient String configHash;
    public KnowledgeDocumentLineVo() {
    }
    public KnowledgeDocumentLineVo(Integer lineNumber, String handler, String content) {
        this.lineNumber = lineNumber;
        this.handler = handler;
        this.content = content;
    }
    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    public String getHandler() {
        return handler;
    }
    public void setHandler(String handler) {
        this.handler = handler;
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
    public Integer getLineNumber() {
        return lineNumber;
    }
    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }
    public Long getKnowledgeDocumentId() {
        return knowledgeDocumentId;
    }
    public void setKnowledgeDocumentId(Long knowledgeDocumentId) {
        this.knowledgeDocumentId = knowledgeDocumentId;
    }
    public Long getKnowledgeDocumentVersionId() {
        return knowledgeDocumentVersionId;
    }
    public void setKnowledgeDocumentVersionId(Long knowledgeDocumentVersionId) {
        this.knowledgeDocumentVersionId = knowledgeDocumentVersionId;
    }
    public String getContentHash() {
        return contentHash;
    }
    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }
    public String getConfigHash() {
        return configHash;
    }
    public void setConfigHash(String configHash) {
        this.configHash = configHash;
    }
    
//    @Override
//    public String toString() {
//        return "oldLineList.add(new LineVo(" + lineNumber + ", \""+ type +"\", \"" + content + "\"));";
//    }
}
