/*
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

package neatlogic.module.knowledge.thread;

import neatlogic.framework.asynchronization.thread.NeatLogicThread;
import neatlogic.framework.knowledge.constvalue.Status;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentTypeVo;
import neatlogic.framework.knowledge.dto.feishu.FeiShuAppCredentialsVo;
import neatlogic.framework.knowledge.dto.feishu.FeiShuNodeVo;
import neatlogic.module.knowledge.service.KnowledgeDocumentTypeService;
import neatlogic.module.knowledge.service.KnowledgeFeiShuService;
import neatlogic.module.knowledge.utils.FeiShuOpenApiUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FeiShuWikiThread extends NeatLogicThread {

    private static KnowledgeFeiShuService knowledgeFeiShuService;

    private static KnowledgeDocumentTypeService knowledgeDocumentTypeService;

    @Autowired
    public void setKnowledgeFeiShuService(KnowledgeFeiShuService _knowledgeFeiShuService) {
        knowledgeFeiShuService = _knowledgeFeiShuService;
    }

    @Autowired
    public void setKnowledgeDocumentTypeService(KnowledgeDocumentTypeService _knowledgeDocumentTypeService) {
        knowledgeDocumentTypeService = _knowledgeDocumentTypeService;
    }

    private List<FeiShuNodeVo> feiShuNodeList;

    public FeiShuWikiThread(List<FeiShuNodeVo> feiShuNodeList) {
        super("KNOWLEDGE-FEISHUWIKI-SYNC-THREAD");
        this.feiShuNodeList = feiShuNodeList;
    }

    @Override
    protected void execute() {
        FeiShuAppCredentialsVo feiShuAppCredentials = knowledgeFeiShuService.getFeiShuAppCredentials();
        String tenantAccessToken = FeiShuOpenApiUtil.getTenantAccessToken(feiShuAppCredentials.getAppId(), feiShuAppCredentials.getAppSecret());
        for (FeiShuNodeVo feiShuNodeVo : feiShuNodeList) {
            saveNode(feiShuNodeVo, feiShuAppCredentials, tenantAccessToken);
        }
        knowledgeDocumentTypeService.rebuildLeftRightCode(feiShuAppCredentials.getKnowledgeCircleId());
    }


    private void saveNode(FeiShuNodeVo feiShuNodeVo, FeiShuAppCredentialsVo feiShuAppCredentials, String tenantAccessToken) {
        boolean flag = knowledgeFeiShuService.updateFeiShuDocumentMappingStatusByNodeToken(feiShuNodeVo.getNodeToken(), Status.WAITING, Status.RUNNING);
        if (!flag) {
            return;
        }
        KnowledgeDocumentTypeVo knowledgeType = knowledgeFeiShuService.getOrCreateKnowledgeType(feiShuNodeVo.getSpaceName(), "0", feiShuAppCredentials.getKnowledgeCircleId());
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
