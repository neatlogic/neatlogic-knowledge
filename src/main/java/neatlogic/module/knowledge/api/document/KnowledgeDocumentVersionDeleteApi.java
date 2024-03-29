package neatlogic.module.knowledge.api.document;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_BASE;
import neatlogic.framework.knowledge.constvalue.KnowledgeDocumentOperate;
import neatlogic.framework.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentFileVo;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentTagVo;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVersionVo;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVo;
import neatlogic.framework.knowledge.exception.KnowledgeDocumentCurrentUserNotOwnerException;
import neatlogic.framework.knowledge.exception.KnowledgeDocumentCurrentUserNotReviewerException;
import neatlogic.framework.knowledge.exception.KnowledgeDocumentCurrentVersionCannotBeDeletedException;
import neatlogic.framework.knowledge.exception.KnowledgeDocumentDraftSubmittedCannotBeDeletedException;
import neatlogic.module.knowledge.service.KnowledgeDocumentService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.DELETE)
@Transactional
public class KnowledgeDocumentVersionDeleteApi extends PrivateApiComponentBase {

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;
    
    @Override
    public String getToken() {
        return "knowledge/document/version/delete";
    }

    @Override
    public String getName() {
        return "nmkad.knowledgedocumentversiondeleteapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }
    
    @Input({
        @Param(name = "knowledgeDocumentVersionId", type = ApiParamType.LONG, isRequired = true, desc = "common.versionid")
    })
    @Description(desc = "nmkad.knowledgedocumentversiondeleteapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long knowledgeDocumentVersionId = jsonObj.getLong("knowledgeDocumentVersionId");
        KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
        if(knowledgeDocumentVersionVo == null) {
            return null;
        }
        KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentLockById(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        if(knowledgeDocumentVo == null){
            knowledgeDocumentMapper.deleteKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
            knowledgeDocumentMapper.deleteKnowledgeDocumentLineByKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
            knowledgeDocumentMapper.deleteKnowledgeDocumentFileByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentFileVo(knowledgeDocumentVersionVo.getKnowledgeDocumentId(), knowledgeDocumentVersionId));
            knowledgeDocumentMapper.deleteKnowledgeDocumentTagByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentTagVo(knowledgeDocumentVersionVo.getKnowledgeDocumentId(), knowledgeDocumentVersionId));
            return null;
        }
        if(Objects.equals(knowledgeDocumentVo.getKnowledgeDocumentVersionId(), knowledgeDocumentVersionId)) {
            throw new KnowledgeDocumentCurrentVersionCannotBeDeletedException();
        }
        knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
        if(knowledgeDocumentVersionVo == null) {
            return null;
        }
        if(knowledgeDocumentVersionVo.getStatus().equals(KnowledgeDocumentVersionStatus.PASSED.getValue())) {
            if(knowledgeDocumentService.isReviewer(knowledgeDocumentVo.getKnowledgeCircleId()) == 0) {
                throw new KnowledgeDocumentCurrentUserNotReviewerException();
            }
            knowledgeDocumentMapper.updateKnowledgeDocumentVersionToDeleteById(knowledgeDocumentVersionVo.getId());
            JSONObject config = new JSONObject();
            config.put("version", knowledgeDocumentVersionVo.getVersion());
            knowledgeDocumentService.audit(knowledgeDocumentVersionVo.getKnowledgeDocumentId(), knowledgeDocumentVersionId, KnowledgeDocumentOperate.DELETEVERSION, config);
        }else if(knowledgeDocumentVersionVo.getStatus().equals(KnowledgeDocumentVersionStatus.SUBMITTED.getValue())) {
            throw new KnowledgeDocumentDraftSubmittedCannotBeDeletedException();
        }else {
            if(!knowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid(true))) {
                throw new KnowledgeDocumentCurrentUserNotOwnerException();
            }
            knowledgeDocumentMapper.deleteKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
            knowledgeDocumentMapper.deleteKnowledgeDocumentLineByKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
            knowledgeDocumentMapper.deleteKnowledgeDocumentFileByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentFileVo(knowledgeDocumentVersionVo.getKnowledgeDocumentId(), knowledgeDocumentVersionId));
            knowledgeDocumentMapper.deleteKnowledgeDocumentTagByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentTagVo(knowledgeDocumentVersionVo.getKnowledgeDocumentId(), knowledgeDocumentVersionId));
        }

        List<KnowledgeDocumentVersionVo> knowledgeDocumentVersionList = knowledgeDocumentMapper.getKnowledgeDocumentVersionListByKnowledgeDocumentId(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        if (CollectionUtils.isNotEmpty(knowledgeDocumentVersionList)) {
            boolean allVersionIsDelete = true;
            for (KnowledgeDocumentVersionVo knowledgeDocumentVersion : knowledgeDocumentVersionList) {
                if (Objects.equals(knowledgeDocumentVersion.getIsDelete(), 0)) {
                    allVersionIsDelete = false;
                    break;
                }
            }
            if (allVersionIsDelete) {
                // 文档所有版本的is_delete都等于1时，将文档is_delete置1
                knowledgeDocumentMapper.updateKnowledgeDocumentToDeleteById(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
            }
        } else {
            // 文档所有版本都已被删除时，删除文档数据
            knowledgeDocumentMapper.deleteKnowledgeDocumentById(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        }
        return null;
    }

}
