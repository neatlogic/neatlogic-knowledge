package codedriver.module.knowledge.api.document;

import java.util.List;

import codedriver.framework.auth.core.AuthAction;
import codedriver.module.knowledge.auth.label.KNOWLEDGE_BASE;
import codedriver.module.knowledge.service.KnowledgeDocumentService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.UserVo;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dao.mapper.KnowledgeCircleMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import codedriver.module.knowledge.dto.KnowledgeCircleVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentInvokeVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentTypeVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotFoundException;
import codedriver.module.knowledge.exception.KnowledgeDocumentVersionNotFoundException;

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
    private UserMapper userMapper;   
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
                if(StringUtils.isNotBlank(knowledgeDocumentVersionVo.getLcu())) {
                    UserVo userVo = userMapper.getUserBaseInfoByUuid(knowledgeDocumentVersionVo.getLcu());
                    if(userVo != null) {
                        UserVo vo = new UserVo();
                        BeanUtils.copyProperties(userVo,vo);
                        knowledgeDocumentVersionVo.setLcuVo(vo);
//                        knowledgeDocumentVersionVo.setLcuName(userVo.getUserName());
//                        knowledgeDocumentVersionVo.setLcuInfo(userVo.getUserInfo());
//                        knowledgeDocumentVersionVo.setLcuVipLevel(userVo.getVipLevel());
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
