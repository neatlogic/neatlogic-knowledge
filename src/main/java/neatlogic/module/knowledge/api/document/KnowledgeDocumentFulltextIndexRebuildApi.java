/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

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
