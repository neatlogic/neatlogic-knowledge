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
import neatlogic.framework.fulltextindex.core.FullTextIndexHandlerFactory;
import neatlogic.framework.fulltextindex.core.IFullTextIndexHandler;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_BASE;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.constvalue.KnowledgeFullTextIndexType;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author lvzk
 * @since 2021/3/23 17:23
 **/
@Service
@OperationType(type = OperationTypeEnum.UPDATE)
@AuthAction(action = KNOWLEDGE_BASE.class)
public class KnowledgeDocumentFulltextIndexRebuildApi extends PrivateApiComponentBase {
    @Resource
    KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Override
    public String getName() {
        return "重建知识版本索引";
    }

    @Override
    public String getConfig() {
        return null;
    }
    @Input({@Param(name = "versionIdList", type = ApiParamType.JSONARRAY, desc = "版本号idList") })
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray versionIdArray = jsonObj.getJSONArray("versionIdList");
        List<Long> versionIdList = null;
        //创建全文检索索引
        IFullTextIndexHandler handler = FullTextIndexHandlerFactory.getHandler(KnowledgeFullTextIndexType.KNOW_DOCUMENT_VERSION);
        if (handler != null) {
            if(CollectionUtils.isNotEmpty(versionIdArray)){
                versionIdList = JSONObject.parseArray(versionIdArray.toJSONString(), Long.class);
            }else{
                versionIdList = knowledgeDocumentMapper.getKnowledgeDocumentVersionIdList();
            }
            for(Long versionIdObj : versionIdList ){
                handler.createIndex(versionIdObj);
            }
        }
        return null;
    }

    @Override
    public String getToken() {
        return "/knowledge/document/version/fulltext/index/rebuild";
    }
}
