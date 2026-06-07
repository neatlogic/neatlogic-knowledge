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
import neatlogic.framework.asynchronization.threadpool.CachedThreadPool;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.file.dao.mapper.FileMapper;
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
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_FEISHU_SYNC_MODIFY;
import neatlogic.module.knowledge.dao.mapper.KnowledgeFeiShuMapper;
import neatlogic.module.knowledge.service.KnowledgeDocumentTypeService;
import neatlogic.module.knowledge.service.KnowledgeFeiShuService;
import neatlogic.module.knowledge.thread.FeiShuWikiThread;
import neatlogic.module.knowledge.utils.FeiShuOpenApiUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = KNOWLEDGE_FEISHU_SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SyncFeiShuWikiDocumentApi extends PrivateApiComponentBase {

    private final Logger logger = LoggerFactory.getLogger(SyncFeiShuWikiDocumentApi.class);

    @Resource
    private KnowledgeFeiShuService knowledgeFeiShuService;
    @Resource
    private KnowledgeFeiShuMapper knowledgeFeiShuMapper;
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
        FeiShuAppCredentialsVo feiShuAppCredentials = knowledgeFeiShuService.getFeiShuAppCredentials();
        if (feiShuAppCredentials == null) {
            return null;
        }
        String tenantAccessToken = FeiShuOpenApiUtil.getTenantAccessToken(feiShuAppCredentials.getAppId(), feiShuAppCredentials.getAppSecret());
        List<Long> spaceIdList = new ArrayList<>();
        List<String> nodeTokenList = new ArrayList<>();
        JSONArray spaceIdArray = paramObj.getJSONArray("spaceIdList");
        if (CollectionUtils.isNotEmpty(spaceIdArray)) {
            spaceIdList = spaceIdArray.toJavaList(Long.class);
        }
        JSONArray nodeTokenArray = paramObj.getJSONArray("nodeTokenList");
        if (CollectionUtils.isNotEmpty(nodeTokenArray)) {
            nodeTokenList = nodeTokenArray.toJavaList(String.class);
        }
//        Map<String, FeiShuNodeVo> feiShuNodeMap = new HashMap<>();
//        Map<Long, FeiShuSpaceVo> feiShuSpaceMap = new HashMap<>();
//        List<String> allNodeTokenList = new ArrayList<>();
//        JSONArray spaceIdArray = paramObj.getJSONArray("spaceIdList");
//        if (CollectionUtils.isNotEmpty(spaceIdArray)) {
//            List<Long> spaceIdList = spaceIdArray.toJavaList(Long.class);
//            for (Long spaceId : spaceIdList) {
//                FeiShuSpaceVo feiShuSpaceVo = getFeiShuSpaceBySpaceId(spaceId, tenantAccessToken, feiShuSpaceMap);
//                if (feiShuSpaceVo != null) {
//                    KnowledgeDocumentTypeVo knowledgeType = knowledgeFeiShuService.getOrCreateKnowledgeType(feiShuSpaceVo.getName(), "0", feiShuAppCredentials.getKnowledgeCircleId());
//                    List<FeiShuNodeVo> nodes = knowledgeFeiShuService.loadWikiNodes(spaceId, null, new ArrayList<>(), tenantAccessToken);
//                    knowledgeFeiShuService.saveNodes(nodes, feiShuAppCredentials, knowledgeType, tenantAccessToken);
//                }
//            }
//        }
//        JSONArray nodeTokenArray = paramObj.getJSONArray("nodeTokenList");
//        if (CollectionUtils.isNotEmpty(nodeTokenArray)) {
//            List<String> nodeTokenList = nodeTokenArray.toJavaList(String.class);
//            List<String> list = ListUtils.removeAll(nodeTokenList, allNodeTokenList);
//            if (CollectionUtils.isNotEmpty(list)) {
//                for (String nodeToken : list) {
//                    FeiShuNodeVo feiShuNodeVo = getFeiShuNodeByNodeToken(nodeToken, tenantAccessToken, feiShuNodeMap);
//                    System.out.println("feiShuNodeVo = " + JSONObject.toJSONString(feiShuNodeVo));
//                    if (feiShuNodeVo != null) {
//                        FeiShuSpaceVo feiShuSpaceVo = getFeiShuSpaceBySpaceId(feiShuNodeVo.getSpaceId(), tenantAccessToken, feiShuSpaceMap);
//                        if (feiShuSpaceVo != null) {
//                            KnowledgeDocumentTypeVo knowledgeType = knowledgeFeiShuService.getOrCreateKnowledgeType(feiShuSpaceVo.getName(), "0", feiShuAppCredentials.getKnowledgeCircleId());
//                            List<FeiShuNodeVo> parentList = new ArrayList<>();
//                            FeiShuNodeVo parent = feiShuNodeVo.getParent();
//                            while (parent != null) {
//                                parentList.add(parent);
//                                parent = parent.getParent();
//                            }
//                            if (CollectionUtils.isNotEmpty(parentList)) {
//                                String parentUuid = knowledgeType.getUuid();
//                                for (int i = parentList.size() - 1; i >= 0; i--) {
//                                    FeiShuNodeVo parentVo = parentList.get(i);
//                                    knowledgeType = knowledgeFeiShuService.getOrCreateKnowledgeType(parentVo.getTitle(), parentUuid, feiShuAppCredentials.getKnowledgeCircleId());
//                                    parentUuid = knowledgeType.getUuid();
//                                }
//                            }
//                            knowledgeFeiShuService.saveFeiShuDocument(feiShuAppCredentials, feiShuNodeVo, knowledgeType.getUuid(), tenantAccessToken);
//                        }
//                    }
//                }
//            }
//        }
//        knowledgeDocumentTypeService.rebuildLeftRightCode(feiShuAppCredentials.getKnowledgeCircleId());

        List<FeiShuNodeVo> feiShuNodeList = getFeiShuNodeList(spaceIdList, nodeTokenList, tenantAccessToken, feiShuAppCredentials);
        if (CollectionUtils.isNotEmpty(feiShuNodeList)) {
            for (int i = feiShuNodeList.size() - 1; i >= 0; i--) {
                FeiShuNodeVo feiShuNodeVo = feiShuNodeList.get(i);
                KnowledgeFeiShuDocumentMappingVo feiShuDocumentMapping = knowledgeFeiShuMapper.getFeiShuDocumentMappingByNodeToken(feiShuNodeVo.getNodeToken());
                if (feiShuDocumentMapping != null) {
                    if (Objects.equals(feiShuDocumentMapping.getStatus(), Status.WAITING.getValue())
                            || Objects.equals(feiShuDocumentMapping.getStatus(), Status.RUNNING.getValue())) {
                        feiShuNodeList.remove(i);
                        continue;
                    }
                }
                knowledgeFeiShuMapper.updateFeiShuDocumentMappingStatusByNodeToken(feiShuNodeVo.getNodeToken(), Status.WAITING.getValue());
            }
            if (CollectionUtils.isNotEmpty(feiShuNodeList)) {
                CachedThreadPool.execute(new FeiShuWikiThread(feiShuNodeList));
            }
        }
        return null;
    }

    private List<FeiShuNodeVo> getFeiShuNodeList(List<Long> spaceIdList, List<String> nodeTokenList, String tenantAccessToken, FeiShuAppCredentialsVo feiShuAppCredentials) {
        List<FeiShuNodeVo> feiShuNodeList = new ArrayList<>();
        Map<String, FeiShuNodeVo> feiShuNodeMap = new HashMap<>();
        List<FeiShuSpaceVo> allFeiShuSpaceList = knowledgeFeiShuService.getFeiShuSpaceList(feiShuAppCredentials);
        Map<Long, FeiShuSpaceVo> feiShuSpaceMap = allFeiShuSpaceList.stream().collect(Collectors.toMap(FeiShuSpaceVo::getSpaceId, e -> e));
//        List<String> allNodeTokenList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(spaceIdList)) {
            for (Long spaceId : spaceIdList) {
//                FeiShuSpaceVo feiShuSpaceVo = getFeiShuSpaceBySpaceId(spaceId, tenantAccessToken, feiShuSpaceMap);
                FeiShuSpaceVo feiShuSpaceVo = feiShuSpaceMap.get(spaceId);
                if (feiShuSpaceVo != null) {
//                    KnowledgeDocumentTypeVo knowledgeType = knowledgeFeiShuService.getOrCreateKnowledgeType(feiShuSpaceVo.getName(), "0", feiShuAppCredentials.getKnowledgeCircleId());
                    List<FeiShuNodeVo> nodes = loadWikiNodes(feiShuSpaceVo, null, new ArrayList<>(), tenantAccessToken);
                    feiShuNodeList.addAll(nodes);
//                    knowledgeFeiShuService.saveNodes(nodes, feiShuAppCredentials, knowledgeType, tenantAccessToken);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(nodeTokenList)) {
            List<String> list = ListUtils.removeAll(nodeTokenList, feiShuNodeList.stream().map(FeiShuNodeVo::getNodeToken).collect(Collectors.toList()));
            if (CollectionUtils.isNotEmpty(list)) {
                for (String nodeToken : list) {
                    FeiShuNodeVo feiShuNodeVo = getFeiShuNodeByNodeToken(nodeToken, tenantAccessToken, feiShuNodeMap);
                    System.out.println("feiShuNodeVo = " + JSONObject.toJSONString(feiShuNodeVo));
                    if (feiShuNodeVo != null) {
//                        FeiShuSpaceVo feiShuSpaceVo = getFeiShuSpaceBySpaceId(feiShuNodeVo.getSpaceId(), tenantAccessToken, feiShuSpaceMap);
                        FeiShuSpaceVo feiShuSpaceVo = feiShuSpaceMap.get(feiShuNodeVo.getSpaceId());
                        if (feiShuSpaceVo != null) {
                            feiShuNodeVo.setSpaceName(feiShuSpaceVo.getName());
                            feiShuNodeList.add(feiShuNodeVo);
                        }
                    }
                }
            }
        }
        return feiShuNodeList;
    }

    private List<FeiShuNodeVo> loadWikiNodes(FeiShuSpaceVo feiShuSpaceVo, FeiShuNodeVo parent, List<String> path, String tenantAccessToken) {
        List<FeiShuNodeVo> nodeList = new ArrayList<>();
        String parentNodeToken = parent != null ? parent.getNodeToken() : null;
        JSONObject result = FeiShuOpenApiUtil.getFeishuWikiNodes(feiShuSpaceVo.getSpaceId(), parentNodeToken, tenantAccessToken);
        JSONObject data = result.getJSONObject("data");
        if (data == null) {
            return nodeList;
        }
        JSONArray items = data.getJSONArray("items");
        if (CollectionUtils.isNotEmpty(items)) {
            for (int i = 0; i < items.size(); i++) {
                JSONObject item = items.getJSONObject(i);
                FeiShuNodeVo node = new FeiShuNodeVo(item);
                node.setSpaceName(feiShuSpaceVo.getName());
                node.setParent(parent);
                node.getPath().addAll(path);
                if (!isDocumentNode(node)) {
                    node.getPath().add(node.getTitle());
                }
                nodeList.add(node);
                if (Objects.equals(item.getBoolean("has_child"), true)) {
                    List<FeiShuNodeVo> children = loadWikiNodes(feiShuSpaceVo, node, node.getPath(), tenantAccessToken);
                    nodeList.addAll(children);
//                    node.setChildren(children);
                }
            }
        }
        return nodeList;
    }

    private boolean isDocumentNode(FeiShuNodeVo node) {
        return "docx".equals(node.getObjType()) || "doc".equals(node.getObjType());
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


}
