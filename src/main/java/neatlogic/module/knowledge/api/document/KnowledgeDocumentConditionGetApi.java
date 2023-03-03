package neatlogic.module.knowledge.api.document;

import java.util.Arrays;
import java.util.List;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_BASE;
import org.springframework.beans.factory.annotation.Autowired;
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
import neatlogic.module.knowledge.condition.KnowledgeConditionBuilder;
import neatlogic.framework.knowledge.constvalue.KnowledgeType;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentConditionGetApi extends PrivateApiComponentBase {

    @Autowired
    KnowledgeDocumentMapper knowledgeDocumentMapper;
    
    @Override
    public String getToken() {
        return "knowledge/document/condition/get";
    }

    @Override
    public String getName() {
        return "获取知识搜索条件";
    }

    @Override
    public String getConfig() {
        return null;
    }
    

    @Input({
        @Param(name = "knowledgeType", type = ApiParamType.ENUM, rule = "all,waitingforreview,share,collect,draft",isRequired = true, desc = "知识类型")
    })
    
    @Output({
        @Param(name = "handler", type = ApiParamType.STRING, desc = "条件"),
        @Param(name = "handlerName", type = ApiParamType.STRING, desc = "条件名"),
        @Param(name = "handlerType", type = ApiParamType.STRING, desc = "控件类型 select|input|radio|userselect|date|area|time"),
        @Param(name = "config", type = ApiParamType.STRING, desc = "控件初始化配置"),
        @Param(name = "sort", type = ApiParamType.INTEGER, desc = "条件排序")
    })
    
    @Description(desc = "获取知识搜索条件")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String knowledgeType = jsonObj.getString("knowledgeType");
        List<String> typelist = Arrays.asList(KnowledgeType.ALL.getValue(),KnowledgeType.WAITINGFORREVIEW.getValue(),KnowledgeType.SHARE.getValue(),KnowledgeType.COLLECT.getValue());
        if(!typelist.contains(knowledgeType)) {
            return null;
        }
        KnowledgeConditionBuilder conditionBuilder = new KnowledgeConditionBuilder();
        conditionBuilder.setLcu(knowledgeType);
        conditionBuilder.setLcd(knowledgeType);
        conditionBuilder.setTag();
        conditionBuilder.setSource();
        conditionBuilder.setReviewDate(knowledgeType);
        conditionBuilder.setReviewer(knowledgeType);
        conditionBuilder.setReviewStatus(knowledgeType);
        return conditionBuilder.build();
    }  
    
}
