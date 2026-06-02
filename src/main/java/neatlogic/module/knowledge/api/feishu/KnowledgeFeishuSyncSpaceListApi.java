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
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeFeishuSyncSpaceListApi extends PrivateApiComponentBase {
    @Resource
    private KnowledgeFeishuSyncService knowledgeFeishuSyncService;
    @Override
    public String getToken() { return "knowledge/feishu/sync/space/list"; }
    @Override
    public String getName() { return "获取飞书 Wiki 空间列表"; }
    @Override
    public String getConfig() { return null; }
    @Input({@Param(name = "configId", type = ApiParamType.LONG, isRequired = true, desc = "配置 ID")})
    @Description(desc = "获取飞书 Wiki 空间列表")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        JSONObject result = new JSONObject();
//        result.put("spaceList", knowledgeFeishuSyncService.listSpaces(jsonObj.getLong("configId")));
        return result;
    }
}
