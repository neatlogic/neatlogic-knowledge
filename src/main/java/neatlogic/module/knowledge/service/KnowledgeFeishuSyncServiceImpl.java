package neatlogic.module.knowledge.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.fulltextindex.core.FullTextIndexHandlerFactory;
import neatlogic.framework.fulltextindex.core.IFullTextIndexHandler;
import neatlogic.framework.knowledge.constvalue.FeiShuBlockType;
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
import org.apache.commons.collections4.MapUtils;
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
//            if (StringUtils.isBlank(config.getSpaceId())) {
//                throw new ParamNotExistsException("spaceId");
//            }
            JSONArray wikiSpaceList = feishuWikiSpaces(config);
            if (CollectionUtils.isNotEmpty(wikiSpaceList)) {
                for (int i = 0; i < wikiSpaceList.size(); i++) {
                    JSONObject wikiSpaceObj = wikiSpaceList.getJSONObject(i);
                    System.out.println("wikiSpaceObj = " + wikiSpaceObj);
                    Long spaceId = wikiSpaceObj.getLong("space_id");
                    if (spaceId != null) {
                        List<FeishuNode> nodes = new ArrayList<>();
                        loadWikiNodes(config, spaceId, null, new ArrayList<>(), nodes);
                        System.out.println("nodes = " + nodes);
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
                            break;
                        }
                    }
                }
            }

            finishAudit(audit, failed == 0 ? "succeed" : "failed", total, success, failed, null, detailList);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            finishAudit(audit, "failed", total, success, failed == 0 ? 1 : failed, ex.getMessage(), detailList);
        }
        updateConfigSyncStatus(config, audit);
        return audit;
    }

