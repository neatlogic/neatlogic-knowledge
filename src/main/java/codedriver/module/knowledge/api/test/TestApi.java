package codedriver.module.knowledge.api.test;

import codedriver.framework.fulltextindex.core.FullTextIndexHandlerFactory;
import codedriver.framework.fulltextindex.core.IFullTextIndexHandler;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.fulltextindex.FullTextIndexType;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

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
        JSONArray versionIdArray = jsonObj.getJSONArray("versionIdList");
        List<Long> versionIdList = null;
        //创建全文检索索引
        IFullTextIndexHandler handler = FullTextIndexHandlerFactory.getComponent(FullTextIndexType.KNOW_DOCUMENT_VERSION);
        if (handler != null) {
            if(jsonObj.containsKey("versionId")) {
                handler.createIndex(jsonObj.getLong("versionId"));
            }else {
                if(CollectionUtils.isNotEmpty(versionIdArray)){
                    versionIdList = JSONObject.parseArray(versionIdArray.toJSONString(), Long.class);
                }else{
                    versionIdList = knowledgeDocumentMapper.getKnowledgeDocumentVersionIdList();
                }
                for(Long versionIdObj : versionIdList ){
                    handler.createIndex(versionIdObj);
                }
            }
        }
        return null;
    }

    @Override
    public String getToken() {
        return "/knows/test";
    }
}
