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
import neatlogic.framework.knowledge.constvalue.Status;
import neatlogic.framework.knowledge.dto.feishu.*;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_FEISHU_SYNC_MODIFY;
import neatlogic.module.knowledge.dao.mapper.KnowledgeFeiShuMapper;
import neatlogic.module.knowledge.service.KnowledgeFeiShuService;
import neatlogic.module.knowledge.thread.FeiShuWikiThread;
import neatlogic.module.knowledge.utils.FeiShuOpenApiUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = KNOWLEDGE_FEISHU_SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SyncFeiShuWikiDocumentApi extends PrivateApiComponentBase {

    @Resource
    private KnowledgeFeiShuService knowledgeFeiShuService;
    @Resource
    private KnowledgeFeiShuMapper knowledgeFeiShuMapper;

    @Override
    public String getToken() {
        return "knowledge/feishu/wiki/document/sync";
    }

    @Override
    public String getName() {
        return "nmkaf.syncfeishuwikidocumentapi.getname";
    }

    @Input({
            @Param(name = "spaceIdList", type = ApiParamType.JSONARRAY, desc = "nmkaf.syncfeishuwikidocumentapi.input.param.desc.spaceidlist"),
            @Param(name = "nodeTokenList", type = ApiParamType.JSONARRAY, desc = "nmkaf.syncfeishuwikidocumentapi.input.param.desc.nodetokenlist"),

    })
    @Description(desc = "nmkaf.syncfeishuwikidocumentapi.getname")
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
                KnowledgeFeiShuDocumentMappingVo documentMappingVo = new KnowledgeFeiShuDocumentMappingVo(feiShuAppCredentials.getAppId(), feiShuNodeVo);
                documentMappingVo.setStatus(Status.WAITING.getValue());
                knowledgeFeiShuMapper.insertFeiShuDocumentMappingStatus(documentMappingVo);
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
        if (CollectionUtils.isNotEmpty(spaceIdList)) {
            for (Long spaceId : spaceIdList) {
                FeiShuSpaceVo feiShuSpaceVo = feiShuSpaceMap.get(spaceId);
                if (feiShuSpaceVo != null) {
                    List<FeiShuNodeVo> nodes = loadWikiNodes(feiShuSpaceVo, null, new ArrayList<>(), tenantAccessToken);
                    feiShuNodeList.addAll(nodes);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(nodeTokenList)) {
            List<String> list = ListUtils.removeAll(nodeTokenList, feiShuNodeList.stream().map(FeiShuNodeVo::getNodeToken).collect(Collectors.toList()));
            if (CollectionUtils.isNotEmpty(list)) {
                for (String nodeToken : list) {
                    FeiShuNodeVo feiShuNodeVo = getFeiShuNodeByNodeToken(nodeToken, tenantAccessToken, feiShuNodeMap);
                    if (feiShuNodeVo != null) {
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
        JSONObject result = FeiShuOpenApiUtil.getFeiShuWikiNodes(feiShuSpaceVo.getSpaceId(), parentNodeToken, tenantAccessToken);
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
            JSONObject feishuSpaceInfo = FeiShuOpenApiUtil.getFeiShuSpaceInfo(spaceId, tenantAccessToken);
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
            JSONObject feishuNodeInfo = FeiShuOpenApiUtil.getFeiShuNodeInfo(nodeToken, tenantAccessToken);
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
