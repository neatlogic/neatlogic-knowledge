package codedriver.module.knowledge.elasticsearch.handler;

import codedriver.framework.elasticsearch.core.ElasticSearchHandlerBase;
import codedriver.framework.knowledge.dto.KnowledgeDocumentVo;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class EsKnowledegeHandler extends ElasticSearchHandlerBase<KnowledgeDocumentVo, JSONObject> {
    /*Logger logger = LoggerFactory.getLogger(EsKnowledegeHandler.class);
    
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
        //List<Long> tagIdList = knowledgeDocumentMapper.getKnowledgeDocumentTagIdListByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentTagVo(documentVo.getId(),documentVo.getKnowledgeDocumentVersionId()));
        //esObject.put("taglist", tagIdList);
        esObject.put("versionid", documentVo.getVersion());
        esObject.put("typeuuid", documentVo.getKnowledgeDocumentTypeUuid());
        esObject.put("circleid", documentVo.getKnowledgeCircleId());
        esObject.put("title", documentVo.getTitle());
        esObject.put("content", HtmlUtil.removeHtml(contentsb.toString(), null));
        esObject.put("fcu", documentVo.getFcu());
        esObject.put("fcd", TimeUtil.convertDateToString(documentVo.getFcd(), TimeUtil.YYYY_MM_DD_HH_MM_SS));
        esObject.put("id", documentId);
        return esObject;
    }

    @Override
    public String buildSql(KnowledgeDocumentVo knowledgeDocumentVo) {
        //仅全局搜索知识标题和内容
        String sql = StringUtils.EMPTY;
        String whereSql = StringUtils.EMPTY;
        if( StringUtils.isNotBlank(knowledgeDocumentVo.getKeyword())) {
            String titleCondition = String.format(Expression.MATCH.getExpressionEs(), "title",knowledgeDocumentVo.getKeyword());
            String contentCondition = String.format(Expression.MATCH.getExpressionEs(), "content",knowledgeDocumentVo.getKeyword());
            whereSql = String.format("(%s or %s)", titleCondition,contentCondition);
        }
        if(StringUtils.isNotBlank(whereSql)){
            whereSql = " where " + whereSql;
        }
        sql = String.format("select id,#title#,#content# from %s %s limit 1000", TenantContext.get().getTenantUuid(),whereSql);
        return sql;
    }

    @Override
    protected JSONObject makeupQueryResult(KnowledgeDocumentVo knowledgeDocumentVo, QueryResult result) {
        List<MultiAttrsObject> resultData = result.getData();
        JSONObject esResultJson = new JSONObject();
        List<Long> knowledgeDocumentIdList = new ArrayList<Long>();
        for (MultiAttrsObject el : resultData) {
            esResultJson.put(el.getId(), el.getHighlightData());
            knowledgeDocumentIdList.add(Long.parseLong(el.getId()));
        }
        esResultJson.put("knowledgeDocumentIdList", knowledgeDocumentIdList);
        return esResultJson;
    }
    
    @Override
    protected JSONObject makeupQueryIterateResult(KnowledgeDocumentVo knowledgeDocumentVo, QueryResultSet resultSet) {
        JSONObject esResultJson = new JSONObject();
        List<Long> knowledgeDocumentIdList = new ArrayList<Long>();
        while (resultSet.hasMoreResults()) {
            QueryResult result = resultSet.fetchResult();
            List<MultiAttrsObject> resultData = result.getData();
            for (MultiAttrsObject el : resultData) {
                esResultJson.put(el.getId(), el.getHighlightData());
                knowledgeDocumentIdList.add(Long.parseLong(el.getId()));
            }
        }
        esResultJson.put("knowledgeDocumentIdList", knowledgeDocumentIdList);
        return esResultJson;
    }*/

}
