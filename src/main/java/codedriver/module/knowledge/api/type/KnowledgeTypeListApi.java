package codedriver.module.knowledge.api.type;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.module.knowledge.constvalue.KnowledgeType;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.dto.KnowledgeTypeVo;
import codedriver.module.knowledge.service.KnowledgeDocumentService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.function.Supplier;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeTypeListApi extends PrivateApiComponentBase {
    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

    private final Map<KnowledgeType, Supplier<Integer>> map = new HashMap<>();
    @PostConstruct
    public void init() {
        map.put(KnowledgeType.ALL, () -> {
            KnowledgeDocumentVo documentVoParam = new KnowledgeDocumentVo();
            String userUuid = UserContext.get().getUserUuid(true);
            documentVoParam.setCircleUserUuid(userUuid);
            documentVoParam.setCircleTeamUuidList(teamMapper.getTeamUuidListByUserUuid(userUuid));
            documentVoParam.setCircleRoleUuidList(userMapper.getRoleUuidListByUserUuid(userUuid));
            documentVoParam.setStatusList(Collections.singletonList(KnowledgeDocumentVersionStatus.PASSED.getValue()));
            return knowledgeDocumentMapper.getKnowledgeDocumentCount(documentVoParam);
        });
        map.put(KnowledgeType.WAITINGFORREVIEW, () -> {
            KnowledgeDocumentVersionVo documentVersionVoParam = new KnowledgeDocumentVersionVo();
            documentVersionVoParam.setStatusList(Collections.singletonList(KnowledgeDocumentVersionStatus.SUBMITTED.getValue()));
            documentVersionVoParam.setReviewerList(Collections.singletonList(GroupSearch.USER.getValuePlugin()+UserContext.get().getUserUuid(true)));
            knowledgeDocumentService.getReviewerParam(documentVersionVoParam);
            return knowledgeDocumentMapper.getKnowledgeDocumentVersionCount(documentVersionVoParam);
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
            documentVoParam.setCircleTeamUuidList(teamMapper.getTeamUuidListByUserUuid(userUuid));
            documentVoParam.setCircleRoleUuidList(userMapper.getRoleUuidListByUserUuid(userUuid));
            documentVoParam.setStatusList(Collections.singletonList(KnowledgeDocumentVersionStatus.PASSED.getValue()));
            documentVoParam.setCollector(userUuid);
            return knowledgeDocumentMapper.getKnowledgeDocumentCount(documentVoParam);
        });
        map.put(KnowledgeType.DRAFT, () -> {
            KnowledgeDocumentVersionVo documentVersionVoParam = new KnowledgeDocumentVersionVo();
            documentVersionVoParam.setStatusList(Collections.singletonList(KnowledgeDocumentVersionStatus.DRAFT.getValue()));
            documentVersionVoParam.setLcuList(Collections.singletonList(GroupSearch.USER.getValuePlugin()+UserContext.get().getUserUuid(true)));
            knowledgeDocumentService.getReviewerParam(documentVersionVoParam);
            return knowledgeDocumentMapper.getKnowledgeDocumentVersionCount(documentVersionVoParam);
        });
    }
    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Autowired
    TeamMapper teamMapper;

    @Autowired
    UserMapper userMapper;

    @Override
    public String getToken() {
        return "knowledge/type/list";
    }

    @Override
    public String getName() {
        return "查询知识分类列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Output({
        @Param(explode = KnowledgeTypeVo[].class, desc = "知识分类列表")
    })
    @Description(desc = "查询知识分类列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<String> teamUuidList = teamMapper.getTeamUuidListByUserUuid(UserContext.get().getUserUuid(true));
        int isReviewable = knowledgeDocumentMapper.checkUserIsApprover(null, UserContext.get().getUserUuid(true), teamUuidList, UserContext.get().getRoleUuidList());
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
