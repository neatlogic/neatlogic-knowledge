package codedriver.module.knowledge.elasticsearch.handler;

import codedriver.framework.elasticsearch.core.ElasticSearchHandlerBase;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class EsKnowledegeVersionHandler extends ElasticSearchHandlerBase<KnowledgeDocumentVersionVo, JSONObject> {
    /*Logger logger = LoggerFactory.getLogger(EsKnowledegeVersionHandler.class);
    
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
        sql = String.format("select id,#title#,#content# from %s %s limit 1000", TenantContext.get().getTenantUuid(),whereSql);
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
    
    @Override
    protected JSONObject makeupQueryIterateResult(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo, QueryResultSet resultSet) {
        JSONObject esResultJson = new JSONObject();
        List<Long> knowledgeDocumentVersionIdList = new ArrayList<Long>();
        while (resultSet.hasMoreResults()) {
            QueryResult result = resultSet.fetchResult();
            List<MultiAttrsObject> resultData = result.getData();
            for (MultiAttrsObject el : resultData) {
                esResultJson.put(el.getId(), el.getHighlightData());
                knowledgeDocumentVersionIdList.add(Long.parseLong(el.getId()));
            }
        }
        esResultJson.put("knowledgeDocumentVersionIdList", knowledgeDocumentVersionIdList);
        return esResultJson;
    }*/

}
