package codedriver.module.knowledge.api.type;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeTypeListApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "knowledge/type/list";
    }

    @Override
    public String getName() {
        return "查询知识分类列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

}
