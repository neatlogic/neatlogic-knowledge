package codedriver.module.knowledge.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.alibaba.fastjson.annotation.JSONField;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BaseEditorVo;
import codedriver.framework.dto.TagVo;
import codedriver.framework.file.dto.FileVo;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;

public class KnowledgeDocumentVo extends BaseEditorVo {

    @EntityField(name = "文档id", type = ApiParamType.LONG)
    private Long id;
    @EntityField(name = "版本id", type = ApiParamType.LONG)
    private Long knowledgeDocumentVersionId;
    @EntityField(name = "类型id", type = ApiParamType.LONG)
    private Long knowledgeTypeId;
    @EntityField(name = "知识圈id", type = ApiParamType.LONG)
    private Long knowledgeCircleId;
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
    @EntityField(name = "修改者额外属性", type = ApiParamType.STRING)
    private String reviewerInfo;
    @EntityField(name = "修改者头像", type = ApiParamType.STRING)
    private String reviewerAvatar;
    @EntityField(name = "审核时间", type = ApiParamType.LONG)
    private Date reviewerTime;
    @EntityField(name = "行数据列表", type = ApiParamType.JSONARRAY)
    private List<KnowledgeDocumentLineVo> lineList = new ArrayList<>();
    @EntityField(name = "附件列表", type = ApiParamType.JSONARRAY)
    private List<FileVo> fileList = new ArrayList<>();
    @EntityField(name = "标签列表", type = ApiParamType.JSONARRAY)
    private List<TagVo> tagList = new ArrayList<>();
    @JSONField(serialize=false)
    private transient List<Long> fileIdList = new ArrayList<>();
    @JSONField(serialize=false)
    private transient List<Long> tagIdList = new ArrayList<>();
    private transient Integer isDelete;
    @JSONField(serialize=false)
    private transient boolean isAutoGenerateId = true;
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
    public Long getKnowledgeDocumentVersionId() {
        return knowledgeDocumentVersionId;
    }
    public void setKnowledgeDocumentVersionId(Long knowledgeDocumentVersionId) {
        this.knowledgeDocumentVersionId = knowledgeDocumentVersionId;
    }
    public Long getKnowledgeTypeId() {
        return knowledgeTypeId;
    }
    public void setKnowledgeTypeId(Long knowledgeTypeId) {
        this.knowledgeTypeId = knowledgeTypeId;
    }
    public Long getKnowledgeCircleId() {
        return knowledgeCircleId;
    }
    public void setKnowledgeCircleId(Long knowledgeCircleId) {
        this.knowledgeCircleId = knowledgeCircleId;
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
    public Date getReviewerTime() {
        return reviewerTime;
    }
    public void setReviewerTime(Date reviewerTime) {
        this.reviewerTime = reviewerTime;
    }
    public List<KnowledgeDocumentLineVo> getLineList() {
        return lineList;
    }
    public void setLineList(List<KnowledgeDocumentLineVo> lineList) {
        this.lineList = lineList;
    }
    public List<FileVo> getFileList() {
        return fileList;
    }
    public void setFileList(List<FileVo> fileList) {
        this.fileList = fileList;
    }
    public List<TagVo> getTagList() {
        return tagList;
    }
    public void setTagList(List<TagVo> tagList) {
        this.tagList = tagList;
    }
    public List<Long> getFileIdList() {
        return fileIdList;
    }
    public void setFileIdList(List<Long> fileIdList) {
        this.fileIdList = fileIdList;
    }
    public List<Long> getTagIdList() {
        return tagIdList;
    }
    public void setTagIdList(List<Long> tagIdList) {
        this.tagIdList = tagIdList;
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
        return reviewerAvatar;
    }
    public void setReviewerAvatar(String reviewerAvatar) {
        this.reviewerAvatar = reviewerAvatar;
    }
    public Integer getIsDelete() {
        return isDelete;
    }
    public void setIsDelete(Integer isDelete) {
        this.isDelete = isDelete;
    }
    
}
