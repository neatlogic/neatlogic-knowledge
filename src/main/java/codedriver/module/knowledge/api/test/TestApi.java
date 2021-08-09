package codedriver.module.knowledge.api.test;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.auth.label.KNOWLEDGE_CIRCLE_MODIFY;
import codedriver.module.knowledge.auth.label.KNOWLEDGE_TEMPLATE_MODIFY;
import codedriver.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @Title: TestApi
 * @Package: codedriver.module.knowledge.api.test
 * @Description: TODO
 * @author: chenqiwei
 * @date: 2021/2/269:24 下午
 * Copyright(c) 2021 TechSure Co.,Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
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
