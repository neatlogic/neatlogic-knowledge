package neatlogic.module.knowledge.api.feishu;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.util.RC4Util;
import neatlogic.framework.dao.mapper.ConfigMapper;
import neatlogic.framework.dto.ConfigVo;
import neatlogic.framework.knowledge.constvalue.KnowledgeTenantConfig;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_FEISHU_SYNC_MODIFY;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = KNOWLEDGE_FEISHU_SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveFeiShuAppCredentialsApi extends PrivateApiComponentBase {

    @Resource
    private ConfigMapper configMapper;

    @Override
    public String getToken() { return "knowledge/feishu/app/credentials/save"; }
    @Override
    public String getName() { return "保存飞书应用凭证"; }

    @Input({
            @Param(name = "appId", type = ApiParamType.STRING, isRequired = true, desc = "App ID"),
            @Param(name = "appSecret", type = ApiParamType.STRING, isRequired = true, desc = "App Secret"),
            @Param(name = "knowledgeCircleId", type = ApiParamType.LONG, isRequired = true, desc = "知识圈 ID"),
    })
    @Output({})
    @Description(desc = "保存飞书应用凭证")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        String appId = jsonObj.getString("appId");
        String appSecret = jsonObj.getString("appSecret");
        Long knowledgeCircleId = jsonObj.getLong("knowledgeCircleId");
        {
            ConfigVo configVo = new ConfigVo();
            configVo.setKey(KnowledgeTenantConfig.FEISHU_APP_ID.getKey());
            configVo.setValue(appId);
            configVo.setDescription(KnowledgeTenantConfig.FEISHU_APP_ID.getDescription());
            configMapper.insertConfig(configVo);
        }
        {
            ConfigVo configVo = new ConfigVo();
            configVo.setKey(KnowledgeTenantConfig.FEISHU_APP_SECRET.getKey());
            appSecret = RC4Util.encrypt(appSecret);
            configVo.setValue(appSecret);
            configVo.setDescription(KnowledgeTenantConfig.FEISHU_APP_SECRET.getDescription());
            configMapper.insertConfig(configVo);
        }
        {
            ConfigVo configVo = new ConfigVo();
            configVo.setKey(KnowledgeTenantConfig.FEISHU_WIKI_KNOWLEDGE_CIRCLE_ID.getKey());
            configVo.setValue(knowledgeCircleId.toString());
            configVo.setDescription(KnowledgeTenantConfig.FEISHU_WIKI_KNOWLEDGE_CIRCLE_ID.getDescription());
            configMapper.insertConfig(configVo);
        }
        return null;
    }
}
