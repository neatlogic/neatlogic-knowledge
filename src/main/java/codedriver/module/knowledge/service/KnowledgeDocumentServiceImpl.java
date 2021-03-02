package codedriver.module.knowledge.service;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.dao.mapper.RoleMapper;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.RoleVo;
import codedriver.framework.dto.TeamVo;
import codedriver.framework.dto.UserVo;
import codedriver.framework.dto.WorkAssignmentUnitVo;
import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.framework.file.dao.mapper.FileMapper;
import codedriver.framework.file.dto.FileVo;
import codedriver.framework.fulltextindex.dao.mapper.FullTextIndexMapper;
import codedriver.framework.fulltextindex.dto.FullTextIndexContentVo;
import codedriver.framework.fulltextindex.dto.FullTextIndexVo;
import codedriver.framework.util.HtmlUtil;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentOperate;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.module.knowledge.dao.mapper.*;
import codedriver.module.knowledge.dto.*;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotFoundException;
import codedriver.module.knowledge.exception.KnowledgeDocumentVersionNotFoundException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    private KnowledgeDocumentAuditMapper knowledgeDocumentAuditMapper;

    @Resource
    private KnowledgeTagMapper knowledgeTagMapper;

    @Resource
    private FileMapper fileMapper;

    @Resource
    private KnowledgeDocumentTypeMapper knowledgeDocumentTypeMappper;

    @Resource
    private KnowledgeCircleMapper knowledgeCircleMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private TeamMapper teamMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private FullTextIndexMapper ftIndexMapper;

    @Override
    public int isDeletable(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo) {
        if (KnowledgeDocumentVersionStatus.SUBMITTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            return 0;
        } else if (KnowledgeDocumentVersionStatus.PASSED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            if (knowledgeDocumentVersionVo.getReviewer().equals(UserContext.get().getUserUuid())) {
                if (knowledgeDocumentMapper.checkIfTheVersionIsTheCurrentVersion(knowledgeDocumentVersionVo) == 0) {
                    return 1;
                }
            }
        } else {
            if (knowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid())) {
                return 1;
            }
        }
        return 0;
    }

    @Override
    public int isEditable(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo) {
        if(isMember(knowledgeDocumentVersionVo.getKnowledgeCircleId()) == 0){
            return 0;
        }
        if (KnowledgeDocumentVersionStatus.DRAFT.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            if (knowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid())) {
                return 1;
            }
        } else if (KnowledgeDocumentVersionStatus.PASSED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            return knowledgeDocumentMapper.checkIfTheVersionIsTheCurrentVersion(knowledgeDocumentVersionVo);
        } else if (KnowledgeDocumentVersionStatus.REJECTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            if (knowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid())) {
                return 1;
            }
        }
        return 0;
    }

    @Override
    public int isReviewer(Long knowledgeCircleId) {
        List<String> teamUuidList = teamMapper.getTeamUuidListByUserUuid(UserContext.get().getUserUuid(true));
        if (knowledgeDocumentMapper.checkUserIsApprover(knowledgeCircleId, UserContext.get().getUserUuid(true), teamUuidList, UserContext.get().getRoleUuidList()) > 0) {
            return 1;
        }
        return 0;
    }

    @Override
    public int isMember(Long knowledgeCircleId) {
        List<String> teamUuidList = teamMapper.getTeamUuidListByUserUuid(UserContext.get().getUserUuid(true));
        if (knowledgeDocumentMapper.checkUserIsMember(knowledgeCircleId, UserContext.get().getUserUuid(true), teamUuidList, UserContext.get().getRoleUuidList()) > 0) {
            return 1;
        }
        return 0;
    }

    @Override
    public KnowledgeDocumentVo getKnowledgeDocumentDetailByKnowledgeDocumentVersionId(Long knowledgeDocumentVersionId) throws PermissionDeniedException {
        KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
        if (knowledgeDocumentVersionVo == null) {
            throw new KnowledgeDocumentVersionNotFoundException(knowledgeDocumentVersionId);
        }
        KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        if (knowledgeDocumentVo == null) {
            throw new KnowledgeDocumentNotFoundException(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        }
        knowledgeDocumentVersionVo.setKnowledgeCircleId(knowledgeDocumentVo.getKnowledgeCircleId());
        knowledgeDocumentVo.setIsReviewer(isReviewer(knowledgeDocumentVo.getKnowledgeCircleId()));
        knowledgeDocumentVo.setIsMember(isMember(knowledgeDocumentVo.getKnowledgeCircleId()));
        knowledgeDocumentVo.setIsReviewable(0);
        knowledgeDocumentVo.setIsEditable(isEditable(knowledgeDocumentVersionVo));
        knowledgeDocumentVo.setIsDeletable(isDeletable(knowledgeDocumentVersionVo));
        if (KnowledgeDocumentVersionStatus.SUBMITTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            if (knowledgeDocumentVo.getIsReviewer() == 0) {
                if (!knowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid(true))) {
                    throw new PermissionDeniedException();
                }
            } else {
                knowledgeDocumentVo.setIsReviewable(1);
            }
            /** 查出审核人 **/
            List<WorkAssignmentUnitVo> reviewerVoList = new ArrayList<>();
            List<KnowledgeCircleUserVo> reviewerList = knowledgeCircleMapper.getKnowledgeCircleUserListByIdAndAuthType(knowledgeDocumentVo.getKnowledgeCircleId(), KnowledgeCircleUserVo.AuthType.APPROVER.getValue());
            for (KnowledgeCircleUserVo reviewer : reviewerList) {
                if (reviewer.getType().equals(GroupSearch.USER.getValue())) {
                    UserVo userVo = userMapper.getUserBaseInfoByUuid(reviewer.getUuid());
                    if (userVo != null && userVo.getIsActive() == 1) {
                        reviewerVoList.add(new WorkAssignmentUnitVo(userVo));
                    }
                } else if (reviewer.getType().equals(GroupSearch.TEAM.getValue())) {
                    TeamVo teamVo = teamMapper.getTeamByUuid(reviewer.getUuid());
                    if (teamVo != null) {
                        reviewerVoList.add(new WorkAssignmentUnitVo(teamVo));
                    }
                } else if (reviewer.getType().equals(GroupSearch.ROLE.getValue())) {
                    RoleVo roleVo = roleMapper.getRoleByUuid(reviewer.getUuid());
                    if (roleVo != null) {
                        reviewerVoList.add(new WorkAssignmentUnitVo(roleVo));
                    }
                }
            }
            knowledgeDocumentVo.setReviewerVoList(reviewerVoList);
        } else if (knowledgeDocumentVersionVo.getStatus().equals(KnowledgeDocumentVersionStatus.DRAFT.getValue())) {
            if (!knowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid(true))) {
                throw new PermissionDeniedException();
            }
        } else if (knowledgeDocumentVersionVo.getStatus().equals(KnowledgeDocumentVersionStatus.REJECTED.getValue())) {
            if (!knowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid(true))) {
                throw new PermissionDeniedException();
            } else {
                KnowledgeDocumentAuditVo rejectAudit = knowledgeDocumentAuditMapper.getKnowledgeDocumentAuditListByDocumentIdAndVersionIdAndOperate(new KnowledgeDocumentAuditVo(knowledgeDocumentVersionVo.getKnowledgeDocumentId(), knowledgeDocumentVersionVo.getId(), KnowledgeDocumentOperate.REJECT.getValue()));
                if (rejectAudit != null) {
                    String rejectReason = knowledgeDocumentAuditMapper.getKnowledgeDocumentAuditConfigStringByHash(rejectAudit.getConfigHash());
                    if (StringUtils.isNotBlank(rejectReason)) {
                        knowledgeDocumentVo.setRejectReason((String) JSONPath.read(rejectReason, "content"));
                    }
                }
                if (StringUtils.isNotBlank(knowledgeDocumentVersionVo.getReviewer())) {
                    UserVo userVo = userMapper.getUserBaseInfoByUuid(knowledgeDocumentVersionVo.getReviewer());
                    if (userVo != null && userVo.getIsActive() == 1) {
                        knowledgeDocumentVo.setReviewerVo(new WorkAssignmentUnitVo(userVo));
                    }
                }
            }
        }
        if (Objects.equals(knowledgeDocumentVersionId, knowledgeDocumentVo.getKnowledgeDocumentVersionId())) {
            if (Objects.equals(knowledgeDocumentVo.getIsDelete(), 0) && Objects.equals(knowledgeDocumentVersionVo.getIsDelete(), 0)) {
                knowledgeDocumentVo.setIsCurrentVersion(1);
            } else {
                knowledgeDocumentVo.setIsCurrentVersion(0);
            }
        } else {
            knowledgeDocumentVo.setKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
            knowledgeDocumentVo.setIsCurrentVersion(0);
        }
        knowledgeDocumentVo.setVersion(knowledgeDocumentVersionVo.getVersion() != null ? knowledgeDocumentVersionVo.getVersion() : knowledgeDocumentVersionVo.getFromVersion());
        knowledgeDocumentVo.setLcu(knowledgeDocumentVersionVo.getLcu());
        knowledgeDocumentVo.setLcd(knowledgeDocumentVersionVo.getLcd());
        UserVo lcuUserVo = userMapper.getUserBaseInfoByUuid(knowledgeDocumentVersionVo.getLcu());
        if (lcuUserVo != null) {
            //使用新对象，防止缓存
            UserVo vo = new UserVo();
            BeanUtils.copyProperties(lcuUserVo, vo);
            knowledgeDocumentVo.setLcuVo(vo);
//            knowledgeDocumentVo.setLcuName(lcuUserVo.getUserName());
//            knowledgeDocumentVo.setLcuInfo(lcuUserVo.getUserInfo());
        }
        List<KnowledgeDocumentLineVo> lineList = knowledgeDocumentMapper.getKnowledgeDocumentLineListByKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
        knowledgeDocumentVo.setLineList(lineList);
        List<Long> fileIdList = knowledgeDocumentMapper.getKnowledgeDocumentFileIdListByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentFileVo(knowledgeDocumentVo.getId(), knowledgeDocumentVersionId));
        if (CollectionUtils.isNotEmpty(fileIdList)) {
            List<FileVo> fileList = fileMapper.getFileListByIdList(fileIdList);
            knowledgeDocumentVo.setFileIdList(fileIdList);
            knowledgeDocumentVo.setFileList(fileList);
        }
        List<Long> tagIdList = knowledgeDocumentMapper.getKnowledgeDocumentTagIdListByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentTagVo(knowledgeDocumentVo.getId(), knowledgeDocumentVersionId));
        if (CollectionUtils.isNotEmpty(tagIdList)) {
            List<String> tagNameList = knowledgeTagMapper.getKnowledgeTagNameListByIdList(tagIdList);
            knowledgeDocumentVo.setTagList(tagNameList);
        }

        KnowledgeCircleVo knowledgeCircleVo = knowledgeCircleMapper.getKnowledgeCircleById(knowledgeDocumentVo.getKnowledgeCircleId());
        if (knowledgeCircleVo != null) {
            knowledgeDocumentVo.getPath().add(knowledgeCircleVo.getName());
        }
        KnowledgeDocumentTypeVo knowledgeDocumentTypeVo = knowledgeDocumentTypeMappper.getTypeByUuid(knowledgeDocumentVo.getKnowledgeDocumentTypeUuid());
        if (knowledgeDocumentTypeVo != null) {
            List<String> typeNameList = knowledgeDocumentTypeMappper.getAncestorsAndSelfNameByLftRht(knowledgeDocumentTypeVo.getLft(), knowledgeDocumentTypeVo.getRht(), knowledgeDocumentTypeVo.getKnowledgeCircleId());
            if (CollectionUtils.isNotEmpty(typeNameList)) {
                knowledgeDocumentVo.getPath().addAll(typeNameList);
            }
        }
        return knowledgeDocumentVo;
    }


    @Override
    public void getReviewerParam(KnowledgeDocumentVersionVo documentVersionVoParam) {
        documentVersionVoParam.getReviewerRoleUuidList().clear();
        documentVersionVoParam.getReviewerTeamUuidList().clear();
        if (CollectionUtils.isNotEmpty(documentVersionVoParam.getReviewerList()) && CollectionUtils.isNotEmpty(documentVersionVoParam.getStatusList())) {
            //如果是“待审批”，则搜 knowledge_circle_user中 auth_type 为 “approver” 数据鉴权
            if (documentVersionVoParam.getStatusList().contains(KnowledgeDocumentVersionStatus.ALL.getValue()) || documentVersionVoParam.getStatusList().contains(KnowledgeDocumentVersionStatus.SUBMITTED.getValue())) {
                documentVersionVoParam.setIsReviewer(0);
                Iterator<String> reviewerIterator = documentVersionVoParam.getReviewerList().iterator();
                List<String> reviewerList = new ArrayList<String>();
                while (reviewerIterator.hasNext()) {
                    String reviewer = reviewerIterator.next();
                    if (reviewer.startsWith(GroupSearch.USER.getValuePlugin()) || (!reviewer.startsWith(GroupSearch.TEAM.getValuePlugin()) && !reviewer.startsWith(GroupSearch.ROLE.getValuePlugin()))) {
                        reviewer = reviewer.replaceAll(GroupSearch.USER.getValuePlugin(), StringUtils.EMPTY);
                        documentVersionVoParam.getReviewerTeamUuidList().addAll(teamMapper.getTeamUuidListByUserUuid(reviewer));
                        documentVersionVoParam.getReviewerRoleUuidList().addAll(userMapper.getRoleUuidListByUserUuid(reviewer));
                        reviewerList.add(reviewer);
                    } else if (reviewer.startsWith(GroupSearch.TEAM.getValuePlugin())) {
                        reviewer = reviewer.replaceAll(GroupSearch.TEAM.getValuePlugin(), StringUtils.EMPTY);
                        documentVersionVoParam.getReviewerTeamUuidList().addAll(teamMapper.getTeamUuidListByUserUuid(reviewer));
                    } else {
                        reviewer = reviewer.replaceAll(GroupSearch.ROLE.getValuePlugin(), StringUtils.EMPTY);
                        documentVersionVoParam.getReviewerRoleUuidList().addAll(userMapper.getRoleUuidListByUserUuid(reviewer));
                    }
                }
                documentVersionVoParam.setReviewerList(reviewerList);
            }

            if (documentVersionVoParam.getStatusList().contains(KnowledgeDocumentVersionStatus.ALL.getValue()) || documentVersionVoParam.getStatusList().contains(KnowledgeDocumentVersionStatus.REJECTED.getValue())//否则查询 knowledge_document_version中的 “reviewer”
                    || documentVersionVoParam.getStatusList().contains(KnowledgeDocumentVersionStatus.PASSED.getValue())) {
                List<String> reviewerList = documentVersionVoParam.getReviewerList();
                if (CollectionUtils.isNotEmpty(reviewerList)) {
                    for (int i = 0; i < reviewerList.size(); i++) {
                        reviewerList.set(i, reviewerList.get(i).replaceAll(GroupSearch.USER.getValuePlugin(), ""));
                    }
                }
                documentVersionVoParam.setIsReviewer(1);
                documentVersionVoParam.setReviewer(documentVersionVoParam.getReviewerList().get(0));
            }
        }
    }

    /**
     * @Description: 记录对文档操作（如提交，审核，切换版本，删除版本）
     * @Author: linbq
     * @Date: 2021/2/4 16:06
     * @Params:[knowledgeDocumentId, knowledgeDocumentVersionId, operate, config]
     * @Returns:void
     **/
    @Override
    public void audit(Long knowledgeDocumentId, Long knowledgeDocumentVersionId, KnowledgeDocumentOperate operate, JSONObject config) {
        KnowledgeDocumentAuditVo knowledgeDocumentAuditVo = new KnowledgeDocumentAuditVo();
        knowledgeDocumentAuditVo.setKnowledgeDocumentId(knowledgeDocumentId);
        knowledgeDocumentAuditVo.setKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
        knowledgeDocumentAuditVo.setFcu(UserContext.get().getUserUuid(true));
        knowledgeDocumentAuditVo.setOperate(operate.getValue());
        if (MapUtils.isNotEmpty(config)) {
            KnowledgeDocumentAuditConfigVo knowledgeDocumentAuditConfigVo = new KnowledgeDocumentAuditConfigVo(config.toJSONString());
            knowledgeDocumentAuditMapper.insertKnowledgeDocumentAuditConfig(knowledgeDocumentAuditConfigVo);
            knowledgeDocumentAuditVo.setConfigHash(knowledgeDocumentAuditConfigVo.getHash());
        }
        knowledgeDocumentAuditMapper.insertKnowledgeDocumentAudit(knowledgeDocumentAuditVo);
    }


    /**
     * @Description: 获取截取后的内容
     * @Author: 89770
     * @Date: 2021/3/1 16:47
     * @Params: [knowledgeDocumentVo, contentSb, contentLen]
     * @Returns: void
     **/
    @Override
    public String getContent(List<KnowledgeDocumentLineVo> lineVoList) {
        StringBuilder contentSb = new StringBuilder();
        if (CollectionUtils.isNotEmpty(lineVoList)) {
            for (KnowledgeDocumentLineVo lineVo : lineVoList) {
                String contentTmp = HtmlUtil.removeHtml(lineVo.getContent());
                contentSb.append(contentTmp);
            }
        }
        return contentSb.toString();
    }

    /**
     * @Description: 一次性获取知识搜索关键字最匹配下标信息,提供给后续循环截取内容和高亮关键字
     * @Author: 89770
     * @Date: 2021/3/2 12:18
     * @Params: [keyword, activeVersionIdList, versionWordOffsetVoMap, versionContentVoMap]
     * @Returns: void
     **/
    @Override
    public void initVersionWordOffsetAndContentMap(List<String> keywordList, List<Long> activeVersionIdList, Map<Long, FullTextIndexVo> versionWordOffsetVoMap, Map<String, String> versionContentVoMap) {
        if (CollectionUtils.isNotEmpty(activeVersionIdList) && CollectionUtils.isNotEmpty(keywordList)) {
            List<Long> targetIdList = new ArrayList<>();
            List<FullTextIndexVo> ftIndexVoList = ftIndexMapper.getFullTextIndexListByKeywordListAndTargetList(keywordList, activeVersionIdList);
            for (FullTextIndexVo indexVo : ftIndexVoList) {
                targetIdList.add(indexVo.getTargetId());
                versionWordOffsetVoMap.put(indexVo.getTargetId(), indexVo);
            }
            List<FullTextIndexContentVo> contentVoList = ftIndexMapper.getContentByTargetIdList(targetIdList);
            for (FullTextIndexContentVo contentVo : contentVoList) {
                versionContentVoMap.put(contentVo.getTargetId()+"_"+contentVo.getTargetField(), contentVo.getContent());
            }
        }

    }
}
