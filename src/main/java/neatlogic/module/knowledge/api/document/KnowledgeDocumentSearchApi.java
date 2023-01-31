/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.knowledge.api.document;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.util.PageUtil;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.fulltextindex.dto.fulltextindex.FullTextIndexVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.service.AuthenticationInfoService;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_BASE;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentTypeVo;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVo;
import neatlogic.framework.knowledge.exception.KnowledgeDocumentTypeNotFoundException;
import neatlogic.module.knowledge.service.KnowledgeDocumentService;
import com.alibaba.fastjson.JSON;
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
public class KnowledgeDocumentSearchApi extends PrivateApiComponentBase {

    @Resource
    KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    private AuthenticationInfoService authenticationInfoService;

    @Resource
    private KnowledgeDocumentTypeMapper knowledgeDocumentTypeMapper;

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    @Override
    public String getToken() {
        return "knowledge/document/search";
    }

    @Override
    public String getName() {
        return "搜索文档";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "搜索关键字"),
            @Param(name = "lcuList", type = ApiParamType.JSONARRAY, desc = "修改人"),
            @Param(name = "collector", type = ApiParamType.STRING, desc = "收藏人"),
            @Param(name = "sourceList", type = ApiParamType.JSONARRAY, desc = "来源"),
            @Param(name = "statusList", type = ApiParamType.JSONARRAY, desc = "审批状态：all|submitted|passed|rejected|draft"),
            @Param(name = "knowledgeDocumentTypeUuid", type = ApiParamType.STRING, desc = "知识文档类型"),
            @Param(name = "lcd", type = ApiParamType.JSONOBJECT, desc = "最近修改时间； {timeRange: 6, timeUnit: 'month'} 或  {startTime: 1605196800000, endTime: 1607961600000}"),
            @Param(name = "reviewDate", type = ApiParamType.JSONOBJECT, desc = "最近修改时间； {timeRange: 6, timeUnit: 'month'} 或  {startTime: 1605196800000, endTime: 1607961600000}"),
            @Param(name = "tagList", type = ApiParamType.JSONARRAY, desc = "标签列表"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页数"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目")
    })
    @Output({
            @Param(name = "dataList[].version", type = ApiParamType.INTEGER, desc = "版本号"),
            @Param(name = "dataList[].knowledgeDocumentVersionId", type = ApiParamType.INTEGER, desc = "版本号id"),
            @Param(name = "dataList[].knowledgeCircleName", type = ApiParamType.STRING, desc = "知识圈名称"),
            @Param(name = "dataList[].title", type = ApiParamType.STRING, desc = "知识标题"),
            @Param(name = "dataList[].content", type = ApiParamType.STRING, desc = "知识内容"),
            @Param(name = "dataList[].lcu", type = ApiParamType.STRING, desc = "知识最近更新人uuid"),
            @Param(name = "dataList[].lcuName", type = ApiParamType.STRING, desc = "知识最近更新人"),
            @Param(name = "dataList[].lcd", type = ApiParamType.STRING, desc = "知识最近更新时间"),
            @Param(name = "dataList[].tagList", type = ApiParamType.JSONARRAY, desc = "知识标签"),
            @Param(name = "dataList[].viewCount", type = ApiParamType.LONG, desc = "知识浏览量"),
            @Param(name = "dataList[].favorCount", type = ApiParamType.LONG, desc = "知识点赞量"),
            @Param(name = "dataList[].collectCount", type = ApiParamType.LONG, desc = "知识收藏量"),
            @Param(name = "dataList[].documentTypePath", type = ApiParamType.STRING, desc = "知识圈分类路径"),
            @Param(name = "dataList[].knowledgeDocumentTypeUuid", type = ApiParamType.STRING, desc = "知识圈分类uuid"),
            @Param(name = "dataList[].status", type = ApiParamType.STRING, desc = "知识当前版本状态"),
            @Param(name = "statusList", type = ApiParamType.JSONARRAY, desc = "”待我审批“、”我提交的“ 知识分类，对应“全部” “待审批” “以通过” “不通过” 分类的数量"),
            @Param(name = "rowNum", type = ApiParamType.INTEGER, desc = "总数"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页数"),
            @Param(name = "pageCount", type = ApiParamType.INTEGER, desc = "总页数"),
    })

    @Description(desc = "搜索文档")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject resultJson = new JSONObject();
        JSONArray returnDataList = new JSONArray();
        KnowledgeDocumentVo documentVoParam = new KnowledgeDocumentVo(jsonObj);
        //补充查看权限条件参数（圈子成员or圈子审批人）
        getDocumentViewParam(documentVoParam);
        //根据keyword等条件，从数据库搜索知识
        List<KnowledgeDocumentVo> documentList = new ArrayList<>();
        List<Long> documentIdList = knowledgeDocumentMapper.searchKnowledgeDocumentId(documentVoParam);
        if (CollectionUtils.isNotEmpty(documentIdList)) {
            documentList = knowledgeDocumentMapper.getKnowledgeDocumentByIdList(documentIdList);
        }
        //根据keyword等条件，从数据库搜索符合条件的知识count
        Integer total = knowledgeDocumentMapper.searchKnowledgeDocumentIdCount(documentVoParam);
        //一次性查询登录用户的所有点赞知识list和收藏的知识list，提供给后续循环判断知识是否已收藏和点赞
        List<Long> collectedKnowledgeDocumentIdList = new ArrayList<Long>();
        List<Long> favorKnowledgeDocumentIdList = new ArrayList<Long>();
        if (CollectionUtils.isNotEmpty(documentList)) {
            collectedKnowledgeDocumentIdList = knowledgeDocumentMapper.getKnowledgeDocumentCollectDocumentIdListByUserUuidAndDocumentIdList(UserContext.get().getUserUuid(true), documentIdList);
            favorKnowledgeDocumentIdList = knowledgeDocumentMapper.getKnowledgeDocumentFavorDocumentIdListByUserUuidAndDocumentIdList(UserContext.get().getUserUuid(true), documentIdList);
        }
        //判断知识圈是否拥有审批权限。如果入参传入知识全类型，则查询是否有该圈子类型的审批权限，否则一次性获取当前登录人拥有审批权限的所有圈子id
        Integer isApprover = null;
        List<Long> approveCircleIdList = new ArrayList<Long>();
        AuthenticationInfoVo authenticationInfoVo = authenticationInfoService.getAuthenticationInfo(UserContext.get().getUserUuid(true));
        if (StringUtils.isNotBlank(documentVoParam.getKnowledgeDocumentTypeUuid())) {
            KnowledgeDocumentTypeVo knowledgeDocumentTypeVo = knowledgeDocumentTypeMapper.getTypeByUuid(documentVoParam.getKnowledgeDocumentTypeUuid());
            if (knowledgeDocumentTypeVo == null) {
                throw new KnowledgeDocumentTypeNotFoundException(documentVoParam.getKnowledgeDocumentTypeUuid());
            }
            isApprover = knowledgeDocumentMapper.checkUserIsApprover(knowledgeDocumentTypeVo.getKnowledgeCircleId(), UserContext.get().getUserUuid(true), authenticationInfoVo.getTeamUuidList(), authenticationInfoVo.getRoleUuidList());
        } else {
            approveCircleIdList = knowledgeDocumentMapper.getUserAllApproverCircleIdList(UserContext.get().getUserUuid(true), authenticationInfoVo.getTeamUuidList(), authenticationInfoVo.getRoleUuidList());
        }
        //获取所有知识当前激活版本
        List<Long> activeVersionIdList = new ArrayList<>();
        for(KnowledgeDocumentVo knowledgeDocumentVo : documentList){
            activeVersionIdList.add(knowledgeDocumentVo.getKnowledgeDocumentVersionId());
        }
        //一次性获取知识搜索关键字最匹配下标信息,提供给后续循环截取内容和高亮关键字
        List<String> keywordList = new ArrayList<>();
        Map<Long, FullTextIndexVo> versionIndexVoMap = new HashMap<>();
        Map<Long,String> versionContentMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(documentVoParam.getKeywordList())) {
            keywordList = new ArrayList<>(documentVoParam.getKeywordList());
        }
        if(CollectionUtils.isNotEmpty(activeVersionIdList)) {
            knowledgeDocumentService.setVersionContentMap(keywordList, activeVersionIdList, versionIndexVoMap, versionContentMap);
        }
        //循环知识，补充额外信息
        for (KnowledgeDocumentVo knowledgeDocumentVo : documentList) {
            knowledgeDocumentVo.setIsEditable(1);
            //判断是否有审批权限，拥有审批权限的人可以删除
            if (isApprover == null) {
                isApprover = 0;
                if (approveCircleIdList.contains(knowledgeDocumentVo.getKnowledgeCircleId())) {
                    isApprover = 1;
                }
            }
            knowledgeDocumentVo.setIsDeletable(isApprover);
            //判断当前登录人是否已经收藏
            if (collectedKnowledgeDocumentIdList.contains(knowledgeDocumentVo.getId())) {
                knowledgeDocumentVo.setIsCollect(1);
            }
            //判断当前登录人是否已经点赞
            if (favorKnowledgeDocumentIdList.contains(knowledgeDocumentVo.getId())) {
                knowledgeDocumentVo.setIsFavor(1);
            }
            //设置标题、截取内容，并高亮
            knowledgeDocumentService.setTitleAndShortcutContentHighlight(keywordList, knowledgeDocumentVo.getKnowledgeDocumentVersionId(),knowledgeDocumentVo, versionIndexVoMap, versionContentMap);
            //组装返回数据
            JSONObject returnData = JSONObject.parseObject(JSON.toJSONString(knowledgeDocumentVo));
            returnData.put("knowledgeDocumentId", returnData.getLong("id"));
            returnData.put("id", returnData.getLong("knowledgeDocumentVersionId"));
            returnDataList.add(returnData);
        }

        resultJson.put("dataList", returnDataList);
        resultJson.put("rowNum", total);
        resultJson.put("pageSize", documentVoParam.getPageSize());
        resultJson.put("currentPage", documentVoParam.getCurrentPage());
        resultJson.put("pageCount", PageUtil.getPageCount(total, documentVoParam.getPageSize()));
        return resultJson;
    }

    /**
     * @return
     * @Author 89770
     * @Time 2020年12月7日
     * @Description: 补充查看权限条件（知识圈成员or知识圈审批人）
     * @Param
     */
    private void getDocumentViewParam(KnowledgeDocumentVo documentVoParam) {
        String userUuid = UserContext.get().getUserUuid(true);
        documentVoParam.setCircleUserUuid(userUuid);

        AuthenticationInfoVo authenticationInfoVo = authenticationInfoService.getAuthenticationInfo(userUuid);
        documentVoParam.setCircleTeamUuidList(authenticationInfoVo.getTeamUuidList());
        documentVoParam.setCircleRoleUuidList(authenticationInfoVo.getRoleUuidList());
    }
}