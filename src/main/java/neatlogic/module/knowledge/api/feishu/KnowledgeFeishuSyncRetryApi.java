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
public class KnowledgeFeishuSyncRetryApi extends PrivateApiComponentBase {
    @Resource
    private KnowledgeFeishuSyncService knowledgeFeishuSyncService;
    @Override
    public String getToken() { return "knowledge/feishu/sync/retry"; }
    @Override
    public String getName() { return "重试飞书云文档同步"; }
    @Override
    public String getConfig() { return null; }
    @Input({@Param(name = "auditId", type = ApiParamType.LONG, isRequired = true, desc = "同步记录 ID")})
    @Description(desc = "重试飞书云文档同步")
    @Override
    public Object myDoService(JSONObject jsonObj) {
//        return knowledgeFeishuSyncService.retry(jsonObj.getLong("auditId"));
        return null;
    }
}
