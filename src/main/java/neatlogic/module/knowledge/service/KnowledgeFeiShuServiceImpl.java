package neatlogic.module.knowledge.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.config.ConfigManager;
import neatlogic.framework.file.dao.mapper.FileMapper;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.fulltextindex.core.FullTextIndexHandlerFactory;
import neatlogic.framework.fulltextindex.core.IFullTextIndexHandler;
import neatlogic.framework.knowledge.constvalue.*;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeCircleMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import neatlogic.framework.knowledge.dto.*;
import neatlogic.framework.knowledge.dto.feishu.FeiShuAppCredentialsVo;
import neatlogic.framework.knowledge.dto.feishu.FeiShuNodeVo;
import neatlogic.framework.knowledge.dto.feishu.FeiShuSpaceVo;
import neatlogic.framework.knowledge.dto.feishu.KnowledgeFeiShuDocumentMappingVo;
import neatlogic.framework.util.UuidUtil;
import neatlogic.module.knowledge.dao.mapper.KnowledgeFeiShuMapper;
import neatlogic.module.knowledge.source.FeiShuSyncSource;
import neatlogic.module.knowledge.utils.FeiShuOpenApiUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class KnowledgeFeiShuServiceImpl implements KnowledgeFeiShuService {

    private final Logger logger = LoggerFactory.getLogger(KnowledgeFeiShuServiceImpl.class);

    @Resource
    private KnowledgeFeiShuMapper knowledgeFeiShuMapper;
    @Resource
    private KnowledgeCircleMapper knowledgeCircleMapper;
    @Resource
    private KnowledgeDocumentTypeMapper knowledgeDocumentTypeMapper;
    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Resource
    private FileMapper fileMapper;

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

    @Override
    public List<FeiShuSpaceVo> getFeiShuSpaceList(FeiShuAppCredentialsVo feiShuAppCredentials) {
        List<FeiShuSpaceVo> feiShuSpaceList = new ArrayList<>();
        if (feiShuAppCredentials != null) {
            String tenantAccessToken = FeiShuOpenApiUtil.getTenantAccessToken(feiShuAppCredentials.getAppId(), feiShuAppCredentials.getAppSecret());
            JSONObject resultObj = FeiShuOpenApiUtil.getFeiShuWikiSpaces(tenantAccessToken);
            JSONObject data = resultObj.getJSONObject("data");
            if (MapUtils.isNotEmpty(data)) {
                JSONArray items = data.getJSONArray("items");
                if (CollectionUtils.isNotEmpty(items)) {
                    for (int i = 0; i < items.size(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        feiShuSpaceList.add(new FeiShuSpaceVo(item));
                    }
                }
            }
        }
        return feiShuSpaceList;
    }

    @Override
    public void saveNodes(List<FeiShuNodeVo> nodes, FeiShuAppCredentialsVo config, KnowledgeDocumentTypeVo knowledgeType, String tenantAccessToken) {
        for (FeiShuNodeVo node : nodes) {
            if (!isDocumentNode(node)) {
                continue;
            }
//            if (!Objects.equals(node.getNodeToken(), "EnH5wTCBMiZibmkDu6lcmI94n8g")) {
//                continue;
//            }
            try {
                saveFeiShuDocument(config, node, knowledgeType.getUuid(), tenantAccessToken);
                if (CollectionUtils.isNotEmpty(node.getChildren())) {
                    KnowledgeDocumentTypeVo childType = getOrCreateKnowledgeType(node.getTitle(), knowledgeType.getUuid(), config.getKnowledgeCircleId());
                    saveNodes(node.getChildren(), config, childType, tenantAccessToken);
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }

    @Override
    public void saveFeiShuDocument(FeiShuAppCredentialsVo appCredentialsVo, FeiShuNodeVo node, String typeUuid, String tenantAccessToken) {
        System.out.println("node = " + JSONObject.toJSONString(node));
        String status = null;
        JSONObject config = new JSONObject();
        KnowledgeDocumentVo documentVo = null;
        try {
//            knowledgeFeiShuMapper.updateFeiShuDocumentMappingStatusByNodeToken(node.getNodeToken(), Status.RUNNING.getValue());
            JSONArray unprocessedItems = new JSONArray();
            List<KnowledgeDocumentLineVo> feishuDocumentLines = getFeiShuDocumentLines(node, tenantAccessToken, unprocessedItems);
            KnowledgeFeiShuDocumentMappingVo mapping = knowledgeFeiShuMapper.getFeiShuDocumentMappingByNodeToken(node.getNodeToken());
            if (mapping != null && mapping.getKnowledgeDocumentId() != null) {
                documentVo = knowledgeDocumentMapper.getKnowledgeDocumentLockById(mapping.getKnowledgeDocumentId());
            }
            if (documentVo != null) {
                documentVo.setTitle(node.getTitle());
                documentVo.setKnowledgeDocumentTypeUuid(typeUuid);
                knowledgeDocumentMapper.updateKnowledgeDocumentTitleById(documentVo);
                knowledgeDocumentMapper.updateKnowledgeDocumentTypeUuidById(documentVo);
            } else {
                documentVo = new KnowledgeDocumentVo();
                documentVo.setTitle(node.getTitle());
                documentVo.setKnowledgeCircleId(appCredentialsVo.getKnowledgeCircleId());
                documentVo.setKnowledgeDocumentTypeUuid(typeUuid);
                documentVo.setVersion(0);
                documentVo.setFcu(UserContext.get().getUserUuid(true));
                documentVo.setSource(FeiShuSyncSource.SOURCE);
                knowledgeDocumentMapper.insertKnowledgeDocument(documentVo);
                knowledgeDocumentMapper.insertKnowledgeDocumentViewCount(documentVo.getId(), 0);
            }
            KnowledgeDocumentVersionVo versionVo = new KnowledgeDocumentVersionVo();
            versionVo.setTitle(node.getTitle());
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

            saveLines(documentVo.getId(), versionVo.getId(), feishuDocumentLines);
            if (CollectionUtils.isNotEmpty(unprocessedItems)) {
                config.put("unprocessedItems", unprocessedItems);
                status = Status.UNSUPPORTED.getValue();
            } else {
                status = Status.SUCCEED.getValue();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            config.put("error", ExceptionUtils.getStackFrames(e));
            status = Status.FAILED.getValue();
        }
        upsertMapping(appCredentialsVo, node, documentVo, status, config);
        IFullTextIndexHandler handler = FullTextIndexHandlerFactory.getHandler(KnowledgeFullTextIndexType.KNOW_DOCUMENT_VERSION);
        if (handler != null && documentVo != null) {
            handler.createIndex(documentVo.getKnowledgeDocumentVersionId());
        }
    }

    @Override
    @Transactional
    public boolean updateFeiShuDocumentMappingStatusByNodeToken(String nodeToken, Status fromStatus, Status toStatus) {
        KnowledgeFeiShuDocumentMappingVo feiShuDocumentMapping = knowledgeFeiShuMapper.getFeiShuDocumentMappingForLockByNodeToken(nodeToken);
        if (feiShuDocumentMapping != null && Objects.equals(feiShuDocumentMapping.getStatus(), fromStatus.getValue())) {
            knowledgeFeiShuMapper.updateFeiShuDocumentMappingStatusByNodeToken(nodeToken, toStatus.getValue());
            return true;
        }
        return false;
    }

    private void upsertMapping(FeiShuAppCredentialsVo appCredentialsVo, FeiShuNodeVo node, KnowledgeDocumentVo documentVo, String status, JSONObject config) {
        KnowledgeFeiShuDocumentMappingVo vo = new KnowledgeFeiShuDocumentMappingVo();
        vo.setAppId(appCredentialsVo.getAppId());
        vo.setSpaceId(node.getSpaceId());
        vo.setTitle(node.getTitle());
        vo.setParentNodeToken(node.getParentNodeToken());
        vo.setNodeToken(node.getNodeToken());
        vo.setObjToken(node.getObjToken());
        vo.setObjType(node.getObjType());
        vo.setUpdateTime(node.getUpdateTime());
        if (documentVo != null) {
            vo.setKnowledgeDocumentId(documentVo.getId());
            vo.setKnowledgeDocumentVersionId(documentVo.getKnowledgeDocumentVersionId());
            vo.setKnowledgeDocumentTypeUuid(documentVo.getKnowledgeDocumentTypeUuid());
        }
        vo.setStatus(status);
        if (MapUtils.isNotEmpty(config)) {
            vo.setConfig(config);
        }
        vo.setLcu(UserContext.get().getUserUuid());
        knowledgeFeiShuMapper.insertFeiShuDocumentMapping(vo);
//        if (knowledgeFeiShuMapper.getFeiShuDocumentMappingByNodeToken(node.getNodeToken()) == null) {
//            knowledgeFeiShuMapper.insertFeiShuDocumentMapping(vo);
//        } else {
//            knowledgeFeiShuMapper.updateFeiShuDocumentMapping(vo);
//        }
    }


    private FileVo downloadMedias(String fileToken) {
        Long fileId = knowledgeFeiShuMapper.getFeiShuMediasMappingFileIdByFileToken(fileToken);
        if (fileId != null) {
            FileVo fileVo = fileMapper.getFileById(fileId);
            if (fileVo != null) {
                return fileVo;
            }
        }
        FeiShuAppCredentialsVo feiShuAppCredentials = getFeiShuAppCredentials();
        if (feiShuAppCredentials == null) {
            return null;
        }
        String tenantAccessToken = FeiShuOpenApiUtil.getTenantAccessToken(feiShuAppCredentials.getAppId(), feiShuAppCredentials.getAppSecret());
        FileVo fileVo = FeiShuOpenApiUtil.downloadMedias(fileToken, tenantAccessToken);
        if (fileVo != null) {
            fileMapper.insertFile(fileVo);
            knowledgeFeiShuMapper.insertFeiShuMediasMapping(fileToken, fileVo.getId());
        }
        return fileVo;
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

    private List<String> handleText(JSONObject item) {
        Integer blockType = item.getInteger("block_type");
        FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
        JSONObject jsonObj = item.getJSONObject(feiShuBlockType.getText());
        if (MapUtils.isNotEmpty(jsonObj)) {
            JSONArray elements = jsonObj.getJSONArray("elements");
            return getContentListFromElements(elements);
        }
        return new ArrayList<>();
    }

    private List<KnowledgeDocumentLineVo> handleText(JSONObject item, List<JSONObject> childItemList, JSONArray unprocessedItems, List<String> allBlockIdList) {
        List<KnowledgeDocumentLineVo> resultList = new ArrayList<>();
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
        resultList.add(knowledgeDocumentLineVo);
        allBlockIdList.remove(blockId);
        if (CollectionUtils.isNotEmpty(childItemList)) {
//            logger.error("handleText 方法中childItemList入参未处理,{}", JSONObject.toJSONString(childItemList));
            List<KnowledgeDocumentLineVo> lines = handleChildItemList(item, childItemList, unprocessedItems, allBlockIdList);
            if (CollectionUtils.isNotEmpty(lines)) {
                resultList.addAll(lines);
            }
        }
        return resultList;
    }

    private List<KnowledgeDocumentLineVo> handleHeading(JSONObject item, List<JSONObject> childItemList, JSONArray unprocessedItems, List<String> allBlockIdList) {
        List<KnowledgeDocumentLineVo> resultList = new ArrayList<>();
        Integer blockType = item.getInteger("block_type");
        FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
        String blockId = item.getString("block_id");
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("heading");
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "heading");
        int level = feiShuBlockType.getValue() - 2;
        configObj.put("level", Math.min(level, 6));
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
        resultList.add(knowledgeDocumentLineVo);
        allBlockIdList.remove(blockId);
        if (CollectionUtils.isNotEmpty(childItemList)) {
//            logger.error("handleHeading 方法中childItemList入参未处理,{}", JSONObject.toJSONString(childItemList));
            List<KnowledgeDocumentLineVo> lines = handleChildItemList(item, childItemList, unprocessedItems, allBlockIdList);
            if (CollectionUtils.isNotEmpty(lines)) {
                resultList.addAll(lines);
            }
        }
        return resultList;
    }

//    private KnowledgeDocumentLineVo handleIframe(JSONObject item) {
//        Integer blockType = item.getInteger("block_type");
//        FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
//        String blockId = item.getString("block_id");
//        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
//        knowledgeDocumentLineVo.setHandler("image");
//        JSONObject configObj = new JSONObject();
//        configObj.put("blockType", "image");
//        configObj.put("blockUuid", blockId);
//        configObj.put("name", "");
//        configObj.put("uploading", false);
//        configObj.put("align", "left");
//        configObj.put("title", "");
//        configObj.put("value", "");
//        configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item));
//        JSONObject jsonObj = item.getJSONObject(feiShuBlockType.getText());
//        if (MapUtils.isNotEmpty(jsonObj)) {
//            JSONObject component = jsonObj.getJSONObject("component");
//            if (MapUtils.isNotEmpty(component)) {
//                Integer iframeType = component.getInteger("iframe_type");
//                String url = component.getString("url");
//                configObj.put("url", url);
//                configObj.put("src", url);
//            }
//        }
//        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
//        return knowledgeDocumentLineVo;
//    }

    private List<KnowledgeDocumentLineVo> handleOrderedList(List<JSONObject> orderedList, List<JSONObject> childItemList, JSONArray unprocessedItems, List<String> allBlockIdList) {
        List<KnowledgeDocumentLineVo> resultList = new ArrayList<>();
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
                allBlockIdList.remove(blockId);
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
        resultList.add(knowledgeDocumentLineVo);
        if (CollectionUtils.isNotEmpty(childItemList)) {
            logger.error("handleOrderedList 方法中childItemList入参未处理,{}", JSONObject.toJSONString(childItemList));
//            List<KnowledgeDocumentLineVo> lines = handleChildItemList(item, childItemList, unprocessedItems, allBlockIdList);
//            if (CollectionUtils.isNotEmpty(lines)) {
//                resultList.addAll(lines);
//            }
        }
        return resultList;
    }

    private List<KnowledgeDocumentLineVo> handleBulletList(List<JSONObject> bulletList, List<JSONObject> childItemList, JSONArray unprocessedItems, List<String> allBlockIdList) {
        List<KnowledgeDocumentLineVo> resultList = new ArrayList<>();
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
                allBlockIdList.remove(blockId);
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
        resultList.add(knowledgeDocumentLineVo);
        if (CollectionUtils.isNotEmpty(childItemList)) {
            logger.error("handleBulletList 方法中childItemList入参未处理,{}", JSONObject.toJSONString(childItemList));
        }
        return resultList;
    }

    private List<KnowledgeDocumentLineVo> handleTodoList(List<JSONObject> todoList, List<JSONObject> childItemList, JSONArray unprocessedItems, List<String> allBlockIdList) {
        List<KnowledgeDocumentLineVo> resultList = new ArrayList<>();
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
                allBlockIdList.remove(blockId);
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
        resultList.add(knowledgeDocumentLineVo);
        if (CollectionUtils.isNotEmpty(childItemList)) {
            logger.error("handleTodoList 方法中childItemList入参未处理,{}", JSONObject.toJSONString(childItemList));
        }
        return resultList;
    }

    private List<KnowledgeDocumentLineVo> handleCode(JSONObject item, List<JSONObject> childItemList, JSONArray unprocessedItems, List<String> allBlockIdList) {
        List<KnowledgeDocumentLineVo> resultList = new ArrayList<>();
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
        resultList.add(knowledgeDocumentLineVo);
        allBlockIdList.remove(blockId);
        if (CollectionUtils.isNotEmpty(childItemList)) {
//            logger.error("handleCode 方法中childItemList入参未处理,{}", JSONObject.toJSONString(childItemList));
            List<KnowledgeDocumentLineVo> lines = handleChildItemList(item, childItemList, unprocessedItems, allBlockIdList);
            if (CollectionUtils.isNotEmpty(lines)) {
                resultList.addAll(lines);
            }
        }
        return resultList;
    }

    private List<KnowledgeDocumentLineVo> handleCallOut(JSONObject item, List<JSONObject> childItemList, JSONArray unprocessedItems, List<String> allBlockIdList) {
        List<KnowledgeDocumentLineVo> resultList = new ArrayList<>();
        String blockId = item.getString("block_id");
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("highlightBlock");
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "highlightBlock");
        configObj.put("blockUuid", blockId);
        configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item).fluentAddAll(childItemList));
        List<String> list = new ArrayList<>();
        for (JSONObject childItem : childItemList) {
            allBlockIdList.remove(childItem.getString("block_id"));
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
        resultList.add(knowledgeDocumentLineVo);
        allBlockIdList.remove(blockId);
//        if (CollectionUtils.isNotEmpty(childItemList)) {
////            logger.error("handleCallOut 方法中childItemList入参未处理,{}", JSONObject.toJSONString(childItemList));
//            List<KnowledgeDocumentLineVo> lines = handleChildItemList(item, childItemList, unprocessedItems, allBlockIdList);
//            if (CollectionUtils.isNotEmpty(lines)) {
//                resultList.addAll(lines);
//            }
//        }
        return resultList;
    }

    private List<KnowledgeDocumentLineVo> handleQuote(JSONObject item, List<JSONObject> childItemList, JSONArray unprocessedItems, List<String> allBlockIdList) {
        List<KnowledgeDocumentLineVo> resultList = new ArrayList<>();
        String blockId = item.getString("block_id");
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("blockquote");
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "blockquote");
        configObj.put("blockUuid", blockId);
        configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item));
        List<String> list = new ArrayList<>();
        Integer blockType = item.getInteger("block_type");
        FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
        JSONObject jsonObj = item.getJSONObject(feiShuBlockType.getText());
        if (MapUtils.isNotEmpty(jsonObj)) {
            JSONArray elements = jsonObj.getJSONArray("elements");
            List<String> contentList = getContentListFromElements(elements);
            for (String content : contentList) {
                list.add("<p>" + content + "</p>");
            }
        }
        knowledgeDocumentLineVo.setContent(String.join("", list));
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        resultList.add(knowledgeDocumentLineVo);
        allBlockIdList.remove(blockId);
        if (CollectionUtils.isNotEmpty(childItemList)) {
//            logger.error("handleQuote 方法中childItemList入参未处理,{}", JSONObject.toJSONString(childItemList));
            List<KnowledgeDocumentLineVo> lines = handleChildItemList(item, childItemList, unprocessedItems, allBlockIdList);
            if (CollectionUtils.isNotEmpty(lines)) {
                resultList.addAll(lines);
            }
        }
        return resultList;
    }

    private List<KnowledgeDocumentLineVo> handleQuoteContainer(JSONObject item, List<JSONObject> childItemList, JSONArray unprocessedItems, List<String> allBlockIdList) {
        List<KnowledgeDocumentLineVo> resultList = new ArrayList<>();
        String blockId = item.getString("block_id");
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("blockquote");
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "blockquote");
        configObj.put("blockUuid", blockId);
        configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item).fluentAddAll(childItemList));
        List<String> list = new ArrayList<>();
        for (JSONObject childItem : childItemList) {
            allBlockIdList.remove(childItem.getString("block_id"));
            Integer blockType = childItem.getInteger("block_type");
            FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
            if (feiShuBlockType == FeiShuBlockType.TEXT) {
                List<String> contentList = handleText(childItem);
                if (CollectionUtils.isNotEmpty(contentList)) {
                    list.add("<p>" + String.join("", contentList) + "</p>");
                }
            } else if (feiShuBlockType == FeiShuBlockType.IMAGE) {
                JSONObject jsonObj = handleImage(childItem);
                if (MapUtils.isNotEmpty(jsonObj)) {
                    String url = jsonObj.getString("url");
                    Integer width = jsonObj.getInteger("width");
                    Integer height = jsonObj.getInteger("height");
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("<img src='");
                    stringBuilder.append(url);
                    stringBuilder.append("' ");
                    if (width != null) {
                        stringBuilder.append("width='");
                        stringBuilder.append(width);
                        stringBuilder.append("' ");
                    }
                    if (height != null) {
                        stringBuilder.append("height='");
                        stringBuilder.append(height);
                        stringBuilder.append("' ");
                    }
                    stringBuilder.append("/>");
                    list.add("<p>" + stringBuilder + "</p>");
                }
            }
        }
        knowledgeDocumentLineVo.setContent(String.join("", list));
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        resultList.add(knowledgeDocumentLineVo);
        allBlockIdList.remove(blockId);
        return resultList;
    }

    private List<KnowledgeDocumentLineVo> handleView(JSONObject item, List<JSONObject> childItemList, JSONArray unprocessedItems, List<String> allBlockIdList) {
        List<KnowledgeDocumentLineVo> resultList = new ArrayList<>();
        String handler = "paragraph";
        String blockId = item.getString("block_id");
        {
            Integer blockType = item.getInteger("block_type");
            FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
            Integer viewType = null;
            JSONObject jsonObj = item.getJSONObject(feiShuBlockType.getText());
            if (MapUtils.isNotEmpty(jsonObj)) {
                viewType = jsonObj.getInteger("view_type");
                if (Objects.equals(viewType, 1)) {
                    handler = "file";
                } else if (Objects.equals(viewType, 2)) {
                    handler = "video";
                } else if (Objects.equals(viewType, 3)) {
//                    handler = "image";
                }
            }
        }
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler(handler);
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", handler);
        configObj.put("blockUuid", blockId);
        configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item).fluentAddAll(childItemList));
        for (JSONObject childItem : childItemList) {
            allBlockIdList.remove(childItem.getString("block_id"));
            Integer blockType = childItem.getInteger("block_type");
            FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
            JSONObject jsonObj = childItem.getJSONObject(feiShuBlockType.getText());
            if (MapUtils.isNotEmpty(jsonObj)) {
                String name = jsonObj.getString("name");
                String token = jsonObj.getString("token");
                FileVo fileVo = downloadMedias(token);
                if (fileVo != null) {
                    if (Objects.equals(handler, "video")) {
                        configObj.put("src", "api/binary/file/download?id=" + fileVo.getId());
                    } else if (Objects.equals(handler, "file")) {
                        configObj.put("name", fileVo.getName());
                        configObj.put("size", fileVo.getSize());
                    }
                    configObj.put("url", "api/binary/file/download?id=" + fileVo.getId());
//                    configObj.put("uploading", false);
                    configObj.put("align", "left");
                }
            }
        }
        if (Objects.equals(handler, "paragraph")) {
            knowledgeDocumentLineVo.setContent(configObj.toJSONString());
        }
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        resultList.add(knowledgeDocumentLineVo);
        allBlockIdList.remove(blockId);
