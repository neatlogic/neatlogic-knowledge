package codedriver.module.knowledge.elasticsearch.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.multiattrsearch.MultiAttrsObject;
import com.techsure.multiattrsearch.query.QueryResult;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.Expression;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.elasticsearch.core.ElasticSearchHandlerBase;
import codedriver.framework.util.HtmlUtil;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentCollectVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentLineVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentTagVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.elasticsearch.constvalue.ESHandler;

@Service
public class EsKnowledegeHandler extends ElasticSearchHandlerBase<KnowledgeDocumentVo, JSONObject> {
    Logger logger = LoggerFactory.getLogger(EsKnowledegeHandler.class);
    
    private Map<String, Function<String, String>> map = new HashMap<>();

    {
        map.put("waitingforreview", sql -> getMyWaitingForReviewSql());
        map.put("share", sql -> getMyShareSql());
        map.put("collect", sql -> getMyCollectSql());
        map.put("draft", sql -> getMyDraftSql());
    }
    
    
    @Autowired
    KnowledgeDocumentMapper knowledgeDocumentMapper;
   
    @Override
    public String getDocument() {
        return ESHandler.KNOWLEDGE.getValue();
    }

    @Override
    public JSONObject mySave(Long documentId) {
        JSONObject esObject = new JSONObject();
        KnowledgeDocumentVo documentVo =  knowledgeDocumentMapper.getKnowledgeDocumentById(documentId);
        //获取知识内容
        List<KnowledgeDocumentLineVo>  documentLineList = knowledgeDocumentMapper.getKnowledgeDocumentLineListByKnowledgeDocumentVersionId(documentVo.getKnowledgeDocumentVersionId());
        StringBuilder contentsb = new StringBuilder();
        for(KnowledgeDocumentLineVo line : documentLineList) {
            contentsb.append(line.getContent());
        }
        
        // TODO 获取附件内容 
        
        
        List<Long> tagIdList = knowledgeDocumentMapper.getKnowledgeDocumentTagIdListByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentTagVo(documentVo.getId(),documentVo.getKnowledgeDocumentVersionId()));
        esObject.put("taglist", tagIdList);
        esObject.put("versionid", documentVo.getVersion());
        esObject.put("typeuuid", documentVo.getKnowledgeDocumentTypeUuid());
        esObject.put("circleid", documentVo.getKnowledgeCircleId());
        esObject.put("title", documentVo.getTitle());
        esObject.put("content", HtmlUtil.removeHtml(contentsb.toString(), null));
        esObject.put("fcu", documentVo.getFcu());
        esObject.put("fcd", documentVo.getFcd());
        esObject.put("id", documentId);
        return esObject;
    }

    @Override
    public String buildSql(KnowledgeDocumentVo knowledgeDocumentVo) {
        String titleCondition = String.format(Expression.MATCH.getExpressionEs(), "title",knowledgeDocumentVo.getKeyword());
        String contentCondition = String.format(Expression.MATCH.getExpressionEs(), "content",knowledgeDocumentVo.getKeyword());
        //根据type 获取对应where sql
        String typeSql = StringUtils.EMPTY;
        Function<String, String> result = map.get(knowledgeDocumentVo.getType());
        if (result != null) {
            typeSql = result.apply("");
        }
        //拼接查询sql
        String sql = String.format("select versionid,typeuuid,circleid,#title#,#content#,fcu,fcd from %s where (%s or %s) %s limit %d,%d ", TenantContext.get().getTenantUuid(),
           titleCondition,contentCondition,typeSql,knowledgeDocumentVo.getStartNum(),knowledgeDocumentVo.getPageSize());
        return sql;
       
    }

    @Override
    protected JSONObject makeupQueryResult(KnowledgeDocumentVo t, QueryResult result) {
        JSONObject resultJson = new JSONObject();
        List<MultiAttrsObject> resultData = result.getData();
        JSONArray dataArray = new JSONArray();
        for (MultiAttrsObject el : resultData) {
            JSONObject documentJson = new JSONObject();
            JSONObject highlightData = el.getHighlightData();
            documentJson.put("id", el.getId());
            KnowledgeDocumentVo documenmtVo = knowledgeDocumentMapper.getKnowledgeDocumentById(Long.valueOf(el.getId()));
            if(documenmtVo != null) {
                if(highlightData.containsKey("title.txt")) {
                    documenmtVo.setTitle(highlightData.getString("title.txt"));
                }
                if(highlightData.containsKey("content.txt")) {
                    documenmtVo.setContent( highlightData.getString("content.txt"));
                }
                dataArray.add(documenmtVo);
            }
        }
        resultJson.put("dataList", dataArray);
        resultJson.put("rowNum", result.getTotal());
        resultJson.put("pageSize", t.getPageSize());
        resultJson.put("currentPage", t.getCurrentPage());
        resultJson.put("pageCount", PageUtil.getPageCount(result.getTotal(), t.getPageSize()));
        return resultJson;
    }
    
    private String getSqlByKnowledgeVersionList(List<KnowledgeDocumentVersionVo>  knowledgeVersionList) {
        String sql = StringUtils.EMPTY;
        List<String> knowledgeDocumentIdList = new ArrayList<String>();
        for(KnowledgeDocumentVersionVo knowVersion : knowledgeVersionList) {
            if(!knowledgeDocumentIdList.contains(knowVersion.getKnowledgeDocumentId().toString())) {
                knowledgeDocumentIdList.add(knowVersion.getKnowledgeDocumentId().toString());
            }
        }
        if(CollectionUtils.isNotEmpty(knowledgeDocumentIdList)) {
            sql = " and " + String.format(Expression.INCLUDE.getExpressionEs(), "id",String.format(" '%s'" ,String.join("','", knowledgeDocumentIdList)));
        }
        return sql;
    }
    
    /**
    * @Author 89770
    * @Time 2020年10月27日  
    * @Description: 获取我的草稿sql
    * @Param 
    * @return
     */
    private String getMyDraftSql() {
        KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = new KnowledgeDocumentVersionVo();
        knowledgeDocumentVersionVo.setStatusList(Arrays.asList(KnowledgeDocumentVersionStatus.DRAFT.getValue()));
        knowledgeDocumentVersionVo.setLcu(UserContext.get().getUserUuid());
        List<KnowledgeDocumentVersionVo>  knowledgeVersionList = knowledgeDocumentMapper.getKnowledgeDocumentVersionMyVersionList(knowledgeDocumentVersionVo);
        return getSqlByKnowledgeVersionList(knowledgeVersionList);
    }

    /**
    * @Author 89770
    * @Time 2020年10月27日  
    * @Description: 获取我的收藏sql
    * @Param 
    * @return
     */
    private String getMyCollectSql() {
        KnowledgeDocumentCollectVo knowledgeDocumentCollectVo = new KnowledgeDocumentCollectVo();
        knowledgeDocumentCollectVo.setUserUuid(UserContext.get().getUserUuid(true));
        List<KnowledgeDocumentVersionVo> knowledgeVersionList =  knowledgeDocumentMapper.getKnowledgeDocumentVersionMyCollectList(knowledgeDocumentCollectVo);
        return getSqlByKnowledgeVersionList(knowledgeVersionList);
    }

    /**
    * @Author 89770
    * @Time 2020年10月27日  
    * @Description: 获取我的分享sql
    * @Param 
    * @return
     */
    private String getMyShareSql() {
        KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = new KnowledgeDocumentVersionVo();
        knowledgeDocumentVersionVo.setStatusList(Arrays.asList(KnowledgeDocumentVersionStatus.PASSED.getValue(),KnowledgeDocumentVersionStatus.SUBMITTED.getValue(),KnowledgeDocumentVersionStatus.REJECTED.getValue(),KnowledgeDocumentVersionStatus.EXPIRED.getValue()));
        knowledgeDocumentVersionVo.setLcu(UserContext.get().getUserUuid());
        knowledgeDocumentVersionVo.setNeedPage(false);
        List<KnowledgeDocumentVersionVo>  knowledgeVersionList = knowledgeDocumentMapper.getKnowledgeDocumentVersionMyVersionList(knowledgeDocumentVersionVo);
        return getSqlByKnowledgeVersionList(knowledgeVersionList);
    }

    /**
    * @Author 89770
    * @Time 2020年10月27日  
    * @Description: 获取待我审批sql
    * @Param 
    * @return
     */
    private String getMyWaitingForReviewSql() {
        KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = new KnowledgeDocumentVersionVo();
        knowledgeDocumentVersionVo.setReviewer(UserContext.get().getUserUuid(true));
        List<KnowledgeDocumentVersionVo> knowledgeVersionList = knowledgeDocumentMapper.getKnowledgeDocumentWaitingForReviewList(knowledgeDocumentVersionVo);
        return getSqlByKnowledgeVersionList(knowledgeVersionList);
    }
}
