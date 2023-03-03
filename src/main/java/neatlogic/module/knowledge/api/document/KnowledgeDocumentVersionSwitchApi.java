package neatlogic.module.knowledge.api.document;

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
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVersionVo;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVo;
import neatlogic.framework.knowledge.exception.*;
import neatlogic.module.knowledge.service.KnowledgeDocumentService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
@Transactional
public class KnowledgeDocumentVersionSwitchApi extends PrivateApiComponentBase {

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;
    
    @Override
    public String getToken() {
        return "knowledge/document/version/switch";
    }

    @Override
    public String getName() {
        return "切换文档版本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
        @Param(name = "knowledgeDocumentVersionId", type = ApiParamType.LONG, isRequired = true, desc = "版本id")
    })
    @Description(desc = "切换文档版本")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long knowledgeDocumentVersionId = jsonObj.getLong("knowledgeDocumentVersionId");
        KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
        if(knowledgeDocumentVersionVo == null) {
            throw new KnowledgeDocumentVersionNotFoundException(knowledgeDocumentVersionId);
        }
        KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentLockById(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        if(knowledgeDocumentVo == null) {
            throw new KnowledgeDocumentNotFoundException(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        }
        if (Objects.equals(knowledgeDocumentVo.getIsDelete(), 1)) {
            throw new KnowledgeDocumentHasBeenDeletedException(knowledgeDocumentVo.getId());
        }
        knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
        if(KnowledgeDocumentVersionStatus.DRAFT.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            throw new KnowledgeDocumentNotHistoricalVersionException(knowledgeDocumentVersionId);
        }else if(KnowledgeDocumentVersionStatus.SUBMITTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            throw new KnowledgeDocumentNotHistoricalVersionException(knowledgeDocumentVersionId);
        }
        else if(KnowledgeDocumentVersionStatus.REJECTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            throw new KnowledgeDocumentNotHistoricalVersionException(knowledgeDocumentVersionId);
        }
        int oldVersion = knowledgeDocumentVo.getVersion();
        if(knowledgeDocumentService.isReviewer(knowledgeDocumentVo.getKnowledgeCircleId()) == 0) {
            throw new KnowledgeDocumentCurrentUserNotReviewerException();
        }

        knowledgeDocumentVo.setKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
        knowledgeDocumentVo.setVersion(knowledgeDocumentVersionVo.getVersion());
        knowledgeDocumentMapper.updateKnowledgeDocumentById(knowledgeDocumentVo);

        JSONObject config = new JSONObject();
        config.put("oldVersion", oldVersion);
        config.put("newVersion", knowledgeDocumentVersionVo.getVersion());
        knowledgeDocumentService.audit(knowledgeDocumentVo.getId(), knowledgeDocumentVersionId, KnowledgeDocumentOperate.SWITCHVERSION, config);
        return null;
    }

}
