package neatlogic.module.knowledge.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.fulltextindex.core.FullTextIndexHandlerFactory;
import neatlogic.framework.fulltextindex.core.IFullTextIndexHandler;
import neatlogic.framework.knowledge.constvalue.KnowledgeFullTextIndexType;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeCircleMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeFeishuSyncMapper;
import neatlogic.framework.knowledge.dto.*;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.framework.util.SnowflakeUtil;
import neatlogic.framework.util.UuidUtil;
import neatlogic.module.knowledge.source.FeishuSyncSource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
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
        if (StringUtils.isBlank(vo.getBaseUrl())) {
            vo.setBaseUrl("https://lqnnbz38z5y.feishu.cn");
        }
        vo.setKnowledgeCircleId(getOrCreateFeishuCircle(vo.getKnowledgeCircleId()));
        if (vo.getId() == null) {
            vo.setId(SnowflakeUtil.uniqueLong());
            if (StringUtils.isBlank(vo.getAppSecret())) {
                throw new ParamNotExistsException("appSecret");
            }
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

    @Override
    public JSONArray listSpaces(Long configId) {
        KnowledgeFeishuSyncConfigVo config = getRequiredConfig(configId);
        JSONObject result = feishuGet(config, "/wiki/v2/spaces", null);
        JSONObject data = result.getJSONObject("data");
        if (data == null) {
            return new JSONArray();
        }
        JSONArray items = data.getJSONArray("items");
        return items == null ? new JSONArray() : items;
    }

    @Override
    @Transactional
    public KnowledgeFeishuSyncAuditVo syncFromFeishu(Long configId) {
        KnowledgeFeishuSyncConfigVo config = getRequiredConfig(configId);
        KnowledgeFeishuSyncAuditVo audit = startAudit(configId, "from_feishu");
        JSONArray detailList = new JSONArray();
        int total = 0;
        int success = 0;
        int failed = 0;
        try {
            if (!Objects.equals(config.getIsActive(), 1)) {
                throw new RuntimeException("同步配置未启用");
            }
            if (StringUtils.isBlank(config.getSpaceId())) {
                throw new ParamNotExistsException("spaceId");
            }
            List<FeishuNode> nodes = new ArrayList<>();
            loadWikiNodes(config, null, new ArrayList<>(), nodes);
            for (FeishuNode node : nodes) {
                if (!isDocumentNode(node)) {
                    continue;
                }
                total++;
                JSONObject item = new JSONObject();
                item.put("title", node.title);
                item.put("nodeToken", node.nodeToken);
                try {
                    String typeUuid = getOrCreateType(config.getKnowledgeCircleId(), node.path);
                    Long documentId = saveFeishuDocument(config, node, typeUuid);
                    item.put("knowledgeDocumentId", documentId);
                    item.put("status", "succeed");
                    success++;
                } catch (Exception ex) {
                    item.put("status", "failed");
                    item.put("error", ex.getMessage());
                    failed++;
                }
                detailList.add(item);
            }
            finishAudit(audit, failed == 0 ? "succeed" : "failed", total, success, failed, null, detailList);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            finishAudit(audit, "failed", total, success, failed == 0 ? 1 : failed, ex.getMessage(), detailList);
        }
        updateConfigSyncStatus(config, audit);
        return audit;
    }

    @Override
    @Transactional
    public KnowledgeFeishuSyncAuditVo syncToFeishu(Long configId, Long knowledgeDocumentId) {
        KnowledgeFeishuSyncConfigVo config = getRequiredConfig(configId);
        KnowledgeFeishuSyncAuditVo audit = startAudit(configId, "to_feishu");
        JSONArray detailList = new JSONArray();
        try {
            KnowledgeFeishuSyncDocumentVo syncDocumentVo = knowledgeFeishuSyncMapper.getSyncDocumentByDocumentId(knowledgeDocumentId);
            if (syncDocumentVo == null) {
                throw new RuntimeException("该知识文档没有飞书同步映射");
            }
            KnowledgeDocumentVo documentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(knowledgeDocumentId);
            if (documentVo == null) {
                throw new RuntimeException("知识文档不存在");
            }
            List<KnowledgeDocumentLineVo> lineList = knowledgeDocumentMapper.getKnowledgeDocumentLineListByKnowledgeDocumentVersionId(documentVo.getKnowledgeDocumentVersionId());
            JSONObject payload = new JSONObject();
            payload.put("title", documentVo.getTitle());
            payload.put("content", toPlainText(lineList));
            JSONObject item = new JSONObject();
            item.put("knowledgeDocumentId", knowledgeDocumentId);
            item.put("nodeToken", syncDocumentVo.getNodeToken());
            JSONObject response = feishuPatch(config, "/docx/v1/documents/" + syncDocumentVo.getObjToken(), payload);
            item.put("response", response);
            item.put("status", "succeed");
            detailList.add(item);
            finishAudit(audit, "succeed", 1, 1, 0, null, detailList);
        } catch (Exception ex) {
            finishAudit(audit, "failed", 1, 0, 1, ex.getMessage(), detailList);
        }
        updateConfigSyncStatus(config, audit);
        return audit;
    }

    @Override
    public KnowledgeFeishuSyncAuditVo retry(Long auditId) {
        KnowledgeFeishuSyncAuditVo audit = knowledgeFeishuSyncMapper.getAuditById(auditId);
        if (audit == null) {
            throw new RuntimeException("同步记录不存在");
        }
        if ("to_feishu".equals(audit.getDirection())) {
            throw new RuntimeException("回写飞书记录请在知识文档详情触发重试");
        }
        return syncFromFeishu(audit.getConfigId());
    }

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

    private KnowledgeFeishuSyncConfigVo getRequiredConfig(Long configId) {
        KnowledgeFeishuSyncConfigVo config = knowledgeFeishuSyncMapper.getConfigById(configId);
        if (config == null) {
            throw new RuntimeException("飞书同步配置不存在");
        }
        return config;
    }

    private Long getOrCreateFeishuCircle(Long circleId) {
        if (circleId != null) {
            return circleId;
        }
        KnowledgeCircleVo query = new KnowledgeCircleVo();
        query.setName("飞书知识圈");
        KnowledgeCircleVo circle = null;
        List<KnowledgeCircleVo> circleList = knowledgeCircleMapper.searchKnowledgeCircle(query);
        if (CollectionUtils.isNotEmpty(circleList)) {
            circle = circleList.get(0);
        }
        if (circle != null) {
            return circle.getId();
        }
        KnowledgeCircleVo newCircle = new KnowledgeCircleVo();
        newCircle.setName("飞书知识圈");
        knowledgeCircleMapper.insertKnowledgeCircle(newCircle);
        KnowledgeCircleUserVo member = new KnowledgeCircleUserVo();
        member.setKnowledgeCircleId(newCircle.getId());
        member.setType("user");
        member.setUuid(UserContext.get().getUserUuid(true));
        member.setAuthType(KnowledgeCircleUserVo.AuthType.MEMBER.getValue());
        KnowledgeCircleUserVo approver = new KnowledgeCircleUserVo();
        approver.setKnowledgeCircleId(newCircle.getId());
        approver.setType("user");
        approver.setUuid(UserContext.get().getUserUuid(true));
        approver.setAuthType(KnowledgeCircleUserVo.AuthType.APPROVER.getValue());
        knowledgeCircleMapper.batchInsertKnowledgeCircleUser(Arrays.asList(member, approver));
        return newCircle.getId();
    }

    private String getOrCreateType(Long circleId, List<String> path) {
        String parentUuid = KnowledgeDocumentTypeVo.ROOT_UUID;
        String lastUuid = null;
        int sort = 0;
        for (String name : path) {
            if (StringUtils.isBlank(name)) {
                continue;
            }
            KnowledgeDocumentTypeVo exists = null;
            List<KnowledgeDocumentTypeVo> children = knowledgeDocumentTypeMapper.getTypeByParentUuid(parentUuid, circleId);
            if (CollectionUtils.isNotEmpty(children)) {
                for (KnowledgeDocumentTypeVo child : children) {
                    if (name.equals(child.getName())) {
                        exists = child;
                        break;
                    }
                }
            }
            if (exists == null) {
                KnowledgeDocumentTypeVo typeVo = new KnowledgeDocumentTypeVo();
                typeVo.setUuid(UuidUtil.randomUuid());
                typeVo.setName(name);
                typeVo.setParentUuid(parentUuid);
                typeVo.setKnowledgeCircleId(circleId);
                typeVo.setSort(sort);
                knowledgeDocumentTypeMapper.batchInsertType(Collections.singletonList(typeVo));
                knowledgeDocumentTypeService.rebuildLeftRightCode(circleId);
                exists = typeVo;
            }
            parentUuid = exists.getUuid();
            lastUuid = exists.getUuid();
            sort++;
        }
        if (lastUuid == null) {
            String name = "默认分类";
            return getOrCreateType(circleId, Collections.singletonList(name));
        }
        return lastUuid;
    }

    private Long saveFeishuDocument(KnowledgeFeishuSyncConfigVo config, FeishuNode node, String typeUuid) {
        KnowledgeFeishuSyncDocumentVo mapping = knowledgeFeishuSyncMapper.getSyncDocumentByNodeToken(config.getId(), node.nodeToken);
        KnowledgeDocumentVo documentVo = new KnowledgeDocumentVo();
        if (mapping == null) {
            documentVo.setTitle(node.title);
            documentVo.setKnowledgeCircleId(config.getKnowledgeCircleId());
            documentVo.setKnowledgeDocumentTypeUuid(typeUuid);
            documentVo.setVersion(0);
            documentVo.setFcu(UserContext.get().getUserUuid(true));
            documentVo.setSource(FeishuSyncSource.SOURCE);
            knowledgeDocumentMapper.insertKnowledgeDocument(documentVo);
            knowledgeDocumentMapper.insertKnowledgeDocumentViewCount(documentVo.getId(), 0);
        } else {
            documentVo = knowledgeDocumentMapper.getKnowledgeDocumentLockById(mapping.getKnowledgeDocumentId());
            documentVo.setTitle(node.title);
            documentVo.setKnowledgeDocumentTypeUuid(typeUuid);
            knowledgeDocumentMapper.updateKnowledgeDocumentTitleById(documentVo);
            knowledgeDocumentMapper.updateKnowledgeDocumentTypeUuidById(documentVo);
        }

        KnowledgeDocumentVersionVo versionVo = new KnowledgeDocumentVersionVo();
        versionVo.setTitle(node.title);
        versionVo.setKnowledgeDocumentId(documentVo.getId());
        versionVo.setKnowledgeDocumentTypeUuid(typeUuid);
        versionVo.setFromVersion(Optional.ofNullable(knowledgeDocumentMapper.getKnowledgeDocumentVersionMaxVerionByKnowledgeDocumentId(documentVo.getId())).orElse(0));
        versionVo.setStatus("passed");
        versionVo.setLcu(UserContext.get().getUserUuid(true));
        knowledgeDocumentMapper.insertKnowledgeDocumentVersion(versionVo);
        versionVo.setVersion(versionVo.getFromVersion() + 1);
        versionVo.setReviewer(UserContext.get().getUserUuid(true));
        knowledgeDocumentMapper.updateKnowledgeDocumentVersionById(versionVo);
        documentVo.setKnowledgeDocumentVersionId(versionVo.getId());
        documentVo.setVersion(versionVo.getVersion());
        knowledgeDocumentMapper.updateKnowledgeDocumentById(documentVo);

        saveLines(documentVo.getId(), versionVo.getId(), getFeishuDocumentLines(config, node));
        upsertMapping(config, node, typeUuid, documentVo.getId());
        IFullTextIndexHandler handler = FullTextIndexHandlerFactory.getHandler(KnowledgeFullTextIndexType.KNOW_DOCUMENT_VERSION);
        if (handler != null) {
            handler.createIndex(versionVo.getId());
        }
        return documentVo.getId();
    }

    private void upsertMapping(KnowledgeFeishuSyncConfigVo config, FeishuNode node, String typeUuid, Long documentId) {
        KnowledgeFeishuSyncDocumentVo vo = new KnowledgeFeishuSyncDocumentVo();
        vo.setConfigId(config.getId());
        vo.setNodeToken(node.nodeToken);
        vo.setObjToken(node.objToken);
        vo.setObjType(node.objType);
        vo.setKnowledgeDocumentId(documentId);
        vo.setKnowledgeDocumentTypeUuid(typeUuid);
        vo.setTitle(node.title);
        vo.setFeishuUpdateTime(node.updateTime);
        if (knowledgeFeishuSyncMapper.getSyncDocumentByNodeToken(config.getId(), node.nodeToken) == null) {
            knowledgeFeishuSyncMapper.insertSyncDocument(vo);
        } else {
            knowledgeFeishuSyncMapper.updateSyncDocument(vo);
        }
    }

    private List<KnowledgeDocumentLineVo> getFeishuDocumentLines(KnowledgeFeishuSyncConfigVo config, FeishuNode node) {
        List<KnowledgeDocumentLineVo> lineList = new ArrayList<>();
        try {
            JSONObject blockResult = feishuGet(config, "/docx/v1/documents/" + node.objToken + "/blocks/" + node.objToken + "/children", null);
            JSONArray items = blockResult.getJSONObject("data") == null ? null : blockResult.getJSONObject("data").getJSONArray("items");
            if (CollectionUtils.isNotEmpty(items)) {
                for (int i = 0; i < items.size(); i++) {
                    String text = extractBlockText(items.getJSONObject(i));
                    if (StringUtils.isNotBlank(text)) {
                        lineList.add(newLine("p", text));
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (CollectionUtils.isEmpty(lineList)) {
            lineList.add(newLine("p", "<a href=\"" + config.getBaseUrl() + "/wiki/" + node.nodeToken + "\">" + node.title + "</a>"));
        }
        return lineList;
    }

    private String extractBlockText(JSONObject block) {
        StringBuilder sb = new StringBuilder();
        JSONObject text = block.getJSONObject("text");
        if (text != null) {
            JSONArray elements = text.getJSONArray("elements");
            appendTextElements(sb, elements);
        }
        JSONObject heading = block.getJSONObject("heading1");
        if (heading == null) {
            heading = block.getJSONObject("heading2");
        }
        if (heading != null) {
            appendTextElements(sb, heading.getJSONArray("elements"));
        }
        return sb.toString();
    }

    private void appendTextElements(StringBuilder sb, JSONArray elements) {
        if (elements == null) {
            return;
        }
        for (int i = 0; i < elements.size(); i++) {
            JSONObject element = elements.getJSONObject(i);
            JSONObject textRun = element.getJSONObject("text_run");
            if (textRun != null) {
                sb.append(textRun.getString("content"));
            }
        }
    }

    private KnowledgeDocumentLineVo newLine(String handler, String content) {
        KnowledgeDocumentLineVo lineVo = new KnowledgeDocumentLineVo();
        lineVo.setHandler(handler);
        lineVo.setContent(content);
        return lineVo;
    }

    private void saveLines(Long documentId, Long versionId, List<KnowledgeDocumentLineVo> lineList) {
        int size = 0;
        int lineNumber = 0;
        for (KnowledgeDocumentLineVo lineVo : lineList) {
            lineVo.setLineNumber(++lineNumber);
            lineVo.setKnowledgeDocumentId(documentId);
            lineVo.setKnowledgeDocumentVersionId(versionId);
            lineVo.setUuid(UuidUtil.randomUuid());
            if (StringUtils.isNotBlank(lineVo.getContent())) {
                size += lineVo.getContent().getBytes(StandardCharsets.UTF_8).length;
                KnowledgeDocumentLineContentVo contentVo = new KnowledgeDocumentLineContentVo(lineVo.getContent());
                lineVo.setContentHash(contentVo.getHash());
                if (knowledgeDocumentMapper.checkKnowledgeDocumentLineContentHashIsExists(contentVo.getHash()) == 0) {
                    knowledgeDocumentMapper.insertKnowledgeDocumentLineContent(contentVo);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(lineList)) {
            knowledgeDocumentMapper.insertKnowledgeDocumentLineList(lineList);
        }
        KnowledgeDocumentVersionVo updateVo = new KnowledgeDocumentVersionVo();
        updateVo.setId(versionId);
        updateVo.setSize(size);
        knowledgeDocumentMapper.updateKnowledgeDocumentVersionById(updateVo);
    }

    private void loadWikiNodes(KnowledgeFeishuSyncConfigVo config, String parentNodeToken, List<String> path, List<FeishuNode> nodeList) {
        String pageToken = null;
        do {
            JSONObject query = new JSONObject();
            query.put("page_size", 50);
            if (StringUtils.isNotBlank(parentNodeToken)) {
                query.put("parent_node_token", parentNodeToken);
            }
            if (StringUtils.isNotBlank(pageToken)) {
                query.put("page_token", pageToken);
            }
            JSONObject result = feishuGet(config, "/wiki/v2/spaces/" + config.getSpaceId() + "/nodes", query);
            System.out.println("result = " + result);
            JSONObject data = result.getJSONObject("data");
            if (data == null) {
                return;
            }
            JSONArray items = data.getJSONArray("items");
            if (items != null) {
                for (int i = 0; i < items.size(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    FeishuNode node = new FeishuNode(item);
                    node.path.addAll(path);
                    if (!isDocumentNode(node)) {
                        node.path.add(node.title);
                    }
                    nodeList.add(node);
                    if (Objects.equals(item.getBoolean("has_child"), true)) {
                        loadWikiNodes(config, node.nodeToken, node.path, nodeList);
                    }
                }
            }
            pageToken = Objects.equals(data.getBoolean("has_more"), true) ? data.getString("page_token") : null;
        } while (StringUtils.isNotBlank(pageToken));
    }

    private boolean isDocumentNode(FeishuNode node) {
        return "docx".equals(node.objType) || "doc".equals(node.objType);
    }

    private String getTenantAccessToken(KnowledgeFeishuSyncConfigVo config) {
        JSONObject body = new JSONObject();
        body.put("app_id", config.getAppId());
        body.put("app_secret", config.getAppSecret());
        JSONObject result = HttpRequestUtil.post("https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal")
                .setPayload(body.toJSONString())
                .sendRequest()
                .getResultJson();
        System.out.println("getTenantAccessToken result = " + result);
        if (result == null || result.getInteger("code") == null || result.getInteger("code") != 0) {
            throw new RuntimeException(result == null ? "获取 tenant_access_token 失败" : result.getString("msg"));
        }
        return result.getString("tenant_access_token");
    }

    private JSONObject feishuGet(KnowledgeFeishuSyncConfigVo config, String path, JSONObject query) {
        String url = "https://open.feishu.cn" + OPEN_API + path;
        System.out.println("url = " + url);
        HttpRequestUtil request = HttpRequestUtil.get(url)
                .addHeader("Authorization", "Bearer " + getTenantAccessToken(config));
        if (query != null) {
            request.setQueryString(query);
        }
        JSONObject result = request.sendRequest().getResultJson();
        System.out.println("feishuGet result = " + result);
        checkFeishuResult(result);
        return result;
    }

    private JSONObject feishuPatch(KnowledgeFeishuSyncConfigVo config, String path, JSONObject payload) {
        JSONObject result = HttpRequestUtil.post("https://open.feishu.cn" + OPEN_API + path)
                .addHeader("Authorization", "Bearer " + getTenantAccessToken(config))
                .addHeader("X-HTTP-Method-Override", "PATCH")
                .setPayload(payload.toJSONString())
                .sendRequest()
                .getResultJson();
        checkFeishuResult(result);
        return result;
    }

    private void checkFeishuResult(JSONObject result) {
        if (result == null) {
            throw new RuntimeException("飞书接口无返回");
        }
        Integer code = result.getInteger("code");
        if (code != null && code != 0) {
            throw new RuntimeException(result.getString("msg"));
        }
    }

    private KnowledgeFeishuSyncAuditVo startAudit(Long configId, String direction) {
        KnowledgeFeishuSyncAuditVo audit = new KnowledgeFeishuSyncAuditVo();
        audit.setConfigId(configId);
        audit.setDirection(direction);
        audit.setStatus("running");
        audit.setTotalCount(0);
        audit.setSuccessCount(0);
        audit.setFailedCount(0);
        audit.setFcu(UserContext.get().getUserUuid(true));
        knowledgeFeishuSyncMapper.insertAudit(audit);
        return audit;
    }

    private void finishAudit(KnowledgeFeishuSyncAuditVo audit, String status, int total, int success, int failed, String error, JSONArray detailList) {
        audit.setStatus(status);
        audit.setTotalCount(total);
        audit.setSuccessCount(success);
        audit.setFailedCount(failed);
        audit.setError(error);
        JSONObject detail = new JSONObject();
        detail.put("items", detailList);
        audit.setDetail(detail);
        knowledgeFeishuSyncMapper.updateAudit(audit);
    }

    private void updateConfigSyncStatus(KnowledgeFeishuSyncConfigVo config, KnowledgeFeishuSyncAuditVo audit) {
        KnowledgeFeishuSyncConfigVo updateVo = new KnowledgeFeishuSyncConfigVo();
        updateVo.setId(config.getId());
        updateVo.setLastSyncAuditId(audit.getId());
        updateVo.setLastSyncStatus(audit.getStatus());
        updateVo.setLcu(UserContext.get().getUserUuid(true));
        knowledgeFeishuSyncMapper.updateConfigLastSync(updateVo);
    }

    private String toPlainText(List<KnowledgeDocumentLineVo> lineList) {
        StringBuilder sb = new StringBuilder();
        if (CollectionUtils.isNotEmpty(lineList)) {
            for (KnowledgeDocumentLineVo lineVo : lineList) {
                if (StringUtils.isNotBlank(lineVo.getContent())) {
                    sb.append(lineVo.getContent().replaceAll("<[^>]+>", "")).append("\n");
                }
            }
        }
        return sb.toString();
    }

    private static class FeishuNode {
        private final String nodeToken;
        private final String objToken;
        private final String objType;
        private final String title;
        private final String updateTime;
        private final List<String> path = new ArrayList<>();

        private FeishuNode(JSONObject item) {
            this.nodeToken = item.getString("node_token");
            this.objToken = item.getString("obj_token");
            this.objType = item.getString("obj_type");
            this.title = StringUtils.defaultIfBlank(item.getString("title"), item.getString("obj_token"));
            this.updateTime = item.getString("obj_edit_time");
        }
    }
}
