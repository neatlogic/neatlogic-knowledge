package neatlogic.framework.knowledge.dto;

import java.util.Date;

public class KnowledgeFeishuSyncDocumentVo {
    private Long configId;
    private String nodeToken;
    private String objToken;
    private String objType;
    private Long knowledgeDocumentId;
    private String knowledgeDocumentTypeUuid;
    private String title;
    private String feishuUpdateTime;
    private Date lastSyncTime;

    public Long getConfigId() { return configId; }
    public void setConfigId(Long configId) { this.configId = configId; }
    public String getNodeToken() { return nodeToken; }
    public void setNodeToken(String nodeToken) { this.nodeToken = nodeToken; }
    public String getObjToken() { return objToken; }
    public void setObjToken(String objToken) { this.objToken = objToken; }
    public String getObjType() { return objType; }
    public void setObjType(String objType) { this.objType = objType; }
    public Long getKnowledgeDocumentId() { return knowledgeDocumentId; }
    public void setKnowledgeDocumentId(Long knowledgeDocumentId) { this.knowledgeDocumentId = knowledgeDocumentId; }
    public String getKnowledgeDocumentTypeUuid() { return knowledgeDocumentTypeUuid; }
    public void setKnowledgeDocumentTypeUuid(String knowledgeDocumentTypeUuid) { this.knowledgeDocumentTypeUuid = knowledgeDocumentTypeUuid; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getFeishuUpdateTime() { return feishuUpdateTime; }
    public void setFeishuUpdateTime(String feishuUpdateTime) { this.feishuUpdateTime = feishuUpdateTime; }
    public Date getLastSyncTime() { return lastSyncTime; }
    public void setLastSyncTime(Date lastSyncTime) { this.lastSyncTime = lastSyncTime; }
}
