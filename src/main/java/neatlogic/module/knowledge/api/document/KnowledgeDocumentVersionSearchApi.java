/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.knowledge.api.document;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVersionVo;
import neatlogic.framework.knowledge.crossover.IKnowledgeDocumentCrossoverService;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_BASE;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/** 知识版本搜索的 HTTP 入口，业务逻辑由共用检索服务处理。 */
@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentVersionSearchApi extends PrivateApiComponentBase {

    @Resource
    private IKnowledgeDocumentCrossoverService knowledgeDocuments;

    @Override
    public String getToken() {
        return "knowledge/document/version/search";
    }

    @Override
    public String getName() {
        return "nmkad.knowledgedocumentsearchapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    /** 委托共用版本检索，保留知识库页面隐藏草稿状态的原响应约定。 */
    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword"),
            @Param(name = "lcuList", type = ApiParamType.JSONARRAY, desc = "common.editorlist"),
            @Param(name = "reviewerList", type = ApiParamType.JSONARRAY, desc = "common.reviewerlist"),
            @Param(name = "collector", type = ApiParamType.STRING, desc = "common.collector"),
            @Param(name = "sourceList", type = ApiParamType.JSONARRAY, desc = "common.sourcelist"),
            @Param(name = "statusList", type = ApiParamType.JSONARRAY, desc = "common.statuslist", help = "审批状态：all|submitted|passed|rejected|draft"),
            @Param(name = "knowledgeDocumentTypeUuid", type = ApiParamType.STRING, desc = "common.typeid"),
            @Param(name = "lcd", type = ApiParamType.JSONOBJECT, desc = "common.editdate", help = "{timeRange: 6, timeUnit: 'month'} 或  {startTime: 1605196800000, endTime: 1607961600000}"),
            @Param(name = "reviewDate", type = ApiParamType.JSONOBJECT, desc = "term.knowledge.reviewdate", help = "{timeRange: 6, timeUnit: 'month'} 或  {startTime: 1605196800000, endTime: 1607961600000}"),
            @Param(name = "tagList", type = ApiParamType.JSONARRAY, desc = "common.taglist"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize")
    })
    @Output({
            @Param(name = "dataList[].version", type = ApiParamType.INTEGER, desc = "common.versionnum"),
            @Param(name = "dataList[].knowledgeDocumentVersionId", type = ApiParamType.INTEGER, desc = "common.versionid"),
            @Param(name = "dataList[].knowledgeCircleName", type = ApiParamType.STRING, desc = "common.name"),
            @Param(name = "dataList[].title", type = ApiParamType.STRING, desc = "common.title"),
            @Param(name = "dataList[].content", type = ApiParamType.STRING, desc = "common.content"),
            @Param(name = "dataList[].lcu", type = ApiParamType.STRING, desc = "common.editor"),
            @Param(name = "dataList[].lcuName", type = ApiParamType.STRING, desc = "common.editorname"),
            @Param(name = "dataList[].lcd", type = ApiParamType.STRING, desc = "common.editdate"),
            @Param(name = "dataList[].tagList", type = ApiParamType.JSONARRAY, desc = "common.taglist"),
            @Param(name = "dataList[].viewCount", type = ApiParamType.LONG, desc = "common.viewcount"),
            @Param(name = "dataList[].favorCount", type = ApiParamType.LONG, desc = "common.favorcount"),
            @Param(name = "dataList[].collectCount", type = ApiParamType.LONG, desc = "common.collectcount"),
            @Param(name = "dataList[].documentTypePath", type = ApiParamType.STRING, desc = "term.knowledge.typepath"),
            @Param(name = "dataList[].knowledgeDocumentTypeUuid", type = ApiParamType.STRING, desc = "common.typeid"),
            @Param(name = "dataList[].status", type = ApiParamType.STRING, desc = "common.status"),
            @Param(name = "statusList", type = ApiParamType.JSONARRAY, desc = "common.statuslist", help = "”待我审批“、”我提交的“ 知识分类，对应“全部” “待审批” “以通过” “不通过” 分类的数量"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "nmkad.knowledgedocumentsearchapi.getname")
    @Override
    @SuppressWarnings("unchecked")
    public Object myDoService(JSONObject jsonObj) {
        JSONObject result = knowledgeDocuments.searchDocumentVersions(jsonObj, false);
        for (KnowledgeDocumentVersionVo version : (List<KnowledgeDocumentVersionVo>) result.get("dataList")) {
            if (KnowledgeDocumentVersionStatus.DRAFT.getValue().equals(version.getStatus())) {
                version.setStatus(null);
                version.setStatusVo(null);
            }
        }
        return result;
    }
}
