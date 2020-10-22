package codedriver.module.knowledge.api.document;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotFoundException;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@OperationType(type = OperationTypeEnum.UPDATE)
public class KnowledgeDocumentViewCountUpdateApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    
    @Override
    public String getToken() {
        return "knowledge/document/viewcount/update";
    }

    @Override
    public String getName() {
        return "更新文档浏览量";
    }

    @Override
    public String getConfig() {
        return null;
    }
    
    @Input({@Param(name = "documentId", type = ApiParamType.LONG, isRequired = true, desc = "文档id")})
    @Output({@Param(name = "count", type = ApiParamType.INTEGER, desc = "文档浏览量"),})
    @Description(desc = "更新文档浏览量")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long documentId = jsonObj.getLong("documentId");
        if(knowledgeDocumentMapper.getKnowledgeDocumentById(documentId) == null){
            throw new KnowledgeDocumentNotFoundException(documentId);
        }
        int count = 0;
        /**上锁避免主键冲突和保证数据正确*/
        synchronized (KnowledgeDocumentViewCountUpdateApi.class){
            if(knowledgeDocumentMapper.checkExistsDocumentViewCount(documentId) == 0){
                knowledgeDocumentMapper.insertKnowledgeDocumentViewCount(documentId,1);
                count = 1;
            }else{
                int viewCount = knowledgeDocumentMapper.getDocumentViewCount(documentId) + 1;
                knowledgeDocumentMapper.updateDocumentViewCount(documentId,viewCount);
                count = viewCount;
            }
        }
        result.put("count",count);
        return result;
    }

}
