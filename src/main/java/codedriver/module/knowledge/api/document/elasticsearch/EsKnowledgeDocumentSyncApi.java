package codedriver.module.knowledge.api.document.elasticsearch;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.auth.label.KNOWLEDGE_BASE;
import codedriver.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Deprecated
@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class EsKnowledgeDocumentSyncApi extends PrivateApiComponentBase {

    @Autowired
    KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Override
    public String getToken() {
        return "knowledge/es/sync";
    }

    @Override
    public String getName() {
        return "更新es知识数据";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
        @Param(name = "fromDate", type = ApiParamType.STRING, desc = "创建时间>=fromDate"),
        @Param(name = "toDate", type = ApiParamType.STRING, desc = "创建时间<toDate"),
        @Param(name = "documentIdList", type = ApiParamType.JSONARRAY, desc = "documentId数组"),
        @Param(name = "documentVersionIdList", type = ApiParamType.JSONARRAY, desc = "documentVersionId数组"),
        @Param(name = "action", type = ApiParamType.STRING, desc = "delete,refresh"),
        @Param(name = "type", type = ApiParamType.STRING, desc = "knowledge,knowledgeversion"),
    })
    
    @Output({

    })
    @Description(desc = "修改知识数据到es")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        /*List<Object> documentIdObjList = jsonObj.getJSONArray("documentIdList");
        List<Object> documentVersionIdObjList = jsonObj.getJSONArray("documentVersionIdList");
        List<Long> documentIdList = null;
        List<Long> documentVersionIdList = null;
        List<String> documentIdStrList = null;
        if (CollectionUtils.isNotEmpty(documentIdObjList)) {
            documentIdList = documentIdObjList.stream().map(object -> Long.parseLong(object.toString())).collect(Collectors.toList());
            documentVersionIdList = documentVersionIdObjList.stream().map(object -> Long.parseLong(object.toString())).collect(Collectors.toList());
            documentIdStrList = documentIdObjList.stream().map(object -> object.toString()).collect(Collectors.toList());
        }
        String fromDate = jsonObj.getString("fromDate");
        String toDate = jsonObj.getString("toDate");
        String action = jsonObj.getString("action");
        String type = jsonObj.getString("type");
        if (action == null) {
            action = "refresh";
        }
        if (type == null) {
            type = "knowledge";
        }
        
        //删除符合条件es数据
        String whereSql = StringUtils.EMPTY;
        if(StringUtils.isNotBlank(fromDate)) {
            whereSql = String.format(" where fcd >= '%s'",fromDate);
        }
        if(StringUtils.isNotBlank(toDate)) {
            if(StringUtils.isBlank(whereSql)) {
                whereSql = String.format(" where fcd < '%s'",toDate);
            }else {
                whereSql = whereSql + String.format(" and fcd < '%s'",toDate);
            }
        }
        
        if(CollectionUtils.isNotEmpty(documentIdList)) {
            if(StringUtils.isBlank(whereSql)) {
                whereSql = String.format(" where id contains any ( '%s' )", String.join("','", documentIdStrList));
            }else {
                whereSql = whereSql + String.format(" and id contains any ( '%s' )", String.join("','", documentIdStrList));
            }
        }
        String esSql = String.format("select id from %s %s limit 20 ",TenantContext.get().getTenantUuid(),whereSql);
        MultiAttrsObjectPool  objectPool = ElasticSearchPoolManager.getObjectPool(type);
        objectPool.checkout(TenantContext.get().getTenantUuid());
        QueryParser parser = objectPool.createQueryParser();
        MultiAttrsQuery query = parser.parse(esSql);
        QueryResultSet resultSet = query.iterate();
        if (resultSet.hasMoreResults()) { 
            QueryResult result = resultSet.fetchResult(); 
            if(!result.getData().isEmpty()) { 
                for (MultiAttrsObject el : result.getData()) { 
                    objectPool.delete(el.getId());
                }
            }
        } 
        //如果需要更新
        if (action.equals("refresh")) {
            if(type.equals(ESHandler.KNOWLEDGE.getValue())) {
                List<KnowledgeDocumentVo> documentVoList = knowledgeDocumentMapper.getKnowledgeDocumentByIdListAndFcd(documentIdList,fromDate,toDate);
                for (KnowledgeDocumentVo knowledgeVo : documentVoList) {
                    ElasticSearchHandlerFactory.getHandler(ESHandler.KNOWLEDGE.getValue()).save(knowledgeVo.getId());
                }
            }else {
                List<Long> versionIdList = knowledgeDocumentMapper.getKnowledgeDocumentVersionIdListByLcd(documentVersionIdList,fromDate,toDate);
                for (Long version : versionIdList) {
                    ElasticSearchHandlerFactory.getHandler(ESHandler.KNOWLEDGE_VERSION.getValue()).save(version);
                }
            }
        }*/

        return null;
    }
}
