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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.knowledge.constvalue.Status;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_FEISHU_SYNC_MODIFY;
import neatlogic.module.knowledge.service.KnowledgeFeiShuService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = KNOWLEDGE_FEISHU_SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class CancelSyncFeiShuWikiDocumentApi extends PrivateApiComponentBase {

    @Resource
    private KnowledgeFeiShuService knowledgeFeiShuService;

    @Override
    public String getToken() {
        return "knowledge/feishu/wiki/document/sync/cancel";
    }

    @Override
    public String getName() {
        return "取消同步文档";
    }

    @Input({
            @Param(name = "nodeToken", type = ApiParamType.STRING, isRequired = true, desc = "nmkaf.getfeishudocumentinfoapi.input.param.desc.nodetoken"),

    })
    @Description(desc = "取消同步文档")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String nodeToken = paramObj.getString("nodeToken");
        boolean flag = knowledgeFeiShuService.updateFeiShuDocumentMappingStatusByNodeToken(nodeToken, Status.WAITING, Status.CANCELED);
        return new JSONObject().fluentPut("result", flag);
    }
}
