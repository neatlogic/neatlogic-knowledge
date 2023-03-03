package neatlogic.module.knowledge.api.test;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_CIRCLE_MODIFY;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_TEMPLATE_MODIFY;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @Title: TestApi
 * @Package: neatlogic.module.knowledge.api.test
 * @Description: TODO
 * @author: chenqiwei
 * @date: 2021/2/269:24 下午
 **/
@Service
@AuthAction(action = KNOWLEDGE_CIRCLE_MODIFY.class)
@AuthAction(action = KNOWLEDGE_TEMPLATE_MODIFY.class)
@Transactional
public class TestApi extends PrivateApiComponentBase {
    @Resource
    KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Override
    public String getName() {
        return "测试";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        return null;
    }

    @Override
    public String getToken() {
        return "/knows/test";
    }
}
