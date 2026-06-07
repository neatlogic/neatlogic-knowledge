package neatlogic.module.knowledge.service;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.config.ConfigManager;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.knowledge.constvalue.KnowledgeTenantConfig;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeCircleMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import neatlogic.framework.knowledge.dto.KnowledgeCircleVo;
import neatlogic.framework.knowledge.dto.feishu.FeiShuAppCredentialsVo;
import neatlogic.module.knowledge.dao.mapper.KnowledgeFeishuSyncMapper;
import neatlogic.framework.knowledge.dto.feishu.KnowledgeFeishuSyncAuditVo;
import neatlogic.framework.knowledge.dto.feishu.KnowledgeFeishuSyncConfigVo;
import neatlogic.framework.util.SnowflakeUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

@Service
public class KnowledgeFeishuSyncServiceImpl implements KnowledgeFeishuSyncService {

    private final Logger logger = LoggerFactory.getLogger(KnowledgeFeishuSyncServiceImpl.class);

    @Resource
    private KnowledgeFeishuSyncMapper knowledgeFeishuSyncMapper;
    @Resource
    private KnowledgeCircleMapper knowledgeCircleMapper;
    @Resource
    private KnowledgeDocumentTypeMapper knowledgeDocumentTypeMapper;
    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    private KnowledgeDocumentTypeService knowledgeDocumentTypeService;

    @Override
    public FeiShuAppCredentialsVo getFeiShuAppCredentials() {
        String appId = ConfigManager.getConfig(KnowledgeTenantConfig.FEISHU_APP_ID);
        String appSecret = ConfigManager.getConfig(KnowledgeTenantConfig.FEISHU_APP_SECRET);
        String knowledgeCircleIdStr = ConfigManager.getConfig(KnowledgeTenantConfig.FEISHU_WIKI_KNOWLEDGE_CIRCLE_ID);
        Long knowledgeCircleId = null;
        if (StringUtils.isNumeric(knowledgeCircleIdStr)) {
            Long circleId = Long.parseLong(knowledgeCircleIdStr);
            KnowledgeCircleVo knowledgeCircleVo = knowledgeCircleMapper.getKnowledgeCircleById(circleId);
            if (knowledgeCircleVo != null) {
                knowledgeCircleId = knowledgeCircleVo.getId();
            }
        }
        if (StringUtils.isNotBlank(appId) && StringUtils.isNotBlank(appSecret) && knowledgeCircleId != null) {
            return new FeiShuAppCredentialsVo(appId, appSecret, knowledgeCircleId);
        }
        return null;
    }


}
