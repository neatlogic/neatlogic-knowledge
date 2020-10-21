package codedriver.module.knowledge.api.type;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dao.mapper.KnowledgeCircleMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeCircleUserVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeTypeVo2;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeTypeListApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    
    @Autowired
    private KnowledgeCircleMapper knowledgeCircleMapper;

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
        @Param(explode = KnowledgeTypeVo2[].class, desc = "知识分类列表")
    })
    @Description(desc = "查询知识分类列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
//        int isReviewable = 0;
//        List<KnowledgeCircleUserVo> knowledgeCircleUserList = knowledgeCircleMapper.getKnowledgeCircleUserListByIdAndAuthType(knowledgeDocumentVo.getKnowledgeCircleId(), KnowledgeCircleUserVo.AuthType.APPROVER.getValue());
//        for(KnowledgeCircleUserVo knowledgeCircleUserVo : knowledgeCircleUserList) {
//            if(GroupSearch.USER.getValue().equals(knowledgeCircleUserVo.getType())) {
//               if(UserContext.get().getUserUuid(true).equals(knowledgeCircleUserVo.getUuid())) {
//                   isReviewable = 1;
//               }
//            }
//        }
//        
//        int count = knowledgeDocumentMapper.getKnowledgeDocumentVersionMyVersionCount(new KnowledgeDocumentVersionVo());
        return null;
    }

}
