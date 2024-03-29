/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.knowledge.api.document;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.common.util.PageUtil;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.fulltextindex.dto.fulltextindex.FullTextIndexVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_BASE;
import neatlogic.framework.knowledge.constvalue.KnowledgeDocumentOperate;
import neatlogic.framework.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentAuditMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentAuditVo;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVersionStatusVo;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVersionVo;
import neatlogic.module.knowledge.service.KnowledgeDocumentService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentVersionSearchApi extends PrivateApiComponentBase {

    @Resource
    KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    KnowledgeDocumentAuditMapper knowledgeDocumentAuditMapper;

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    @Override
    public String getToken() {
        return "knowledge/document/version/search";
    }

    @Override
    public String getName() {
        return "nmkad.knowledgedocumentsearchapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword"),
            @Param(name = "lcuList", type = ApiParamType.JSONARRAY, desc = "common.editorlist"),
            @Param(name = "reviewerList", type = ApiParamType.JSONARRAY, desc = "common.reviewerlist"),
            @Param(name = "collector", type = ApiParamType.STRING, desc = "common.collector"),
            @Param(name = "sourceList", type = ApiParamType.JSONARRAY, desc = "common.sourcelist"),
            @Param(name = "statusList", type = ApiParamType.JSONARRAY, desc = "common.statuslist", help = "审批状态：all|submitted|passed|rejected|draft"),
            @Param(name = "knowledgeDocumentTypeUuid", type = ApiParamType.STRING, desc = "common.typeid"),
            @Param(name = "lcd", type = ApiParamType.JSONOBJECT, desc = "common.editdate", help = "{timeRange: 6, timeUnit: 'month'} 或  {startTime: 1605196800000, endTime: 1607961600000}"),
            @Param(name = "reviewDate", type = ApiParamType.JSONOBJECT, desc = "term.knowledge.reviewdate", help = "{timeRange: 6, timeUnit: 'month'} 或  {startTime: 1605196800000, endTime: 1607961600000}"),
            @Param(name = "tagList", type = ApiParamType.JSONARRAY, desc = "common.taglist"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize")
    })
    @Output({
            @Param(name = "dataList[].version", type = ApiParamType.INTEGER, desc = "common.versionnum"),
            @Param(name = "dataList[].knowledgeDocumentVersionId", type = ApiParamType.INTEGER, desc = "common.versionid"),
            @Param(name = "dataList[].knowledgeCircleName", type = ApiParamType.STRING, desc = "common.name"),
            @Param(name = "dataList[].title", type = ApiParamType.STRING, desc = "common.title"),
            @Param(name = "dataList[].content", type = ApiParamType.STRING, desc = "common.content"),
            @Param(name = "dataList[].lcu", type = ApiParamType.STRING, desc = "common.editor"),
            @Param(name = "dataList[].lcuName", type = ApiParamType.STRING, desc = "common.editorname"),
            @Param(name = "dataList[].lcd", type = ApiParamType.STRING, desc = "common.editdate"),
            @Param(name = "dataList[].tagList", type = ApiParamType.JSONARRAY, desc = "common.taglist"),
            @Param(name = "dataList[].viewCount", type = ApiParamType.LONG, desc = "common.viewcount"),
            @Param(name = "dataList[].favorCount", type = ApiParamType.LONG, desc = "common.favorcount"),
            @Param(name = "dataList[].collectCount", type = ApiParamType.LONG, desc = "common.collectcount"),
            @Param(name = "dataList[].documentTypePath", type = ApiParamType.STRING, desc = "term.knowledge.typepath"),
            @Param(name = "dataList[].knowledgeDocumentTypeUuid", type = ApiParamType.STRING, desc = "common.typeid"),
            @Param(name = "dataList[].status", type = ApiParamType.STRING, desc = "common.status"),
            @Param(name = "statusList", type = ApiParamType.JSONARRAY, desc = "common.statuslist", help = "”待我审批“、”我提交的“ 知识分类，对应“全部” “待审批” “以通过” “不通过” 分类的数量"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "nmkad.knowledgedocumentsearchapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject resultJson = new JSONObject();
        List<Long> documentVersionIdList = null;
        Integer total = 0;
        KnowledgeDocumentVersionVo documentVersionVoParam = new KnowledgeDocumentVersionVo(jsonObj);
        //拼装 “审批人”条件
        knowledgeDocumentService.getReviewerParam(documentVersionVoParam);
        //根据keyword等条件，查询符合条件的知识版本
        List<String> statusList = documentVersionVoParam.getStatusList();
        //status all,所有审批人的条件比较特殊=待审批（圈审批人）+审批后（version）
        if (statusList.contains(KnowledgeDocumentVersionStatus.ALL.getValue())) {
            documentVersionIdList = knowledgeDocumentMapper.getMyAllReviewKnowledgeDocumentVersionIdList(documentVersionVoParam);
            total = knowledgeDocumentMapper.getMyAllReviewKnowledgeDocumentVersionCount(documentVersionVoParam);
        } else {
            documentVersionIdList = knowledgeDocumentMapper.searchKnowledgeDocumentVersionId(documentVersionVoParam);
            total = knowledgeDocumentMapper.searchKnowledgeDocumentVersionIdCount(documentVersionVoParam);
        }
        //根据keyword等条件，从数据库搜索符合条件的知识版本count
        List<KnowledgeDocumentVersionVo> documentVersionList = new ArrayList<KnowledgeDocumentVersionVo>();
        if (CollectionUtils.isNotEmpty(documentVersionIdList)) {
            documentVersionList = knowledgeDocumentMapper.getKnowledgeDocumentVersionByIdList(documentVersionIdList);
        }
        //一次性查出所有分页版本id的不通过原因
        List<Long> failVersionIdList = new ArrayList<>();
        List<Long> versionIdList = new ArrayList<>();
        List<KnowledgeDocumentAuditVo> failAuditList = null;
        for (KnowledgeDocumentVersionVo versionVo : documentVersionList) {
            versionIdList.add(versionVo.getId());
            if (KnowledgeDocumentVersionStatus.REJECTED.getValue().equals(versionVo.getStatus())) {
                failVersionIdList.add(versionVo.getId());
            }
        }
        if (CollectionUtils.isNotEmpty(failVersionIdList)) {
            failAuditList = knowledgeDocumentAuditMapper.getKnowledgeDocumentAuditListByDocumentVersionIdListAndOperate(failVersionIdList, KnowledgeDocumentOperate.REJECT.getValue());
        }
        //一次性获取知识搜索关键字最匹配下标信息,提供给后续循环截取内容和高亮关键字
        Map<Long, FullTextIndexVo> versionIndexVoMap = new HashMap<>();
        Map<Long, String> versionContentMap = new HashMap<>();
        List<String> keywordList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(documentVersionVoParam.getKeywordList())) {
            keywordList = new ArrayList<>(documentVersionVoParam.getKeywordList());
        }
        if (CollectionUtils.isNotEmpty(versionIdList)) {
            knowledgeDocumentService.setVersionContentMap(keywordList, versionIdList, versionIndexVoMap, versionContentMap);
        }
        //循环知识版本，补充额外信息
        for (KnowledgeDocumentVersionVo knowledgeDocumentVersionVo : documentVersionList) {
            //跟新操作（如果是草稿,可以删除或编辑）
            if (documentVersionVoParam.getStatusList().contains(KnowledgeDocumentVersionStatus.DRAFT.getValue()) && knowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid())) {
                //如果不在圈子内则不允许编辑
                AuthenticationInfoVo authenticationInfoVo = UserContext.get().getAuthenticationInfoVo();
                if (knowledgeDocumentMapper.checkUserIsMember(knowledgeDocumentVersionVo.getKnowledgeCircleId(), UserContext.get().getUserUuid(true), authenticationInfoVo.getTeamUuidList(), authenticationInfoVo.getRoleUuidList()) > 0) {
                    knowledgeDocumentVersionVo.setIsEditable(1);
                }
                knowledgeDocumentVersionVo.setIsDeletable(1);
            }
            //如果审核不通过，则补充原因
            if (CollectionUtils.isNotEmpty(failAuditList)) {
                for (KnowledgeDocumentAuditVo failAudit : failAuditList) {
                    if (failAudit.getKnowledgeDocumentVersionId().equals(knowledgeDocumentVersionVo.getId())) {
                        knowledgeDocumentVersionVo.setRejectReason(failAudit.getConfig() == null ? StringUtils.EMPTY:failAudit.getConfig().getString("content"));
                    }
                }
            }
            //设置标题、截取内容，并高亮
            knowledgeDocumentService.setTitleAndShortcutContentHighlight(keywordList, knowledgeDocumentVersionVo.getId(), knowledgeDocumentVersionVo, versionIndexVoMap, versionContentMap);
            //去掉未提交status，前端不需要展示
            KnowledgeDocumentVersionStatusVo statusVo = knowledgeDocumentVersionVo.getStatusVo();
            if (knowledgeDocumentVersionVo.getStatusVo() != null && KnowledgeDocumentVersionStatus.DRAFT.getValue().equals(statusVo.getValue())) {
                knowledgeDocumentVersionVo.setStatus(null);
                knowledgeDocumentVersionVo.setStatusVo(null);
            }
        }
        //补充状态
        setStatusCount(documentVersionVoParam, resultJson);

        resultJson.put("dataList", documentVersionList);
        resultJson.put("rowNum", total);
        resultJson.put("pageSize", documentVersionVoParam.getPageSize());
        resultJson.put("currentPage", documentVersionVoParam.getCurrentPage());
        resultJson.put("pageCount", PageUtil.getPageCount(total, documentVersionVoParam.getPageSize()));
        return resultJson;
    }

    /**
     * @Description: 统计非草稿的版本状态数量
     * @Author: 89770
     * @Date: 2021/3/2 12:04
     * @Params: [documentVersionVoParam, resultJson]
     * @Returns: void
     **/
    private void setStatusCount(KnowledgeDocumentVersionVo documentVersionVoParam, JSONObject resultJson) {
        if (!documentVersionVoParam.getStatusList().contains(KnowledgeDocumentVersionStatus.DRAFT.getValue())) {
            JSONArray statusArray = new JSONArray();
            JSONObject jsonAll = new JSONObject();
            List<String> statusTmpList = new ArrayList<>();
            statusArray.add(jsonAll);
            JSONObject jsonSubmit = new JSONObject();
            statusArray.add(jsonSubmit);
            //通过
            JSONObject jsonPass = new JSONObject();
            jsonPass.put("value", KnowledgeDocumentVersionStatus.PASSED.getValue());
            jsonPass.put("text", KnowledgeDocumentVersionStatus.PASSED.getText());
            statusTmpList.add(KnowledgeDocumentVersionStatus.PASSED.getValue());
            documentVersionVoParam.setStatusList(statusTmpList);
            documentVersionVoParam.setIsReviewer(1);
            jsonPass.put("count", knowledgeDocumentMapper.searchKnowledgeDocumentVersionIdCount(documentVersionVoParam));
            statusArray.add(jsonPass);
            //不通过
            JSONObject jsonReject = new JSONObject();
            jsonReject.put("value", KnowledgeDocumentVersionStatus.REJECTED.getValue());
            jsonReject.put("text", KnowledgeDocumentVersionStatus.REJECTED.getText());
            statusTmpList.clear();
            statusTmpList.add(KnowledgeDocumentVersionStatus.REJECTED.getValue());
            documentVersionVoParam.setStatusList(statusTmpList);
            documentVersionVoParam.setIsReviewer(1);
            jsonReject.put("count", knowledgeDocumentMapper.searchKnowledgeDocumentVersionIdCount(documentVersionVoParam));
            statusArray.add(jsonReject);
            //待审批
            jsonSubmit.put("value", KnowledgeDocumentVersionStatus.SUBMITTED.getValue());
            jsonSubmit.put("text", KnowledgeDocumentVersionStatus.SUBMITTED.getText());
            statusTmpList.clear();
            statusTmpList.add(KnowledgeDocumentVersionStatus.SUBMITTED.getValue());
            documentVersionVoParam.setStatusList(statusTmpList);
            knowledgeDocumentService.getReviewerParam(documentVersionVoParam);
            jsonSubmit.put("count", knowledgeDocumentMapper.searchKnowledgeDocumentVersionIdCount(documentVersionVoParam));
            jsonAll.put("value", KnowledgeDocumentVersionStatus.ALL.getValue());
            jsonAll.put("text", KnowledgeDocumentVersionStatus.ALL.getText());
            jsonAll.put("count", jsonSubmit.getLong("count") + jsonPass.getLong("count") + jsonReject.getLong("count"));
            resultJson.put("statusList", statusArray);
        }
    }

}
