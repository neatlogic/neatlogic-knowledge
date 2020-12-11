package codedriver.module.knowledge.api.document;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.exception.KnowledgeDocumentHasBeenFavoredException;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotFoundException;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@OperationType(type = OperationTypeEnum.OPERATE)
public class KnowledgeDocumentFavorToggleApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    
    @Override
    public String getToken() {
        return "knowledge/document/favor/toggle";
    }

    @Override
    public String getName() {
        return "点赞或取消点赞文档";
    }

    @Override
    public String getConfig() {
        return null;
    }
    
    @Input({
        @Param(name = "documentId", type = ApiParamType.LONG, isRequired = true, desc = "文档id"),
        @Param(name = "isFavor", type = ApiParamType.ENUM,rule = "0,1",isRequired = true, desc = "是否点赞(0：否；1：是)"),
    })
    @Output({
            @Param(name = "count", type = ApiParamType.INTEGER, desc = "文档点赞数"),
    })
    @Description(desc = "点赞或取消点赞文档")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long documentId = jsonObj.getLong("documentId");
        int isFavor = jsonObj.getIntValue("isFavor");
        if(knowledgeDocumentMapper.getKnowledgeDocumentById(documentId) == null){
            throw new KnowledgeDocumentNotFoundException(documentId);
        }
        if(isFavor == 1){
            if(knowledgeDocumentMapper.checkDocumentHasBeenFavored(documentId,UserContext.get().getUserUuid()) > 0){
                KnowledgeDocumentVo documentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(documentId);
                throw new KnowledgeDocumentHasBeenFavoredException(documentVo.getTitle());
            }
            knowledgeDocumentMapper.insertKnowledgeDocumentFavor(documentId, UserContext.get().getUserUuid());
        }else if(isFavor == 0){
            knowledgeDocumentMapper.deleteKnowledgeDocumentFavor(documentId, UserContext.get().getUserUuid());
        }
        int favorCount = knowledgeDocumentMapper.getDocumentFavorCount(documentId);
        result.put("count",favorCount);
        return result;
    }

}
