package codedriver.module.knowledge.elasticsearch.handler;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.techsure.multiattrsearch.MultiAttrsObject;
import com.techsure.multiattrsearch.query.QueryResult;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.common.constvalue.Expression;
import codedriver.framework.common.util.FileUtil;
import codedriver.framework.elasticsearch.core.ElasticSearchHandlerBase;
import codedriver.framework.file.dao.mapper.FileMapper;
import codedriver.framework.file.dto.FileVo;
import codedriver.framework.util.HtmlUtil;
import codedriver.framework.util.TikaUtil;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentFileVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentLineVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.elasticsearch.constvalue.ESHandler;

@Service
public class EsKnowledegeVersionHandler extends ElasticSearchHandlerBase<KnowledgeDocumentVersionVo, JSONObject> {
    Logger logger = LoggerFactory.getLogger(EsKnowledegeVersionHandler.class);
    
    @Autowired
    KnowledgeDocumentMapper knowledgeDocumentMapper;
    
    @Autowired
    FileMapper fileMapper;
   
    @Override
    public String getDocument() {
        return ESHandler.KNOWLEDGE_VERSION.getValue();
    }

    @Override
    public JSONObject mySave(Long documentVersionId) {
        JSONObject esObject = new JSONObject();
        KnowledgeDocumentVersionVo  documentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(documentVersionId);
        KnowledgeDocumentVo documentVo =  knowledgeDocumentMapper.getKnowledgeDocumentById(documentVersionVo.getKnowledgeDocumentId());
        //获取知识内容
        List<KnowledgeDocumentLineVo>  documentLineList = knowledgeDocumentMapper.getKnowledgeDocumentLineListByKnowledgeDocumentVersionId(documentVersionId);
        StringBuilder contentsb = new StringBuilder();
        for(KnowledgeDocumentLineVo line : documentLineList) {
            contentsb.append(line.getContent());
        }
        //获取附件内容 
        StringBuilder fileContentsb = new StringBuilder();
        List<Long> fileIdList = knowledgeDocumentMapper.getKnowledgeDocumentFileIdListByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentFileVo(documentVersionVo.getKnowledgeDocumentId(),documentVersionId));
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
        esObject.put("title", documentVo.getTitle());
        esObject.put("content", HtmlUtil.removeHtml(contentsb.toString(), null));
        esObject.put("id", documentVersionVo.getId());
        return esObject;
    }

    @Override
    public String buildSql(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo) {
        //仅全局搜索知识标题和内容
        String sql = StringUtils.EMPTY;
        String whereSql = StringUtils.EMPTY;
        if( StringUtils.isNotBlank(knowledgeDocumentVersionVo.getKeyword())) {
            String titleCondition = String.format(Expression.MATCH.getExpressionEs(), "title",knowledgeDocumentVersionVo.getKeyword());
            String contentCondition = String.format(Expression.MATCH.getExpressionEs(), "content",knowledgeDocumentVersionVo.getKeyword());
            whereSql = String.format("(%s or %s)", titleCondition,contentCondition);
        }
        if(StringUtils.isNotBlank(whereSql)){
            whereSql = " where " + whereSql;
        }
        sql = String.format("select id,#title#,#content# from %s %s ", TenantContext.get().getTenantUuid(),whereSql);
        return sql;
    }

    @Override
    protected JSONObject makeupQueryResult(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo, QueryResult result) {
        List<MultiAttrsObject> resultData = result.getData();
        JSONObject esResultJson = new JSONObject();
        List<Long> knowledgeDocumentVersionIdList = new ArrayList<Long>();
        for (MultiAttrsObject el : resultData) {
            esResultJson.put(el.getId(), el.getHighlightData());
            knowledgeDocumentVersionIdList.add(Long.parseLong(el.getId()));
        }
        esResultJson.put("knowledgeDocumentVersionIdList", knowledgeDocumentVersionIdList);
        return esResultJson;
    }

}
