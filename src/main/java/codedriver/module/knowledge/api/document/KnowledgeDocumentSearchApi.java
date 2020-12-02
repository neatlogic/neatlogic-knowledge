package codedriver.module.knowledge.api.document;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.UserVo;
import codedriver.framework.elasticsearch.core.ElasticSearchHandlerFactory;
import codedriver.framework.elasticsearch.core.IElasticSearchHandler;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.HtmlUtil;
import codedriver.framework.util.TimeUtil;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentOperate;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentAuditMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentAuditVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentLineVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentTypeVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.elasticsearch.constvalue.ESHandler;
import codedriver.module.knowledge.exception.KnowledgeDocumentTypeNotFoundException;
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentSearchApi extends PrivateApiComponentBase {

    @Autowired
    KnowledgeDocumentMapper knowledgeDocumentMapper;
    
    @Autowired
    KnowledgeDocumentAuditMapper knowledgeDocumentAuditMapper;
    
    @Autowired
    TeamMapper teamMapper;
    
    @Autowired
    UserMapper userMapper;
    
    @Autowired
    private KnowledgeDocumentTypeMapper knowledgeDocumentTypeMapper;
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
        @Param(name = "type", type = ApiParamType.STRING, desc = "搜索知识对象类型： document|documentVersion, 默认document"),
        @Param(name = "keyword", type = ApiParamType.STRING, desc = "搜索关键字"),
        @Param(name = "lcuList", type = ApiParamType.JSONARRAY, desc = "修改人"),
        @Param(name = "reviewerList", type = ApiParamType.JSONARRAY, desc = "审批人"),
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
        @Param(name="dataList[].version", type = ApiParamType.INTEGER, desc="版本号"),
        @Param(name="dataList[].knowledgeDocumentVersionId", type = ApiParamType.INTEGER, desc="版本号id"),
        @Param(name="dataList[].knowledgeCircleName", type = ApiParamType.STRING, desc="知识圈名称"),
        @Param(name="dataList[].title", type = ApiParamType.STRING, desc="知识标题"),
        @Param(name="dataList[].content", type = ApiParamType.STRING, desc="知识内容"),
        @Param(name="dataList[].lcu", type = ApiParamType.STRING, desc="知识最近更新人uuid"),
        @Param(name="dataList[].lcuName", type = ApiParamType.STRING, desc="知识最近更新人"),
        @Param(name="dataList[].lcd", type = ApiParamType.STRING, desc="知识最近更新时间"),
        @Param(name="dataList[].tagList", type = ApiParamType.JSONARRAY, desc="知识标签"),
        @Param(name="dataList[].viewCount", type = ApiParamType.LONG, desc="知识浏览量"),
        @Param(name="dataList[].favorCount", type = ApiParamType.LONG, desc="知识点赞量"),
        @Param(name="dataList[].collectCount", type = ApiParamType.LONG, desc="知识收藏量"),
        @Param(name="dataList[].documentTypePath", type = ApiParamType.STRING, desc="知识圈分类路径"),
        @Param(name="dataList[].knowledgeDocumentTypeUuid", type = ApiParamType.STRING, desc="知识圈分类uuid"),
        @Param(name="dataList[].status", type = ApiParamType.STRING, desc="知识当前版本状态"),
        @Param(name="rowNum", type = ApiParamType.INTEGER, desc="总数"),
        @Param(name="pageSize", type = ApiParamType.INTEGER, desc="每页数据条目"),
        @Param(name="currentPage", type = ApiParamType.INTEGER, desc="当前页数"),
        @Param(name="pageCount", type = ApiParamType.INTEGER, desc="总页数"),
    })
    
    @Description(desc = "搜索文档")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject resultJson = new JSONObject();
        String type = jsonObj.getString("type");
        if("documentVersion".equals(type)) {
            setDocumentVersionList(resultJson,jsonObj);
        }else {
            setDocumentList(resultJson,jsonObj);
        }
        return resultJson;
    }  
    
    /*
     * 根据搜索条件，最终返回知识
     */
    @SuppressWarnings("unchecked")
    private void setDocumentList(JSONObject resultJson,JSONObject jsonObj) {
        JSONObject lcd = jsonObj.getJSONObject("lcd");
        jsonObj.remove("lcd");
        KnowledgeDocumentVo documentVoParam = JSON.toJavaObject(jsonObj, KnowledgeDocumentVo.class);
        
        if(lcd != null) {
            JSONObject lcdJson = getTime(lcd);
            documentVoParam.setLcdStartTime(lcdJson.getString("startTime"));
            documentVoParam.setLcdEndTime(lcdJson.getString("endTime"));
        }
        //仅根据keyword,从es搜索标题和内容
        JSONObject data = null;
        if(StringUtils.isNotBlank(documentVoParam.getKeyword())){
            IElasticSearchHandler<KnowledgeDocumentVo, JSONObject> esHandler = ElasticSearchHandlerFactory.getHandler(ESHandler.KNOWLEDGE.getValue());
            data = JSONObject.parseObject(esHandler.iterateSearch(documentVoParam).toString());
            List<Long> documentIdList = JSONObject.parseArray(data.getJSONArray("knowledgeDocumentIdList").toJSONString(),Long.class);
            //将从es搜索符合的知识送到数据库做二次过滤
            documentVoParam.setKnowledgeDocumentIdList(documentIdList);
        }
        List<Long> documentIdList = knowledgeDocumentMapper.getKnowledgeDocumentIdList(documentVoParam);
        List<KnowledgeDocumentVo> documentList = null;
        if(CollectionUtils.isNotEmpty(documentIdList)) {
            documentList = knowledgeDocumentMapper.getKnowledgeDocumentByIdList(documentIdList);
        }else {
            documentList = new ArrayList<KnowledgeDocumentVo>();
        }
        Integer total = knowledgeDocumentMapper.getKnowledgeDocumentCount(documentVoParam);
       
        JSONArray returnDataList = new JSONArray();
        List<Long> collectedKnowledgeDocumentIdList = new ArrayList<Long>();
        List<Long> favorKnowledgeDocumentIdList = new ArrayList<Long>();
        if(CollectionUtils.isNotEmpty(documentList)) {
           collectedKnowledgeDocumentIdList = knowledgeDocumentMapper.getKnowledgeDocumentCollectDocumentIdListByUserUuidAndDocumentIdList(UserContext.get().getUserUuid(true), documentIdList);
           favorKnowledgeDocumentIdList = knowledgeDocumentMapper.getKnowledgeDocumentFavorDocumentIdListByUserUuidAndDocumentIdList(UserContext.get().getUserUuid(true), documentIdList);
        }
        for(KnowledgeDocumentVo knowledgeDocumentVo : documentList) {
            //补充头像信息
            UserVo userVo = userMapper.getUserBaseInfoByUuid(knowledgeDocumentVo.getLcu());
            if(userVo != null) {
                knowledgeDocumentVo.setLcuName(userVo.getUserName());
                knowledgeDocumentVo.setLcuInfo(userVo.getUserInfo());
            }
            //如果入参条件存在知识类型，则直接判断当前用户是不是知识圈审批人
            Integer isApprover = null;
            List<Long> approveCircleIdList = new ArrayList<Long>();
            List<String> teamUuidList= teamMapper.getTeamUuidListByUserUuid(UserContext.get().getUserUuid(true));
            if(StringUtils.isNotBlank(documentVoParam.getKnowledgeDocumentTypeUuid())) {
                KnowledgeDocumentTypeVo knowledgeDocumentTypeVo = knowledgeDocumentTypeMapper.getTypeByUuid(documentVoParam.getKnowledgeDocumentTypeUuid());
                if(knowledgeDocumentTypeVo == null) {
                    throw new KnowledgeDocumentTypeNotFoundException(documentVoParam.getKnowledgeDocumentTypeUuid());
                }
                isApprover = knowledgeDocumentMapper.checkUserIsApprover(knowledgeDocumentTypeVo.getKnowledgeCircleId(), UserContext.get().getUserUuid(true), teamUuidList, UserContext.get().getRoleUuidList());
            }else {
                //查询当前登录人所有圈子的审批权限
                approveCircleIdList = knowledgeDocumentMapper.getUserAllApproverCircleIdList(UserContext.get().getUserUuid(true), teamUuidList, UserContext.get().getRoleUuidList());
                
            }
            //判断是否有权限编辑删除
            List<Long> knowledgeDocumentIdTmpList = new ArrayList<>(); 
            knowledgeDocumentVo.setIsEditable(1);
            if(isApprover == null ) {
                isApprover = 0;
                if(approveCircleIdList.contains(knowledgeDocumentVo.getKnowledgeCircleId())) {
                    isApprover = 1;
                }
            }
            knowledgeDocumentVo.setIsDeletable(isApprover);
            knowledgeDocumentIdTmpList.add(knowledgeDocumentVo.getId());
            
            //判断本人是否已经收藏
            if(collectedKnowledgeDocumentIdList.contains(knowledgeDocumentVo.getId())) {
                knowledgeDocumentVo.setIsCollect(1);
            }
           
            //判断本人是否已经点赞
            if(favorKnowledgeDocumentIdList.contains(knowledgeDocumentVo.getId())) {
                knowledgeDocumentVo.setIsFavor(1);
            }

            //替换 highlight 字段
            if(StringUtils.isNotBlank(documentVoParam.getKeyword())){
                JSONObject highlightData = data.getJSONObject(knowledgeDocumentVo.getId().toString());
                if(MapUtils.isNotEmpty(highlightData)) {
                    if(highlightData.containsKey("title.txt")) {
                        knowledgeDocumentVo.setTitle(String.join("\n", JSONObject.parseArray(highlightData.getString("title.txt"),String.class)));
                    }
                    if(highlightData.containsKey("content.txt")) {
                        knowledgeDocumentVo.setContent( String.join("\n", JSONObject.parseArray(highlightData.getString("content.txt"),String.class)));
                    }
                }
            }
            //如果es找不到内容 则从数据库获取
            if(StringUtils.isBlank(knowledgeDocumentVo.getContent())) {
                StringBuilder contentsb = new StringBuilder();
                List<KnowledgeDocumentLineVo> documentLineList = knowledgeDocumentVo.getLineList();
                if(CollectionUtils.isNotEmpty(documentLineList)) {
                    for(KnowledgeDocumentLineVo line : documentLineList) {
                        contentsb.append(line.getContent());
                    }
                    String content =HtmlUtil.removeHtml(contentsb.toString(), null);
                    knowledgeDocumentVo.setContent(HtmlUtil.removeHtml(contentsb.toString(), null).substring(0, content.length()> 250?250:content.length()));
                    knowledgeDocumentVo.setLineList(null);
                }
               
            }
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
    }
    
    /*
     * 根据搜索条件，最终返回知识版本
     */
    @SuppressWarnings("unchecked")
    private void setDocumentVersionList(JSONObject resultJson,JSONObject jsonObj) {
        JSONObject lcd = jsonObj.getJSONObject("lcd");
        jsonObj.remove("lcd");
        KnowledgeDocumentVersionVo documentVersionVoParam = JSON.toJavaObject(jsonObj, KnowledgeDocumentVersionVo.class);
        if(lcd != null) {
            JSONObject lcdJson = getTime(lcd);
            documentVersionVoParam.setLcdStartTime(lcdJson.getString("startTime"));
            documentVersionVoParam.setLcdEndTime(lcdJson.getString("endTime"));
        }
        JSONObject reviewDate = jsonObj.getJSONObject("reviewDate");
        if(reviewDate != null) {
            JSONObject reviewDateJson = getTime(reviewDate);
            documentVersionVoParam.setReviewDateStartTime(reviewDateJson.getString("startTime"));
            documentVersionVoParam.setReviewDateEndTime(reviewDateJson.getString("endTime"));
        }
        //status all
        List<String>  statusList = documentVersionVoParam.getStatusList();
        if(statusList.contains(KnowledgeDocumentVersionStatus.ALL.getValue())) {
            statusList.remove(KnowledgeDocumentVersionStatus.ALL.getValue());
            statusList.add(KnowledgeDocumentVersionStatus.PASSED.getValue());
            statusList.add(KnowledgeDocumentVersionStatus.REJECTED.getValue());
            statusList.add(KnowledgeDocumentVersionStatus.SUBMITTED.getValue());
            documentVersionVoParam.setStatusList(statusList);
        }
        //仅根据keyword,从es搜索标题和内容
        JSONObject data = null;
        if(StringUtils.isNotBlank(documentVersionVoParam.getKeyword())){
            IElasticSearchHandler<KnowledgeDocumentVersionVo, JSONObject> esHandler = ElasticSearchHandlerFactory.getHandler(ESHandler.KNOWLEDGE_VERSION.getValue());
            data = JSONObject.parseObject(esHandler.iterateSearch(documentVersionVoParam).toString());
            List<Long> documentVersionIdList = JSONObject.parseArray(data.getJSONArray("knowledgeDocumentVersionIdList").toJSONString(),Long.class);
            //将从es搜索符合的知识送到数据库做二次过滤
            documentVersionVoParam.setKnowledgeDocumentVersionIdList(documentVersionIdList);
        }
        
        //拼装 “审批人”条件 
        if(CollectionUtils.isNotEmpty(documentVersionVoParam.getReviewerList()) && CollectionUtils.isNotEmpty(documentVersionVoParam.getStatusList())) {
            //如果是“待审批”，则搜 knowledge_circle_user中 auth_type 为 “approver” 数据鉴权
            if(documentVersionVoParam.getStatusList().contains(KnowledgeDocumentVersionStatus.SUBMITTED.getValue())) {
                documentVersionVoParam.setIsReviewer(1);
                Iterator<String> reviewerIterator = documentVersionVoParam.getReviewerList().iterator();
                List<String> reviewerList = new ArrayList<String>();
                while(reviewerIterator.hasNext()) {
                    String reviewer = reviewerIterator.next();
                    if(reviewer.startsWith(GroupSearch.USER.getValuePlugin())){
                        reviewer = reviewer.replaceAll(GroupSearch.USER.getValuePlugin(), StringUtils.EMPTY);
                        documentVersionVoParam.getReviewerTeamUuidList().addAll(teamMapper.getTeamUuidListByUserUuid(reviewer));
                        documentVersionVoParam.getReviewerRoleUuidList().addAll(userMapper.getRoleUuidListByUserUuid(reviewer));
                        reviewerList.add(reviewer);
                    }else if(reviewer.startsWith(GroupSearch.TEAM.getValuePlugin())) {
                        reviewer = reviewer.replaceAll(GroupSearch.TEAM.getValuePlugin(), StringUtils.EMPTY);
                        documentVersionVoParam.getReviewerTeamUuidList().addAll(teamMapper.getTeamUuidListByUserUuid(reviewer));
                    }else {
                        reviewer = reviewer.replaceAll(GroupSearch.ROLE.getValuePlugin(), StringUtils.EMPTY);
                        documentVersionVoParam.getReviewerRoleUuidList().addAll(userMapper.getRoleUuidListByUserUuid(reviewer));
                    }
                }
                documentVersionVoParam.setReviewerList(reviewerList);
            }else if(documentVersionVoParam.getStatusList().contains(KnowledgeDocumentVersionStatus.REJECTED.getValue())//否则查询 knowledge_document_version中的 “reviewer”
                ||documentVersionVoParam.getStatusList().contains(KnowledgeDocumentVersionStatus.PASSED.getValue())){
                List<String> reviewerList = documentVersionVoParam.getReviewerList();
                if(CollectionUtils.isNotEmpty(reviewerList)) {
                    for(int i = 0;i<reviewerList.size();i++) {
                        reviewerList.set(i, reviewerList.get(i).replaceAll(GroupSearch.USER.getValuePlugin(), ""));
                    }
                }
                documentVersionVoParam.setIsReviewer(0);
                documentVersionVoParam.setReviewer(documentVersionVoParam.getReviewerList().get(0));
            }
        }
        
        //查询符合条件的知识版本
        List<Long> documentVersionIdList = knowledgeDocumentMapper.getKnowledgeDocumentVersionIdList(documentVersionVoParam);
        List<KnowledgeDocumentVersionVo> documentVersionList = null;
        if(CollectionUtils.isNotEmpty(documentVersionIdList)) {
            documentVersionList = knowledgeDocumentMapper.getKnowledgeDocumentVersionByIdList(documentVersionIdList);
        }else {
            documentVersionList = new ArrayList<KnowledgeDocumentVersionVo>();
        }
        Integer total = knowledgeDocumentMapper.getKnowledgeDocumentVersionCount(documentVersionVoParam);
       
        
        for(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo : documentVersionList) {
            //跟新操作（如果是草稿,可以删除或编辑）
            if(documentVersionVoParam.getStatusList().contains(KnowledgeDocumentVersionStatus.DRAFT.getValue())) {
                int isApprover = 0;
                if(knowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid())) {
                    isApprover = 1;
                }
                knowledgeDocumentVersionVo.setIsEditable(isApprover);
                knowledgeDocumentVersionVo.setIsDeletable(isApprover);
            }
            //替换 highlight 字段
            if(StringUtils.isNotBlank(documentVersionVoParam.getKeyword())){
                JSONObject highlightData = data.getJSONObject(knowledgeDocumentVersionVo.getId().toString());
                if(MapUtils.isNotEmpty(highlightData)) {
                    if(highlightData.containsKey("title.txt")) {
                        knowledgeDocumentVersionVo.setTitle(String.join("\n", JSONObject.parseArray(highlightData.getString("title.txt"),String.class)));
                    }
                    if(highlightData.containsKey("content.txt")) {
                        knowledgeDocumentVersionVo.setContent( String.join("\n", JSONObject.parseArray(highlightData.getString("content.txt"),String.class)));
                    }
                }
            }
            //如果es找不到内容 则从数据库获取
            if(StringUtils.isBlank(knowledgeDocumentVersionVo.getContent())) {
                StringBuilder contentsb = new StringBuilder();
                List<KnowledgeDocumentLineVo> documentLineList = knowledgeDocumentVersionVo.getKnowledgeDocumentLineList();
                if(CollectionUtils.isNotEmpty(documentLineList)) {
                    for(KnowledgeDocumentLineVo line : documentLineList) {
                        contentsb.append(line.getContent());
                    }
                    String content =HtmlUtil.removeHtml(contentsb.toString(), null);
                    knowledgeDocumentVersionVo.setContent(HtmlUtil.removeHtml(contentsb.toString(), null).substring(0, content.length()> 250?250:content.length()));
                    knowledgeDocumentVersionVo.setKnowledgeDocumentLineList(null);
                }
            }
            //如果审核不通过，则补充原因
            if(KnowledgeDocumentVersionStatus.REJECTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
                KnowledgeDocumentAuditVo  rejectAudit = knowledgeDocumentAuditMapper.getKnowledgeDocumentAuditListByDocumentIdAndVersionIdAndOperate(new KnowledgeDocumentAuditVo(knowledgeDocumentVersionVo.getKnowledgeDocumentId(),knowledgeDocumentVersionVo.getId(),KnowledgeDocumentOperate.REJECT.getValue()));
                if(rejectAudit != null) {
                    String rejectReason =knowledgeDocumentAuditMapper.getKnowledgeDocumentAuditConfigStringByHash(rejectAudit.getConfigHash());
                    if(StringUtils.isNotBlank(rejectReason)) {
                        knowledgeDocumentVersionVo.setRejectReason(JSONObject.parseObject(rejectReason).getString("content"));
                    }
                }
            }
            
        }
       
        //补充状态
        if(!KnowledgeDocumentVersionStatus.DRAFT.getValue().equals(documentVersionVoParam.getStatus())){
            JSONArray statusArray = new JSONArray();
            JSONObject jsonAll = new JSONObject();
            List<String> statusTmpList = new ArrayList<>();
            jsonAll.put("value",  KnowledgeDocumentVersionStatus.ALL.getValue());
            jsonAll.put("text", KnowledgeDocumentVersionStatus.ALL.getText());
            statusTmpList.add(KnowledgeDocumentVersionStatus.PASSED.getValue());
            statusTmpList.add(KnowledgeDocumentVersionStatus.REJECTED.getValue());
            statusTmpList.add(KnowledgeDocumentVersionStatus.SUBMITTED.getValue());
            documentVersionVoParam.setStatusList(statusTmpList);
            jsonAll.put("count", knowledgeDocumentMapper.getKnowledgeDocumentVersionCount(documentVersionVoParam));
            statusArray.add(jsonAll);
            JSONObject jsonSubmit = new JSONObject();
            jsonSubmit.put("value", KnowledgeDocumentVersionStatus.SUBMITTED.getValue());
            jsonSubmit.put("text", KnowledgeDocumentVersionStatus.SUBMITTED.getText());
            statusTmpList.clear();
            statusTmpList.add(KnowledgeDocumentVersionStatus.SUBMITTED.getValue());
            documentVersionVoParam.setStatusList(statusTmpList);
            jsonSubmit.put("count", knowledgeDocumentMapper.getKnowledgeDocumentVersionCount(documentVersionVoParam));
            statusArray.add(jsonSubmit);
            JSONObject jsonPass = new JSONObject();
            jsonPass.put("value", KnowledgeDocumentVersionStatus.PASSED.getValue());
            jsonPass.put("text", KnowledgeDocumentVersionStatus.PASSED.getText());
            statusTmpList.clear();
            statusTmpList.add(KnowledgeDocumentVersionStatus.PASSED.getValue());
            documentVersionVoParam.setStatusList(statusTmpList);
            jsonPass.put("count", knowledgeDocumentMapper.getKnowledgeDocumentVersionCount(documentVersionVoParam));
            statusArray.add(jsonPass);
            JSONObject jsonReject = new JSONObject();
            jsonReject.put("value", KnowledgeDocumentVersionStatus.REJECTED.getValue());
            jsonReject.put("text", KnowledgeDocumentVersionStatus.REJECTED.getText());
            statusTmpList.clear();
            statusTmpList.add(KnowledgeDocumentVersionStatus.REJECTED.getValue());
            documentVersionVoParam.setStatusList(statusTmpList);
            jsonReject.put("count", knowledgeDocumentMapper.getKnowledgeDocumentVersionCount(documentVersionVoParam));
            statusArray.add(jsonReject);
            resultJson.put("statusList", statusArray);
        }
        resultJson.put("dataList", documentVersionList);
        resultJson.put("rowNum", total);
        resultJson.put("pageSize", documentVersionVoParam.getPageSize());
        resultJson.put("currentPage", documentVersionVoParam.getCurrentPage());
        resultJson.put("pageCount", PageUtil.getPageCount(total, documentVersionVoParam.getPageSize()));
    }
    
    /**
    * @Author 89770
    * @Time 2020年11月6日  
    * @Description: 解析最近修改时间入参
    * @Param 
    * @return
     */
    private JSONObject getTime(JSONObject lcdConfig) {
        JSONObject json = new JSONObject();
        String startTime = StringUtils.EMPTY;
        String endTime = StringUtils.EMPTY;
        SimpleDateFormat format = new SimpleDateFormat(TimeUtil.YYYY_MM_DD_HH_MM_SS);
        if (lcdConfig.containsKey("startTime")) {
            startTime = format.format(new Date(lcdConfig.getLong("startTime")));
            endTime = format.format(new Date(lcdConfig.getLong("endTime")));
        } else {
            startTime = TimeUtil.timeTransfer(lcdConfig.getInteger("timeRange"), lcdConfig.getString("timeUnit"));
            endTime = TimeUtil.timeNow();
        }
        json.put("startTime", startTime);
        json.put("endTime", endTime);
        
        return json;
    }
}
