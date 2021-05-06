package codedriver.module.knowledge.api.document;

import java.util.ArrayList;
import java.util.List;

import codedriver.framework.auth.core.AuthAction;
import codedriver.module.knowledge.auth.label.KNOWLEDGE_BASE;
import codedriver.module.knowledge.service.KnowledgeDocumentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.UserVo;
import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.FreemarkerUtil;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentOperate;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentAuditMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentAuditVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotFoundException;

import javax.annotation.Resource;

@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentAuditListApi extends PrivateApiComponentBase {

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    private KnowledgeDocumentService knowledgeDocumentSerice;

    @Resource
    private KnowledgeDocumentAuditMapper knowledgeDocumentAuditMapper;
    
    @Resource
    private UserMapper userMapper;

    @Override
    public String getToken() {
        return "knowledge/document/audit/list";
    }

    @Override
    public String getName() {
        return "知识库文档活动列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
        @Param(name = "knowledgeDocumentId", type = ApiParamType.LONG, isRequired = true, desc = "文档id"),
        @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true"),
        @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页条目"),
        @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页")
    })
    @Output({
        @Param(explode = BasePageVo.class),
        @Param(name = "list", explode = KnowledgeDocumentAuditVo[].class, desc = "文档版本列表")
    })
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        resultObj.put("list", new ArrayList<>());
        KnowledgeDocumentAuditVo searchVo = JSON.toJavaObject(jsonObj, KnowledgeDocumentAuditVo.class);
        KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(searchVo.getKnowledgeDocumentId());
        if(knowledgeDocumentVo == null) {
            throw new KnowledgeDocumentNotFoundException(searchVo.getKnowledgeDocumentId());
        }
        if(knowledgeDocumentSerice.isMember(knowledgeDocumentVo.getKnowledgeCircleId()) == 0) {
            throw new PermissionDeniedException();
        }
        int pageCount = 0;
        if(searchVo.getNeedPage()) {
            int rowNum = knowledgeDocumentAuditMapper.getKnowledgeDocumentAuditCountByKnowledgeDocumentId(searchVo);
            pageCount = PageUtil.getPageCount(rowNum, searchVo.getPageSize());
            resultObj.put("currentPage", searchVo.getCurrentPage());
            resultObj.put("pageSize", searchVo.getPageSize());
            resultObj.put("pageCount", pageCount);
            resultObj.put("rowNum", rowNum);
        }
        if(!searchVo.getNeedPage() || searchVo.getCurrentPage() <= pageCount) {
            List<KnowledgeDocumentAuditVo> knowledgeDocumentAuditList = knowledgeDocumentAuditMapper.getKnowledgeDocumentAuditListByKnowledgeDocumentId(searchVo);
            for(KnowledgeDocumentAuditVo knowledgeDocumentAuditVo : knowledgeDocumentAuditList) {
                if(StringUtils.isNotBlank(knowledgeDocumentAuditVo.getFcu())) {
                    UserVo userVo = userMapper.getUserBaseInfoByUuid(knowledgeDocumentAuditVo.getFcu());
                    //使用新对象，防止缓存
                    UserVo vo = new UserVo();
                    BeanUtils.copyProperties(userVo,vo);
                    knowledgeDocumentAuditVo.setFcuVo(vo);
//                    knowledgeDocumentAuditVo.setFcuName(userVo.getUserName());
//                    knowledgeDocumentAuditVo.setFcuInfo(userVo.getUserInfo());
//                    knowledgeDocumentAuditVo.setFcuVipLevel(userVo.getVipLevel());
                }
                String title = KnowledgeDocumentOperate.getTitle(knowledgeDocumentAuditVo.getOperate());
                if(StringUtils.isNotBlank(knowledgeDocumentAuditVo.getConfigHash())) {
                    String configStr = knowledgeDocumentAuditMapper.getKnowledgeDocumentAuditDetailStringByHash(knowledgeDocumentAuditVo.getConfigHash());
                    if(StringUtils.isNotBlank(configStr)) {
                        JSONObject config = JSON.parseObject(configStr);
                        String content = config.getString("content");
                        if(StringUtils.isNotBlank(content)) {
                            knowledgeDocumentAuditVo.setContent(content);
                        }                       
                        if(KnowledgeDocumentOperate.isNeedReplaceParam(knowledgeDocumentAuditVo.getOperate())) {
                            title = FreemarkerUtil.transform(config, title);
                        }
                    }
                }
                knowledgeDocumentAuditVo.setTitle(title);
            }
            resultObj.put("list", knowledgeDocumentAuditList);
        }
        return resultObj;
    }

}
