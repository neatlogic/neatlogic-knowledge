/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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
