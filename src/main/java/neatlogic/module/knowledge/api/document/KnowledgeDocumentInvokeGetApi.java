package neatlogic.module.knowledge.api.document;

import java.util.List;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_BASE;
import neatlogic.module.knowledge.service.KnowledgeDocumentService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeCircleMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import neatlogic.framework.knowledge.dto.KnowledgeCircleVo;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentInvokeVo;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentTypeVo;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVersionVo;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVo;
import neatlogic.framework.knowledge.exception.KnowledgeDocumentNotFoundException;
import neatlogic.framework.knowledge.exception.KnowledgeDocumentVersionNotFoundException;

import javax.annotation.Resource;

@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentInvokeGetApi extends PrivateApiComponentBase {

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;
    @Resource
    private KnowledgeCircleMapper knowledgeCircleMapper;
    @Resource
    private KnowledgeDocumentTypeMapper knowledgeDocumentTypeMappper;

    @Override
    public String getToken() {
        return "knowledge/document/invoke/get";
    }

    @Override
    public String getName() {
        return "根据调用者查询关联文档信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
        @Param(name = "invokeId", type = ApiParamType.LONG, isRequired = true, desc = "调用者id"),
        @Param(name = "source", type = ApiParamType.STRING, isRequired = true, desc = "来源")
    })
    @Output({
        @Param(name = "isTransferKnowledge", type = ApiParamType.INTEGER, desc = "是否有权限"),
        @Param(name = "knowledgeDocumentVersion", explode = KnowledgeDocumentVersionVo.class, desc = "知识文档版本信息")
    })
    @Description(desc = "根据调用者查询关联文档信息")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        Long invokeId = jsonObj.getLong("invokeId");
        String source = jsonObj.getString("source");
        KnowledgeDocumentInvokeVo knowledgeDocumentInvokeVo = new KnowledgeDocumentInvokeVo(invokeId, source);
        Long knowledgeDocumentId = knowledgeDocumentMapper.getKnowledgeDocumentIdByInvokeIdAndSource(knowledgeDocumentInvokeVo);
        if(knowledgeDocumentId != null) {
            resultObj.put("isTransferKnowledge", 0);
            KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(knowledgeDocumentId);
            if(knowledgeDocumentVo == null) {
                throw new KnowledgeDocumentNotFoundException(knowledgeDocumentId);
            }
            KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = null;
            if(knowledgeDocumentVo.getKnowledgeDocumentVersionId() != null) {
                knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVo.getKnowledgeDocumentVersionId());
                if(knowledgeDocumentVersionVo == null) {
                    throw new KnowledgeDocumentVersionNotFoundException(knowledgeDocumentVo.getKnowledgeDocumentVersionId());
                }
            }else {
                knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionByknowledgeDocumentIdLimitOne(knowledgeDocumentId);
            }
            if(knowledgeDocumentVersionVo != null) {
                knowledgeDocumentVersionVo.setTitle(knowledgeDocumentVo.getTitle());
                KnowledgeCircleVo knowledgeCircleVo = knowledgeCircleMapper.getKnowledgeCircleById(knowledgeDocumentVo.getKnowledgeCircleId());
                if(knowledgeCircleVo != null) {
                    knowledgeDocumentVersionVo.getPath().add(knowledgeCircleVo.getName());
                }
                KnowledgeDocumentTypeVo knowledgeDocumentTypeVo = knowledgeDocumentTypeMappper.getTypeByUuid(knowledgeDocumentVo.getKnowledgeDocumentTypeUuid());
                if(knowledgeDocumentTypeVo != null) {
                    List<String> typeNameList = knowledgeDocumentTypeMappper.getAncestorsAndSelfNameByLftRht(knowledgeDocumentTypeVo.getLft(), knowledgeDocumentTypeVo.getRht(), knowledgeDocumentTypeVo.getKnowledgeCircleId());
                    if(CollectionUtils.isNotEmpty(typeNameList)) {
                        knowledgeDocumentVersionVo.getPath().addAll(typeNameList);
                    }
                }
                resultObj.put("knowledgeDocumentVersion", knowledgeDocumentVersionVo);               
            }
        }else {
            if(knowledgeDocumentService.isMember(null) == 0) {
                resultObj.put("isTransferKnowledge", 0);
            }else {
                resultObj.put("isTransferKnowledge", 1);
            }
        }
        return resultObj;
    }

}
