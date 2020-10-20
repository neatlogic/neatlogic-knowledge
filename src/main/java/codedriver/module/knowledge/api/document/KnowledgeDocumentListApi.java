package codedriver.module.knowledge.api.document;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentListApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "knowledge/document/list";
    }

    @Override
    public String getName() {
        return "查询文档版本列表";
    }

    @Override
    public String getConfig() {
        return null;
    }
    
    @Input({})
    @Output({
        @Param(explode = KnowledgeDocumentVersionVo[].class, desc = "文档版本列表")
    })
    @Description(desc = "查询文档版本列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

}
