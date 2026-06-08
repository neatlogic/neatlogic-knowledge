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
import neatlogic.framework.knowledge.dto.feishu.FeiShuAppCredentialsVo;
import neatlogic.framework.knowledge.dto.feishu.FeiShuNodeVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_FEISHU_SYNC_MODIFY;
import neatlogic.module.knowledge.service.KnowledgeFeiShuService;
import neatlogic.module.knowledge.utils.FeiShuOpenApiUtil;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = KNOWLEDGE_FEISHU_SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetFeiShuDocumentInfoApi extends PrivateApiComponentBase {
    @Resource
    private KnowledgeFeiShuService knowledgeFeiShuService;

    @Override
    public String getToken() {
        return "knowledge/feishu/document/get";
    }

    @Override
    public String getName() {
        return "nmkaf.getfeishudocumentinfoapi.getname";
    }

    @Input({
            @Param(name = "nodeToken", type = ApiParamType.STRING, isRequired = true, desc = "nmkaf.getfeishudocumentinfoapi.input.param.desc.nodetoken"),
    })
    @Description(desc = "nmkaf.getfeishudocumentinfoapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String nodeToken = paramObj.getString("nodeToken");
        FeiShuAppCredentialsVo feiShuAppCredentials = knowledgeFeiShuService.getFeiShuAppCredentials();
        if (feiShuAppCredentials == null) {
            return null;
        }
        String tenantAccessToken = FeiShuOpenApiUtil.getTenantAccessToken(feiShuAppCredentials.getAppId(), feiShuAppCredentials.getAppSecret());
        if (StringUtils.isBlank(tenantAccessToken)) {
            return null;
        }
        JSONObject feishuNodeInfo = FeiShuOpenApiUtil.getFeiShuNodeInfo(nodeToken, tenantAccessToken);
        if (feishuNodeInfo != null) {
            JSONObject data = feishuNodeInfo.getJSONObject("data");
            if (MapUtils.isNotEmpty(data)) {
                JSONObject node = data.getJSONObject("node");
                if (MapUtils.isNotEmpty(node)) {
                    FeiShuNodeVo feiShuNodeVo = new FeiShuNodeVo(node);
                    return FeiShuOpenApiUtil.getDocumentBlocks(feiShuNodeVo.getObjToken(), tenantAccessToken);
                }
            }
        }
        return null;
    }
}
