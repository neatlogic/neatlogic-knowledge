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
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeFeishuSyncConfigSearchApi extends PrivateApiComponentBase {
    @Resource
    private KnowledgeFeishuSyncService knowledgeFeishuSyncService;

    @Override
    public String getToken() { return "knowledge/feishu/sync/config/search"; }
    @Override
    public String getName() { return "查询飞书云文档同步配置"; }
    @Override
    public String getConfig() { return null; }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数量")
    })
    @Description(desc = "查询飞书云文档同步配置")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        return knowledgeFeishuSyncService.searchConfig(JSON.toJavaObject(jsonObj, KnowledgeFeishuSyncConfigVo.class));
    }
}
