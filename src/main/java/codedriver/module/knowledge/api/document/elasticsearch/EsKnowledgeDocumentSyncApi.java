package codedriver.module.knowledge.api.document.elasticsearch;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.techsure.multiattrsearch.MultiAttrsObject;
import com.techsure.multiattrsearch.MultiAttrsObjectPool;
import com.techsure.multiattrsearch.MultiAttrsQuery;
import com.techsure.multiattrsearch.QueryResultSet;
import com.techsure.multiattrsearch.query.QueryParser;
import com.techsure.multiattrsearch.query.QueryResult;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.elasticsearch.core.ElasticSearchHandlerFactory;
import codedriver.framework.elasticsearch.core.ElasticSearchPoolManager;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.elasticsearch.constvalue.ESHandler;

@Service
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
        @Param(name = "fromDate", type = ApiParamType.STRING, desc = "创建时间>fromDate"),
        @Param(name = "toDate", type = ApiParamType.STRING, desc = "创建时间<toDate"),
        @Param(name = "documentIdList", type = ApiParamType.JSONARRAY, desc = "documentId数组"),
        @Param(name = "action", type = ApiParamType.STRING, desc = "delete,refresh")})
    @Output({

    })
    @Description(desc = "修改知识数据到es")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<Object> documentIdObjList = jsonObj.getJSONArray("documentIdList");
        List<Long> documentIdList = null;
        List<String> documentIdStrList = null;
        if (CollectionUtils.isNotEmpty(documentIdObjList)) {
            documentIdList = documentIdObjList.stream().map(object -> Long.parseLong(object.toString())).collect(Collectors.toList());
            documentIdStrList = documentIdObjList.stream().map(object -> object.toString()).collect(Collectors.toList());
        }
        String fromDate = jsonObj.getString("fromDate");
        String toDate = jsonObj.getString("toDate");
        String action = jsonObj.getString("action");
        if (action == null) {
            action = "refresh";
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
        String esSql = String.format("select id from %s %s limit 0,20 ",TenantContext.get().getTenantUuid(),whereSql);
        MultiAttrsObjectPool  objectPool = ElasticSearchPoolManager.getObjectPool("knowledge");
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
            List<KnowledgeDocumentVo> documentVoList = knowledgeDocumentMapper.getKnowledgeDocumentByIdListAndFcd(documentIdList,fromDate,toDate);
            for (KnowledgeDocumentVo knowledgeVo : documentVoList) {
                ElasticSearchHandlerFactory.getHandler(ESHandler.KNOWLEDGE.getValue()).save(knowledgeVo.getId());
            }
        }

        return null;
    }
}
