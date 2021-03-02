package codedriver.module.knowledge.api.document;

import java.util.Objects;

import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.exception.KnowledgeDocumentVersionNotFoundException;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotFoundException;
import codedriver.module.knowledge.service.KnowledgeDocumentService;

import javax.annotation.Resource;

@Service
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
        Long knowledgeDocumentId = jsonObj.getLong("knowledgeDocumentId");
        KnowledgeDocumentVo documentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(knowledgeDocumentId);
        if(documentVo == null) {
            throw new KnowledgeDocumentNotFoundException(knowledgeDocumentId);
        }

        boolean isLcu = false;
        Integer isReadOnly = jsonObj.getInteger("isReadOnly");
        Long currentVersionId = documentVo.getKnowledgeDocumentVersionId();
        Long knowledgeDocumentVersionId = jsonObj.getLong("knowledgeDocumentVersionId");
        if(knowledgeDocumentVersionId != null) {
            currentVersionId = knowledgeDocumentVersionId;
            if(Objects.equals(isReadOnly, 1)){
                KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
                if (knowledgeDocumentVersionVo == null) {
                    throw new KnowledgeDocumentVersionNotFoundException(knowledgeDocumentVersionId);
                }
                if(knowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid(true))){
                    isLcu = true;
                }
            }
        }
        /** 如果当前用户不是成员，但是该版本的作者，可以有查看权限 **/
        if(!isLcu && knowledgeDocumentService.isMember(documentVo.getKnowledgeCircleId()) == 0) {
            throw new PermissionDeniedException();
        }

        if(Objects.equals(isReadOnly, 1)) {
            knowledgeDocumentMapper.updateKnowledgeViewCountIncrementOne(knowledgeDocumentId);
        }
        KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentService.getKnowledgeDocumentDetailByKnowledgeDocumentVersionId(currentVersionId);
        knowledgeDocumentVo.setFavorCount(knowledgeDocumentMapper.getDocumentFavorCount(knowledgeDocumentVo.getId()));
        knowledgeDocumentVo.setCollectCount(knowledgeDocumentMapper.getDocumentCollectCount(knowledgeDocumentVo.getId()));
        knowledgeDocumentVo.setViewCount(knowledgeDocumentMapper.getDocumentViewCount(knowledgeDocumentVo.getId()));
        knowledgeDocumentVo.setIsCollect(knowledgeDocumentMapper.checkDocumentHasBeenCollected(knowledgeDocumentVo.getId(), UserContext.get().getUserUuid(true)));
        knowledgeDocumentVo.setIsFavor(knowledgeDocumentMapper.checkDocumentHasBeenFavored(knowledgeDocumentVo.getId(), UserContext.get().getUserUuid(true)));
        return knowledgeDocumentVo;
    }

}
