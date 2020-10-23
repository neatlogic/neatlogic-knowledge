package codedriver.module.knowledge.elasticsearch.handler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.multiattrsearch.MultiAttrsObject;
import com.techsure.multiattrsearch.query.QueryResult;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.common.constvalue.Expression;
import codedriver.framework.elasticsearch.core.ElasticSearchHandlerBase;
import codedriver.framework.util.HtmlUtil;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentLineVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.elasticsearch.constvalue.ESHandler;

@Service
public class EsKnowledegeHandler extends ElasticSearchHandlerBase<KnowledgeDocumentVo, JSONArray> {
    Logger logger = LoggerFactory.getLogger(EsKnowledegeHandler.class);

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
        
        // TODO 获取知识标签
        
        esObject.put("versionid", documentVo.getVersion());
        esObject.put("typeuuid", documentVo.getKnowledgeDocumentTypeUuid());
        esObject.put("circleid", documentVo.getKnowledgeCircleId());
        esObject.put("title", documentVo.getTitle());
        //esObject.put("contentincludehtml", contentsb.toString());
        esObject.put("content", HtmlUtil.removeHtml(contentsb.toString(), null));
        esObject.put("fcu", documentVo.getFcu());
        esObject.put("fcd", documentVo.getFcd());
        return esObject;
    }

    @Override
    public String buildSql(KnowledgeDocumentVo knowledgeDocumentVo) {
        String titleCondition = String.format(Expression.MATCH.getExpressionEs(), "title",knowledgeDocumentVo.getKeyword());
        String contentCondition = String.format(Expression.MATCH.getExpressionEs(), "content",knowledgeDocumentVo.getKeyword());
        String sql = String.format("select versionid,typeuuid,circleid,#title#,#content#,fcu,fcd from %s where %s or %s limit %d,%d ", TenantContext.get().getTenantUuid(),
           titleCondition,contentCondition,knowledgeDocumentVo.getStartNum(),knowledgeDocumentVo.getPageSize());
        return sql;
       
    }

    @Override
    protected JSONArray makeupQueryResult(KnowledgeDocumentVo t, QueryResult result) {
        List<MultiAttrsObject> resultData = result.getData();
        JSONArray dataArray = new JSONArray();
        for (MultiAttrsObject el : resultData) {
            JSONObject documentJson = new JSONObject();
            JSONObject highlightData = el.getHighlightData();
            documentJson.put("id", el.getId());
           // knowledgeDocumentMapper.getKnowledgeDocumentVersionById(id)
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
        return dataArray;
    }
    
  
}
