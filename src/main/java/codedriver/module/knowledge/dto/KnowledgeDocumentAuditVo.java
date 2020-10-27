package codedriver.module.knowledge.dto;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BaseEditorVo;
import codedriver.framework.restful.annotation.EntityField;

public class KnowledgeDocumentAuditVo extends BaseEditorVo {
    
    @EntityField(name = "文档id", type = ApiParamType.LONG)
    private Long KnowledgeDocumentId;
    @EntityField(name = "操作类型", type = ApiParamType.STRING)
    private String operate;
    @EntityField(name = "操作描述", type = ApiParamType.STRING)
    private String title;
    @EntityField(name = "内容", type = ApiParamType.STRING)
    private String content;
    private transient JSONObject config;
    private transient String configHash;
    public Long getKnowledgeDocumentId() {
        return KnowledgeDocumentId;
    }
    public void setKnowledgeDocumentId(Long knowledgeDocumentId) {
        KnowledgeDocumentId = knowledgeDocumentId;
    }
    public String getOperate() {
        return operate;
    }
    public void setOperate(String operate) {
        this.operate = operate;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public JSONObject getConfig() {
        return config;
    }
    public void setConfig(JSONObject config) {
        this.config = config;
    }
    public String getConfigHash() {
        return configHash;
    }
    public void setConfigHash(String configHash) {
        this.configHash = configHash;
    }
}
