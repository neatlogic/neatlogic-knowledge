package neatlogic.module.knowledge.service;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeCircleMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
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
    private static final String OPEN_API = "/open-apis";

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
    public JSONObject searchConfig(KnowledgeFeishuSyncConfigVo vo) {
        int count = knowledgeFeishuSyncMapper.searchConfigCount(vo);
        List<KnowledgeFeishuSyncConfigVo> list = count > 0 ? knowledgeFeishuSyncMapper.searchConfig(vo) : new ArrayList<>();
        JSONObject result = new JSONObject();
        result.put("tbodyList", list);
        result.put("rowNum", count);
        result.put("currentPage", vo.getCurrentPage());
        result.put("pageSize", vo.getPageSize());
        return result;
    }

    @Override
    public KnowledgeFeishuSyncConfigVo getConfig(Long id) {
        return knowledgeFeishuSyncMapper.getConfigById(id);
    }

    @Override
    @Transactional
    public Long saveConfig(KnowledgeFeishuSyncConfigVo vo) {
        if (knowledgeFeishuSyncMapper.checkNameIsRepeat(vo) > 0) {
            throw new RuntimeException("同步配置名称已存在");
        }
        String userUuid = UserContext.get().getUserUuid(true);
        vo.setLcu(userUuid);
        if (vo.getIsActive() == null) {
            vo.setIsActive(1);
        }
//        if (StringUtils.isBlank(vo.getBaseUrl())) {
//            vo.setBaseUrl("https://lqnnbz38z5y.feishu.cn");
//        }
//        vo.setKnowledgeCircleId(getOrCreateFeishuCircle(vo.getKnowledgeCircleId()));
        if (vo.getId() == null) {
            vo.setId(SnowflakeUtil.uniqueLong());
            if (StringUtils.isBlank(vo.getAppSecret())) {
                throw new ParamNotExistsException("appSecret");
            }
//            if (StringUtils.isBlank(vo.getUserAccessToken())) {
//                throw new ParamNotExistsException("userAccessToken");
//            }
            vo.setFcu(userUuid);
            knowledgeFeishuSyncMapper.insertConfig(vo);
        } else {
            knowledgeFeishuSyncMapper.updateConfig(vo);
        }
        return vo.getId();
    }

    @Override
    public void updateStatus(Long id, Integer isActive) {
        knowledgeFeishuSyncMapper.updateConfigStatus(id, isActive, UserContext.get().getUserUuid(true));
    }

    @Override
    public void deleteConfig(Long id) {
        knowledgeFeishuSyncMapper.deleteConfig(id);
    }

//    @Override
//    public JSONArray listSpaces(Long configId) {
//        KnowledgeFeishuSyncConfigVo config = getRequiredConfig(configId);
//        JSONObject result = feishuGet(config, "/wiki/v2/spaces", null);
//        JSONObject data = result.getJSONObject("data");
//        if (data == null) {
//            return new JSONArray();
//        }
//        JSONArray items = data.getJSONArray("items");
//        return items == null ? new JSONArray() : items;
//    }

//    @Override
//    public KnowledgeFeishuSyncAuditVo retry(Long auditId) {
//        KnowledgeFeishuSyncAuditVo audit = knowledgeFeishuSyncMapper.getAuditById(auditId);
//        if (audit == null) {
//            throw new RuntimeException("同步记录不存在");
//        }
//        if ("to_feishu".equals(audit.getDirection())) {
//            throw new RuntimeException("回写飞书记录请在知识文档详情触发重试");
//        }
//        return syncFromFeishu(audit.getConfigId());
//    }

    @Override
    public JSONObject searchAudit(KnowledgeFeishuSyncAuditVo vo) {
        int count = knowledgeFeishuSyncMapper.searchAuditCount(vo);
        List<KnowledgeFeishuSyncAuditVo> list = count > 0 ? knowledgeFeishuSyncMapper.searchAudit(vo) : new ArrayList<>();
        JSONObject result = new JSONObject();
        result.put("tbodyList", list);
        result.put("rowNum", count);
        result.put("currentPage", vo.getCurrentPage());
        result.put("pageSize", vo.getPageSize());
        return result;
    }

//    private KnowledgeFeishuSyncConfigVo getRequiredConfig(Long configId) {
//        KnowledgeFeishuSyncConfigVo config = knowledgeFeishuSyncMapper.getConfigById(configId);
//        if (config == null) {
//            throw new RuntimeException("飞书同步配置不存在");
//        }
//        return config;
//    }

//    private Long getOrCreateFeishuCircle(Long circleId) {
//        if (circleId != null) {
//            return circleId;
//        }
//        KnowledgeCircleVo query = new KnowledgeCircleVo();
//        query.setName("飞书知识圈");
//        KnowledgeCircleVo circle = null;
//        List<KnowledgeCircleVo> circleList = knowledgeCircleMapper.searchKnowledgeCircle(query);
//        if (CollectionUtils.isNotEmpty(circleList)) {
//            circle = circleList.get(0);
//        }
//        if (circle != null) {
//            return circle.getId();
//        }
//        KnowledgeCircleVo newCircle = new KnowledgeCircleVo();
//        newCircle.setName("飞书知识圈");
//        knowledgeCircleMapper.insertKnowledgeCircle(newCircle);
//        KnowledgeCircleUserVo member = new KnowledgeCircleUserVo();
//        member.setKnowledgeCircleId(newCircle.getId());
//        member.setType("user");
//        member.setUuid(UserContext.get().getUserUuid(true));
//        member.setAuthType(KnowledgeCircleUserVo.AuthType.MEMBER.getValue());
//        KnowledgeCircleUserVo approver = new KnowledgeCircleUserVo();
//        approver.setKnowledgeCircleId(newCircle.getId());
//        approver.setType("user");
//        approver.setUuid(UserContext.get().getUserUuid(true));
//        approver.setAuthType(KnowledgeCircleUserVo.AuthType.APPROVER.getValue());
//        knowledgeCircleMapper.batchInsertKnowledgeCircleUser(Arrays.asList(member, approver));
//        return newCircle.getId();
//    }


//    private String getUserAccessToken(KnowledgeFeishuSyncConfigVo config) {
//        if (StringUtils.isBlank(config.getUserAccessToken())) {
//            throw new ParamNotExistsException("userAccessToken");
//        }
//        return config.getUserAccessToken();
//    }

//    public static class FeishuNode {
//        private final String nodeToken;
//        private final String objToken;
//        private final String objType;
//        private final String title;
//        private final String updateTime;
//        private final List<String> path = new ArrayList<>();
//
//        public FeishuNode(JSONObject item) {
//            this.nodeToken = item.getString("node_token");
//            this.objToken = item.getString("obj_token");
//            this.objType = item.getString("obj_type");
//            this.title = StringUtils.defaultIfBlank(item.getString("title"), item.getString("obj_token"));
//            this.updateTime = item.getString("obj_edit_time");
//        }
//
//        public String getNodeToken() {
//            return nodeToken;
//        }
//
//        public String getObjToken() {
//            return objToken;
//        }
//
//        public String getObjType() {
//            return objType;
//        }
//
//        public String getTitle() {
//            return title;
//        }
//
//        public String getUpdateTime() {
//            return updateTime;
//        }
//
//        public List<String> getPath() {
//            return path;
//        }
//    }
}
