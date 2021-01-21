package codedriver.module.knowledge.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.dao.mapper.RoleMapper;
import codedriver.framework.dto.RoleVo;
import codedriver.framework.dto.TeamVo;
import codedriver.framework.dto.WorkAssignmentUnitVo;
import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentOperate;
import codedriver.module.knowledge.dao.mapper.*;
import codedriver.module.knowledge.dto.*;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.UserVo;
import codedriver.framework.file.dao.mapper.FileMapper;
import codedriver.framework.file.dto.FileVo;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotFoundException;
import codedriver.module.knowledge.exception.KnowledgeDocumentVersionNotFoundException;

@Service
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Autowired
    private KnowledgeDocumentAuditMapper knowledgeDocumentAuditMapper;

    @Autowired
    private KnowledgeTagMapper knowledgeTagMapper;
    
    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private KnowledgeDocumentTypeMapper knowledgeDocumentTypeMappper;
    
    @Autowired
    private KnowledgeCircleMapper knowledgeCircleMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private TeamMapper teamMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Override
    public int isDeletable(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo) {
        if(KnowledgeDocumentVersionStatus.SUBMITTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            return 0;
        }else if(KnowledgeDocumentVersionStatus.PASSED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            if(knowledgeDocumentVersionVo.getReviewer().equals(UserContext.get().getUserUuid())) {
                if(knowledgeDocumentMapper.checkIfTheVersionIsTheCurrentVersion(knowledgeDocumentVersionVo) == 0) {
                    return 1;
                }
            }
        }else {
            if(knowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid())) {
                return 1;
            }
        }
        return 0;
    }

    @Override
    public int isEditable(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo) {
        if(KnowledgeDocumentVersionStatus.DRAFT.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            if(knowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid())) {
                return 1;
            }
        }else if(KnowledgeDocumentVersionStatus.PASSED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            return knowledgeDocumentMapper.checkIfTheVersionIsTheCurrentVersion(knowledgeDocumentVersionVo);
        }else if(KnowledgeDocumentVersionStatus.REJECTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            if(knowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid())) {
                return 1;
            }
        }
        return 0;
    }

    @Override
    public int isReviewer(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo) {
        List<String> teamUuidList = teamMapper.getTeamUuidListByUserUuid(UserContext.get().getUserUuid(true));
        if(knowledgeDocumentMapper.checkUserIsApprover(knowledgeDocumentVersionVo.getKnowledgeCircleId(), UserContext.get().getUserUuid(true), teamUuidList, UserContext.get().getRoleUuidList()) > 0) {
            return 1;
        }
        return 0;
    }

    @Override
    public KnowledgeDocumentVo getKnowledgeDocumentDetailByKnowledgeDocumentVersionId(Long knowledgeDocumentVersionId) throws PermissionDeniedException {
        KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
        if(knowledgeDocumentVersionVo == null) {
            throw new KnowledgeDocumentVersionNotFoundException(knowledgeDocumentVersionId);
        }
        KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        if(knowledgeDocumentVo == null) {
            throw new KnowledgeDocumentNotFoundException(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        }
        knowledgeDocumentVersionVo.setKnowledgeCircleId(knowledgeDocumentVo.getKnowledgeCircleId());
        knowledgeDocumentVo.setIsReviewer(isReviewer(knowledgeDocumentVersionVo));
        knowledgeDocumentVo.setIsReviewable(0);
        knowledgeDocumentVo.setIsEditable(isEditable(knowledgeDocumentVersionVo));
        knowledgeDocumentVo.setIsDeletable(isDeletable(knowledgeDocumentVersionVo));
        if(KnowledgeDocumentVersionStatus.SUBMITTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())){
            if(knowledgeDocumentVo.getIsReviewer() == 0){
                if(!knowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid(true))) {
                    throw new PermissionDeniedException();
                }
            }else {
                knowledgeDocumentVo.setIsReviewable(1);
            }
            /** 查出审核人 **/
            List<WorkAssignmentUnitVo> reviewerVoList = new ArrayList<>();
            List<KnowledgeCircleUserVo> reviewerList = knowledgeCircleMapper.getKnowledgeCircleUserListByIdAndAuthType(knowledgeDocumentVo.getKnowledgeCircleId(), KnowledgeCircleUserVo.AuthType.APPROVER.getValue());
            for(KnowledgeCircleUserVo reviewer : reviewerList){
                if(reviewer.getType().equals(GroupSearch.USER.getValue())){
                    UserVo userVo = userMapper.getUserBaseInfoByUuid(reviewer.getUuid());
                    if(userVo != null && userVo.getIsActive() == 1){
                        reviewerVoList.add(new WorkAssignmentUnitVo(userVo));
                    }
                }else if(reviewer.getType().equals(GroupSearch.TEAM.getValue())){
                    TeamVo teamVo = teamMapper.getTeamByUuid(reviewer.getUuid());
                    if(teamVo != null){
                        reviewerVoList.add(new WorkAssignmentUnitVo(teamVo));
                    }
                }else if(reviewer.getType().equals(GroupSearch.ROLE.getValue())){
                    RoleVo roleVo = roleMapper.getRoleByUuid(reviewer.getUuid());
                    if(roleVo != null){
                        reviewerVoList.add(new WorkAssignmentUnitVo(roleVo));
                    }
                }
            }
            knowledgeDocumentVo.setReviewerVoList(reviewerVoList);
        }else if(knowledgeDocumentVersionVo.getStatus().equals(KnowledgeDocumentVersionStatus.DRAFT.getValue())){
            if(!knowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid(true))) {
                throw new PermissionDeniedException();
            }
        }else if(knowledgeDocumentVersionVo.getStatus().equals(KnowledgeDocumentVersionStatus.REJECTED.getValue())){
            if(!knowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid(true))) {
                throw new PermissionDeniedException();
            }else {
                KnowledgeDocumentAuditVo  rejectAudit = knowledgeDocumentAuditMapper.getKnowledgeDocumentAuditListByDocumentIdAndVersionIdAndOperate(new KnowledgeDocumentAuditVo(knowledgeDocumentVersionVo.getKnowledgeDocumentId(),knowledgeDocumentVersionVo.getId(), KnowledgeDocumentOperate.REJECT.getValue()));
                if(rejectAudit != null) {
                    String rejectReason =knowledgeDocumentAuditMapper.getKnowledgeDocumentAuditConfigStringByHash(rejectAudit.getConfigHash());
                    if(StringUtils.isNotBlank(rejectReason)) {
                        knowledgeDocumentVo.setRejectReason((String) JSONPath.read(rejectReason,"content"));
                    }
                }
                if(StringUtils.isNotBlank(knowledgeDocumentVersionVo.getReviewer())){
                    UserVo userVo = userMapper.getUserBaseInfoByUuid(knowledgeDocumentVersionVo.getReviewer());
                    if(userVo != null && userVo.getIsActive() == 1){
                        knowledgeDocumentVo.setReviewerVo(new WorkAssignmentUnitVo(userVo));
                    }
                }
            }
        }
        if(Objects.equals(knowledgeDocumentVersionId, knowledgeDocumentVo.getKnowledgeDocumentVersionId())) {
            knowledgeDocumentVo.setIsCurrentVersion(1);
        }else {
            knowledgeDocumentVo.setKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
            knowledgeDocumentVo.setIsCurrentVersion(0);
        }
        knowledgeDocumentVo.setVersion(knowledgeDocumentVersionVo.getVersion() != null ? knowledgeDocumentVersionVo.getVersion() : knowledgeDocumentVersionVo.getFromVersion());
        knowledgeDocumentVo.setLcu(knowledgeDocumentVersionVo.getLcu());
        knowledgeDocumentVo.setLcd(knowledgeDocumentVersionVo.getLcd());
        UserVo lcuUserVo = userMapper.getUserBaseInfoByUuid(knowledgeDocumentVersionVo.getLcu());
        if(lcuUserVo != null) {
            //使用新对象，防止缓存
            UserVo vo = new UserVo();
            BeanUtils.copyProperties(lcuUserVo,vo);
            knowledgeDocumentVo.setLcuVo(vo);
//            knowledgeDocumentVo.setLcuName(lcuUserVo.getUserName());
//            knowledgeDocumentVo.setLcuInfo(lcuUserVo.getUserInfo());
        }
        List<KnowledgeDocumentLineVo> lineList = knowledgeDocumentMapper.getKnowledgeDocumentLineListByKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
        knowledgeDocumentVo.setLineList(lineList);
        List<Long> fileIdList = knowledgeDocumentMapper.getKnowledgeDocumentFileIdListByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentFileVo(knowledgeDocumentVo.getId(), knowledgeDocumentVersionId));
        if(CollectionUtils.isNotEmpty(fileIdList)) {
            List<FileVo> fileList = fileMapper.getFileListByIdList(fileIdList);
            knowledgeDocumentVo.setFileIdList(fileIdList);
            knowledgeDocumentVo.setFileList(fileList);
        }
        List<Long> tagIdList = knowledgeDocumentMapper.getKnowledgeDocumentTagIdListByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentTagVo(knowledgeDocumentVo.getId(), knowledgeDocumentVersionId));
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
        return knowledgeDocumentVo;
    }

}
