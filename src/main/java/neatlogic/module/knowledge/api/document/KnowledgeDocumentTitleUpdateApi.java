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
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.fulltextindex.core.FullTextIndexHandlerFactory;
import neatlogic.framework.fulltextindex.core.IFullTextIndexHandler;
import neatlogic.framework.knowledge.constvalue.KnowledgeFullTextIndexType;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVo;
import neatlogic.framework.knowledge.exception.KnowledgeDocumentNotFoundException;
import neatlogic.framework.knowledge.exception.KnowledgeDocumentTitleRepeatException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_BASE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class KnowledgeDocumentTitleUpdateApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Override
    public String getToken() {
        return "knowledge/document/title/update";
    }

    @Override
    public String getName() {
        return "更新知识库文档标题";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
        @Param(name = "knowledgeDocumentId", type = ApiParamType.LONG, isRequired = true, desc = "文档id"),
        @Param(name = "title", type = ApiParamType.STRING, isRequired = true, minLength = 1, desc = "新标题")
    })
    @Description(desc = "更新知识库文档标题")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long knowledgeDocumentId = jsonObj.getLong("knowledgeDocumentId");
        KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentLockById(knowledgeDocumentId);
        if(knowledgeDocumentVo == null) {
            throw new KnowledgeDocumentNotFoundException(knowledgeDocumentId);
        }
        String title = jsonObj.getString("title");
        if(!knowledgeDocumentVo.getTitle().equals(title)) {
            knowledgeDocumentVo.setTitle(title);
            if(knowledgeDocumentMapper.checkKnowledgeDocumentTitleIsRepeat(knowledgeDocumentVo) > 0){
                throw new KnowledgeDocumentTitleRepeatException(knowledgeDocumentVo.getTitle());
            }
            knowledgeDocumentMapper.updateKnowledgeDocumentTitleById(knowledgeDocumentVo);
            //创建全文检索索引
            IFullTextIndexHandler handler = FullTextIndexHandlerFactory.getHandler(KnowledgeFullTextIndexType.KNOW_DOCUMENT_VERSION);
            if (handler != null) {
                handler.createIndex(knowledgeDocumentVo.getKnowledgeDocumentVersionId());
            }
        }
        return null;
    }

    public IValid title() {
        return value -> {
            Long knowledgeDocumentId = value.getLong("knowledgeDocumentId");
            String title = value.getString("title");
            KnowledgeDocumentVo knowledgeDocumentVo = new KnowledgeDocumentVo();
            knowledgeDocumentVo.setId(knowledgeDocumentId);
            knowledgeDocumentVo.setTitle(title);
            if(knowledgeDocumentMapper.checkKnowledgeDocumentTitleIsRepeat(knowledgeDocumentVo) > 0){
                return new FieldValidResultVo(new KnowledgeDocumentTitleRepeatException(knowledgeDocumentVo.getTitle()));
            }
            return new FieldValidResultVo();
        };
    }

}
