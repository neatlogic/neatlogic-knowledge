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
import neatlogic.module.knowledge.dao.mapper.KnowledgeFeiShuMapper;
import neatlogic.module.knowledge.service.KnowledgeDocumentTypeService;
import neatlogic.module.knowledge.service.KnowledgeFeiShuService;
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
        Map<String, FeiShuNodeVo> feiShuNodeMap = new HashMap<>();
        Map<Long, FeiShuSpaceVo> feiShuSpaceMap = new HashMap<>();
        List<String> allNodeTokenList = new ArrayList<>();
        JSONArray spaceIdArray = paramObj.getJSONArray("spaceIdList");
        if (CollectionUtils.isNotEmpty(spaceIdArray)) {
            List<Long> spaceIdList = spaceIdArray.toJavaList(Long.class);
            for (Long spaceId : spaceIdList) {
                FeiShuSpaceVo feiShuSpaceVo = getFeiShuSpaceBySpaceId(spaceId, tenantAccessToken, feiShuSpaceMap);
                if (feiShuSpaceVo != null) {
                    KnowledgeDocumentTypeVo knowledgeType = knowledgeFeiShuService.getOrCreateKnowledgeType(feiShuSpaceVo.getName(), "0", feiShuAppCredentials.getKnowledgeCircleId());
                    List<FeiShuNodeVo> nodes = knowledgeFeiShuService.loadWikiNodes(spaceId, null, new ArrayList<>(), tenantAccessToken);
//                    knowledgeFeishuSyncMapper.updateSyncDocumentStatusByNodeToken(feiShuNodeVo.getNodeToken(), Status.RUNNING.getValue());
                    knowledgeFeiShuService.saveNodes(nodes, feiShuAppCredentials, knowledgeType, tenantAccessToken);
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
                            KnowledgeDocumentTypeVo knowledgeType = knowledgeFeiShuService.getOrCreateKnowledgeType(feiShuSpaceVo.getName(), "0", feiShuAppCredentials.getKnowledgeCircleId());
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
                                    knowledgeType = knowledgeFeiShuService.getOrCreateKnowledgeType(parentVo.getTitle(), parentUuid, feiShuAppCredentials.getKnowledgeCircleId());
                                    parentUuid = knowledgeType.getUuid();
                                }
                            }
                            knowledgeFeiShuService.saveFeiShuDocument(feiShuAppCredentials, feiShuNodeVo, knowledgeType.getUuid(), tenantAccessToken);
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


}
