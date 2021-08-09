package codedriver.module.knowledge.api.document;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import codedriver.framework.auth.core.AuthAction;
import codedriver.module.knowledge.auth.label.KNOWLEDGE_BASE;
import codedriver.module.knowledge.service.KnowledgeDocumentService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.UserVo;
import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.framework.knowledge.dto.KnowledgeDocumentHistoricalVersionVo;
import codedriver.framework.knowledge.dto.KnowledgeDocumentVo;
import codedriver.framework.knowledge.exception.KnowledgeDocumentNotFoundException;

import javax.annotation.Resource;

@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentHistoricalVersionListApi extends PrivateApiComponentBase {

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;
    @Resource
    private UserMapper userMapper;

    @Override
    public String getToken() {
        return "knowledge/document/historicalversion/list";
    }

    @Override
    public String getName() {
        return "查询历史版本列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
        @Param(name = "knowledgeDocumentId", type = ApiParamType.LONG, isRequired = true, desc = "文档id")
    })
    @Output({
        @Param(explode = BasePageVo.class),
        @Param(name = "currentVersion", explode = KnowledgeDocumentHistoricalVersionVo.class, desc = "文档当前版本"),
        @Param(name = "historicalVersionList", explode = KnowledgeDocumentHistoricalVersionVo[].class, desc = "文档历史版本列表")
    })
    @Description(desc = "查询历史版本列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        Long knowledgeDocumentId = jsonObj.getLong("knowledgeDocumentId");
        KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(knowledgeDocumentId);
        if(knowledgeDocumentVo == null) {
            throw new KnowledgeDocumentNotFoundException(knowledgeDocumentId);
        }
        if(knowledgeDocumentService.isMember(knowledgeDocumentVo.getKnowledgeCircleId()) == 0) {
            throw new PermissionDeniedException();
        }
        int isReviewable = knowledgeDocumentService.isReviewer(knowledgeDocumentVo.getKnowledgeCircleId());
        List<KnowledgeDocumentHistoricalVersionVo> historicalVersionList = knowledgeDocumentMapper.getKnowledgeDocumentHistorialVersionListByKnowledgeDocumentId(knowledgeDocumentId);
        Iterator<KnowledgeDocumentHistoricalVersionVo> iterator = historicalVersionList.iterator();
        while(iterator.hasNext()) {
            KnowledgeDocumentHistoricalVersionVo item = iterator.next();
            if(Objects.equals(item.getId(), knowledgeDocumentVo.getKnowledgeDocumentVersionId())) {
                iterator.remove();
                resultObj.put("currentVersion", item);
                item.setIsDeletable(0);
                item.setIsSwitchable(0);
            }else {
                item.setIsDeletable(isReviewable);
                item.setIsSwitchable(isReviewable);
            }
            UserVo userVo = userMapper.getUserBaseInfoByUuid(item.getLcu());
            if(userVo != null) {
//                item.setLcuName(userVo.getUserName());
                //使用新对象，防止缓存
                UserVo vo = new UserVo();
                BeanUtils.copyProperties(userVo,vo);
                item.setLcuVo(vo);
            }
        }
        resultObj.put("historicalVersionList", historicalVersionList);        
        return resultObj;
    }

}
