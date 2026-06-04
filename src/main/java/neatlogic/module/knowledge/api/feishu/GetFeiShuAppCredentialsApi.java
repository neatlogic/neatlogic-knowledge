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
import neatlogic.framework.config.ConfigManager;
import neatlogic.framework.knowledge.constvalue.KnowledgeTenantConfig;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_FEISHU_SYNC_MODIFY;
import neatlogic.module.knowledge.service.KnowledgeFeishuSyncService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = KNOWLEDGE_FEISHU_SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetFeiShuAppCredentialsApi extends PrivateApiComponentBase {

    @Resource
    private KnowledgeFeishuSyncService knowledgeFeishuSyncService;

    @Override
    public String getToken() {
        return "knowledge/feishu/app/credentials/get";
    }

    @Override
    public String getName() {
        return "获取飞书应用凭证";
    }

    @Input({})
    @Output({
            @Param(name = "appId", type = ApiParamType.STRING, isRequired = true, desc = "App ID"),
            @Param(name = "appSecret", type = ApiParamType.STRING, isRequired = true, desc = "App Secret"),
    })
    @Description(desc = "保存飞书应用凭证")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        return knowledgeFeishuSyncService.getFeiShuAppCredentials();
    }
}
