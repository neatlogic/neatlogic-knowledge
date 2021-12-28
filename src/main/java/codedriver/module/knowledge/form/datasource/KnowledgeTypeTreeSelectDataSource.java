/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.knowledge.form.datasource;

import codedriver.framework.form.treeselect.core.TreeSelectDataSourceBase;
import codedriver.framework.knowledge.dao.mapper.KnowledgeCircleMapper;
import codedriver.framework.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import codedriver.framework.knowledge.dto.KnowledgeCircleVo;
import codedriver.framework.knowledge.dto.KnowledgeDocumentTypeVo;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2021/8/11 17:46
 **/
@Component
public class KnowledgeTypeTreeSelectDataSource extends TreeSelectDataSourceBase {

    @Resource
    private KnowledgeDocumentTypeMapper knowledgeDocumentTypeMapper;
    @Resource
    private KnowledgeCircleMapper knowledgeCircleMapper;

    /**
     * 获取组件英文名
     *
     * @return 组件英文名
     */
    @Override
    public String getHandler() {
        return "knowledgeType";
    }

    /**
     * 获取组件中文名
     *
     * @return 组件中文名
     */
    @Override
    public String getHandlerName() {
        return "知识圈类型";
    }

    /**
     * 获取数据源配置
     *
     * @return 配置
     */
    @Override
    public JSONObject getConfig() {
        return new JSONObject(){{
            put("url","/api/rest/knowledge/document/type/tree/forselect");
            put("valueName", "uuid");
            put("textName", "name");
        }};
    }

    @Override
    public String valueConversionTextPath(Object value) {
        KnowledgeDocumentTypeVo typeVo = knowledgeDocumentTypeMapper.getTypeByUuid((String)value);
        if (typeVo != null) {
            List<String> pathList = new ArrayList<>();
            KnowledgeCircleVo knowledgeCircleVo = knowledgeCircleMapper.getKnowledgeCircleById(typeVo.getKnowledgeCircleId());
            if (knowledgeCircleVo != null) {
                pathList.add(knowledgeCircleVo.getName());
            }
            List<KnowledgeDocumentTypeVo> typeVoList = knowledgeDocumentTypeMapper.getAncestorsAndSelfByLftRht(typeVo.getLft(), typeVo.getRht(), typeVo.getKnowledgeCircleId());
            if (CollectionUtils.isNotEmpty(typeVoList)) {
                List<String> nameList = typeVoList.stream().map(KnowledgeDocumentTypeVo::getName).collect(Collectors.toList());
                pathList.addAll(nameList);
            }

            return String.join("/", pathList);
        }
        return null;
    }
}
