package codedriver.module.knowledge.dto;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.annotation.JSONField;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BaseEditorVo;
import codedriver.framework.elasticsearch.annotation.ESKey;
import codedriver.framework.elasticsearch.constvalue.ESKeyType;
import codedriver.framework.file.dto.FileVo;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;

public class KnowledgeDocumentVo extends BaseEditorVo {
    @ESKey(type = ESKeyType.PKEY, name = "documentId")
    @EntityField(name = "文档id", type = ApiParamType.LONG)
    private Long id;
    @EntityField(name = "版本id", type = ApiParamType.LONG)
    private Long knowledgeDocumentVersionId;
    @EntityField(name = "版本号", type = ApiParamType.STRING)
    private Integer version;
    @EntityField(name = "类型uuid", type = ApiParamType.STRING)
    private String knowledgeDocumentTypeUuid;
    @EntityField(name = "知识圈id", type = ApiParamType.LONG)
    private Long knowledgeCircleId;
    @EntityField(name = "标题", type = ApiParamType.STRING)
    private String title;
//    @EntityField(name = "文档大小，单位是字节", type = ApiParamType.INTEGER)
//    private Integer size;
//    @EntityField(name = "文档大小描述", type = ApiParamType.STRING)
//    private Integer sizeDesc;
//    @EntityField(name = "审核人", type = ApiParamType.STRING)
//    private String reviewer;
//    @EntityField(name = "审核人名", type = ApiParamType.STRING)
//    private String reviewerName;
//    @EntityField(name = "修改者额外属性", type = ApiParamType.STRING)
//    private String reviewerInfo;
//    @EntityField(name = "修改者头像", type = ApiParamType.STRING)
//    private String reviewerAvatar;
//    @EntityField(name = "审核时间", type = ApiParamType.LONG)
//    private Date reviewerTime;
    @EntityField(name = "行数据列表", type = ApiParamType.JSONARRAY)
    private List<KnowledgeDocumentLineVo> lineList = new ArrayList<>();
    @EntityField(name = "附件列表", type = ApiParamType.JSONARRAY)
    private List<FileVo> fileList = new ArrayList<>();
    @EntityField(name = "标签列表", type = ApiParamType.JSONARRAY)
    private List<String> tagList = new ArrayList<>();
    @EntityField(name = "是否可编辑", type = ApiParamType.INTEGER)
    private Integer isEditable;
    @EntityField(name = "是否可删除", type = ApiParamType.INTEGER)
    private Integer isDeletable;
    @EntityField(name = "是否可审核", type = ApiParamType.INTEGER)
    private Integer isReviewable;
    private List<Long> fileIdList = new ArrayList<>();
    private List<Long> tagIdList = new ArrayList<>();
    @EntityField(name = "是否收藏", type = ApiParamType.INTEGER)
    private int isCollect;
    @EntityField(name = "是否点赞", type = ApiParamType.INTEGER)
    private int isFavor;
    @EntityField(name = "点赞量", type = ApiParamType.INTEGER)
    private int favorCount;
    @EntityField(name = "收藏量", type = ApiParamType.INTEGER)
    private int collectCount;
    @EntityField(name = "浏览量", type = ApiParamType.INTEGER)
    private int pageviews;
    @EntityField(name = "知识内容", type = ApiParamType.STRING)
    private String content;
    @EntityField(name = "路径", type = ApiParamType.JSONARRAY)
    private List<String> path = new ArrayList<>();
    @EntityField(name = "是否是当前版本", type = ApiParamType.INTEGER)
    private Integer isCurrentVersion;
    @JSONField(serialize=false)
    private transient Integer isDelete;
    @JSONField(serialize=false)
    private transient boolean isAutoGenerateId = true;
    @JSONField(serialize=false)
    private transient String type;
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
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
    public Integer getVersion() {
        return version;
    }
    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getKnowledgeDocumentTypeUuid() {
        return knowledgeDocumentTypeUuid;
    }

    public void setKnowledgeDocumentTypeUuid(String knowledgeDocumentTypeUuid) {
        this.knowledgeDocumentTypeUuid = knowledgeDocumentTypeUuid;
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
    public List<String> getTagList() {
        return tagList;
    }
    public void setTagList(List<String> tagList) {
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
    public Integer getIsDelete() {
        return isDelete;
    }
    public void setIsDelete(Integer isDelete) {
        this.isDelete = isDelete;
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
    public Integer getIsReviewable() {
        return isReviewable;
    }
    public void setIsReviewable(Integer isReviewable) {
        this.isReviewable = isReviewable;
    }
    public int getIsCollect() {
        return isCollect;
    }
    public void setIsCollect(int isCollect) {
        this.isCollect = isCollect;
    }
    public int getIsFavor() {
        return isFavor;
    }
    public void setIsFavor(int isFavor) {
        this.isFavor = isFavor;
    }
    public int getFavorCount() {
        return favorCount;
    }
    public void setFavorCount(int favorCount) {
        this.favorCount = favorCount;
    }
    public int getCollectCount() {
        return collectCount;
    }
    public void setCollectCount(int collectCount) {
        this.collectCount = collectCount;
    }
    public int getPageviews() {
        return pageviews;
    }
    public void setPageviews(int pageviews) {
        this.pageviews = pageviews;
    }
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public List<String> getPath() {
        return path;
    }
    public void setPath(List<String> path) {
        this.path = path;
    }
    public Integer getIsCurrentVersion() {
        return isCurrentVersion;
    }
    public void setIsCurrentVersion(Integer isCurrentVersion) {
        this.isCurrentVersion = isCurrentVersion;
    }
    
}
