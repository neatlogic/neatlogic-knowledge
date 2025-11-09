/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.knowledge.form.datasource;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.form.treeselect.core.TreeSelectDataSourceBase;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeCircleMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import neatlogic.framework.knowledge.dto.KnowledgeCircleVo;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentTypeVo;
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
