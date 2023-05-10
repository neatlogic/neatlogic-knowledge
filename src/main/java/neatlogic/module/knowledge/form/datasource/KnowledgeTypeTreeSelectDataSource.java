/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.knowledge.form.datasource;

import neatlogic.framework.form.treeselect.core.TreeSelectDataSourceBase;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeCircleMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import neatlogic.framework.knowledge.dto.KnowledgeCircleVo;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentTypeVo;
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
    public List<String> valueConversionTextPathList(Object value) {
        List<String> pathList = new ArrayList<>();
        KnowledgeDocumentTypeVo typeVo = knowledgeDocumentTypeMapper.getTypeByUuid((String)value);
        if (typeVo != null) {
            KnowledgeCircleVo knowledgeCircleVo = knowledgeCircleMapper.getKnowledgeCircleById(typeVo.getKnowledgeCircleId());
            if (knowledgeCircleVo != null) {
                pathList.add(knowledgeCircleVo.getName());
            }
            List<KnowledgeDocumentTypeVo> typeVoList = knowledgeDocumentTypeMapper.getAncestorsAndSelfByLftRht(typeVo.getLft(), typeVo.getRht(), typeVo.getKnowledgeCircleId());
            if (CollectionUtils.isNotEmpty(typeVoList)) {
                List<String> nameList = typeVoList.stream().map(KnowledgeDocumentTypeVo::getName).collect(Collectors.toList());
                pathList.addAll(nameList);
            }
        }
        return pathList;
    }
}
