package neatlogic.module.knowledge.api.feishu;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_FEISHU_SYNC_MODIFY;
import neatlogic.module.knowledge.service.KnowledgeFeishuSyncService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = KNOWLEDGE_FEISHU_SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class KnowledgeFeishuSyncExecuteApi extends PrivateApiComponentBase {
    @Resource
    private KnowledgeFeishuSyncService knowledgeFeishuSyncService;
    @Override
    public String getToken() { return "knowledge/feishu/sync/execute"; }
    @Override
    public String getName() { return "执行飞书云文档同步"; }
    @Override
    public String getConfig() { return null; }
    @Input({
            @Param(name = "configId", type = ApiParamType.LONG, isRequired = true, desc = "配置 ID"),
            @Param(name = "direction", type = ApiParamType.ENUM, rule = "from_feishu,to_feishu", desc = "同步方向"),
            @Param(name = "knowledgeDocumentId", type = ApiParamType.LONG, desc = "知识文档 ID")
    })
    @Description(desc = "执行飞书云文档同步")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        String direction = jsonObj.getString("direction");
        if ("to_feishu".equals(direction)) {
            return knowledgeFeishuSyncService.syncToFeishu(jsonObj.getLong("configId"), jsonObj.getLong("knowledgeDocumentId"));
        }
        return knowledgeFeishuSyncService.syncFromFeishu(jsonObj.getLong("configId"));
    }
}
