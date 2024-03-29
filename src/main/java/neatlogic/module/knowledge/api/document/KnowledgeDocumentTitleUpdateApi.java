/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.knowledge.api.document;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.fulltextindex.core.FullTextIndexHandlerFactory;
import neatlogic.framework.fulltextindex.core.IFullTextIndexHandler;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_BASE;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVo;
import neatlogic.framework.knowledge.exception.KnowledgeDocumentNotFoundException;
import neatlogic.framework.knowledge.exception.KnowledgeDocumentTitleRepeatException;
import neatlogic.framework.knowledge.constvalue.KnowledgeFullTextIndexType;
import com.alibaba.fastjson.JSONObject;
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
