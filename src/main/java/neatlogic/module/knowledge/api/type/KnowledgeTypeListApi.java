package neatlogic.module.knowledge.api.type;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.GroupSearch;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_BASE;
import neatlogic.framework.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import neatlogic.framework.knowledge.constvalue.KnowledgeType;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVersionVo;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVo;
import neatlogic.framework.knowledge.dto.KnowledgeTypeVo;
import neatlogic.module.knowledge.service.KnowledgeDocumentService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.function.Supplier;

@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeTypeListApi extends PrivateApiComponentBase {
    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    private final Map<KnowledgeType, Supplier<Integer>> map = new HashMap<>();
    @PostConstruct
    public void init() {
        map.put(KnowledgeType.ALL, () -> {
            KnowledgeDocumentVo documentVoParam = new KnowledgeDocumentVo();
            String userUuid = UserContext.get().getUserUuid(true);
            documentVoParam.setCircleUserUuid(userUuid);
            AuthenticationInfoVo authenticationInfoVo = UserContext.get().getAuthenticationInfoVo();
            documentVoParam.setCircleTeamUuidList(authenticationInfoVo.getTeamUuidList());
            documentVoParam.setCircleRoleUuidList(authenticationInfoVo.getRoleUuidList());
            documentVoParam.setStatusList(Collections.singletonList(KnowledgeDocumentVersionStatus.PASSED.getValue()));
            return knowledgeDocumentMapper.searchKnowledgeDocumentIdCount(documentVoParam);
        });
        map.put(KnowledgeType.WAITINGFORREVIEW, () -> {
            KnowledgeDocumentVersionVo documentVersionVoParam = new KnowledgeDocumentVersionVo();
            documentVersionVoParam.setStatusList(Collections.singletonList(KnowledgeDocumentVersionStatus.SUBMITTED.getValue()));
            documentVersionVoParam.setReviewerList(Collections.singletonList(GroupSearch.USER.getValuePlugin()+UserContext.get().getUserUuid(true)));
            knowledgeDocumentService.getReviewerParam(documentVersionVoParam);
            return knowledgeDocumentMapper.searchKnowledgeDocumentVersionIdCount(documentVersionVoParam);
        });
        map.put(KnowledgeType.SHARE, () -> {
            KnowledgeDocumentVersionVo documentVersionVoParam = new KnowledgeDocumentVersionVo();
            documentVersionVoParam.setStatusList(Collections.singletonList(KnowledgeDocumentVersionStatus.ALL.getValue()));
            documentVersionVoParam.setLcuList(Collections.singletonList(GroupSearch.USER.getValuePlugin()+UserContext.get().getUserUuid(true)));
            knowledgeDocumentService.getReviewerParam(documentVersionVoParam);
            return knowledgeDocumentMapper.getMyAllReviewKnowledgeDocumentVersionCount(documentVersionVoParam);
        });
        map.put(KnowledgeType.COLLECT, () -> {
            KnowledgeDocumentVo documentVoParam = new KnowledgeDocumentVo();
            String userUuid = UserContext.get().getUserUuid(true);
            documentVoParam.setCircleUserUuid(userUuid);
            AuthenticationInfoVo authenticationInfoVo = UserContext.get().getAuthenticationInfoVo();
            documentVoParam.setCircleTeamUuidList(authenticationInfoVo.getTeamUuidList());
            documentVoParam.setCircleRoleUuidList(authenticationInfoVo.getRoleUuidList());
            documentVoParam.setStatusList(Collections.singletonList(KnowledgeDocumentVersionStatus.PASSED.getValue()));
            documentVoParam.setCollector(userUuid);
            return knowledgeDocumentMapper.searchKnowledgeDocumentIdCount(documentVoParam);
        });
        map.put(KnowledgeType.DRAFT, () -> {
            KnowledgeDocumentVersionVo documentVersionVoParam = new KnowledgeDocumentVersionVo();
            documentVersionVoParam.setStatusList(Collections.singletonList(KnowledgeDocumentVersionStatus.DRAFT.getValue()));
            documentVersionVoParam.setLcuList(Collections.singletonList(GroupSearch.USER.getValuePlugin()+UserContext.get().getUserUuid(true)));
            knowledgeDocumentService.getReviewerParam(documentVersionVoParam);
            return knowledgeDocumentMapper.searchKnowledgeDocumentVersionIdCount(documentVersionVoParam);
        });
    }
    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Override
    public String getToken() {
        return "knowledge/type/list";
    }

    @Override
    public String getName() {
        return "nmkat.knowledgetypelistapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Output({
        @Param(explode = KnowledgeTypeVo[].class, desc = "common.tbodylist")
    })
    @Description(desc = "nmkat.knowledgetypelistapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        int isReviewable = knowledgeDocumentService.isReviewer(null);
        List<KnowledgeTypeVo> resultList = new ArrayList<>();
        for(KnowledgeType type : KnowledgeType.values()) {
            if(KnowledgeType.WAITINGFORREVIEW == type && isReviewable == 0) {
                continue;
            }
            KnowledgeTypeVo knowledgeTypeVo = new KnowledgeTypeVo(type);
            knowledgeTypeVo.setCount(map.computeIfAbsent(type, k -> () -> 0).get());
            resultList.add(knowledgeTypeVo);
        }
        return resultList;
    }

}
