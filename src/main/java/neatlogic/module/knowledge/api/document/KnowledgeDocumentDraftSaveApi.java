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
import neatlogic.framework.exception.type.PermissionDeniedException;
import javax.annotation.Resource;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.knowledge.crossover.IKnowledgeDocumentCrossoverService;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.exception.KnowledgeDocumentTitleRepeatException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_BASE;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 知识文档草稿保存的 HTTP 入口。 */
@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
@Transactional
public class KnowledgeDocumentDraftSaveApi extends PrivateApiComponentBase {

    @Resource
    private IKnowledgeDocumentCrossoverService knowledgeDocuments;

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Override
    public String getToken() {
        return "knowledge/document/draft/save";
    }

    @Override
    public String getName() {
        return "nmkad.knowledgedocumentdraftsaveapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "knowledgeDocumentVersionId", type = ApiParamType.LONG, desc = "common.versionid"),
            @Param(name = "knowledgeDocumentTypeUuid", type = ApiParamType.STRING, isRequired = true, desc = "common.typeid"),
            @Param(name = "title", type = ApiParamType.STRING, isRequired = true, minLength = 1, desc = "common.title"),
            @Param(name = "lineList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "nmkad.knowledgedocumentdraftsaveapi.linelist"),
            @Param(name = "fileIdList", type = ApiParamType.JSONARRAY, desc = "common.fileidlist"),
            @Param(name = "tagList", type = ApiParamType.JSONARRAY, desc = "common.taglist"),
            @Param(name = "invokeId", type = ApiParamType.LONG, desc = "nmkad.knowledgedocumentdraftsaveapi.invokeid"),
            @Param(name = "source", type = ApiParamType.STRING, desc = "common.source"),
            @Param(name = "isSubmit", type = ApiParamType.INTEGER, desc = "nmkad.knowledgedocumentdraftsaveapi.issubmit")
    })
    @Output({
            @Param(name = "knowledgeDocumentId", type = ApiParamType.LONG, desc = "term.knowledge.documentid"),
            @Param(name = "knowledgeDocumentVersionId", type = ApiParamType.LONG, desc = "common.versionid"),
            @Param(name = "isReviewable", type = ApiParamType.INTEGER, desc = "common.isreviewable"),
    })
    @Description(desc = "nmkad.knowledgedocumentdraftsaveapi.getname")
    //@ResubmitInterval(value = 5) //无需限制，用户可能存在快速修改保存的情况。
    @Override
    public Object myDoService(JSONObject jsonObj) throws PermissionDeniedException {
        return knowledgeDocuments.saveDraft(jsonObj);
    }

    /** 在编辑器中提前提示重复的文档标题。 */
    public IValid title() {
        return value -> {
            String title = value.getString("title");
            if (knowledgeDocumentMapper.getKnowledgeDocumentByTitle(title) != null) {
                return new FieldValidResultVo(new KnowledgeDocumentTitleRepeatException(title));
            }
            return new FieldValidResultVo();
        };
    }
}
