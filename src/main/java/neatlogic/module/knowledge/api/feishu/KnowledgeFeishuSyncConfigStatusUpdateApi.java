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
public class KnowledgeFeishuSyncConfigStatusUpdateApi extends PrivateApiComponentBase {
    @Resource
    private KnowledgeFeishuSyncService knowledgeFeishuSyncService;
    @Override
    public String getToken() { return "knowledge/feishu/sync/config/status/update"; }
    @Override
    public String getName() { return "更新飞书云文档同步配置状态"; }
    @Override
    public String getConfig() { return null; }
    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "id"),
            @Param(name = "isActive", type = ApiParamType.INTEGER, isRequired = true, desc = "是否启用")
    })
    @Description(desc = "更新飞书云文档同步配置状态")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        knowledgeFeishuSyncService.updateStatus(jsonObj.getLong("id"), jsonObj.getInteger("isActive"));
        return null;
    }
}
