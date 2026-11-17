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
import javax.annotation.Resource;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.knowledge.auth.label.KNOWLEDGE;
import neatlogic.framework.knowledge.crossover.IKnowledgeDocumentCrossoverService;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;

/** 知识文档检索的 HTTP 入口。 */
@Service
@AuthAction(action = KNOWLEDGE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentSearchApi extends PrivateApiComponentBase {

    @Resource
    private IKnowledgeDocumentCrossoverService knowledgeDocuments;

    @Override
    public String getToken() {
        return "knowledge/document/search";
    }

    @Override
    public String getName() {
        return "nmkad.knowledgedocumentsearchapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword"),
            @Param(name = "lcuList", type = ApiParamType.JSONARRAY, desc = "common.editorlist"),
            @Param(name = "collector", type = ApiParamType.STRING, desc = "common.collector"),
            @Param(name = "sourceList", type = ApiParamType.JSONARRAY, desc = "common.sourcelist"),
            @Param(name = "statusList", type = ApiParamType.JSONARRAY, desc = "common.statuslist", help = "nmkad.knowledgedocumentsearchapi.help.statusfilter"),
            @Param(name = "knowledgeDocumentTypeUuid", type = ApiParamType.STRING, desc = "common.typeid"),
            @Param(name = "lcd", type = ApiParamType.JSONOBJECT, desc = "common.editdate", help = "nmkad.knowledgedocumentsearchapi.help.daterange"),
            @Param(name = "reviewDate", type = ApiParamType.JSONOBJECT, desc = "term.knowledge.reviewdate", help = "nmkad.knowledgedocumentsearchapi.help.daterange"),
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
            @Param(name = "statusList", type = ApiParamType.JSONARRAY, desc = "common.statuslist", help = "nmkad.knowledgedocumentsearchapi.help.statuscounts"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "nmkad.knowledgedocumentsearchapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        return knowledgeDocuments.searchDocuments(jsonObj);
    }
}
