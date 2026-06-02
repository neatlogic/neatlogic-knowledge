package neatlogic.module.knowledge.api.feishu;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.knowledge.dto.feishu.KnowledgeFeishuSyncConfigVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_FEISHU_SYNC_MODIFY;
import neatlogic.module.knowledge.service.KnowledgeFeishuSyncService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = KNOWLEDGE_FEISHU_SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class KnowledgeFeishuSyncConfigSaveApi extends PrivateApiComponentBase {
    @Resource
    private KnowledgeFeishuSyncService knowledgeFeishuSyncService;
    @Override
    public String getToken() { return "knowledge/feishu/sync/config/save"; }
    @Override
    public String getName() { return "保存飞书云文档同步配置"; }
    @Override
    public String getConfig() { return null; }
    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "id"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "名称", xss = true),
//            @Param(name = "baseUrl", type = ApiParamType.STRING, isRequired = true, desc = "飞书平台地址"),
            @Param(name = "appId", type = ApiParamType.STRING, isRequired = true, desc = "App ID"),
            @Param(name = "appSecret", type = ApiParamType.STRING, isRequired = true, desc = "App Secret"),
//            @Param(name = "userAccessToken", type = ApiParamType.STRING, desc = "User Access Token"),
//            @Param(name = "spaceId", type = ApiParamType.STRING, desc = "Wiki 空间 ID"),
//            @Param(name = "spaceName", type = ApiParamType.STRING, desc = "Wiki 空间名称"),
            @Param(name = "knowledgeCircleId", type = ApiParamType.LONG, isRequired = true, desc = "知识圈 ID"),
            @Param(name = "isActive", type = ApiParamType.INTEGER, desc = "是否启用")
    })
    @Description(desc = "保存飞书云文档同步配置")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        KnowledgeFeishuSyncConfigVo knowledgeFeishuSyncConfigVo = JSON.toJavaObject(jsonObj, KnowledgeFeishuSyncConfigVo.class);
        Long id = knowledgeFeishuSyncService.saveConfig(knowledgeFeishuSyncConfigVo);
        JSONObject result = new JSONObject();
        result.put("id", id);
        return result;
    }
}
