package codedriver.module.knowledge.api.document;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.module.knowledge.dao.mapper.KnowledgeCircleMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeCircleUserVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.exception.KnowledgeDocumentDraftStatusException;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotFoundException;
import codedriver.module.knowledge.exception.KnowledgeDocumentVersionNotFoundException;
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
@Transactional
public class KnowledgeDocumentDraftSubmitApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Autowired
    private KnowledgeCircleMapper knowledgeCircleMapper;

    @Override
    public String getToken() {
        return "knowledge/document/draft/submit";
    }

    @Override
    public String getName() {
        return "提交审核文档草稿";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
        @Param(name = "knowledgeDocumentVersionId", type = ApiParamType.LONG, isRequired = true, desc = "版本id")
    })
    @Output({
        @Param(name = "isReviewable", type = ApiParamType.ENUM, rule = "0,1", desc = "返回1代表当前用户有审核权限，0代表当前用户没有审核权限")
    })
    @Description(desc = "提交审核文档草稿")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long knowledgeDocumentVersionId = jsonObj.getLong("knowledgeDocumentVersionId");
        KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
        if(knowledgeDocumentVersionVo == null) {
            throw new KnowledgeDocumentVersionNotFoundException(knowledgeDocumentVersionId);
        }
        knowledgeDocumentMapper.getKnowledgeDocumentLockById(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        if(knowledgeDocumentVo == null) {
            throw new KnowledgeDocumentNotFoundException(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        }
        knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
        if(KnowledgeDocumentVersionStatus.PASSED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            throw new KnowledgeDocumentDraftStatusException(knowledgeDocumentVersionId, KnowledgeDocumentVersionStatus.PASSED, "不能再提交审核");
        }else if(KnowledgeDocumentVersionStatus.SUBMITED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            throw new KnowledgeDocumentDraftStatusException(knowledgeDocumentVersionId, KnowledgeDocumentVersionStatus.SUBMITED, "不能再提交审核");
        }else if(KnowledgeDocumentVersionStatus.EXPIRED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            throw new KnowledgeDocumentDraftStatusException(knowledgeDocumentVersionId, KnowledgeDocumentVersionStatus.EXPIRED, "不能再提交审核，请在新版本上修改再提交");
        }else if(KnowledgeDocumentVersionStatus.REJECTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            throw new KnowledgeDocumentDraftStatusException(knowledgeDocumentVersionId, KnowledgeDocumentVersionStatus.REJECTED, "不能再提交审核，请修改后再提交");
        }
        KnowledgeDocumentVersionVo updateStatusVo = new KnowledgeDocumentVersionVo();
        updateStatusVo.setId(knowledgeDocumentVersionId);
        updateStatusVo.setStatus(KnowledgeDocumentVersionStatus.SUBMITED.getValue());
        knowledgeDocumentMapper.updateKnowledgeDocumentVersionById(updateStatusVo);
        
        int isReviewable = 0;
        List<KnowledgeCircleUserVo> knowledgeCircleUserList = knowledgeCircleMapper.getKnowledgeCircleUserListByIdAndAuthType(knowledgeDocumentVo.getKnowledgeCircleId(), KnowledgeCircleUserVo.AuthType.APPROVER.getValue());
        for(KnowledgeCircleUserVo knowledgeCircleUserVo : knowledgeCircleUserList) {
            if(GroupSearch.USER.getValue().equals(knowledgeCircleUserVo.getType())) {
               if(UserContext.get().getUserUuid(true).equals(knowledgeCircleUserVo.getUuid())) {
                   isReviewable = 1;
               }
            }
        }
        JSONObject resultObj = new JSONObject();
        resultObj.put("isReviewable", isReviewable);
        return resultObj;
    }

}