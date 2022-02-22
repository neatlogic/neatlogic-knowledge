package codedriver.module.knowledge.api.document;

import java.util.*;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.dto.AuthenticationInfoVo;
import codedriver.framework.service.AuthenticationInfoService;
import codedriver.module.knowledge.auth.label.KNOWLEDGE_BASE;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.UserVo;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.framework.knowledge.constvalue.KnowledgeType;
import codedriver.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.framework.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import codedriver.framework.knowledge.dto.KnowledgeDocumentCollectVo;
import codedriver.framework.knowledge.dto.KnowledgeDocumentTypeVo;
import codedriver.framework.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.framework.knowledge.dto.KnowledgeDocumentVo;
import codedriver.framework.knowledge.exception.KnowledgeDocumentCurrentUserNotMemberException;
import codedriver.framework.knowledge.exception.KnowledgeDocumentTypeNotFoundException;
import codedriver.module.knowledge.service.KnowledgeDocumentService;
@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentListApi extends PrivateApiComponentBase {

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    
    @Resource
    private KnowledgeDocumentTypeMapper knowledgeDocumentTypeMapper;
    
    @Resource
    private UserMapper userMapper;

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    @Resource
    private AuthenticationInfoService authenticationInfoService;

    private Map<String, Function<JSONObject, JSONObject>> map = new HashMap<>();
    
    @PostConstruct
    public void init() {
        map.put(KnowledgeType.ALL.getValue(), (jsonObj) -> {
            KnowledgeDocumentVo searchVo = JSON.toJavaObject(jsonObj, KnowledgeDocumentVo.class);
            if(searchVo.getKnowledgeDocumentTypeUuid() == null) {
                throw new ParamNotExistsException("knowledgeDocumentTypeUuid");
            }
            KnowledgeDocumentTypeVo knowledgeDocumentTypeVo = knowledgeDocumentTypeMapper.getTypeByUuid(searchVo.getKnowledgeDocumentTypeUuid());
            if(knowledgeDocumentTypeVo == null) {
                throw new KnowledgeDocumentTypeNotFoundException(searchVo.getKnowledgeDocumentTypeUuid());
            }
            if(knowledgeDocumentService.isMember(knowledgeDocumentTypeVo.getKnowledgeCircleId()) == 0) {
                throw new KnowledgeDocumentCurrentUserNotMemberException();
            }
            int isApprover = knowledgeDocumentService.isReviewer(knowledgeDocumentTypeVo.getKnowledgeCircleId());
            JSONObject resultObj = new JSONObject();
            resultObj.put("theadList", getTheadList());
            resultObj.put("tbodyList", new ArrayList<>());
            int pageCount = 0;
            if(searchVo.getNeedPage()) {
                int rowNum = knowledgeDocumentMapper.getKnowledgeDocumentCountByKnowledgeDocumentTypeUuid(searchVo);
                pageCount = PageUtil.getPageCount(rowNum, searchVo.getPageSize());
                resultObj.put("currentPage", searchVo.getCurrentPage());
                resultObj.put("pageSize", searchVo.getPageSize());
                resultObj.put("pageCount", pageCount);
                resultObj.put("rowNum", rowNum);
            }
            if(!searchVo.getNeedPage() || searchVo.getCurrentPage() <= pageCount) {
                List<Long> knowledgeDocumentIdList = new ArrayList<>();               
                List<KnowledgeDocumentVersionVo> knowledgeDocumentVersionList = knowledgeDocumentMapper.getKnowledgeDocumentListByKnowledgeDocumentTypeUuid(searchVo);
                for(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo : knowledgeDocumentVersionList) {
                    knowledgeDocumentVersionVo.setIsEditable(1);
                    knowledgeDocumentVersionVo.setIsDeletable(isApprover);
                    knowledgeDocumentIdList.add(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
                    knowledgeDocumentVersionVo.setAutoGenerateId(false);
                    knowledgeDocumentVersionVo.setId(null);
                }
                if(CollectionUtils.isNotEmpty(knowledgeDocumentIdList)) {
                    List<Long> collectedKnowledgeDocumentIdList = knowledgeDocumentMapper.getKnowledgeDocumentCollectDocumentIdListByUserUuidAndDocumentIdList(UserContext.get().getUserUuid(true), knowledgeDocumentIdList);
                    for(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo : knowledgeDocumentVersionList) {
                        if(collectedKnowledgeDocumentIdList.contains(knowledgeDocumentVersionVo.getKnowledgeDocumentId())) {
                            knowledgeDocumentVersionVo.setIsCollect(1);
                        }
                    }
                }
                
                resultObj.put("tbodyList", knowledgeDocumentVersionList);
            }
            return resultObj;
        });
        
        map.put(KnowledgeType.WAITINGFORREVIEW.getValue(), (jsonObj) -> {
            JSONObject resultObj = new JSONObject();
            resultObj.put("theadList", getWaitingForMyReviewTheadList());
            resultObj.put("tbodyList", new ArrayList<>());
            AuthenticationInfoVo authenticationInfoVo = authenticationInfoService.getAuthenticationInfo(UserContext.get().getUserUuid(true));
            BasePageVo searchVo = JSON.toJavaObject(jsonObj, BasePageVo.class);
            int pageCount = 0;
            if(searchVo.getNeedPage()) {
                int rowNum = knowledgeDocumentMapper.getKnowledgeDocumentWaitingForReviewCount(searchVo, UserContext.get().getUserUuid(true), authenticationInfoVo.getTeamUuidList(), authenticationInfoVo.getRoleUuidList());
                pageCount = PageUtil.getPageCount(rowNum, searchVo.getPageSize());
                resultObj.put("currentPage", searchVo.getCurrentPage());
                resultObj.put("pageSize", searchVo.getPageSize());
                resultObj.put("pageCount", pageCount);
                resultObj.put("rowNum", rowNum);
            }
            if(!searchVo.getNeedPage() || searchVo.getCurrentPage() <= pageCount) {
                List<KnowledgeDocumentVersionVo> knowledgeDocumentVersionList = knowledgeDocumentMapper.getKnowledgeDocumentWaitingForReviewList(searchVo, UserContext.get().getUserUuid(true), authenticationInfoVo.getTeamUuidList(), authenticationInfoVo.getRoleUuidList());
                resultObj.put("tbodyList", knowledgeDocumentVersionList);
            }
            return resultObj;
        });
        
        map.put(KnowledgeType.SHARE.getValue(), (jsonObj) -> {
            JSONObject resultObj = new JSONObject();
            resultObj.put("theadList", getMyShareTheadList());
            resultObj.put("tbodyList", new ArrayList<>());
            List<String> statusList = Arrays.asList(KnowledgeDocumentVersionStatus.PASSED.getValue(), KnowledgeDocumentVersionStatus.REJECTED.getValue(), KnowledgeDocumentVersionStatus.SUBMITTED.getValue());
            KnowledgeDocumentVersionVo searchVo = JSON.toJavaObject(jsonObj, KnowledgeDocumentVersionVo.class);
            searchVo.setLcu(UserContext.get().getUserUuid(true));
            searchVo.setStatusList(statusList);
            int pageCount = 0;
            if(searchVo.getNeedPage()) {
                int rowNum = knowledgeDocumentMapper.getKnowledgeDocumentVersionMyVersionCount(searchVo);
                pageCount = PageUtil.getPageCount(rowNum, searchVo.getPageSize());
                resultObj.put("currentPage", searchVo.getCurrentPage());
                resultObj.put("pageSize", searchVo.getPageSize());
                resultObj.put("pageCount", pageCount);
                resultObj.put("rowNum", rowNum);
            }
            if(!searchVo.getNeedPage() || searchVo.getCurrentPage() <= pageCount) {
                List<KnowledgeDocumentVersionVo> knowledgeDocumentVersionList = knowledgeDocumentMapper.getKnowledgeDocumentVersionMyVersionList(searchVo);
                for(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo : knowledgeDocumentVersionList) {
                    if(StringUtils.isNotBlank(knowledgeDocumentVersionVo.getReviewer())) {
                        UserVo reviewerUserVo = userMapper.getUserBaseInfoByUuid(knowledgeDocumentVersionVo.getReviewer());
                        knowledgeDocumentVersionVo.setReviewerName(reviewerUserVo.getUserName());
                        knowledgeDocumentVersionVo.setReviewerInfo(reviewerUserVo.getUserInfo());
                    }
                    knowledgeDocumentVersionVo.setIsDeletable(knowledgeDocumentService.isDeletable(knowledgeDocumentVersionVo));
                    knowledgeDocumentVersionVo.setIsEditable(knowledgeDocumentService.isEditable(knowledgeDocumentVersionVo));
                }
                resultObj.put("tbodyList", knowledgeDocumentVersionList);
            }
            return resultObj;
        });
        
        map.put(KnowledgeType.COLLECT.getValue(), (jsonObj) -> {
            JSONObject resultObj = new JSONObject();
            resultObj.put("theadList", getMyFavoritesTheadList());
            resultObj.put("tbodyList", new ArrayList<>());
            KnowledgeDocumentCollectVo searchVo = JSON.toJavaObject(jsonObj, KnowledgeDocumentCollectVo.class);
            searchVo.setUserUuid(UserContext.get().getUserUuid(true));
            int pageCount = 0;
            if(searchVo.getNeedPage()) {
                int rowNum = knowledgeDocumentMapper.getKnowledgeDocumentVersionMyCollectCount(searchVo);
                pageCount = PageUtil.getPageCount(rowNum, searchVo.getPageSize());
                resultObj.put("currentPage", searchVo.getCurrentPage());
                resultObj.put("pageSize", searchVo.getPageSize());
                resultObj.put("pageCount", pageCount);
                resultObj.put("rowNum", rowNum);
            }
            if(!searchVo.getNeedPage() || searchVo.getCurrentPage() <= pageCount) {
                List<KnowledgeDocumentVersionVo> knowledgeDocumentVersionList = knowledgeDocumentMapper.getKnowledgeDocumentVersionMyCollectList(searchVo);
                for(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo : knowledgeDocumentVersionList) {
                    knowledgeDocumentVersionVo.setIsDeletable(knowledgeDocumentService.isDeletable(knowledgeDocumentVersionVo));
                    knowledgeDocumentVersionVo.setIsEditable(1);
                    knowledgeDocumentVersionVo.setIsCollect(1);
                    knowledgeDocumentVersionVo.setAutoGenerateId(false);
                    knowledgeDocumentVersionVo.setId(null);
                }
                resultObj.put("tbodyList", knowledgeDocumentVersionList);
            }
            return resultObj;
        });
        
        map.put(KnowledgeType.DRAFT.getValue(), (jsonObj) -> { 
            JSONObject resultObj = new JSONObject();
            resultObj.put("theadList", getMyDraftTheadList());
            resultObj.put("tbodyList", new ArrayList<>());
            List<String> statusList = Arrays.asList(KnowledgeDocumentVersionStatus.DRAFT.getValue());
            KnowledgeDocumentVersionVo searchVo = JSON.toJavaObject(jsonObj, KnowledgeDocumentVersionVo.class);
            searchVo.setLcu(UserContext.get().getUserUuid(true));
            searchVo.setStatusList(statusList);
            int pageCount = 0;
            if(searchVo.getNeedPage()) {
                int rowNum = knowledgeDocumentMapper.getKnowledgeDocumentVersionMyVersionCount(searchVo);
                pageCount = PageUtil.getPageCount(rowNum, searchVo.getPageSize());
                resultObj.put("currentPage", searchVo.getCurrentPage());
                resultObj.put("pageSize", searchVo.getPageSize());
                resultObj.put("pageCount", pageCount);
                resultObj.put("rowNum", rowNum);
            }
            if(!searchVo.getNeedPage() || searchVo.getCurrentPage() <= pageCount) {
                List<KnowledgeDocumentVersionVo> knowledgeDocumentVersionList = knowledgeDocumentMapper.getKnowledgeDocumentVersionMyVersionList(searchVo);
                for(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo : knowledgeDocumentVersionList) {
                    knowledgeDocumentVersionVo.setIsDeletable(1);
                    knowledgeDocumentVersionVo.setIsEditable(knowledgeDocumentService.isEditable(knowledgeDocumentVersionVo));
                }
                resultObj.put("tbodyList", knowledgeDocumentVersionList);
            }
            return resultObj;
        });
    }
    
    @Override
    public String getToken() {
        return "knowledge/document/list";
    }

    @Override
    public String getName() {
        return "查询文档列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
        @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字，匹配名称"),
        @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true"),
        @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页条目"),
        @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
        @Param(name = "knowledgeType", type = ApiParamType.ENUM, rule = "all,waitingforreview,share,collect,draft",isRequired = true, desc = "知识类型"),
        @Param(name = "knowledgeDocumentTypeUuid", type = ApiParamType.STRING, minLength = 32, maxLength = 32, desc = "类型id")
    })
    @Output({
        @Param(explode = BasePageVo.class),
        @Param(name = "theadList", type = ApiParamType.JSONARRAY, desc = "表头列表"),
        @Param(name = "tbodyList", explode = KnowledgeDocumentVersionVo[].class, desc = "文档列表")
    })
    @Description(desc = "查询文档列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String knowledgeType = jsonObj.getString("knowledgeType");
        return map.computeIfAbsent(knowledgeType, k -> (param) -> new JSONObject()).apply(jsonObj);
    }
    
    @SuppressWarnings({"serial"})
    private JSONArray getTheadList() {
        JSONArray theadList = new JSONArray();
        theadList.add(new JSONObject() {{this.put("title", ""); this.put("key", "isCollect");}});
        theadList.add(new JSONObject() {{this.put("title", "标题"); this.put("key", "title");}});
        theadList.add(new JSONObject() {{this.put("title", "提交人"); this.put("key", "lcuName");}});
        theadList.add(new JSONObject() {{this.put("title", "通过审批时间"); this.put("key", "reviewTime");}});
        theadList.add(new JSONObject() {{this.put("key", "action");}});
        return theadList;
    }
    
    @SuppressWarnings({"serial"})
    private JSONArray getWaitingForMyReviewTheadList() {
        JSONArray theadList = new JSONArray();
        theadList.add(new JSONObject() {{this.put("title", "标题"); this.put("key", "title");}});
        theadList.add(new JSONObject() {{this.put("title", "提交人"); this.put("key", "lcuName");}});
        theadList.add(new JSONObject() {{this.put("title", "提交时间"); this.put("key", "lcd");}});
        theadList.add(new JSONObject() {{this.put("key", "action");}});
        return theadList;
    }
    
    @SuppressWarnings({"serial"})
    private JSONArray getMyShareTheadList() {
        JSONArray theadList = new JSONArray();
        theadList.add(new JSONObject() {{this.put("title", "标题"); this.put("key", "title");}});
        theadList.add(new JSONObject() {{this.put("title", "提交时间"); this.put("key", "lcd");}});
        theadList.add(new JSONObject() {{this.put("title", "审核人"); this.put("key", "reviewer");}});
        theadList.add(new JSONObject() {{this.put("title", "审核时间"); this.put("key", "reviewTime");}});
        theadList.add(new JSONObject() {{this.put("title", "原版本"); this.put("key", "fromVersionName");}});
        theadList.add(new JSONObject() {{this.put("title", "新版本"); this.put("key", "versionName");}});
        theadList.add(new JSONObject() {{this.put("title", "状态"); this.put("key", "statusVo.text");}});
        theadList.add(new JSONObject() {{this.put("key", "action");}});
        return theadList;
    }
    
    @SuppressWarnings({"serial"})
    private JSONArray getMyFavoritesTheadList() {
        JSONArray theadList = new JSONArray();
        theadList.add(new JSONObject() {{this.put("title", ""); this.put("key", "isCollect");}});
        theadList.add(new JSONObject() {{this.put("title", "标题"); this.put("key", "title");}});
        theadList.add(new JSONObject() {{this.put("title", "创建人"); this.put("key", "fcuName");}});
        theadList.add(new JSONObject() {{this.put("title", "最后修改人"); this.put("key", "lcuName");}});
        theadList.add(new JSONObject() {{this.put("title", "最后一次审批时间"); this.put("key", "reviewTime");}});
        theadList.add(new JSONObject() {{this.put("key", "action");}});
        return theadList;
    }
    
    @SuppressWarnings({"serial"})
    private JSONArray getMyDraftTheadList() {
        JSONArray theadList = new JSONArray();
        theadList.add(new JSONObject() {{this.put("title", "标题"); this.put("key", "title");}});
        theadList.add(new JSONObject() {{this.put("title", "原版本"); this.put("key", "fromVersionName");}});
        theadList.add(new JSONObject() {{this.put("title", "最后一次修改时间"); this.put("key", "lcd");}});
        theadList.add(new JSONObject() {{this.put("title", "状态"); this.put("key", "statusVo.text");}});
        theadList.add(new JSONObject() {{this.put("key", "action");}});
        return theadList;
    }
}
