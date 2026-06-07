package neatlogic.module.knowledge.service;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.knowledge.dto.feishu.FeiShuAppCredentialsVo;
import neatlogic.framework.knowledge.dto.feishu.KnowledgeFeishuSyncAuditVo;
import neatlogic.framework.knowledge.dto.feishu.KnowledgeFeishuSyncConfigVo;

public interface KnowledgeFeishuSyncService {

    FeiShuAppCredentialsVo getFeiShuAppCredentials();
}
