package neatlogic.module.knowledge.api.document;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_BASE;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVo;
import neatlogic.module.knowledge.service.KnowledgeDocumentService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentGetApi extends PrivateApiComponentBase {

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    @Override
    public String getToken() {
        return "knowledge/document/get";
    }

    @Override
    public String getName() {
        return "查询文档内容";
    }

    @Override
    public String getConfig() {
        return null;
    }
    
    @Input({
        @Param(name = "knowledgeDocumentId", type = ApiParamType.LONG, isRequired = true, desc = "文档id"),
        @Param(name = "knowledgeDocumentVersionId", type = ApiParamType.LONG, desc = "版本id"),
        @Param(name = "isReadOnly", type = ApiParamType.ENUM, rule = "0,1", desc = "是否增加浏览量")
    })
    @Output({
        @Param(explode = KnowledgeDocumentVo.class, desc = "文档内容")
    })
    @Description(desc = "查询文档内容")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String userUuid = UserContext.get().getUserUuid(true);
        Long knowledgeDocumentId = jsonObj.getLong("knowledgeDocumentId");
        Long knowledgeDocumentVersionId = jsonObj.getLong("knowledgeDocumentVersionId");
        Long currentVersionId = knowledgeDocumentService.checkViewPermissionByDocumentIdAndVersionId(knowledgeDocumentId,knowledgeDocumentVersionId);

        Integer isReadOnly = jsonObj.getInteger("isReadOnly");
        if(Objects.equals(isReadOnly, 1)) {
            knowledgeDocumentMapper.updateKnowledgeViewCountIncrementOne(knowledgeDocumentId);
        }
        KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentService.getKnowledgeDocumentDetailByKnowledgeDocumentVersionId(currentVersionId);
        knowledgeDocumentVo.setFavorCount(knowledgeDocumentMapper.getDocumentFavorCount(knowledgeDocumentVo.getId()));
        knowledgeDocumentVo.setCollectCount(knowledgeDocumentMapper.getDocumentCollectCount(knowledgeDocumentVo.getId()));
        knowledgeDocumentVo.setViewCount(knowledgeDocumentMapper.getDocumentViewCount(knowledgeDocumentVo.getId()));
        knowledgeDocumentVo.setIsCollect(knowledgeDocumentMapper.checkDocumentHasBeenCollected(knowledgeDocumentVo.getId(), userUuid));
        knowledgeDocumentVo.setIsFavor(knowledgeDocumentMapper.checkDocumentHasBeenFavored(knowledgeDocumentVo.getId(), userUuid));
        return knowledgeDocumentVo;
    }

}