//        if (CollectionUtils.isNotEmpty(childItemList)) {
//            logger.error("handleView 方法中childItemList入参未处理,{}", JSONObject.toJSONString(childItemList));
//        }
        return resultList;
    }

    private JSONObject handleImage(JSONObject item) {
        JSONObject configObj = new JSONObject();
        Integer blockType = item.getInteger("block_type");
        FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
        JSONObject jsonObj = item.getJSONObject(feiShuBlockType.getText());
        if (MapUtils.isNotEmpty(jsonObj)) {
            configObj.putAll(jsonObj);
            Integer align = jsonObj.getInteger("align");
            configObj.put("align", FeiShuAlignType.getFeiShuAlignText(align));
            String token = jsonObj.getString("token");
            FileVo fileVo = downloadMedias(token);
            if (fileVo != null) {
                configObj.put("url", "api/binary/file/download?id=" + fileVo.getId());
            }
        }
        return configObj;
    }

    private List<KnowledgeDocumentLineVo> handleImage(JSONObject item, List<JSONObject> childItemList, JSONArray unprocessedItems, List<String> allBlockIdList) {
        List<KnowledgeDocumentLineVo> resultList = new ArrayList<>();
        String blockId = item.getString("block_id");
        Integer blockType = item.getInteger("block_type");
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("image");
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "image");
        configObj.put("blockUuid", blockId);
        configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item));
        FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
        JSONObject jsonObj = item.getJSONObject(feiShuBlockType.getText());
        if (MapUtils.isNotEmpty(jsonObj)) {
            Integer width = jsonObj.getInteger("width");
            configObj.put("width", width);
            Integer height = jsonObj.getInteger("height");
            configObj.put("height", height);
            Integer scale = jsonObj.getInteger("scale");
            Integer align = jsonObj.getInteger("align");
            configObj.put("align", FeiShuAlignType.getFeiShuAlignText(align));
            String token = jsonObj.getString("token");
            FileVo fileVo = downloadMedias(token);
            if (fileVo != null) {
                configObj.put("url", "api/binary/file/download?id=" + fileVo.getId());
            }
        }
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        resultList.add(knowledgeDocumentLineVo);
        allBlockIdList.remove(blockId);
        if (CollectionUtils.isNotEmpty(childItemList)) {
//            logger.error("handleImage 方法中childItemList入参未处理,{}", JSONObject.toJSONString(childItemList));
            List<KnowledgeDocumentLineVo> lines = handleChildItemList(item, childItemList, unprocessedItems, allBlockIdList);
            if (CollectionUtils.isNotEmpty(lines)) {
                resultList.addAll(lines);
            }
        }
        return resultList;
    }

    private List<KnowledgeDocumentLineVo> handleTable(JSONObject item, List<JSONObject> childItemList, JSONArray unprocessedItems, List<String> allBlockIdList) {
        List<KnowledgeDocumentLineVo> resultList = new ArrayList<>();
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
                allBlockIdList.remove(childItem.getString("block_id"));
                JSONArray tableCellChildren = childItem.getJSONArray("children");
                if (CollectionUtils.isNotEmpty(tableCellChildren)) {
                    for (String tableCellChildBlockId : tableCellChildren.toJavaList(String.class)) {
                        JSONObject tableCellChildItem = childItemMap.get(tableCellChildBlockId);
                        if (MapUtils.isNotEmpty(tableCellChildItem)) {
                            allBlockIdList.remove(tableCellChildItem.getString("block_id"));
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
        resultList.add(knowledgeDocumentLineVo);
        allBlockIdList.remove(blockId);
        return resultList;
    }

    private List<KnowledgeDocumentLineVo> handleChildItemList(JSONObject item, List<JSONObject> childItemList, JSONArray unprocessedItems, List<String> allBlockIdList) {
        Map<String, JSONObject> childItemMap = new HashMap<>();
        List<String> childBlockIdList = new ArrayList<>();
        for (JSONObject childItem : childItemList) {
            String childBlockId = childItem.getString("block_id");
            childItemMap.put(childBlockId, childItem);
            if (Objects.equals(item.getString("block_id"), childItem.getString("parent_id"))) {
                childBlockIdList.add(childBlockId);
            }
        }
        return handleItemList(childBlockIdList, childItemMap, unprocessedItems, allBlockIdList);
    }

    private List<KnowledgeDocumentLineVo> getFeiShuDocumentLines(FeiShuNodeVo node, String tenantAccessToken, JSONArray unprocessedItems) {
        List<KnowledgeDocumentLineVo> lineList = new ArrayList<>();
        JSONObject blockResult = FeiShuOpenApiUtil.getDocumentBlocks(node.getObjToken(), tenantAccessToken);
        JSONArray items = blockResult.getJSONObject("data") == null ? null : blockResult.getJSONObject("data").getJSONArray("items");
        if (CollectionUtils.isNotEmpty(items)) {
            JSONObject pageItem = null;
            List<String> allBlockIdList = new ArrayList<>();
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
                    } else {
                        allBlockIdList.add(blockId);
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
                    lineList = handleItemList(children.toJavaList(String.class), itemMap, unprocessedItems, allBlockIdList);
                }
            }
            if (CollectionUtils.isNotEmpty(allBlockIdList)) {
                logger.error("未处理blockIdList = {}", JSONObject.toJSONString(allBlockIdList));
            }
        }
        return lineList;
    }

    private List<KnowledgeDocumentLineVo> handleItemList(List<String> blockIdList, Map<String, JSONObject> itemMap, JSONArray unprocessedItems, List<String> allBlockIdList) {
        List<String> handledBlockIdList = new ArrayList<>();
        List<KnowledgeDocumentLineVo> lineList = new ArrayList<>();
        for (String blockId : blockIdList) {
            JSONObject item = itemMap.get(blockId);
            String parentId = item.getString("parent_id");
            Integer blockType = item.getInteger("block_type");
            FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
            if (handledBlockIdList.contains(blockId)) {
                continue;
            }
            handledBlockIdList.add(blockId);
            if (feiShuBlockType == null) {
                unprocessedItems.add(item);
                List<JSONObject> childItemList = collectChildItemList(item, itemMap);
                handledBlockIdList.addAll(collectChildItemBlockIdList(childItemList));
                unprocessedItems.addAll(childItemList);
                continue;
            }
            item.put("block_type_text", feiShuBlockType.getText());
            item.put("block_type_description", feiShuBlockType.getDescription());
            if (feiShuBlockType == FeiShuBlockType.TEXT) {
                List<JSONObject> childItemList = collectChildItemList(item, itemMap);
                handledBlockIdList.addAll(collectChildItemBlockIdList(childItemList));
                List<KnowledgeDocumentLineVo> lines = handleText(item, childItemList, unprocessedItems, allBlockIdList);
                lineList.addAll(lines);
            } else if (feiShuBlockType == FeiShuBlockType.HEADING1) {
                List<JSONObject> childItemList = collectChildItemList(item, itemMap);
                handledBlockIdList.addAll(collectChildItemBlockIdList(childItemList));
                List<KnowledgeDocumentLineVo> lines = handleHeading(item, childItemList, unprocessedItems, allBlockIdList);
                lineList.addAll(lines);
            } else if (feiShuBlockType == FeiShuBlockType.HEADING2) {
                List<JSONObject> childItemList = collectChildItemList(item, itemMap);
                handledBlockIdList.addAll(collectChildItemBlockIdList(childItemList));
                List<KnowledgeDocumentLineVo> lines = handleHeading(item, childItemList, unprocessedItems, allBlockIdList);
                lineList.addAll(lines);
            } else if (feiShuBlockType == FeiShuBlockType.HEADING3) {
                List<JSONObject> childItemList = collectChildItemList(item, itemMap);
                handledBlockIdList.addAll(collectChildItemBlockIdList(childItemList));
                List<KnowledgeDocumentLineVo> lines = handleHeading(item, childItemList, unprocessedItems, allBlockIdList);
                lineList.addAll(lines);
            } else if (feiShuBlockType == FeiShuBlockType.HEADING4) {
                List<JSONObject> childItemList = collectChildItemList(item, itemMap);
                handledBlockIdList.addAll(collectChildItemBlockIdList(childItemList));
                List<KnowledgeDocumentLineVo> lines = handleHeading(item, childItemList, unprocessedItems, allBlockIdList);
                lineList.addAll(lines);
            } else if (feiShuBlockType == FeiShuBlockType.HEADING5) {
                List<JSONObject> childItemList = collectChildItemList(item, itemMap);
                handledBlockIdList.addAll(collectChildItemBlockIdList(childItemList));
                List<KnowledgeDocumentLineVo> lines = handleHeading(item, childItemList, unprocessedItems, allBlockIdList);
                lineList.addAll(lines);
            } else if (feiShuBlockType == FeiShuBlockType.HEADING6) {
                List<JSONObject> childItemList = collectChildItemList(item, itemMap);
                handledBlockIdList.addAll(collectChildItemBlockIdList(childItemList));
                List<KnowledgeDocumentLineVo> lines = handleHeading(item, childItemList, unprocessedItems, allBlockIdList);
                lineList.addAll(lines);
            } else if (feiShuBlockType == FeiShuBlockType.HEADING7) {
                List<JSONObject> childItemList = collectChildItemList(item, itemMap);
                handledBlockIdList.addAll(collectChildItemBlockIdList(childItemList));
                List<KnowledgeDocumentLineVo> lines = handleHeading(item, childItemList, unprocessedItems, allBlockIdList);
                lineList.addAll(lines);
            } else if (feiShuBlockType == FeiShuBlockType.HEADING8) {
                List<JSONObject> childItemList = collectChildItemList(item, itemMap);
                handledBlockIdList.addAll(collectChildItemBlockIdList(childItemList));
                List<KnowledgeDocumentLineVo> lines = handleHeading(item, childItemList, unprocessedItems, allBlockIdList);
                lineList.addAll(lines);
            } else if (feiShuBlockType == FeiShuBlockType.HEADING9) {
                List<JSONObject> childItemList = collectChildItemList(item, itemMap);
                handledBlockIdList.addAll(collectChildItemBlockIdList(childItemList));
                List<KnowledgeDocumentLineVo> lines = handleHeading(item, childItemList, unprocessedItems, allBlockIdList);
                lineList.addAll(lines);
            } else if (feiShuBlockType == FeiShuBlockType.BULLET) {
                List<JSONObject> allChildItemList = new ArrayList<>();
                List<JSONObject> bulletList = new ArrayList<>();
                boolean isStart = false;
                for (Map.Entry<String, JSONObject> entry : itemMap.entrySet()) {
                    JSONObject value = entry.getValue();
                    String key = entry.getKey();
                    if (!isStart && Objects.equals(key, blockId)) {
                        bulletList.add(value);
                        isStart = true;
                        List<JSONObject> childItemList = collectChildItemList(value, itemMap);
                        handledBlockIdList.addAll(collectChildItemBlockIdList(childItemList));
                        allChildItemList.addAll(childItemList);
                    } else if (isStart) {
                        if (Objects.equals(value.getInteger("block_type"), feiShuBlockType.getValue())
                                && Objects.equals(value.getString("parent_id"), parentId)) {
                            bulletList.add(value);
                            handledBlockIdList.add(value.getString("block_id"));
                            List<JSONObject> childItemList = collectChildItemList(value, itemMap);
                            handledBlockIdList.addAll(collectChildItemBlockIdList(childItemList));
                            allChildItemList.addAll(childItemList);
                        } else {
                            break;
                        }
                    }
                }
                if (CollectionUtils.isNotEmpty(bulletList)) {
                    List<KnowledgeDocumentLineVo> lines = handleBulletList(bulletList, allChildItemList, unprocessedItems, allBlockIdList);
                    lineList.addAll(lines);
                }
            } else if (feiShuBlockType == FeiShuBlockType.ORDERED) {
                List<JSONObject> allChildItemList = new ArrayList<>();
                List<JSONObject> orderedList = new ArrayList<>();
                boolean isStart = false;
                for (Map.Entry<String, JSONObject> entry : itemMap.entrySet()) {
                    JSONObject value = entry.getValue();
                    String key = entry.getKey();
                    if (!isStart && Objects.equals(key, blockId)) {
                        orderedList.add(value);
                        isStart = true;
                        List<JSONObject> childItemList = collectChildItemList(value, itemMap);
                        handledBlockIdList.addAll(collectChildItemBlockIdList(childItemList));
                        allChildItemList.addAll(childItemList);
                    } else if (isStart) {
                        if (Objects.equals(value.getInteger("block_type"), feiShuBlockType.getValue())
                                && Objects.equals(value.getString("parent_id"), parentId)) {
                            orderedList.add(value);
                            handledBlockIdList.add(value.getString("block_id"));
                            List<JSONObject> childItemList = collectChildItemList(value, itemMap);
                            handledBlockIdList.addAll(collectChildItemBlockIdList(childItemList));
                            allChildItemList.addAll(childItemList);
                        } else {
                            break;
                        }
                    }
                }
                if (CollectionUtils.isNotEmpty(orderedList)) {
                    List<KnowledgeDocumentLineVo> lines = handleOrderedList(orderedList, allChildItemList, unprocessedItems, allBlockIdList);
                    lineList.addAll(lines);
                }
            } else if (feiShuBlockType == FeiShuBlockType.CODE) {
                List<JSONObject> childItemList = collectChildItemList(item, itemMap);
                handledBlockIdList.addAll(collectChildItemBlockIdList(childItemList));
                List<KnowledgeDocumentLineVo> lines = handleCode(item, childItemList, unprocessedItems, allBlockIdList);
                lineList.addAll(lines);
            } else if (feiShuBlockType == FeiShuBlockType.QUOTE) {
                List<JSONObject> childItemList = collectChildItemList(item, itemMap);
                handledBlockIdList.addAll(collectChildItemBlockIdList(childItemList));
                List<KnowledgeDocumentLineVo> lines = handleQuote(item, childItemList, unprocessedItems, allBlockIdList);
                lineList.addAll(lines);
            } else if (feiShuBlockType == FeiShuBlockType.QUOTE_CONTAINER) {
                List<JSONObject> childItemList = collectChildItemList(item, itemMap);
                handledBlockIdList.addAll(collectChildItemBlockIdList(childItemList));
                List<KnowledgeDocumentLineVo> lines = handleQuoteContainer(item, childItemList, unprocessedItems, allBlockIdList);
                lineList.addAll(lines);
            } else if (feiShuBlockType == FeiShuBlockType.TODO) {
                List<JSONObject> allChildItemList = new ArrayList<>();
                List<JSONObject> todoList = new ArrayList<>();
                boolean isStart = false;
                for (Map.Entry<String, JSONObject> entry : itemMap.entrySet()) {
                    JSONObject value = entry.getValue();
                    String key = entry.getKey();
                    if (!isStart && Objects.equals(key, blockId)) {
                        todoList.add(value);
                        isStart = true;
                        List<JSONObject> childItemList = collectChildItemList(value, itemMap);
                        handledBlockIdList.addAll(collectChildItemBlockIdList(childItemList));
                        allChildItemList.addAll(childItemList);
                    } else if (isStart) {
                        if (Objects.equals(value.getInteger("block_type"), feiShuBlockType.getValue())
                                && Objects.equals(value.getString("parent_id"), parentId)) {
                            todoList.add(value);
                            handledBlockIdList.add(value.getString("block_id"));
                            List<JSONObject> childItemList = collectChildItemList(value, itemMap);
                            handledBlockIdList.addAll(collectChildItemBlockIdList(childItemList));
                            allChildItemList.addAll(childItemList);
                        } else {
                            break;
                        }
                    }
                }
                if (CollectionUtils.isNotEmpty(todoList)) {
                    List<KnowledgeDocumentLineVo> lines = handleTodoList(todoList, allChildItemList, unprocessedItems, allBlockIdList);
                    lineList.addAll(lines);
                }
            } else if (feiShuBlockType == FeiShuBlockType.CALLOUT) {
                List<JSONObject> childItemList = collectChildItemList(item, itemMap);
                handledBlockIdList.addAll(collectChildItemBlockIdList(childItemList));
                List<KnowledgeDocumentLineVo> lines = handleCallOut(item, childItemList, unprocessedItems, allBlockIdList);
                lineList.addAll(lines);
            } else if (feiShuBlockType == FeiShuBlockType.DIVIDER) {
                KnowledgeDocumentLineVo line = new KnowledgeDocumentLineVo();
                JSONObject configObj = new JSONObject();
                configObj.put("blockType", "divider");
                configObj.put("blockUuid", blockId);
                line.setConfig(configObj.toJSONString());
                line.setHandler("divider");
                lineList.add(line);
            } else if (feiShuBlockType == FeiShuBlockType.VIEW) {
                List<JSONObject> childItemList = collectChildItemList(item, itemMap);
                handledBlockIdList.addAll(collectChildItemBlockIdList(childItemList));
                List<KnowledgeDocumentLineVo> lines = handleView(item, childItemList, unprocessedItems, allBlockIdList);
                lineList.addAll(lines);
            } else if (feiShuBlockType == FeiShuBlockType.IMAGE) {
                List<JSONObject> childItemList = collectChildItemList(item, itemMap);
                handledBlockIdList.addAll(collectChildItemBlockIdList(childItemList));
                List<KnowledgeDocumentLineVo> lines = handleImage(item, childItemList, unprocessedItems, allBlockIdList);
                lineList.addAll(lines);
            } else if (feiShuBlockType == FeiShuBlockType.TABLE) {
                List<JSONObject> childItemList = collectChildItemList(item, itemMap);
                handledBlockIdList.addAll(collectChildItemBlockIdList(childItemList));
                List<KnowledgeDocumentLineVo> lines = handleTable(item, childItemList, unprocessedItems, allBlockIdList);
                lineList.addAll(lines);
            } else {
                unprocessedItems.add(item);
                List<JSONObject> childItemList = collectChildItemList(item, itemMap);
                handledBlockIdList.addAll(collectChildItemBlockIdList(childItemList));
                unprocessedItems.addAll(childItemList);
            }
        }
        return lineList;
    }

    private List<JSONObject> collectChildItemList(JSONObject item, Map<String, JSONObject> itemMap) {
        List<JSONObject> resultList = new ArrayList<>();
        JSONArray children = item.getJSONArray("children");
        if (CollectionUtils.isNotEmpty(children)) {
            for (int i = 0; i < children.size(); i++) {
                String childBlockId = children.getString(i);
                JSONObject childItem = itemMap.get(childBlockId);
                if (MapUtils.isNotEmpty(childItem)) {
                    resultList.add(childItem);
                    List<JSONObject> childItemList = collectChildItemList(childItem, itemMap);
                    resultList.addAll(childItemList);
                }
            }
        }
        return resultList;
    }

    private List<String> collectChildItemBlockIdList(List<JSONObject> childItemList) {
        List<String> blockIdList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(childItemList)) {
            for (JSONObject childItem : childItemList) {
                blockIdList.add(childItem.getString("block_id"));
            }
        }
        return blockIdList;
    }

    private void saveLines(Long documentId, Long versionId, List<KnowledgeDocumentLineVo> lineList) {
        int size = 0;
        int lineNumber = 0;
        List<KnowledgeDocumentLineVo> knowledgeDocumentLineListTmp = new ArrayList<>(100);
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
            knowledgeDocumentLineListTmp.add(lineVo);
            if (knowledgeDocumentLineListTmp.size() >= 100) {
                knowledgeDocumentMapper.insertKnowledgeDocumentLineList(knowledgeDocumentLineListTmp);
                knowledgeDocumentLineListTmp.clear();
            }
        }
        if (CollectionUtils.isNotEmpty(knowledgeDocumentLineListTmp)) {
            knowledgeDocumentMapper.insertKnowledgeDocumentLineList(knowledgeDocumentLineListTmp);
            knowledgeDocumentLineListTmp.clear();
        }
        KnowledgeDocumentVersionVo updateVo = new KnowledgeDocumentVersionVo();
        updateVo.setId(versionId);
        updateVo.setSize(size);
        knowledgeDocumentMapper.updateKnowledgeDocumentVersionById(updateVo);
    }

    @Override
    public KnowledgeDocumentTypeVo getOrCreateKnowledgeType(String name, String parentUuid, Long knowledgeCircleId) {
        KnowledgeDocumentTypeVo exists = null;
        List<KnowledgeDocumentTypeVo> children = knowledgeDocumentTypeMapper.getTypeByParentUuid(parentUuid, knowledgeCircleId);
        if (CollectionUtils.isNotEmpty(children)) {
            for (KnowledgeDocumentTypeVo child : children) {
                if (Objects.equals(child.getName(), name)) {
                    exists = child;
                    break;
                }
            }
        }
        if (exists != null) {
            return exists;
        } else {
            List<KnowledgeDocumentTypeVo> typeListByParentUuid = knowledgeDocumentTypeMapper.getTypeByParentUuid(parentUuid, knowledgeCircleId);
            List<KnowledgeDocumentTypeVo> list = new ArrayList<>();
            KnowledgeDocumentTypeVo knowledgeDocumentTypeVo = new KnowledgeDocumentTypeVo();
            knowledgeDocumentTypeVo.setUuid(UuidUtil.randomUuid());
            knowledgeDocumentTypeVo.setParentUuid(parentUuid);
            knowledgeDocumentTypeVo.setName(name);
            knowledgeDocumentTypeVo.setKnowledgeCircleId(knowledgeCircleId);
            /** sort的用处在于重建左右编码 */
            knowledgeDocumentTypeVo.setSort(typeListByParentUuid.size());
            list.add(knowledgeDocumentTypeVo);
            knowledgeDocumentTypeMapper.batchInsertType(list);
            return knowledgeDocumentTypeVo;
        }
    }

    @Override
    public List<FeiShuNodeVo> loadWikiNodes(Long spaceId, String parentNodeToken, List<String> path, String tenantAccessToken) {
        List<FeiShuNodeVo> nodeList = new ArrayList<>();
        JSONObject result = FeiShuOpenApiUtil.getFeiShuWikiNodes(spaceId, parentNodeToken, tenantAccessToken);
        JSONObject data = result.getJSONObject("data");
        if (data == null) {
            return nodeList;
        }
        JSONArray items = data.getJSONArray("items");
        if (CollectionUtils.isNotEmpty(items)) {
            for (int i = 0; i < items.size(); i++) {
                JSONObject item = items.getJSONObject(i);
                FeiShuNodeVo node = new FeiShuNodeVo(item);
                node.getPath().addAll(path);
                if (!isDocumentNode(node)) {
                    node.getPath().add(node.getTitle());
                }
                nodeList.add(node);
                if (Objects.equals(item.getBoolean("has_child"), true)) {
                    List<FeiShuNodeVo> children = loadWikiNodes(spaceId, node.getNodeToken(), node.getPath(), tenantAccessToken);
                    node.setChildren(children);
                }
            }
        }
        return nodeList;
    }

    private boolean isDocumentNode(FeiShuNodeVo node) {
        return "docx".equals(node.getObjType()) || "doc".equals(node.getObjType());
    }
}
