package codedriver.module.knowledge.api;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentVersionDeleteApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "knowledge/document/version/delete";
    }

    @Override
    public String getName() {
        return "删除文档版本";
    }

    @Override
    public String getConfig() {
        return null;
    }
    
    @Input({})
    @Output({
        @Param(explode = KnowledgeDocumentVo[].class, desc = "两个版本文档内容")
    })
    @Description(desc = "比较两个版本文档内容差异")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

}
