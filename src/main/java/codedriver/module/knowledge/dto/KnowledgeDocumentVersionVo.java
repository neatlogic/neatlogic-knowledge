package codedriver.module.knowledge.dto;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BaseEditorVo;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;

public class KnowledgeDocumentVersionVo extends BaseEditorVo {

    @EntityField(name = "版本id", type = ApiParamType.LONG)
    private Long id;
    @EntityField(name = "文档id", type = ApiParamType.LONG)
    private Long knowledgeDocumentId;
    @EntityField(name = "版本号", type = ApiParamType.STRING)
    private Integer version;
    @EntityField(name = "版本名", type = ApiParamType.STRING)
    private String versionName;
    @EntityField(name = "状态", type = ApiParamType.STRING)
    private String status;
    @EntityField(name = "状态信息", type = ApiParamType.JSONOBJECT)
    private KnowledgeDocumentVersionStatusVo statusVo;
    @EntityField(name = "标题", type = ApiParamType.STRING)
    private String title;
    @EntityField(name = "文档大小，单位是字节", type = ApiParamType.INTEGER)
    private Integer size;
    @EntityField(name = "文档大小描述", type = ApiParamType.STRING)
    private Integer sizeDesc;
    @EntityField(name = "审核人", type = ApiParamType.STRING)
    private String reviewer;
    @EntityField(name = "审核人名", type = ApiParamType.STRING)
    private String reviewerName;
    @EntityField(name = "审核人额外属性", type = ApiParamType.STRING)
    private String reviewerInfo;
    @EntityField(name = "审核人头像", type = ApiParamType.STRING)
    private String reviewerAvatar;
    @EntityField(name = "审核时间", type = ApiParamType.LONG)
    private Date reviewTime;
    @EntityField(name = "是否可编辑", type = ApiParamType.INTEGER)
    private Integer isEditable;
    @EntityField(name = "是否可删除", type = ApiParamType.INTEGER)
    private Integer isDeletable;
    @JSONField(serialize=false)
    private transient boolean isAutoGenerateId = true;
    @JSONField(serialize=false)
    private transient List<String> statusList;
    public boolean isAutoGenerateId() {
        return isAutoGenerateId;
    }
    public void setAutoGenerateId(boolean isAutoGenerateId) {
        this.isAutoGenerateId = isAutoGenerateId;
    }
    public synchronized Long getId() {
        if(id == null && isAutoGenerateId) {
            id = SnowflakeUtil.uniqueLong();
        }
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getKnowledgeDocumentId() {
        return knowledgeDocumentId;
    }
    public void setKnowledgeDocumentId(Long knowledgeDocumentId) {
        this.knowledgeDocumentId = knowledgeDocumentId;
    }
    public Integer getVersion() {
        return version;
    }
    public void setVersion(Integer version) {
        this.version = version;
    }
    public String getVersionName() {
        if(StringUtils.isBlank(versionName) && version != null) {
            versionName = "版本" + version;
        }
        return versionName;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public KnowledgeDocumentVersionStatusVo getStatusVo() {
        if(statusVo == null && StringUtils.isNotBlank(status)) {
            statusVo = new KnowledgeDocumentVersionStatusVo(status, KnowledgeDocumentVersionStatus.getText(status));
        }
        return statusVo;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public Integer getSize() {
        return size;
    }
    public void setSize(Integer size) {
        this.size = size;
    }
    public String getReviewer() {
        return reviewer;
    }
    public void setReviewer(String reviewer) {
        this.reviewer = reviewer;
    }
    public Date getReviewTime() {
        return reviewTime;
    }
    public void setReviewTime(Date reviewerTime) {
        this.reviewTime = reviewerTime;
    }
    public Integer getSizeDesc() {
        return sizeDesc;
    }
    public void setSizeDesc(Integer sizeDesc) {
        this.sizeDesc = sizeDesc;
    }
    public String getReviewerName() {
        return reviewerName;
    }
    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }
    public String getReviewerInfo() {
        return reviewerInfo;
    }
    public void setReviewerInfo(String reviewerInfo) {
        this.reviewerInfo = reviewerInfo;
    }
    public String getReviewerAvatar() {
        if (StringUtils.isBlank(reviewerAvatar) && StringUtils.isNotBlank(reviewerInfo)) {
            JSONObject jsonObject = JSONObject.parseObject(reviewerInfo);
            reviewerAvatar = jsonObject.getString("avatar");
        }
        return reviewerAvatar;
    }
    public void setReviewerAvatar(String reviewerAvatar) {
        this.reviewerAvatar = reviewerAvatar;
    }
    public List<String> getStatusList() {
        return statusList;
    }
    public void setStatusList(List<String> statusList) {
        this.statusList = statusList;
    }
    public Integer getIsEditable() {
        return isEditable;
    }
    public void setIsEditable(Integer isEditable) {
        this.isEditable = isEditable;
    }
    public Integer getIsDeletable() {
        return isDeletable;
    }
    public void setIsDeletable(Integer isDeletable) {
        this.isDeletable = isDeletable;
    }
}
