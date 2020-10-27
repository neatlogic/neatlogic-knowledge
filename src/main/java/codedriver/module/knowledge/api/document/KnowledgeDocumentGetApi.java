package codedriver.module.knowledge.api.document;

import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.UserVo;
import codedriver.framework.file.dao.mapper.FileMapper;
import codedriver.framework.file.dto.FileVo;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.module.knowledge.dao.mapper.KnowledgeCircleMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeTagMapper;
import codedriver.module.knowledge.dto.KnowledgeCircleVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentFileVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentLineVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentTagVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentTypeVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotFoundException;
import codedriver.module.knowledge.exception.KnowledgeDocumentVersionNotFoundException;
import codedriver.module.knowledge.service.KnowledgeDocumentService;
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentGetApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Autowired
    private KnowledgeTagMapper knowledgeTagMapper;
    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

    @Autowired
    private KnowledgeDocumentTypeMapper knowledgeDocumentTypeMappper;
    
    @Autowired
    private KnowledgeCircleMapper knowledgeCircleMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Override
    public String getToken() {
        return "knowledge/document/get";
    }

    @Override
    public String getName() {
        return "查询文档内容";
    }

    @Override
    public String getConfig() {
        return null;
    }
    
    @Input({
        @Param(name = "knowledgeDocumentId", type = ApiParamType.LONG, isRequired = true, desc = "文档id"),
        @Param(name = "knowledgeDocumentVersionId", type = ApiParamType.LONG, desc = "版本id"),
        @Param(name = "isReadOnly", type = ApiParamType.ENUM, rule = "0,1", desc = "是否增加浏览量")
    })
    @Output({
        @Param(explode = KnowledgeDocumentVo.class, desc = "文档内容")
    })
    @Description(desc = "查询文档内容")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long knowledgeDocumentId = jsonObj.getLong("knowledgeDocumentId");
        KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(knowledgeDocumentId);
        if(knowledgeDocumentVo == null) {
            throw new KnowledgeDocumentNotFoundException(knowledgeDocumentId);
        }
        Long knowledgeDocumentVersionId = jsonObj.getLong("knowledgeDocumentVersionId");
        if(knowledgeDocumentVersionId == null) {
            knowledgeDocumentVersionId = knowledgeDocumentVo.getKnowledgeDocumentVersionId();
            knowledgeDocumentVo.setIsCurrentVersion(1);
        }else {
            if(Objects.equals(knowledgeDocumentVersionId, knowledgeDocumentVo.getKnowledgeDocumentVersionId())) {
                knowledgeDocumentVo.setIsCurrentVersion(1);
            }else {
                knowledgeDocumentVo.setKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
                knowledgeDocumentVo.setIsCurrentVersion(0);
            }
        }
        KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
        if(knowledgeDocumentVersionVo == null) {
            throw new KnowledgeDocumentVersionNotFoundException(knowledgeDocumentVersionId);
        }
        knowledgeDocumentVo.setTitle(knowledgeDocumentVersionVo.getTitle());
        knowledgeDocumentVo.setKnowledgeDocumentTypeUuid(knowledgeDocumentVersionVo.getKnowledgeDocumentTypeUuid());
        knowledgeDocumentVo.setLcu(knowledgeDocumentVersionVo.getLcu());
        UserVo lcuUserVo = userMapper.getUserBaseInfoByUuid(knowledgeDocumentVersionVo.getLcu());
        if(lcuUserVo != null) {
            knowledgeDocumentVo.setLcuName(lcuUserVo.getUserName());
            knowledgeDocumentVo.setLcuInfo(lcuUserVo.getUserInfo());
        }
        List<KnowledgeDocumentLineVo> lineList = knowledgeDocumentMapper.getKnowledgeDocumentLineListByKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
        knowledgeDocumentVo.setLineList(lineList);
        List<Long> fileIdList = knowledgeDocumentMapper.getKnowledgeDocumentFileIdListByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentFileVo(knowledgeDocumentId, knowledgeDocumentVersionId));
        if(CollectionUtils.isNotEmpty(fileIdList)) {
            List<FileVo> fileList = fileMapper.getFileListByIdList(fileIdList);
            knowledgeDocumentVo.setFileIdList(fileIdList);
            knowledgeDocumentVo.setFileList(fileList);
        }
        List<Long> tagIdList = knowledgeDocumentMapper.getKnowledgeDocumentTagIdListByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentTagVo(knowledgeDocumentId, knowledgeDocumentVersionId));
        if(CollectionUtils.isNotEmpty(tagIdList)) {
            List<String> tagNameList = knowledgeTagMapper.getKnowledgeTagNameListByIdList(tagIdList);
            knowledgeDocumentVo.setTagList(tagNameList);
        }
        
        KnowledgeCircleVo knowledgeCircleVo = knowledgeCircleMapper.getKnowledgeCircleById(knowledgeDocumentVo.getKnowledgeCircleId());
        if(knowledgeCircleVo != null) {
            knowledgeDocumentVo.getPath().add(knowledgeCircleVo.getName());
        }
        KnowledgeDocumentTypeVo knowledgeDocumentTypeVo = knowledgeDocumentTypeMappper.getTypeByUuid(knowledgeDocumentVo.getKnowledgeDocumentTypeUuid());
        if(knowledgeDocumentTypeVo != null) {
            List<String> typeNameList = knowledgeDocumentTypeMappper.getAncestorsAndSelfNameByLftRht(knowledgeDocumentTypeVo.getLft(), knowledgeDocumentTypeVo.getRht(), knowledgeDocumentTypeVo.getKnowledgeCircleId());
            if(CollectionUtils.isNotEmpty(typeNameList)) {
                knowledgeDocumentVo.getPath().addAll(typeNameList);
            }
        }
        
        knowledgeDocumentVo.setFavorCount(knowledgeDocumentMapper.getDocumentFavorCount(knowledgeDocumentVo.getId()));
        knowledgeDocumentVo.setCollectCount(knowledgeDocumentMapper.getDocumentCollectCount(knowledgeDocumentVo.getId()));
        knowledgeDocumentVo.setPageviews(knowledgeDocumentMapper.getDocumentViewCount(knowledgeDocumentVo.getId()));
        knowledgeDocumentVo.setIsCollect(knowledgeDocumentMapper.checkDocumentHasBeenCollected(knowledgeDocumentVo.getId(), UserContext.get().getUserUuid(true)));
        knowledgeDocumentVo.setIsFavor(knowledgeDocumentMapper.checkDocumentHasBeenFavored(knowledgeDocumentVo.getId(), UserContext.get().getUserUuid(true)));
        
        knowledgeDocumentVo.setIsEditable(knowledgeDocumentService.isEditable(knowledgeDocumentVersionVo));
        knowledgeDocumentVo.setIsDeletable(knowledgeDocumentService.isDeletable(knowledgeDocumentVersionVo));
        knowledgeDocumentVo.setIsReviewable(knowledgeDocumentService.isReviewable(knowledgeDocumentVersionVo));
        
        Integer isReadOnly = jsonObj.getInteger("isReadOnly");
        if(Objects.equals(isReadOnly, 1)) {
            if(KnowledgeDocumentVersionStatus.PASSED.getValue().equals(knowledgeDocumentVersionVo.getStatus()) && Objects.equals(knowledgeDocumentVersionId, knowledgeDocumentVo.getKnowledgeDocumentVersionId())) {
                knowledgeDocumentMapper.updateKnowledgeViewCountIncrementOne(knowledgeDocumentVo.getId());
            }
        }
        return knowledgeDocumentVo;
    }

}