//    @Override
//    @Transactional
//    public KnowledgeFeishuSyncAuditVo syncToFeishu(Long configId, Long knowledgeDocumentId) {
//        KnowledgeFeishuSyncConfigVo config = getRequiredConfig(configId);
//        KnowledgeFeishuSyncAuditVo audit = startAudit(configId, "to_feishu");
//        JSONArray detailList = new JSONArray();
//        try {
//            KnowledgeFeishuSyncDocumentVo syncDocumentVo = knowledgeFeishuSyncMapper.getSyncDocumentByDocumentId(knowledgeDocumentId);
//            if (syncDocumentVo == null) {
//                throw new RuntimeException("该知识文档没有飞书同步映射");
//            }
//            KnowledgeDocumentVo documentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(knowledgeDocumentId);
//            if (documentVo == null) {
//                throw new RuntimeException("知识文档不存在");
//            }
//            List<KnowledgeDocumentLineVo> lineList = knowledgeDocumentMapper.getKnowledgeDocumentLineListByKnowledgeDocumentVersionId(documentVo.getKnowledgeDocumentVersionId());
//            JSONObject payload = new JSONObject();
//            payload.put("title", documentVo.getTitle());
//            payload.put("content", toPlainText(lineList));
//            JSONObject item = new JSONObject();
//            item.put("knowledgeDocumentId", knowledgeDocumentId);
//            item.put("nodeToken", syncDocumentVo.getNodeToken());
//            JSONObject response = feishuPatch(config, "/docx/v1/documents/" + syncDocumentVo.getObjToken(), payload);
//            item.put("response", response);
//            item.put("status", "succeed");
//            detailList.add(item);
//            finishAudit(audit, "succeed", 1, 1, 0, null, detailList);
//        } catch (Exception ex) {
//            finishAudit(audit, "failed", 1, 0, 1, ex.getMessage(), detailList);
//        }
//        updateConfigSyncStatus(config, audit);
//        return audit;
//    }

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

    private String getContentFromElements(JSONArray elements) {
        StringBuilder builder = new StringBuilder();
        if (CollectionUtils.isNotEmpty(elements)) {
            for (int i = 0; i < elements.size(); i++) {
                JSONObject element = elements.getJSONObject(i);
                if (MapUtils.isNotEmpty(element)) {
                    JSONObject textRun = element.getJSONObject("text_run");
                    if (MapUtils.isNotEmpty(textRun)) {
                        String content = textRun.getString("content");
                        if (StringUtils.isNotBlank(content)) {
                            JSONObject textElementStyle = textRun.getJSONObject("text_element_style");
                            if (MapUtils.isNotEmpty(textElementStyle)) {
                                JSONObject link = textElementStyle.getJSONObject("link");
                                if (MapUtils.isNotEmpty(link)) {
                                    String url = link.getString("url");
                                    if (StringUtils.isNotBlank(url)) {
                                        content = String.format("<a href=\"%s\" target=\"_blank\">%s</a>", url, content);
                                    }
                                }
                            }
                            builder.append(content);
                        }
                    }
                }
            }
        }
        return builder.toString();
    }

    private List<String> getContentListFromElements(JSONArray elements) {
        List<String> list = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(elements)) {
            for (int i = 0; i < elements.size(); i++) {
                JSONObject element = elements.getJSONObject(i);
                if (MapUtils.isNotEmpty(element)) {
                    JSONObject textRun = element.getJSONObject("text_run");
                    if (MapUtils.isNotEmpty(textRun)) {
                        String content = textRun.getString("content");
                        if (StringUtils.isNotBlank(content)) {
                            JSONObject textElementStyle = textRun.getJSONObject("text_element_style");
                            if (MapUtils.isNotEmpty(textElementStyle)) {
                                JSONObject link = textElementStyle.getJSONObject("link");
                                if (MapUtils.isNotEmpty(link)) {
                                    String url = link.getString("url");
                                    if (StringUtils.isNotBlank(url)) {
                                        content = String.format("<a href=\"%s\" target=\"_blank\">%s</a>", url, content);
                                    }
                                }
                            }
                            list.add(content);
                        }
                    }
                }
            }
        }
        return list;
    }

    private KnowledgeDocumentLineVo handleText(JSONObject item) {
        Integer blockType = item.getInteger("block_type");
        FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
        String blockId = item.getString("block_id");
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("paragraph");
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "paragraph");
        configObj.put("blockUuid", blockId);
        configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item));
        JSONObject jsonObj = item.getJSONObject(feiShuBlockType.getText());
        if (MapUtils.isNotEmpty(jsonObj)) {
            JSONArray elements = jsonObj.getJSONArray("elements");
            String content = getContentFromElements(elements);
            knowledgeDocumentLineVo.setContent(content);
//            configObj.put("content", content);
        }
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        return knowledgeDocumentLineVo;
    }

    private KnowledgeDocumentLineVo handleHeading(JSONObject item) {
        Integer blockType = item.getInteger("block_type");
        FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
        String blockId = item.getString("block_id");
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("heading");
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "heading");
        configObj.put("level", feiShuBlockType.getValue() - 2);
        configObj.put("blockUuid", blockId);
        configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item));
        JSONObject jsonObj = item.getJSONObject(feiShuBlockType.getText());
        if (MapUtils.isNotEmpty(jsonObj)) {
            JSONArray elements = jsonObj.getJSONArray("elements");
            String content = getContentFromElements(elements);
            knowledgeDocumentLineVo.setContent(content);
            configObj.put("content", content);
        }
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        return knowledgeDocumentLineVo;
    }

    private KnowledgeDocumentLineVo handleIframe(JSONObject item) {
        Integer blockType = item.getInteger("block_type");
        FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
        String blockId = item.getString("block_id");
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("image");
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "image");
        configObj.put("blockUuid", blockId);
        configObj.put("name", "");
        configObj.put("uploading", false);
        configObj.put("align", "left");
        configObj.put("title", "");
        configObj.put("value", "");
        configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item));
        JSONObject jsonObj = item.getJSONObject(feiShuBlockType.getText());
        if (MapUtils.isNotEmpty(jsonObj)) {
            JSONObject component = jsonObj.getJSONObject("component");
            if (MapUtils.isNotEmpty(component)) {
                Integer iframeType = component.getInteger("iframe_type");// TODO
                String url = component.getString("url");
                configObj.put("url", url);
                configObj.put("src", url);
            }
        }
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        return knowledgeDocumentLineVo;
    }

    private KnowledgeDocumentLineVo handleOrderedList(List<JSONObject> orderedList) {
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("orderedList");
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "orderedList");
        configObj.put("className", "decimal");
        configObj.put("feiShuBlockList", orderedList);
        if (CollectionUtils.isNotEmpty(orderedList)) {
            Integer start = null;
            List<String> blockIdList = new ArrayList<>();
            List<String> contentList = new ArrayList<>();
            for (JSONObject item : orderedList) {
                String blockId = item.getString("block_id");
                blockIdList.add(blockId);
                Integer blockType = item.getInteger("block_type");
                FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
                JSONObject jsonObj = item.getJSONObject(feiShuBlockType.getText());
                if (MapUtils.isNotEmpty(jsonObj)) {
                    JSONArray elements = jsonObj.getJSONArray("elements");
                    String content = getContentFromElements(elements);
                    contentList.add("<li>" + content + "</li>");
                    if (start == null) {
                        JSONObject style = jsonObj.getJSONObject("style");
                        if (MapUtils.isNotEmpty(style)) {
                            String sequence = style.getString("sequence");
                            if (StringUtils.isNumeric(sequence)) {
                                start = Integer.parseInt(sequence);
                            }
                        }
                    }
                }
            }
            configObj.put("start", start != null ? start : 1);
            knowledgeDocumentLineVo.setContent(String.join("", contentList));
            configObj.put("content", String.join("", contentList));
            configObj.put("blockUuid", String.join(",", blockIdList));
        }
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        return knowledgeDocumentLineVo;
    }

    private KnowledgeDocumentLineVo handleBulletList(List<JSONObject> bulletList) {
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("bulletList");
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "bulletList");
        configObj.put("className", "disc");
        configObj.put("feiShuBlockList", bulletList);
        if (CollectionUtils.isNotEmpty(bulletList)) {
            Integer start = null;
            List<String> blockIdList = new ArrayList<>();
            List<String> contentList = new ArrayList<>();
            for (JSONObject item : bulletList) {
                String blockId = item.getString("block_id");
                blockIdList.add(blockId);
                Integer blockType = item.getInteger("block_type");
                FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
                JSONObject jsonObj = item.getJSONObject(feiShuBlockType.getText());
                if (MapUtils.isNotEmpty(jsonObj)) {
                    JSONArray elements = jsonObj.getJSONArray("elements");
                    String content = getContentFromElements(elements);
                    contentList.add("<li>" + content + "</li>");
//                    if (start == null) {
//                        JSONObject style = jsonObj.getJSONObject("style");
//                        if (MapUtils.isNotEmpty(style)) {
//                            String sequence = style.getString("sequence");
//                            if (StringUtils.isNumeric(sequence)) {
//                                start = Integer.parseInt(sequence);
//                            }
//                        }
//                    }
                }
            }
//            configObj.put("start", start != null ? start : 1);
            knowledgeDocumentLineVo.setContent(String.join("", contentList));
            configObj.put("content", String.join("", contentList));
            configObj.put("blockUuid", String.join(",", blockIdList));
        }
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        return knowledgeDocumentLineVo;
    }

    private KnowledgeDocumentLineVo handleTodoList(List<JSONObject> todoList) {
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("taskList");
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "taskList");
//        configObj.put("className", "disc");
        configObj.put("feiShuBlockList", todoList);
        if (CollectionUtils.isNotEmpty(todoList)) {
            Integer start = null;
            List<String> blockIdList = new ArrayList<>();
            List<String> contentList = new ArrayList<>();
            List<JSONObject> contentObjList = new ArrayList<>();
            for (JSONObject item : todoList) {
                String blockId = item.getString("block_id");
                blockIdList.add(blockId);
                Integer blockType = item.getInteger("block_type");
                FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
                JSONObject jsonObj = item.getJSONObject(feiShuBlockType.getText());
                if (MapUtils.isNotEmpty(jsonObj)) {
                    JSONArray elements = jsonObj.getJSONArray("elements");
                    String content = getContentFromElements(elements);
                    contentList.add("<li>" + content + "</li>");
                    Boolean done = null;
                    JSONObject style = jsonObj.getJSONObject("style");
                    if (MapUtils.isNotEmpty(style)) {
                        done = style.getBoolean("done");
                    }
                    contentObjList.add(new JSONObject().fluentPut("content", content).fluentPut("checked", done));
                }
            }
//            configObj.put("start", start != null ? start : 1);
            knowledgeDocumentLineVo.setContent(String.join("", contentList));
            configObj.put("contentList", contentObjList);
            configObj.put("blockUuid", String.join(",", blockIdList));
        }
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        return knowledgeDocumentLineVo;
    }

    private KnowledgeDocumentLineVo handleCode(JSONObject item) {
        Integer blockType = item.getInteger("block_type");
        FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
        String blockId = item.getString("block_id");
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("codeBlock");
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "codeBlock");
        configObj.put("blockUuid", blockId);
        configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item));
        JSONObject jsonObj = item.getJSONObject(feiShuBlockType.getText());
        if (MapUtils.isNotEmpty(jsonObj)) {
            JSONArray elements = jsonObj.getJSONArray("elements");
            List<String> contentList = getContentListFromElements(elements);
            knowledgeDocumentLineVo.setContent(String.join("\n", contentList));
//            configObj.put("content", content);
            configObj.put("value", String.join("\n", contentList));
            JSONObject style = jsonObj.getJSONObject("style");
            if (MapUtils.isNotEmpty(style)) {
                Integer language = style.getInteger("language");
                if (language != null) {
                    configObj.put("codeMode", language); // TODO
                }
            }
        }
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        return knowledgeDocumentLineVo;
    }

    private KnowledgeDocumentLineVo handleCallOut(JSONObject item, List<JSONObject> childItemList) {
        String blockId = item.getString("block_id");
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("highlightBlock");
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "highlightBlock");
        configObj.put("blockUuid", blockId);
        configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item).fluentAddAll(childItemList));
        List<String> list = new ArrayList<>();
        for (JSONObject childItem : childItemList) {
            Integer blockType = childItem.getInteger("block_type");
            FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
            JSONObject jsonObj = childItem.getJSONObject(feiShuBlockType.getText());
            if (MapUtils.isNotEmpty(jsonObj)) {
                JSONArray elements = jsonObj.getJSONArray("elements");
                List<String> contentList = getContentListFromElements(elements);
                for (String content : contentList) {
                    list.add("<p>" + content + "</p>");
                }
            }
        }
        knowledgeDocumentLineVo.setContent(String.join("", list));
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        return knowledgeDocumentLineVo;
    }

    private KnowledgeDocumentLineVo handleQuoteContainer(JSONObject item, List<JSONObject> childItemList) {
        String blockId = item.getString("block_id");
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("blockquote");
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "blockquote");
        configObj.put("blockUuid", blockId);
        configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item).fluentAddAll(childItemList));
        List<String> list = new ArrayList<>();
        for (JSONObject childItem : childItemList) {
            Integer blockType = childItem.getInteger("block_type");
            FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
            JSONObject jsonObj = childItem.getJSONObject(feiShuBlockType.getText());
            if (MapUtils.isNotEmpty(jsonObj)) {
                JSONArray elements = jsonObj.getJSONArray("elements");
                List<String> contentList = getContentListFromElements(elements);
                for (String content : contentList) {
                    list.add("<p>" + content + "</p>");
                }
            }
        }
        knowledgeDocumentLineVo.setContent(String.join("", list));
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        return knowledgeDocumentLineVo;
    }

    private KnowledgeDocumentLineVo handleView(JSONObject item, List<JSONObject> childItemList) {
        String blockId = item.getString("block_id");
        Integer viewType = null;
        JSONObject view = item.getJSONObject("view");
        if (MapUtils.isNotEmpty(view)) {
            viewType = view.getInteger("view_type");
        }
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("");
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "");
        configObj.put("blockUuid", blockId);
        configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item).fluentAddAll(childItemList));
        for (JSONObject childItem : childItemList) {
            Integer blockType = childItem.getInteger("block_type");
            FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
            JSONObject jsonObj = childItem.getJSONObject(feiShuBlockType.getText());
            if (MapUtils.isNotEmpty(jsonObj)) {
                String name = jsonObj.getString("name");
                String token = jsonObj.getString("token");
                FileVo fileVo = downloadMedias(token);
            }
        }
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        return knowledgeDocumentLineVo;
    }

    private FileVo downloadMedias(String token) {
        // https://open.feishu.cn/open-apis/drive/v1/medias/:file_token/download
        return null;
    }

    private KnowledgeDocumentLineVo handleTable(JSONObject item, List<JSONObject> childItemList) {
        String blockId = item.getString("block_id");
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("table");
        Map<String, JSONObject> childItemMap = new HashMap<>();
        for (JSONObject childItem : childItemList) {
            childItemMap.put(childItem.getString("block_id"), childItem);
        }
        JSONObject tableObj = item.getJSONObject(FeiShuBlockType.TABLE.getText());
        JSONObject property = tableObj.getJSONObject("property");
        Integer columnSize = property.getInteger("column_size");
        Integer rowSize = property.getInteger("row_size");
        JSONArray mergeInfo = property.getJSONArray("merge_info");
        List<JSONObject> headerList = new ArrayList<>(columnSize);
        for (int i = 0; i < columnSize; i++) {
            headerList.add(new JSONObject().fluentPut("width", 100));
        }
        List<JSONObject> lefterList = new ArrayList<>(rowSize);
        for (int i = 0; i < rowSize; i++) {
            lefterList.add(new JSONObject().fluentPut("height", 45));
        }
        List<JSONObject> tableList = new ArrayList<>(columnSize * rowSize);
        JSONArray tableChildren = item.getJSONArray("children");
        if (CollectionUtils.isNotEmpty(tableChildren)) {
            for (int i = 0; i < tableChildren.size(); i++) {
                int rowIndex = i / columnSize;
                int colIndex = i % columnSize;
                JSONObject mergeInfoElement = mergeInfo.getJSONObject(i);
                JSONObject tableElement = new JSONObject();
                tableElement.put("col", colIndex);
                tableElement.put("row", rowIndex);
                tableElement.put("colspan", mergeInfoElement.getInteger("colspan"));
                tableElement.put("rowspan", mergeInfoElement.getInteger("rowspan"));
                tableElement.put("verticalAlign", "top");
                tableElement.put("isHeader", 0);
                tableElement.put("cellStyle", "border-bottom:1px solid #dfe1e5");
                tableElement.put("content", "");
                String tableChildBlockId = tableChildren.getString(i);
                JSONObject childItem = childItemMap.get(tableChildBlockId);
                JSONArray tableCellChildren = childItem.getJSONArray("children");
                if (CollectionUtils.isNotEmpty(tableCellChildren)) {
                    for (String tableCellChildBlockId : tableCellChildren.toJavaList(String.class)) {
                        JSONObject tableCellChildItem = childItemMap.get(tableCellChildBlockId);
                        if (MapUtils.isNotEmpty(tableCellChildItem)) {
                            Integer blockType = tableCellChildItem.getInteger("block_type");
                            FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
                            if (feiShuBlockType == FeiShuBlockType.TEXT) {
                                JSONObject jsonObj = tableCellChildItem.getJSONObject(FeiShuBlockType.TEXT.getText());
                                if (MapUtils.isNotEmpty(jsonObj)) {
                                    JSONArray elements = jsonObj.getJSONArray("elements");
                                    String content = getContentFromElements(elements);
                                    tableElement.put("content", content);
                                }
                            }
                        }
                    }
                }
                tableList.add(tableElement);
            }
        }
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "table");
        configObj.put("blockUuid", blockId);
        configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item).fluentAddAll(childItemList));
        configObj.put("col", columnSize);
        configObj.put("row", rowSize);
        configObj.put("headerList", headerList);
        configObj.put("lefterList", lefterList);
        configObj.put("tableList", tableList);
        configObj.put("tableStyle", new JSONObject().fluentPut("td", "border-bottom:1px solid #dfe1e5").fluentPut("tr", "height:42px").fluentPut("table", "table-layout:fixed;border-collapse:collapse;width:100%;text-align:left;border:none;"));
        configObj.put("style", "table-layout:fixed;border-collapse:collapse;text-align:left;border:none");
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        return knowledgeDocumentLineVo;
    }

    private List<KnowledgeDocumentLineVo> getFeishuDocumentLines(KnowledgeFeishuSyncConfigVo config, FeishuNode node) {
        List<KnowledgeDocumentLineVo> lineList = new ArrayList<>();
        try {
//            JSONObject blockResult = feishuGet(config, "/docx/v1/documents/" + node.objToken + "/blocks/" + node.objToken + "/children", null);
            System.out.println("node.title = " + node.title);
            JSONObject blockResult = feishuGet(config, "/docx/v1/documents/" + node.objToken + "/blocks", null);
            JSONArray items = blockResult.getJSONObject("data") == null ? null : blockResult.getJSONObject("data").getJSONArray("items");
            if (CollectionUtils.isNotEmpty(items)) {
                JSONObject pageItem = null;
                Map<String, JSONObject> itemMap = new LinkedHashMap<>();
                for (int i = 0; i < items.size(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    if (MapUtils.isNotEmpty(item)) {
                        String blockId = item.getString("block_id");
                        itemMap.put(blockId, item);
                        String parentId = item.getString("parent_id");
                        Integer blockType = item.getInteger("block_type");
                        if (Objects.equals(blockType, FeiShuBlockType.PAGE.getValue()) && StringUtils.isBlank(parentId)) {
                            pageItem = item;
                        }
                    }
                }
                if (pageItem != null) {
                    String title = null;
                    JSONObject pageObj = pageItem.getJSONObject(FeiShuBlockType.PAGE.getText());
                    if (MapUtils.isNotEmpty(pageObj)) {
                        JSONArray elements = pageObj.getJSONArray("elements");
                        title = getContentFromElements(elements);
                    }
                    JSONArray children = pageItem.getJSONArray("children");
                    if (CollectionUtils.isNotEmpty(children)) {
                        List<String> handledBlockIdList = new ArrayList<>();
                        List<String> blockIdList = children.toJavaList(String.class);
                        for (String blockId : blockIdList) {
                            JSONObject item = itemMap.get(blockId);
                            String parentId = item.getString("parent_id");
                            Integer blockType = item.getInteger("block_type");
                            System.out.println("blockType = " + blockType);
                            FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
                            System.out.println("feiShuBlockType = " + feiShuBlockType.getText());
                            if (handledBlockIdList.contains(blockId)) {
                                continue;
                            }
                            handledBlockIdList.add(blockId);
                            if (feiShuBlockType == FeiShuBlockType.TEXT) {
                                KnowledgeDocumentLineVo line = handleText(item);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.HEADING1) {
                                KnowledgeDocumentLineVo line = handleHeading(item);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.HEADING2) {
                                KnowledgeDocumentLineVo line = handleHeading(item);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.HEADING3) {
                                KnowledgeDocumentLineVo line = handleHeading(item);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.HEADING4) {
                                KnowledgeDocumentLineVo line = handleHeading(item);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.HEADING5) {
                                KnowledgeDocumentLineVo line = handleHeading(item);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.HEADING6) {
                                KnowledgeDocumentLineVo line = handleHeading(item);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.HEADING7) {
                                KnowledgeDocumentLineVo line = handleHeading(item);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.HEADING8) {
                                KnowledgeDocumentLineVo line = handleHeading(item);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.HEADING9) {
                                KnowledgeDocumentLineVo line = handleHeading(item);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.BULLET) {
                                List<JSONObject> bulletList = new ArrayList<>();
                                boolean isStart = false;
                                for (Map.Entry<String, JSONObject> entry : itemMap.entrySet()) {
                                    JSONObject value = entry.getValue();
                                    String key = entry.getKey();
                                    if (!isStart && Objects.equals(key, blockId)) {
                                        bulletList.add(value);
                                        isStart = true;
                                    } else if (isStart) {
                                        if (Objects.equals(value.getInteger("block_type"), feiShuBlockType.getValue())
                                                && Objects.equals(value.getString("parent_id"), parentId)) {
                                            bulletList.add(value);
                                            handledBlockIdList.add(value.getString("block_id"));
                                        } else {
                                            break;
                                        }
                                    }
                                }
                                if (CollectionUtils.isNotEmpty(bulletList)) {
                                    KnowledgeDocumentLineVo line = handleBulletList(bulletList);
                                    lineList.add(line);
                                }
                            } else if (feiShuBlockType == FeiShuBlockType.ORDERED) {
                                List<JSONObject> orderedList = new ArrayList<>();
                                boolean isStart = false;
                                for (Map.Entry<String, JSONObject> entry : itemMap.entrySet()) {
                                    JSONObject value = entry.getValue();
                                    String key = entry.getKey();
                                    if (!isStart && Objects.equals(key, blockId)) {
                                        orderedList.add(value);
                                        isStart = true;
                                    } else if (isStart) {
                                        if (Objects.equals(value.getInteger("block_type"), feiShuBlockType.getValue())
                                                && Objects.equals(value.getString("parent_id"), parentId)) {
                                            orderedList.add(value);
                                            handledBlockIdList.add(value.getString("block_id"));
                                        } else {
                                            break;
                                        }
                                    }
                                }
                                if (CollectionUtils.isNotEmpty(orderedList)) {
                                    KnowledgeDocumentLineVo line = handleOrderedList(orderedList);
                                    lineList.add(line);
                                }
                            } else if (feiShuBlockType == FeiShuBlockType.CODE) {
                                KnowledgeDocumentLineVo line = handleCode(item);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.QUOTE_CONTAINER) {
                                List<JSONObject> childItemList = new ArrayList<>();
                                JSONArray array = item.getJSONArray("children");
                                if (CollectionUtils.isNotEmpty(array)) {
                                    List<String> list = array.toJavaList(String.class);
                                    for (String str : list) {
                                        JSONObject childItem = itemMap.get(str);
                                        if (MapUtils.isNotEmpty(childItem)) {
                                            childItemList.add(childItem);
                                            handledBlockIdList.add(str);
                                        }
                                    }
                                }
                                KnowledgeDocumentLineVo line = handleQuoteContainer(item, childItemList);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.TODO) {
                                List<JSONObject> todoList = new ArrayList<>();
                                boolean isStart = false;
                                for (Map.Entry<String, JSONObject> entry : itemMap.entrySet()) {
                                    JSONObject value = entry.getValue();
                                    String key = entry.getKey();
                                    if (!isStart && Objects.equals(key, blockId)) {
                                        todoList.add(value);
                                        isStart = true;
                                    } else if (isStart) {
                                        if (Objects.equals(value.getInteger("block_type"), feiShuBlockType.getValue())
                                                && Objects.equals(value.getString("parent_id"), parentId)) {
                                            todoList.add(value);
                                            handledBlockIdList.add(value.getString("block_id"));
                                        } else {
                                            break;
                                        }
                                    }
                                }
                                if (CollectionUtils.isNotEmpty(todoList)) {
                                    KnowledgeDocumentLineVo line = handleTodoList(todoList);
                                    lineList.add(line);
                                }
                            } else if (feiShuBlockType == FeiShuBlockType.CALLOUT) {
                                List<JSONObject> childItemList = new ArrayList<>();
                                JSONArray array = item.getJSONArray("children");
                                if (CollectionUtils.isNotEmpty(array)) {
                                    List<String> list = array.toJavaList(String.class);
                                    for (String str : list) {
                                        JSONObject childItem = itemMap.get(str);
                                        if (MapUtils.isNotEmpty(childItem)) {
                                            childItemList.add(childItem);
                                            handledBlockIdList.add(str);
                                        }
                                    }
                                }
                                KnowledgeDocumentLineVo line = handleCallOut(item, childItemList);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.DIVIDER) {
                                KnowledgeDocumentLineVo line = new KnowledgeDocumentLineVo();
                                JSONObject configObj = new JSONObject();
                                configObj.put("blockType", "divider");
                                configObj.put("blockUuid", blockId);
                                line.setConfig(configObj.toJSONString());
                                line.setHandler("divider");
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.IFRAME) {
                                KnowledgeDocumentLineVo line = handleIframe(item);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.VIEW) {
                                List<JSONObject> childItemList = new ArrayList<>();
                                JSONArray array = item.getJSONArray("children");
                                if (CollectionUtils.isNotEmpty(array)) {
                                    List<String> list = array.toJavaList(String.class);
                                    for (String str : list) {
                                        JSONObject childItem = itemMap.get(str);
                                        if (MapUtils.isNotEmpty(childItem)) {
                                            childItemList.add(childItem);
                                            handledBlockIdList.add(str);
                                        }
                                    }
                                }
                                KnowledgeDocumentLineVo line = handleView(item, childItemList);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.IMAGE) {
//                                KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
//                                knowledgeDocumentLineVo.setHandler("image");

                            } else if (feiShuBlockType == FeiShuBlockType.TABLE) {
                                List<JSONObject> childItemList = new ArrayList<>();
                                JSONArray array = item.getJSONArray("children");
                                if (CollectionUtils.isNotEmpty(array)) {
                                    List<String> list = array.toJavaList(String.class);
                                    for (String str : list) {
                                        JSONObject childItem = itemMap.get(str);
                                        if (MapUtils.isNotEmpty(childItem)) {
                                            childItemList.add(childItem);
                                            handledBlockIdList.add(str);
                                            Integer childBlockType = childItem.getInteger("block_type");
                                            FeiShuBlockType childFeiShuBlockType = FeiShuBlockType.getFeiShuBlockType(childBlockType);
                                            if (childFeiShuBlockType == FeiShuBlockType.TABLE_CELL) {
                                                JSONArray tableCellChildArray = childItem.getJSONArray("children");
                                                if (CollectionUtils.isNotEmpty(tableCellChildArray)) {
                                                    List<String> tableCellChildList = tableCellChildArray.toJavaList(String.class);
                                                    for (String tableCellChild : tableCellChildList) {
                                                        JSONObject tableCellChildItem = itemMap.get(tableCellChild);
                                                        if (MapUtils.isNotEmpty(tableCellChildItem)) {
                                                            childItemList.add(tableCellChildItem);
                                                            handledBlockIdList.add(tableCellChild);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                KnowledgeDocumentLineVo line = handleTable(item, childItemList);
                                lineList.add(line);
                            } else {
                                KnowledgeDocumentLineVo line = new KnowledgeDocumentLineVo();
                                JSONObject configObj = new JSONObject();
                                configObj.put("blockType", feiShuBlockType.getText());
                                configObj.put("blockUuid", blockId);
                                configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item));
                                line.setConfig(configObj.toJSONString());
                                line.setHandler(feiShuBlockType.getText());
                                lineList.add(line);
                            }
                        }
                    }
                }
//                for (int i = 0; i < items.size(); i++) {
//                    JSONObject item = items.getJSONObject(i);
//
//
//                    String text = extractBlockText(items.getJSONObject(i));
//                    if (StringUtils.isNotBlank(text)) {
//                        lineList.add(newLine("p", text));
//                    }
//                }
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
            if (lineVo.getConfig() != null) {
                KnowledgeDocumentLineConfigVo knowledgeDocumentLineConfigVo = new KnowledgeDocumentLineConfigVo(lineVo.getConfigStr());
                lineVo.setConfigHash(knowledgeDocumentLineConfigVo.getHash());
                if (knowledgeDocumentMapper.checkKnowledgeDocumentLineConfigHashIsExists(knowledgeDocumentLineConfigVo.getHash()) == 0) {
                    knowledgeDocumentMapper.insertKnowledgeDocumentLineConfig(knowledgeDocumentLineConfigVo);
                }
            }
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

    private void loadWikiNodes(KnowledgeFeishuSyncConfigVo config, Long spaceId, String parentNodeToken, List<String> path, List<FeishuNode> nodeList) {
        String pageToken = null;
        Boolean hasMore = false;
        do {
            JSONObject query = new JSONObject();
            query.put("page_size", 50);
            if (StringUtils.isNotBlank(parentNodeToken)) {
                query.put("parent_node_token", parentNodeToken);
            }
            if (StringUtils.isNotBlank(pageToken)) {
                query.put("page_token", pageToken);
            }
            JSONObject result = feishuGet(config, "/wiki/v2/spaces/" + spaceId + "/nodes", query);
            System.out.println("result = " + result);
            checkFeishuResult(result);
            JSONObject data = result.getJSONObject("data");
            if (data == null) {
                return;
            }
            hasMore = data.getBoolean("has_more");
            pageToken = data.getString("page_token");
            JSONArray items = data.getJSONArray("items");
            if (CollectionUtils.isNotEmpty(items)) {
                for (int i = 0; i < items.size(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    FeishuNode node = new FeishuNode(item);
                    node.path.addAll(path);
                    if (!isDocumentNode(node)) {
                        node.path.add(node.title);
                    }
                    nodeList.add(node);
                    if (Objects.equals(item.getBoolean("has_child"), true)) {
                        System.out.println("子查询");
                        loadWikiNodes(config, spaceId, node.nodeToken, node.path, nodeList);
                    }
                }
            }
//            pageToken = Objects.equals(data.getBoolean("has_more"), true) ? data.getString("page_token") : null;
        } while (Objects.equals(hasMore, true));
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

//    private String getUserAccessToken(KnowledgeFeishuSyncConfigVo config) {
//        if (StringUtils.isBlank(config.getUserAccessToken())) {
//            throw new ParamNotExistsException("userAccessToken");
//        }
//        return config.getUserAccessToken();
//    }

    private JSONArray feishuWikiSpaces(KnowledgeFeishuSyncConfigVo config) {
        JSONArray resultArray = new JSONArray();
        String pageToken = null;
        Boolean hasMore = false;
        do {
            JSONObject query = new JSONObject();
            query.put("page_size", 50);
            if (StringUtils.isNotBlank(pageToken)) {
                query.put("page_token", pageToken);
            }
            String url = "https://open.feishu.cn/open-apis/wiki/v2/spaces";
            HttpRequestUtil request = HttpRequestUtil.get(url)
                    .addHeader("Authorization", "Bearer " + getTenantAccessToken(config));
//                .addHeader("Authorization", "Bearer " + getUserAccessToken(config));
            if (query != null) {
                request.setQueryString(query);
            }
            JSONObject resultObj = request.sendRequest().getResultJson();
            System.out.println("feishuWikiSpaces resultObj = " + resultObj);
            checkFeishuResult(resultObj);
            JSONObject data = resultObj.getJSONObject("data");
            if (MapUtils.isNotEmpty(data)) {
                pageToken = data.getString("page_token");
                hasMore = data.getBoolean("has_more");
                JSONArray items = data.getJSONArray("items");
                if (CollectionUtils.isNotEmpty(items)) {
                    resultArray.addAll(items);
                }
            }
        } while (Objects.equals(hasMore, true));
        return resultArray;
    }

    private JSONObject feishuGet(KnowledgeFeishuSyncConfigVo config, String path, JSONObject query) {
        String url = "https://open.feishu.cn" + OPEN_API + path;
        System.out.println("url = " + url);
        HttpRequestUtil request = HttpRequestUtil.get(url)
                .addHeader("Authorization", "Bearer " + getTenantAccessToken(config));
//                .addHeader("Authorization", "Bearer " + getUserAccessToken(config));
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
//                .addHeader("Authorization", "Bearer " + getUserAccessToken(config))
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
