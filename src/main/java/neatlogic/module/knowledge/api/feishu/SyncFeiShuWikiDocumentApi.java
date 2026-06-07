/*
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

package neatlogic.module.knowledge.api.feishu;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.file.dao.mapper.FileMapper;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.fulltextindex.core.FullTextIndexHandlerFactory;
import neatlogic.framework.fulltextindex.core.IFullTextIndexHandler;
import neatlogic.framework.knowledge.constvalue.FeiShuAlignType;
import neatlogic.framework.knowledge.constvalue.FeiShuBlockType;
import neatlogic.framework.knowledge.constvalue.KnowledgeFullTextIndexType;
import neatlogic.framework.knowledge.constvalue.Status;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import neatlogic.framework.knowledge.dto.*;
import neatlogic.framework.knowledge.dto.feishu.*;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.UuidUtil;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_FEISHU_SYNC_MODIFY;
import neatlogic.module.knowledge.dao.mapper.KnowledgeFeishuSyncMapper;
import neatlogic.module.knowledge.service.KnowledgeDocumentTypeService;
import neatlogic.module.knowledge.service.KnowledgeFeishuSyncService;
import neatlogic.module.knowledge.source.FeishuSyncSource;
import neatlogic.module.knowledge.utils.FeiShuOpenApiUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@AuthAction(action = KNOWLEDGE_FEISHU_SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SyncFeiShuWikiDocumentApi extends PrivateApiComponentBase {

    private final Logger logger = LoggerFactory.getLogger(SyncFeiShuWikiDocumentApi.class);

    @Resource
    private KnowledgeFeishuSyncService knowledgeFeishuSyncService;
    @Resource
    private KnowledgeFeishuSyncMapper knowledgeFeishuSyncMapper;
    @Resource
    private FileMapper fileMapper;
    //    @Resource
//    private KnowledgeCircleMapper knowledgeCircleMapper;
    @Resource
    private KnowledgeDocumentTypeMapper knowledgeDocumentTypeMapper;
    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Resource
    private KnowledgeDocumentTypeService knowledgeDocumentTypeService;

    @Override
    public String getToken() {
        return "knowledge/feishu/wiki/document/sync";
    }

    @Override
    public String getName() {
        return "同步飞书Wiki文档";
    }

    @Input({
            @Param(name = "spaceIdList", type = ApiParamType.JSONARRAY, desc = "空间ID列表"),
            @Param(name = "nodeTokenList", type = ApiParamType.JSONARRAY, desc = "节点token列表"),

    })
    @Description(desc = "同步飞书Wiki文档")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        FeiShuAppCredentialsVo feiShuAppCredentials = knowledgeFeishuSyncService.getFeiShuAppCredentials();
        if (feiShuAppCredentials == null) {
            return null;
        }
        String tenantAccessToken = FeiShuOpenApiUtil.getTenantAccessToken(feiShuAppCredentials.getAppId(), feiShuAppCredentials.getAppSecret());
        Map<String, FeiShuNodeVo> feiShuNodeMap = new HashMap<>();
        Map<Long, FeiShuSpaceVo> feiShuSpaceMap = new HashMap<>();
        List<String> allNodeTokenList = new ArrayList<>();
        JSONArray spaceIdArray = paramObj.getJSONArray("spaceIdList");
        if (CollectionUtils.isNotEmpty(spaceIdArray)) {
            List<Long> spaceIdList = spaceIdArray.toJavaList(Long.class);
            for (Long spaceId : spaceIdList) {
                FeiShuSpaceVo feiShuSpaceVo = getFeiShuSpaceBySpaceId(spaceId, tenantAccessToken, feiShuSpaceMap);
                if (feiShuSpaceVo != null) {
                    KnowledgeDocumentTypeVo knowledgeType = getOrCreateKnowledgeType(feiShuSpaceVo.getName(), "0", feiShuAppCredentials.getKnowledgeCircleId());
                    List<FeiShuNodeVo> nodes = loadWikiNodes(spaceId, null, new ArrayList<>(), tenantAccessToken);
                    saveNodes(spaceId, feiShuSpaceVo.getName(), nodes, feiShuAppCredentials, knowledgeType, tenantAccessToken);
                }
            }
        }
        JSONArray nodeTokenArray = paramObj.getJSONArray("nodeTokenList");
        if (CollectionUtils.isNotEmpty(nodeTokenArray)) {
            List<String> nodeTokenList = nodeTokenArray.toJavaList(String.class);
            List<String> list = ListUtils.removeAll(nodeTokenList, allNodeTokenList);
            if (CollectionUtils.isNotEmpty(list)) {
                for (String nodeToken : list) {
                    FeiShuNodeVo feiShuNodeVo = getFeiShuNodeByNodeToken(nodeToken, tenantAccessToken, feiShuNodeMap);
                    System.out.println("feiShuNodeVo = " + JSONObject.toJSONString(feiShuNodeVo));
                    if (feiShuNodeVo != null) {
                        FeiShuSpaceVo feiShuSpaceVo = getFeiShuSpaceBySpaceId(feiShuNodeVo.getSpaceId(), tenantAccessToken, feiShuSpaceMap);
                        if (feiShuSpaceVo != null) {
                            KnowledgeDocumentTypeVo knowledgeType = getOrCreateKnowledgeType(feiShuSpaceVo.getName(), "0", feiShuAppCredentials.getKnowledgeCircleId());
                            List<FeiShuNodeVo> parentList = new ArrayList<>();
                            FeiShuNodeVo parent = feiShuNodeVo.getParent();
                            while (parent != null) {
                                parentList.add(parent);
                                parent = parent.getParent();
                            }
                            if (CollectionUtils.isNotEmpty(parentList)) {
                                String parentUuid = knowledgeType.getUuid();
                                for (int i = parentList.size() - 1; i >= 0; i--) {
                                    FeiShuNodeVo parentVo = parentList.get(i);
                                    knowledgeType = getOrCreateKnowledgeType(parentVo.getTitle(), parentUuid, feiShuAppCredentials.getKnowledgeCircleId());
                                    parentUuid = knowledgeType.getUuid();
                                }
                            }
                            saveFeishuDocument(feiShuAppCredentials, feiShuNodeVo, knowledgeType.getUuid(), tenantAccessToken);
                        }
                    }
                }
            }
        }
        knowledgeDocumentTypeService.rebuildLeftRightCode(feiShuAppCredentials.getKnowledgeCircleId());
        return null;
    }

    private FeiShuSpaceVo getFeiShuSpaceBySpaceId(Long spaceId, String tenantAccessToken, Map<Long, FeiShuSpaceVo> feiShuSpaceMap) {
        FeiShuSpaceVo feiShuSpaceVo = feiShuSpaceMap.get(spaceId);
        if (feiShuSpaceVo == null) {
            JSONObject feishuSpaceInfo = FeiShuOpenApiUtil.getFeishuSpaceInfo(spaceId, tenantAccessToken);
            JSONObject data = feishuSpaceInfo.getJSONObject("data");
            if (MapUtils.isNotEmpty(data)) {
                JSONObject space = data.getJSONObject("space");
                if (MapUtils.isNotEmpty(space)) {
                    feiShuSpaceVo = new FeiShuSpaceVo(space);
                    feiShuSpaceMap.put(feiShuSpaceVo.getSpaceId(), feiShuSpaceVo);
                }
            }
        }
        return feiShuSpaceVo;
    }

    private FeiShuNodeVo getFeiShuNodeByNodeToken(String nodeToken, String tenantAccessToken, Map<String, FeiShuNodeVo> feiShuNodeMap) {
        FeiShuNodeVo feiShuNodeVo = feiShuNodeMap.get(nodeToken);
        if (feiShuNodeVo == null) {
            JSONObject feishuNodeInfo = FeiShuOpenApiUtil.getFeishuNodeInfo(nodeToken, tenantAccessToken);
            System.out.println("feishuNodeInfo = " + feishuNodeInfo);
            JSONObject data = feishuNodeInfo.getJSONObject("data");
            if (MapUtils.isNotEmpty(data)) {
                JSONObject node = data.getJSONObject("node");
                if (MapUtils.isNotEmpty(node)) {
                    feiShuNodeVo = new FeiShuNodeVo(node);
                    feiShuNodeMap.put(feiShuNodeVo.getNodeToken(), feiShuNodeVo);
                }
            }
        }
        if (feiShuNodeVo != null) {
            if (StringUtils.isNotBlank(feiShuNodeVo.getParentNodeToken())) {
                FeiShuNodeVo parent = getFeiShuNodeByNodeToken(feiShuNodeVo.getParentNodeToken(), tenantAccessToken, feiShuNodeMap);
                feiShuNodeVo.setParent(parent);
            }
        }
        return feiShuNodeVo;
    }

    private void saveNodes(Long spaceId, String spaceName, List<FeiShuNodeVo> nodes, FeiShuAppCredentialsVo config, KnowledgeDocumentTypeVo knowledgeType, String tenantAccessToken) {
        for (FeiShuNodeVo node : nodes) {
            System.out.println("node = " + JSONArray.toJSON(node));
            if (!isDocumentNode(node)) {
                continue;
            }
//            if (!Objects.equals(node.getNodeToken(), "EnH5wTCBMiZibmkDu6lcmI94n8g")) {
//                continue;
//            }
//            JSONObject item = new JSONObject();
//            Long documentId = null;
//            String status = null;
            try {
                saveFeishuDocument(config, node, knowledgeType.getUuid(), tenantAccessToken);
//                documentId = resultObj.getLong("knowledgeDocumentId");
//                status = "succeed";
//                JSONArray unprocessedItems = resultObj.getJSONArray("unprocessedItems");
//                if (CollectionUtils.isNotEmpty(unprocessedItems)) {
//                    item.put("unprocessedItems", unprocessedItems);
//                }
                if (CollectionUtils.isNotEmpty(node.getChildren())) {
                    KnowledgeDocumentTypeVo childType = getOrCreateKnowledgeType(node.getTitle(), knowledgeType.getUuid(), config.getKnowledgeCircleId());
                    saveNodes(spaceId, spaceName, node.getChildren(), config, childType, tenantAccessToken);
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
//                status = "failed";
//                item.put("error", ex.getMessage());
            }
//            if (MapUtils.isNotEmpty(item)) {
//                item.put("spaceId", spaceId);
//                item.put("spaceName", spaceName);
//                item.put("title", node.getTitle());
//                item.put("nodeToken", node.getNodeToken());
//                item.put("knowledgeDocumentId", documentId);
//                item.put("status", status);
//            }
        }
    }

    private void saveFeishuDocument(FeiShuAppCredentialsVo appCredentialsVo, FeiShuNodeVo node, String typeUuid, String tenantAccessToken) {
        String status = null;
        JSONObject config = new JSONObject();
        KnowledgeDocumentVo documentVo = new KnowledgeDocumentVo();
        try {
            KnowledgeFeishuSyncDocumentVo mapping = knowledgeFeishuSyncMapper.getSyncDocumentByNodeToken(node.getNodeToken());
            if (mapping == null) {
                documentVo.setTitle(node.getTitle());
                documentVo.setKnowledgeCircleId(appCredentialsVo.getKnowledgeCircleId());
                documentVo.setKnowledgeDocumentTypeUuid(typeUuid);
                documentVo.setVersion(0);
                documentVo.setFcu(UserContext.get().getUserUuid(true));
                documentVo.setSource(FeishuSyncSource.SOURCE);
                knowledgeDocumentMapper.insertKnowledgeDocument(documentVo);
                knowledgeDocumentMapper.insertKnowledgeDocumentViewCount(documentVo.getId(), 0);
            } else {
                documentVo = knowledgeDocumentMapper.getKnowledgeDocumentLockById(mapping.getKnowledgeDocumentId());
                documentVo.setTitle(node.getTitle());
                documentVo.setKnowledgeDocumentTypeUuid(typeUuid);
                knowledgeDocumentMapper.updateKnowledgeDocumentTitleById(documentVo);
                knowledgeDocumentMapper.updateKnowledgeDocumentTypeUuidById(documentVo);
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

            JSONArray unprocessedItems = new JSONArray();
            List<KnowledgeDocumentLineVo> feishuDocumentLines = getFeishuDocumentLines(node, tenantAccessToken, unprocessedItems);
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
        if (handler != null) {
            handler.createIndex(documentVo.getKnowledgeDocumentVersionId());
        }
//        return new JSONObject().fluentPut("knowledgeDocumentId", documentVo.getId()).fluentPut("unprocessedItems", unprocessedItems);
    }

    private void upsertMapping(FeiShuAppCredentialsVo appCredentialsVo, FeiShuNodeVo node, KnowledgeDocumentVo documentVo, String status, JSONObject config) {
        KnowledgeFeishuSyncDocumentVo vo = new neatlogic.framework.knowledge.dto.feishu.KnowledgeFeishuSyncDocumentVo();
        vo.setAppId(appCredentialsVo.getAppId());
        vo.setTitle(node.getTitle());
        vo.setNodeToken(node.getNodeToken());
        vo.setObjToken(node.getObjToken());
        vo.setObjType(node.getObjType());
        vo.setUpdateTime(node.getUpdateTime());
        vo.setKnowledgeDocumentId(documentVo.getId());
        vo.setKnowledgeDocumentVersionId(documentVo.getKnowledgeDocumentVersionId());
        vo.setKnowledgeDocumentTypeUuid(documentVo.getKnowledgeDocumentTypeUuid());
        vo.setStatus(status);
        if (MapUtils.isNotEmpty(config)) {
            vo.setConfig(config);
        }
        vo.setLcu(UserContext.get().getUserUuid());
        if (knowledgeFeishuSyncMapper.getSyncDocumentByNodeToken(node.getNodeToken()) == null) {
            knowledgeFeishuSyncMapper.insertSyncDocument(vo);
        } else {
            knowledgeFeishuSyncMapper.updateSyncDocument(vo);
        }
    }


    private FileVo downloadMedias(String fileToken, String tenantAccessToken) {
        Long fileId = knowledgeFeishuSyncMapper.getSyncMediasMappingFileIdByUuid(fileToken);
        System.out.println("getSyncMediasMappingFileIdByUuid fileId = " + fileId);
        if (fileId != null) {
            FileVo fileVo = fileMapper.getFileById(fileId);
            if (fileVo != null) {
                return fileVo;
            }
        }
        FileVo fileVo = FeiShuOpenApiUtil.downloadMedias(fileToken, tenantAccessToken);
        fileMapper.insertFile(fileVo);
        knowledgeFeishuSyncMapper.insertSyncMediasMapping(fileToken, fileVo.getId());
        System.out.println("insertSyncMediasMapping fileId = " + fileVo.getId());
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
        return knowledgeDocumentLineVo;
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

    private KnowledgeDocumentLineVo handleQuote(JSONObject item) {
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

    private KnowledgeDocumentLineVo handleView(JSONObject item, List<JSONObject> childItemList, String tenantAccessToken) {
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
            Integer blockType = childItem.getInteger("block_type");
            FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
            JSONObject jsonObj = childItem.getJSONObject(feiShuBlockType.getText());
            if (MapUtils.isNotEmpty(jsonObj)) {
                String name = jsonObj.getString("name");
                String token = jsonObj.getString("token");
                FileVo fileVo = downloadMedias(token, tenantAccessToken);
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
        return knowledgeDocumentLineVo;
    }

    private KnowledgeDocumentLineVo handleImage(JSONObject item, String tenantAccessToken) {
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
            FileVo fileVo = downloadMedias(token, tenantAccessToken);
            if (fileVo != null) {
                configObj.put("url", "api/binary/file/download?id=" + fileVo.getId());
            }
        }
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        return knowledgeDocumentLineVo;
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

    private List<KnowledgeDocumentLineVo> getFeishuDocumentLines(FeiShuNodeVo node, String tenantAccessToken, JSONArray unprocessedItems) {
        List<KnowledgeDocumentLineVo> lineList = new ArrayList<>();
        try {
            System.out.println("node.title = " + node.getTitle());
            JSONObject blockResult = FeiShuOpenApiUtil.getDocumentBlocks(node.getObjToken(), tenantAccessToken);
//            System.out.println("blockResult = " + blockResult);
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
                            FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
                            if (handledBlockIdList.contains(blockId)) {
                                continue;
                            }
                            handledBlockIdList.add(blockId);
                            if (feiShuBlockType == null) {
                                unprocessedItems.add(item);
                                List<JSONObject> childItemList = collectChildItemList(item, itemMap);
                                if (CollectionUtils.isNotEmpty(childItemList)) {
                                    for (JSONObject childItem : childItemList) {
                                        handledBlockIdList.add(childItem.getString("block_id"));
//                                        FeiShuBlockType childBlockType = FeiShuBlockType.getFeiShuBlockType(childItem.getInteger("block_type"));
//                                        if (childBlockType != null) {
//                                            childItem.put("block_type_text", childBlockType.getText());
//                                            childItem.put("block_type_description", childBlockType.getDescription());
//                                        }
                                    }
                                }
                                unprocessedItems.addAll(childItemList);
                                continue;
                            }
                            item.put("block_type_text", feiShuBlockType.getText());
                            item.put("block_type_description", feiShuBlockType.getDescription());
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
                            } else if (feiShuBlockType == FeiShuBlockType.QUOTE) {
                                KnowledgeDocumentLineVo line = handleQuote(item);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.QUOTE_CONTAINER) {
                                List<JSONObject> childItemList = collectChildItemList(item, itemMap);
                                if (CollectionUtils.isNotEmpty(childItemList)) {
                                    for (JSONObject childItem : childItemList) {
                                        handledBlockIdList.add(childItem.getString("block_id"));
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
                                List<JSONObject> childItemList = collectChildItemList(item, itemMap);
                                if (CollectionUtils.isNotEmpty(childItemList)) {
                                    for (JSONObject childItem : childItemList) {
                                        handledBlockIdList.add(childItem.getString("block_id"));
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
//                            } else if (feiShuBlockType == FeiShuBlockType.IFRAME) {
//                                KnowledgeDocumentLineVo line = handleIframe(item);
//                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.VIEW) {
                                List<JSONObject> childItemList = collectChildItemList(item, itemMap);
                                if (CollectionUtils.isNotEmpty(childItemList)) {
                                    for (JSONObject childItem : childItemList) {
                                        handledBlockIdList.add(childItem.getString("block_id"));
                                    }
                                }
                                KnowledgeDocumentLineVo line = handleView(item, childItemList, tenantAccessToken);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.IMAGE) {
                                KnowledgeDocumentLineVo line = handleImage(item, tenantAccessToken);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.TABLE) {
                                List<JSONObject> childItemList = collectChildItemList(item, itemMap);
                                if (CollectionUtils.isNotEmpty(childItemList)) {
                                    for (JSONObject childItem : childItemList) {
                                        handledBlockIdList.add(childItem.getString("block_id"));
                                    }
                                }
                                KnowledgeDocumentLineVo line = handleTable(item, childItemList);
                                lineList.add(line);
                            } else {
                                unprocessedItems.add(item);
                                List<JSONObject> childItemList = collectChildItemList(item, itemMap);
                                if (CollectionUtils.isNotEmpty(childItemList)) {
                                    for (JSONObject childItem : childItemList) {
                                        handledBlockIdList.add(childItem.getString("block_id"));
//                                        FeiShuBlockType childBlockType = FeiShuBlockType.getFeiShuBlockType(childItem.getInteger("block_type"));
//                                        if (childBlockType != null) {
//                                            childItem.put("block_type_text", childBlockType.getText());
//                                            childItem.put("block_type_description", childBlockType.getDescription());
//                                        }
                                    }
                                }
                                unprocessedItems.addAll(childItemList);
//                                KnowledgeDocumentLineVo line = new KnowledgeDocumentLineVo();
//                                JSONObject configObj = new JSONObject();
//                                configObj.put("blockType", feiShuBlockType.getText());
//                                configObj.put("blockUuid", blockId);
//                                configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item).fluentAddAll(childItemList));
//                                line.setConfig(configObj.toJSONString());
//                                line.setHandler("paragraph");
//                                line.setContent(item.toJSONString());
//                                lineList.add(line);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
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


    private KnowledgeDocumentTypeVo getOrCreateKnowledgeType(String name, String parentUuid, Long knowledgeCircleId) {
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

    private List<FeiShuNodeVo> loadWikiNodes(Long spaceId, String parentNodeToken, List<String> path, String tenantAccessToken) {
        List<FeiShuNodeVo> nodeList = new ArrayList<>();
        JSONObject result = FeiShuOpenApiUtil.getFeishuWikiNodes(spaceId, parentNodeToken, tenantAccessToken);
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
