package neatlogic.module.knowledge.service;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.knowledge.dto.feishu.FeiShuAppCredentialsVo;
import neatlogic.framework.knowledge.dto.feishu.KnowledgeFeishuSyncAuditVo;
import neatlogic.framework.knowledge.dto.feishu.KnowledgeFeishuSyncConfigVo;

public interface KnowledgeFeishuSyncService {
    JSONObject searchConfig(KnowledgeFeishuSyncConfigVo vo);

    KnowledgeFeishuSyncConfigVo getConfig(Long id);

    FeiShuAppCredentialsVo getFeiShuAppCredentials();

    Long saveConfig(KnowledgeFeishuSyncConfigVo vo);

    void updateStatus(Long id, Integer isActive);

    void deleteConfig(Long id);

//    JSONArray listSpaces(Long configId);

//    KnowledgeFeishuSyncAuditVo syncFromFeishu(Long configId);

//    KnowledgeFeishuSyncAuditVo retry(Long auditId);

    JSONObject searchAudit(KnowledgeFeishuSyncAuditVo vo);
}
