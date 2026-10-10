package neatlogic.module.knowledge.api.document;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
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
public class KnowledgeDocumentFavorToggleApi extends PrivateApiComponentBase {

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    private UserMapper userMapper;
    
    @Override
    public String getToken() {
        return "knowledge/document/favor/toggle";
    }

    @Override
    public String getName() {
        return "点赞或取消点赞文档";
    }

    @Override
    public String getConfig() {
        return null;
    }
    
    @Input({
            @Param(name = "documentId", type = ApiParamType.LONG, isRequired = true, desc = "文档id"),
            @Param(name = "isFavor", type = ApiParamType.ENUM, rule = "0,1", desc = "是否点赞"),
    })
    @Output({
            @Param(name = "count", type = ApiParamType.INTEGER, desc = "文档点赞数"),
            @Param(name = "isFavor", type = ApiParamType.ENUM, rule = "0,1", desc = "是否点赞"),
            @Param(name = "userNameList", type = ApiParamType.JSONARRAY, desc = "用户名列表"),
    })
    @Description(desc = "点赞或取消点赞文档")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long documentId = jsonObj.getLong("documentId");
        if(knowledgeDocumentMapper.getKnowledgeDocumentLockById(documentId) == null){
            throw new KnowledgeDocumentNotFoundException(documentId);
        }
        Integer isFavor = jsonObj.getInteger("isFavor");
        if (isFavor != null) {
            if (isFavor == 0) {
                knowledgeDocumentMapper.deleteKnowledgeDocumentFavor(documentId, UserContext.get().getUserUuid());
                result.put("isFavor", 0);
            } else {
                if(knowledgeDocumentMapper.checkDocumentHasBeenFavored(documentId,UserContext.get().getUserUuid()) == 0){
                    knowledgeDocumentMapper.insertKnowledgeDocumentFavor(documentId, UserContext.get().getUserUuid());
                }
                result.put("isFavor", 1);
            }
        } else {
            result.put("isFavor", knowledgeDocumentMapper.checkDocumentHasBeenFavored(documentId,UserContext.get().getUserUuid()));
        }
        int favorCount = knowledgeDocumentMapper.getDocumentFavorCount(documentId);
        result.put("count",favorCount);
        if (favorCount > 0) {
            List<String> userUuidList = knowledgeDocumentMapper.getDocumentFavorUserUuidList(documentId);
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
