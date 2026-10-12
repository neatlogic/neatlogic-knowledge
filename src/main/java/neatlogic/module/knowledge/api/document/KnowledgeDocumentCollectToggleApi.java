package neatlogic.module.knowledge.api.document;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_BASE;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.exception.KnowledgeDocumentNotFoundException;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class KnowledgeDocumentCollectToggleApi extends PrivateApiComponentBase {

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    private UserMapper userMapper;
    
    @Override
    public String getToken() {
        return "knowledge/document/collect/toggle";
    }

    @Override
    public String getName() {
        return "收藏或取消收藏文档";
    }

    @Override
    public String getConfig() {
        return null;
    }
    
    @Input({
            @Param(name = "documentId", type = ApiParamType.LONG, isRequired = true, desc = "文档id"),
            @Param(name = "isCollect", type = ApiParamType.ENUM, rule = "0,1", desc = "是否收藏"),
    })
    @Output({
            @Param(name = "count", type = ApiParamType.INTEGER, desc = "文档收藏数"),
            @Param(name = "isCollect", type = ApiParamType.ENUM, rule = "0,1", desc = "是否收藏"),
            @Param(name = "userNameList", type = ApiParamType.JSONARRAY, desc = "用户名列表"),
    })
    @Description(desc = "收藏或取消收藏文档")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long documentId = jsonObj.getLong("documentId");
        if(knowledgeDocumentMapper.getKnowledgeDocumentLockById(documentId) == null){
            throw new KnowledgeDocumentNotFoundException(documentId);
        }
        Integer isCollect = jsonObj.getInteger("isCollect");
        if (isCollect != null) {
            if (isCollect == 0) {
                knowledgeDocumentMapper.deleteKnowledgeDocumentCollect(documentId, UserContext.get().getUserUuid());
                result.put("isCollect", 0);
            } else {
                if (knowledgeDocumentMapper.checkDocumentHasBeenCollected(documentId,UserContext.get().getUserUuid()) == 0){
                    knowledgeDocumentMapper.insertKnowledgeDocumentCollect(documentId, UserContext.get().getUserUuid());
                }
                result.put("isCollect", 1);
            }
        } else {
            result.put("isCollect", knowledgeDocumentMapper.checkDocumentHasBeenCollected(documentId,UserContext.get().getUserUuid()));
        }
        int collectCount = knowledgeDocumentMapper.getDocumentCollectCount(documentId);
        result.put("count",collectCount);
        result.put("userNameList",new ArrayList<>());
        if (collectCount > 0) {
            List<String> userUuidList = knowledgeDocumentMapper.getDocumentCollectUserUuidList(documentId);
            if (CollectionUtils.isNotEmpty(userUuidList)) {
                List<UserVo> userList = userMapper.getUserByUserUuidList(userUuidList);
                Map<String, UserVo> userMap = userList.stream().collect(Collectors.toMap(UserVo::getUuid, e -> e));
                List<String> userNameList = new ArrayList<>();
                for (String userUuid : userUuidList) {
                    UserVo userVo = userMap.get(userUuid);
                    if (userVo != null) {
                        userNameList.add(userVo.getUserName());
                    }
                }
                result.put("userNameList",userNameList);
            }
        }
        return result;
    }

}
