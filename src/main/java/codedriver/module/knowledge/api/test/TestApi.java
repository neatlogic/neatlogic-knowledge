package codedriver.module.knowledge.api.test;

import codedriver.framework.fulltextindex.core.FullTextIndexHandlerFactory;
import codedriver.framework.fulltextindex.core.IFullTextIndexHandler;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.fulltextindex.FullTextIndexType;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional
public class TestApi extends PrivateApiComponentBase {
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
        //创建全文检索索引
        IFullTextIndexHandler handler = FullTextIndexHandlerFactory.getComponent(FullTextIndexType.KNOW_DOCUMENT_VERSION);
        if (handler != null) {
            handler.createIndex(jsonObj.getLong("versionId"));
        }
        return null;
    }

    @Override
    public String getToken() {
        return "/knows/test";
    }
}
