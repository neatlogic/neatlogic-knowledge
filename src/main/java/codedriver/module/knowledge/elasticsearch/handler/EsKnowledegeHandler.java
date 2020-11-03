package codedriver.module.knowledge.elasticsearch.handler;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.techsure.multiattrsearch.MultiAttrsObject;
import com.techsure.multiattrsearch.query.QueryResult;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.Expression;
import codedriver.framework.common.util.FileUtil;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.elasticsearch.core.ElasticSearchHandlerBase;
import codedriver.framework.file.dao.mapper.FileMapper;
import codedriver.framework.file.dto.FileVo;
import codedriver.framework.util.HtmlUtil;
import codedriver.framework.util.TikaUtil;
import codedriver.framework.util.TimeUtil;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentCollectVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentFileVo;
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
    
    @Autowired
    FileMapper fileMapper;
   
    @Override
    public String getDocument() {
        return ESHandler.KNOWLEDGE.getValue();
    }

    @Override
    public JSONObject mySave(Long documentId) {
        JSONObject esObject = new JSONObject();
        KnowledgeDocumentVo documentVo =  knowledgeDocumentMapper.getKnowledgeDocumentById(documentId);
        KnowledgeDocumentVersionVo  documentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(documentVo.getKnowledgeDocumentVersionId());
        //获取知识内容
        List<KnowledgeDocumentLineVo>  documentLineList = knowledgeDocumentMapper.getKnowledgeDocumentLineListByKnowledgeDocumentVersionId(documentVo.getKnowledgeDocumentVersionId());
        StringBuilder contentsb = new StringBuilder();
        for(KnowledgeDocumentLineVo line : documentLineList) {
            contentsb.append(line.getContent());
        }
        //获取附件内容 
        StringBuilder fileContentsb = new StringBuilder();
        List<Long> fileIdList = knowledgeDocumentMapper.getKnowledgeDocumentFileIdListByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentFileVo(documentVo.getId(),documentVo.getKnowledgeDocumentVersionId()));
        if(CollectionUtils.isNotEmpty(fileIdList)) {
            List<FileVo> fileVoList = fileMapper.getFileListByIdList(fileIdList);
            try {
                for(FileVo fileVo : fileVoList) {
                    InputStream in = FileUtil.getData(fileVo.getPath());
                    JSONObject fileJson = TikaUtil.getFileContentByAutoParser(in, true);
                    fileContentsb.append(fileJson.getString("content"));
                }
            } catch ( Exception e) {
                logger.error(e.getMessage(),e);
            }
        }
        esObject.put("filecontent", fileContentsb.toString());
        //tagList
        List<Long> tagIdList = knowledgeDocumentMapper.getKnowledgeDocumentTagIdListByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentTagVo(documentVo.getId(),documentVo.getKnowledgeDocumentVersionId()));
        esObject.put("taglist", tagIdList);
        esObject.put("versionid", documentVo.getVersion());
        esObject.put("typeuuid", documentVo.getKnowledgeDocumentTypeUuid());
        esObject.put("circleid", documentVo.getKnowledgeCircleId());
        esObject.put("title", documentVersionVo.getTitle());
        esObject.put("content", HtmlUtil.removeHtml(contentsb.toString(), null));
        esObject.put("fcu", documentVo.getFcu());
        esObject.put("fcd", TimeUtil.convertDateToString(documentVo.getFcd(), TimeUtil.YYYY_MM_DD_HH_MM_SS));
        esObject.put("id", documentId);
        return esObject;
    }

    @Override
    public String buildSql(KnowledgeDocumentVo knowledgeDocumentVo) {
        String sql = StringUtils.EMPTY;
        String whereSql = StringUtils.EMPTY;
        if( StringUtils.isNotBlank(knowledgeDocumentVo.getKeyword())) {
            String titleCondition = String.format(Expression.MATCH.getExpressionEs(), "title",knowledgeDocumentVo.getKeyword());
            String contentCondition = String.format(Expression.MATCH.getExpressionEs(), "content",knowledgeDocumentVo.getKeyword());
            whereSql = String.format("(%s or %s)", titleCondition,contentCondition);
        }
        //根据type 获取对应where sql
        Function<String, String> result = map.get(knowledgeDocumentVo.getType());
        if (result != null) {
            String typeSql = result.apply("");
            if(StringUtils.isNotBlank(typeSql)&&StringUtils.isNotBlank(whereSql)) {
                whereSql = whereSql +" and " + typeSql;
            }
        }
        //拼接查询sql
        if(StringUtils.isNotBlank(whereSql)) {
            whereSql = " where " + whereSql;
        }
        sql = String.format("select versionid,typeuuid,circleid,#title#,#content#,fcu,fcd from %s %s limit %d,%d ", TenantContext.get().getTenantUuid(),
            whereSql,knowledgeDocumentVo.getStartNum(),knowledgeDocumentVo.getPageSize());
        return sql;
       
    }

    @Override
    protected JSONObject makeupQueryResult(KnowledgeDocumentVo t, QueryResult result) {
        JSONObject resultJson = new JSONObject();
        List<MultiAttrsObject> resultData = result.getData();
        List<Long> documentIdList = new ArrayList<Long>();
        Map<String,JSONObject> documentHighlightMap = new HashMap<String,JSONObject>();
        for (MultiAttrsObject el : resultData) {
            JSONObject documentJson = new JSONObject();
            JSONObject highlightData = el.getHighlightData();
            documentJson.put("id", el.getId());
            documentIdList.add(Long.valueOf(el.getId()));
            documentHighlightMap.put(el.getId(), highlightData);
           
        }
        //替换 highlight 字段
        List<KnowledgeDocumentVo> documentList = new ArrayList<KnowledgeDocumentVo>();
        if(CollectionUtils.isNotEmpty(documentIdList)) {
            documentList = knowledgeDocumentMapper.getKnowledgeDocumentByIdList(documentIdList);
            for(KnowledgeDocumentVo documentVo : documentList) {
                if(documentVo != null) {
                    JSONObject highlightData = documentHighlightMap.get(documentVo.getId().toString());
                    if(MapUtils.isNotEmpty(highlightData)) {
                        if(highlightData.containsKey("title.txt")) {
                            documentVo.setTitle(String.join("\n", JSONObject.parseArray(highlightData.getString("title.txt"),String.class)));
                        }
                        if(highlightData.containsKey("content.txt")) {
                            documentVo.setContent( String.join("\n", JSONObject.parseArray(highlightData.getString("content.txt"),String.class)));
                        }
                    }
                }
            }
        }
        resultJson.put("dataList", documentList);
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
            sql = String.format(Expression.INCLUDE.getExpressionEs(), "id",String.format(" '%s'" ,String.join("','", knowledgeDocumentIdList)));
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
        knowledgeDocumentVersionVo.setStatusList(Arrays.asList(KnowledgeDocumentVersionStatus.PASSED.getValue(),KnowledgeDocumentVersionStatus.SUBMITTED.getValue(),KnowledgeDocumentVersionStatus.REJECTED.getValue()));
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
