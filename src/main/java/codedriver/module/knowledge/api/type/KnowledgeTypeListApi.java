package codedriver.module.knowledge.api.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.module.knowledge.constvalue.KnowledgeType;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeTypeVo;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeTypeListApi extends PrivateApiComponentBase {

    private Map<KnowledgeType, Supplier<Integer>> map = new HashMap<>();
    @PostConstruct
    public void init() {
        map.put(KnowledgeType.ALL, () -> {
            List<String> teamUuidList = teamMapper.getTeamUuidListByUserUuid(UserContext.get().getUserUuid());
            return knowledgeDocumentMapper.getCurrentUserKnowledgeDocumentCount(UserContext.get().getUserUuid(), teamUuidList, UserContext.get().getRoleUuidList());
        });
        map.put(KnowledgeType.WAITINGFORREVIEW, () -> {
            KnowledgeDocumentVersionVo searchVo = new KnowledgeDocumentVersionVo();
            searchVo.setReviewer(UserContext.get().getUserUuid(true));
            return knowledgeDocumentMapper.getKnowledgeDocumentWaitingForReviewCount(searchVo);
        });
        map.put(KnowledgeType.SHARE, () -> {
            KnowledgeDocumentVersionVo searchVo = new KnowledgeDocumentVersionVo();
            searchVo.setLcu(UserContext.get().getUserUuid(true));
            List<String> statusList = Arrays.asList(KnowledgeDocumentVersionStatus.PASSED.getValue(), KnowledgeDocumentVersionStatus.REJECTED.getValue(), KnowledgeDocumentVersionStatus.SUBMITTED.getValue());
            searchVo.setStatusList(statusList);
            return knowledgeDocumentMapper.getKnowledgeDocumentVersionMyVersionCount(searchVo);
        });
        map.put(KnowledgeType.FAVORITES, () -> {
            //TODO linbq后面补做
            return 0;
        });
        map.put(KnowledgeType.DRAFT, () -> {
            KnowledgeDocumentVersionVo searchVo = new KnowledgeDocumentVersionVo();
            searchVo.setLcu(UserContext.get().getUserUuid(true));
            List<String> statusList = Arrays.asList(KnowledgeDocumentVersionStatus.DRAFT.getValue(), KnowledgeDocumentVersionStatus.EXPIRED.getValue());
            searchVo.setStatusList(statusList);
            return knowledgeDocumentMapper.getKnowledgeDocumentVersionMyVersionCount(searchVo);
        });
    }
    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    
    @Autowired
    private TeamMapper teamMapper;

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
        int isReviewable = knowledgeDocumentMapper.checkUserIsApprover(UserContext.get().getUserUuid(true), null);
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
