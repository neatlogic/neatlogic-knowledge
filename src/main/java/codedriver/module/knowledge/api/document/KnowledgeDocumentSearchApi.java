package codedriver.module.knowledge.api.document;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.UserVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.HtmlUtil;
import codedriver.framework.util.TimeUtil;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentOperate;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentAuditMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import codedriver.module.knowledge.dto.*;
import codedriver.module.knowledge.exception.KnowledgeDocumentTypeNotFoundException;
import codedriver.module.knowledge.service.KnowledgeDocumentService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentSearchApi extends PrivateApiComponentBase {

    @Resource
    KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    KnowledgeDocumentAuditMapper knowledgeDocumentAuditMapper;

    @Resource
    TeamMapper teamMapper;

    @Resource
    UserMapper userMapper;

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
        String type = jsonObj.getString("type");
        if ("documentVersion".equals(type)) {
            setDocumentVersionList(resultJson, jsonObj);
        } else {
            setDocumentList(resultJson, jsonObj);
        }
        return resultJson;
    }

    /*
     * 根据搜索条件，最终返回知识
     */
    @SuppressWarnings("unchecked")
    private void setDocumentList(JSONObject resultJson, JSONObject jsonObj) {
        JSONObject lcd = jsonObj.getJSONObject("lcd");
        jsonObj.remove("lcd");
        KnowledgeDocumentVo documentVoParam = JSON.toJavaObject(jsonObj, KnowledgeDocumentVo.class);

        if (lcd != null) {
            JSONObject lcdJson = getTime(lcd);
            documentVoParam.setLcdStartTime(lcdJson.getString("startTime"));
            documentVoParam.setLcdEndTime(lcdJson.getString("endTime"));
        }
        //仅根据keyword,从es搜索标题和内容
       /* JSONObject data = null;
        if(StringUtils.isNotBlank(documentVoParam.getKeyword())){
            IElasticSearchHandler<KnowledgeDocumentVo, JSONObject> esHandler = ElasticSearchHandlerFactory.getHandler(ESHandler.KNOWLEDGE.getValue());
            data = JSONObject.parseObject(esHandler.iterateSearch(documentVoParam).toString());
            List<Long> documentIdList = JSONObject.parseArray(data.getJSONArray("knowledgeDocumentIdList").toJSONString(),Long.class);
            //将从es搜索符合的知识送到数据库做二次过滤
            documentVoParam.setKnowledgeDocumentIdList(documentIdList);
        }*/

        //仅根据keyword,从mysql搜索标题和内容
        Map<Long,KnowledgeDocumentVo> documentFtMap = new HashMap<>();
        if (StringUtils.isNotBlank(documentVoParam.getKeyword())&&documentVoParam.getKeyword().trim().length() > 1) {
            String keywordStr = documentVoParam.getKeyword().replaceAll(" ", "\" \"");
            List<KnowledgeDocumentVo> documentFtList = knowledgeDocumentMapper.getKnowledgeDocumentByTitleAndContent(String.format("\"%s\"", keywordStr));
            for(KnowledgeDocumentVo knowledgeDocumentVo : documentFtList){
                documentFtMap.put(knowledgeDocumentVo.getId(),knowledgeDocumentVo);
            }
            documentVoParam.setKnowledgeDocumentIdList(documentFtList.stream().map(KnowledgeDocumentVo::getId).collect(Collectors.toList()));
        }

        //补充查看权限条件参数（圈子成员or圈子审批人）
        getDocumentViewParam(documentVoParam);
        //从db过滤知识
        List<Long> documentIdList = knowledgeDocumentMapper.getKnowledgeDocumentIdList(documentVoParam);
        List<KnowledgeDocumentVo> documentList = null;
        if (CollectionUtils.isNotEmpty(documentIdList)) {
            documentList = knowledgeDocumentMapper.getKnowledgeDocumentByIdList(documentIdList);
        } else {
            documentList = new ArrayList<KnowledgeDocumentVo>();
        }
        Integer total = knowledgeDocumentMapper.getKnowledgeDocumentCount(documentVoParam);

        JSONArray returnDataList = new JSONArray();
        List<Long> collectedKnowledgeDocumentIdList = new ArrayList<Long>();
        List<Long> favorKnowledgeDocumentIdList = new ArrayList<Long>();
        if (CollectionUtils.isNotEmpty(documentList)) {
            collectedKnowledgeDocumentIdList = knowledgeDocumentMapper.getKnowledgeDocumentCollectDocumentIdListByUserUuidAndDocumentIdList(UserContext.get().getUserUuid(true), documentIdList);
            favorKnowledgeDocumentIdList = knowledgeDocumentMapper.getKnowledgeDocumentFavorDocumentIdListByUserUuidAndDocumentIdList(UserContext.get().getUserUuid(true), documentIdList);
        }
        for (KnowledgeDocumentVo knowledgeDocumentVo : documentList) {
            //补充头像信息
            UserVo userVo = userMapper.getUserBaseInfoByUuid(knowledgeDocumentVo.getLcu());
            if (userVo != null) {
                //使用新对象，防止缓存
                UserVo vo = new UserVo();
                BeanUtils.copyProperties(userVo, vo);
                knowledgeDocumentVo.setLcuVo(vo);
//                knowledgeDocumentVo.setLcuName(userVo.getUserName());
//                knowledgeDocumentVo.setLcuInfo(userVo.getUserInfo());
            }
            //如果入参条件存在知识类型，则直接判断当前用户是不是知识圈审批人
            Integer isApprover = null;
            List<Long> approveCircleIdList = new ArrayList<Long>();
            List<String> teamUuidList = teamMapper.getTeamUuidListByUserUuid(UserContext.get().getUserUuid(true));
            if (StringUtils.isNotBlank(documentVoParam.getKnowledgeDocumentTypeUuid())) {
                KnowledgeDocumentTypeVo knowledgeDocumentTypeVo = knowledgeDocumentTypeMapper.getTypeByUuid(documentVoParam.getKnowledgeDocumentTypeUuid());
                if (knowledgeDocumentTypeVo == null) {
                    throw new KnowledgeDocumentTypeNotFoundException(documentVoParam.getKnowledgeDocumentTypeUuid());
                }
                isApprover = knowledgeDocumentMapper.checkUserIsApprover(knowledgeDocumentTypeVo.getKnowledgeCircleId(), UserContext.get().getUserUuid(true), teamUuidList, UserContext.get().getRoleUuidList());
            } else {
                //查询当前登录人所有圈子的审批权限
                approveCircleIdList = knowledgeDocumentMapper.getUserAllApproverCircleIdList(UserContext.get().getUserUuid(true), teamUuidList, UserContext.get().getRoleUuidList());

            }
            //判断是否有权限编辑删除
//            List<Long> knowledgeDocumentIdTmpList = new ArrayList<>();
            knowledgeDocumentVo.setIsEditable(1);
            if (isApprover == null) {
                isApprover = 0;
                if (approveCircleIdList.contains(knowledgeDocumentVo.getKnowledgeCircleId())) {
                    isApprover = 1;
                }
            }
            knowledgeDocumentVo.setIsDeletable(isApprover);
//            knowledgeDocumentIdTmpList.add(knowledgeDocumentVo.getId());

            //判断本人是否已经收藏
            if (collectedKnowledgeDocumentIdList.contains(knowledgeDocumentVo.getId())) {
                knowledgeDocumentVo.setIsCollect(1);
            }

            //判断本人是否已经点赞
            if (favorKnowledgeDocumentIdList.contains(knowledgeDocumentVo.getId())) {
                knowledgeDocumentVo.setIsFavor(1);
            }

            //替换 highlight 字段
           /* if(StringUtils.isNotBlank(documentVoParam.getKeyword())){
                JSONObject highlightData = data.getJSONObject(knowledgeDocumentVo.getId().toString());
                if(MapUtils.isNotEmpty(highlightData)) {
                    if(highlightData.containsKey("title.txt")) {
                        knowledgeDocumentVo.setTitle(String.join("\n", JSONObject.parseArray(highlightData.getString("title.txt"),String.class)));
                    }
                    if(highlightData.containsKey("content.txt")) {
                        knowledgeDocumentVo.setContent( String.join("\n", JSONObject.parseArray(highlightData.getString("content.txt"),String.class)));
                    }
                }
            }*/
            //如果es找不到内容 则从数据库获取
            /*if(StringUtils.isBlank(knowledgeDocumentVo.getContent())) {
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
               
            }*/
            //拼接content，并高亮搜索keyword
            StringBuilder contentSb = new StringBuilder();
            List<Integer> lineNumberList = new ArrayList<>();
            int startIndex = 1;
            int endIndex = 6;
            if(MapUtils.isNotEmpty(documentFtMap)&&documentFtMap.containsKey(knowledgeDocumentVo.getId())) {
                KnowledgeDocumentVo keywordDocumentVo = documentFtMap.get(knowledgeDocumentVo.getId());
                List<KnowledgeDocumentLineVo> lineVoList = keywordDocumentVo.getLineList();
                if(CollectionUtils.isNotEmpty(lineVoList)){
                    KnowledgeDocumentLineVo lineVo = lineVoList.get(0);
                    //获取目标line的上下两行
                    endIndex = lineVo.getLineNumber() + 3;
                    if (lineVo.getLineNumber() > 1) {
                        startIndex = lineVo.getLineNumber() - 2;
                    }
                }

            }
            for(int i = startIndex; i< endIndex;i++){
                lineNumberList.add(i);
            }

            List<KnowledgeDocumentLineVo> documentLineList = knowledgeDocumentMapper.getKnowledgeDocumentLineListByKnowledgeDocumentVersionIdAndLineNumberList(knowledgeDocumentVo.getKnowledgeDocumentVersionId(),lineNumberList);
            for(KnowledgeDocumentLineVo lineVo : documentLineList){
                contentSb.append(HtmlUtil.removeHtml(HtmlUtil.decodeHtml(lineVo.getContent()), null).replaceAll("[^0-9a-zA-Z\u4e00-\u9fa5.，,。？“”-]+", ""));
            }
            int startSubIndex = 0;
            int endSubIndex = 1000;
            if (MapUtils.isNotEmpty(documentFtMap) && documentFtMap.containsKey(knowledgeDocumentVo.getId())) {
                String[] keywordArray = documentVoParam.getKeyword().split(" ");
                for (String keyword : keywordArray) {
                    int index = contentSb.indexOf(keyword);
                    if (index > 0) {
                        endSubIndex = index + keyword.length() + endSubIndex / 2;
                        if (index > endSubIndex / 2) {
                            startSubIndex = index - endSubIndex / 2;
                        }
                        break;
                    }
                }
            }
            String content = contentSb.substring(startSubIndex, Math.min(endSubIndex,contentSb.length()));
            //高亮
            if(MapUtils.isNotEmpty(documentFtMap)&&documentFtMap.containsKey(knowledgeDocumentVo.getId())) {
                String[] keywordArray = documentVoParam.getKeyword().split(" ");
                for (String keyword : keywordArray) {
                    //高亮内容
                    content = content.replaceAll(keyword, String.format("<em>%s</em>", keyword));
                    //高亮标题
                    knowledgeDocumentVo.setTitle(knowledgeDocumentVo.getTitle().replaceAll(keyword, String.format("<em>%s</em>", keyword)));
                }
            }
            knowledgeDocumentVo.setContent(content);
            knowledgeDocumentVo.setLineList(null);


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
        documentVoParam.setCircleTeamUuidList(teamMapper.getTeamUuidListByUserUuid(userUuid));
        documentVoParam.setCircleRoleUuidList(userMapper.getRoleUuidListByUserUuid(userUuid));
    }

    /*
     * 根据搜索条件，最终返回知识版本
     */
    private void setDocumentVersionList(JSONObject resultJson, JSONObject jsonObj) {
        JSONObject lcd = jsonObj.getJSONObject("lcd");
        jsonObj.remove("lcd");
        KnowledgeDocumentVersionVo documentVersionVoParam = JSON.toJavaObject(jsonObj, KnowledgeDocumentVersionVo.class);
        if (lcd != null) {
            JSONObject lcdJson = getTime(lcd);
            documentVersionVoParam.setLcdStartTime(lcdJson.getString("startTime"));
            documentVersionVoParam.setLcdEndTime(lcdJson.getString("endTime"));
        }
        JSONObject reviewDate = jsonObj.getJSONObject("reviewDate");
        if (reviewDate != null) {
            JSONObject reviewDateJson = getTime(reviewDate);
            documentVersionVoParam.setReviewDateStartTime(reviewDateJson.getString("startTime"));
            documentVersionVoParam.setReviewDateEndTime(reviewDateJson.getString("endTime"));
        }

        //仅根据keyword,从es搜索标题和内容
        /*JSONObject data = null;
        if(StringUtils.isNotBlank(documentVersionVoParam.getKeyword())){
            IElasticSearchHandler<KnowledgeDocumentVersionVo, JSONObject> esHandler = ElasticSearchHandlerFactory.getHandler(ESHandler.KNOWLEDGE_VERSION.getValue());
            data = JSONObject.parseObject(esHandler.iterateSearch(documentVersionVoParam).toString());
            List<Long> documentVersionIdList = JSONObject.parseArray(data.getJSONArray("knowledgeDocumentVersionIdList").toJSONString(),Long.class);
            //将从es搜索符合的知识送到数据库做二次过滤
            documentVersionVoParam.setKnowledgeDocumentVersionIdList(documentVersionIdList);
        }*/
        Map<Long,KnowledgeDocumentVersionVo> documentVersionFtMap = new HashMap<>();
        if (StringUtils.isNotBlank(documentVersionVoParam.getKeyword())) {
            String keywordStr = documentVersionVoParam.getKeyword().replaceAll(" ", "\" \"");
            List<KnowledgeDocumentVersionVo> documentVersionFtList = knowledgeDocumentMapper.getKnowledgeDocumentVersionByTitleAndContent(String.format("\"%s\"", keywordStr));
            for(KnowledgeDocumentVersionVo knowledgeDocumentVersion : documentVersionFtList){
                documentVersionFtMap.put(knowledgeDocumentVersion.getId(),knowledgeDocumentVersion);
            }
            documentVersionVoParam.setKnowledgeDocumentVersionIdList(documentVersionFtList.stream().map(KnowledgeDocumentVersionVo::getId).collect(Collectors.toList()));
        }

        //拼装 “审批人”条件 
        knowledgeDocumentService.getReviewerParam(documentVersionVoParam);


        //查询符合条件的知识版本
        List<Long> documentVersionIdList = null;
        Integer total = 0;
        //status all
        List<String> statusList = documentVersionVoParam.getStatusList();
        if (statusList.contains(KnowledgeDocumentVersionStatus.ALL.getValue())) {
            documentVersionIdList = knowledgeDocumentMapper.getMyAllReviewKnowledgeDocumentVersionIdList(documentVersionVoParam);
            total = knowledgeDocumentMapper.getMyAllReviewKnowledgeDocumentVersionCount(documentVersionVoParam);
        } else {
            documentVersionIdList = knowledgeDocumentMapper.getKnowledgeDocumentVersionIdList(documentVersionVoParam);
            total = knowledgeDocumentMapper.getKnowledgeDocumentVersionCount(documentVersionVoParam);
        }

        List<KnowledgeDocumentVersionVo> documentVersionList = null;
        if (CollectionUtils.isNotEmpty(documentVersionIdList)) {
            documentVersionList = knowledgeDocumentMapper.getKnowledgeDocumentVersionByIdList(documentVersionIdList);
        } else {
            documentVersionList = new ArrayList<KnowledgeDocumentVersionVo>();
        }


        for (KnowledgeDocumentVersionVo knowledgeDocumentVersionVo : documentVersionList) {
            //跟新操作（如果是草稿,可以删除或编辑）
            if (documentVersionVoParam.getStatusList().contains(KnowledgeDocumentVersionStatus.DRAFT.getValue())) {
                int isApprover = 0;
                if (knowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid())) {
                    isApprover = 1;
                }
                knowledgeDocumentVersionVo.setIsEditable(isApprover);
                knowledgeDocumentVersionVo.setIsDeletable(isApprover);
            }
            //替换 highlight 字段
            /*if(StringUtils.isNotBlank(documentVersionVoParam.getKeyword())){
                JSONObject highlightData = data.getJSONObject(knowledgeDocumentVersionVo.getId().toString());
                if(MapUtils.isNotEmpty(highlightData)) {
                    if(highlightData.containsKey("title.txt")) {
                        knowledgeDocumentVersionVo.setTitle(String.join("\n", JSONObject.parseArray(highlightData.getString("title.txt"),String.class)));
                    }
                    if(highlightData.containsKey("content.txt")) {
                        knowledgeDocumentVersionVo.setContent( String.join("\n", JSONObject.parseArray(highlightData.getString("content.txt"),String.class)));
                    }
                }
            }*/
            //拼接内容
            List<Integer> lineNumberList = new ArrayList<>();
            Integer keywordLineNum = null;
            int startIndex = 1;
            int endIndex = 4;
            StringBuilder contentSb = new StringBuilder();
            if(MapUtils.isNotEmpty(documentVersionFtMap)&&documentVersionFtMap.containsKey(knowledgeDocumentVersionVo.getId())) {
                KnowledgeDocumentVersionVo keywordDocumentVersionVo = documentVersionFtMap.get(knowledgeDocumentVersionVo.getId());
                List<KnowledgeDocumentLineVo> lineVoList = keywordDocumentVersionVo.getKnowledgeDocumentLineList();
                if(CollectionUtils.isNotEmpty(lineVoList)){
                    KnowledgeDocumentLineVo lineVo = lineVoList.get(0);
                    keywordLineNum = lineVo.getLineNumber();
                    //获取目标line的上下两行
                    endIndex = keywordLineNum + 3;
                    if(keywordLineNum-3 >0) {
                        startIndex = keywordLineNum - 3;
                    }
                }
            }
            for(int i = startIndex; i<= endIndex;i++){
                lineNumberList.add(i);
            }

            List<KnowledgeDocumentLineVo> documentLineList = knowledgeDocumentMapper.getKnowledgeDocumentLineListByKnowledgeDocumentVersionIdAndLineNumberList(knowledgeDocumentVersionVo.getId(),lineNumberList);
            for(KnowledgeDocumentLineVo lineVo : documentLineList){
                contentSb.append(HtmlUtil.removeHtml(HtmlUtil.decodeHtml(lineVo.getContent()), null).replaceAll("[^0-9a-zA-Z\u4e00-\u9fa5.，,。？“”-]+", ""));
            }
            int startSubIndex = 0;
            int endSubIndex = 1000;
            if (MapUtils.isNotEmpty(documentVersionFtMap) && documentVersionFtMap.containsKey(knowledgeDocumentVersionVo.getId())) {
                String[] keywordArray = documentVersionVoParam.getKeyword().split(" ");
                for (String keyword : keywordArray) {
                    int index = contentSb.indexOf(keyword);
                    if (index > 0) {
                        endSubIndex = index + keyword.length() + endSubIndex / 2;
                        if (index > endSubIndex / 2) {
                            startSubIndex = index - endSubIndex / 2;
                        }
                        break;
                    }
                }
            }
            String content = contentSb.substring(startSubIndex, Math.min(endSubIndex,contentSb.length()));
            //高亮
            if(MapUtils.isNotEmpty(documentVersionFtMap)&&documentVersionFtMap.containsKey(knowledgeDocumentVersionVo.getId())) {
                String[] keywordArray = documentVersionVoParam.getKeyword().split(" ");
                for (String keyword : keywordArray) {
                    //高亮内容
                    content = content.replaceAll(keyword, String.format("<em>%s</em>", keyword));
                    //高亮标题
                    knowledgeDocumentVersionVo.setTitle(knowledgeDocumentVersionVo.getTitle().replaceAll(keyword, String.format("<em>%s</em>", keyword)));
                }
            }
            knowledgeDocumentVersionVo.setContent(content);
            knowledgeDocumentVersionVo.setKnowledgeDocumentLineList(null);

            //如果审核不通过，则补充原因
            if (KnowledgeDocumentVersionStatus.REJECTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
                KnowledgeDocumentAuditVo rejectAudit = knowledgeDocumentAuditMapper.getKnowledgeDocumentAuditListByDocumentIdAndVersionIdAndOperate(new KnowledgeDocumentAuditVo(knowledgeDocumentVersionVo.getKnowledgeDocumentId(), knowledgeDocumentVersionVo.getId(), KnowledgeDocumentOperate.REJECT.getValue()));
                if (rejectAudit != null) {
                    String rejectReason = knowledgeDocumentAuditMapper.getKnowledgeDocumentAuditConfigStringByHash(rejectAudit.getConfigHash());
                    if (StringUtils.isNotBlank(rejectReason)) {
                        knowledgeDocumentVersionVo.setRejectReason(JSONObject.parseObject(rejectReason).getString("content"));
                    }
                }
            }

            //去掉未提交status
            KnowledgeDocumentVersionStatusVo statusVo = knowledgeDocumentVersionVo.getStatusVo();
            if (knowledgeDocumentVersionVo.getStatusVo() != null && KnowledgeDocumentVersionStatus.DRAFT.getValue().equals(statusVo.getValue())) {
                knowledgeDocumentVersionVo.setStatus(null);
                knowledgeDocumentVersionVo.setStatusVo(null);
            }

            //补充lcu信息
            UserVo userVo = userMapper.getUserBaseInfoByUuid(knowledgeDocumentVersionVo.getLcu());
            if (userVo != null) {
                //使用新对象，防止缓存
                UserVo vo = new UserVo();
                BeanUtils.copyProperties(userVo, vo);
                knowledgeDocumentVersionVo.setLcuVo(vo);
            }
        }

        //补充状态
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
            jsonPass.put("count", knowledgeDocumentMapper.getKnowledgeDocumentVersionCount(documentVersionVoParam));
            statusArray.add(jsonPass);
            //不通过
            JSONObject jsonReject = new JSONObject();
            jsonReject.put("value", KnowledgeDocumentVersionStatus.REJECTED.getValue());
            jsonReject.put("text", KnowledgeDocumentVersionStatus.REJECTED.getText());
            statusTmpList.clear();
            statusTmpList.add(KnowledgeDocumentVersionStatus.REJECTED.getValue());
            documentVersionVoParam.setStatusList(statusTmpList);
            documentVersionVoParam.setIsReviewer(1);
            jsonReject.put("count", knowledgeDocumentMapper.getKnowledgeDocumentVersionCount(documentVersionVoParam));
            statusArray.add(jsonReject);
            //待审批
            jsonSubmit.put("value", KnowledgeDocumentVersionStatus.SUBMITTED.getValue());
            jsonSubmit.put("text", KnowledgeDocumentVersionStatus.SUBMITTED.getText());
            statusTmpList.clear();
            statusTmpList.add(KnowledgeDocumentVersionStatus.SUBMITTED.getValue());
            documentVersionVoParam.setStatusList(statusTmpList);
            knowledgeDocumentService.getReviewerParam(documentVersionVoParam);
            jsonSubmit.put("count", knowledgeDocumentMapper.getKnowledgeDocumentVersionCount(documentVersionVoParam));
            jsonAll.put("value", KnowledgeDocumentVersionStatus.ALL.getValue());
            jsonAll.put("text", KnowledgeDocumentVersionStatus.ALL.getText());
            jsonAll.put("count", jsonSubmit.getLong("count") + jsonPass.getLong("count") + jsonReject.getLong("count"));

            resultJson.put("statusList", statusArray);
        }
        resultJson.put("dataList", documentVersionList);
        resultJson.put("rowNum", total);
        resultJson.put("pageSize", documentVersionVoParam.getPageSize());
        resultJson.put("currentPage", documentVersionVoParam.getCurrentPage());
        resultJson.put("pageCount", PageUtil.getPageCount(total, documentVersionVoParam.getPageSize()));
    }

    /**
     * @return
     * @Author 89770
     * @Time 2020年11月6日
     * @Description: 解析最近修改时间入参
     * @Param
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
