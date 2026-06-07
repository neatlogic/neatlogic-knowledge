package neatlogic.module.knowledge.service;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.knowledge.dto.feishu.FeiShuAppCredentialsVo;

public interface KnowledgeFeishuSyncService {

    FeiShuAppCredentialsVo getFeiShuAppCredentials();
}
