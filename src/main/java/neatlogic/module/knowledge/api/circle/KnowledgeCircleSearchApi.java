package neatlogic.module.knowledge.api.circle;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.GroupSearch;
import neatlogic.framework.common.constvalue.UserType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.common.util.PageUtil;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_BASE;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeCircleMapper;
import neatlogic.framework.knowledge.dto.KnowledgeCircleUserVo;
import neatlogic.framework.knowledge.dto.KnowledgeCircleVo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeCircleSearchApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeCircleMapper knowledgeCircleMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public String getToken() {
        return "knowledge/circle/search";
    }

    @Override
    public String getName() {
        return "查询知识圈";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword",
                    type = ApiParamType.STRING,
                    desc = "关键词",
                    xss = true),
            @Param(name = "currentPage",
                    type = ApiParamType.INTEGER,
                    desc = "当前页"),
            @Param(name = "pageSize",
                    type = ApiParamType.INTEGER,
                    desc = "每页数据条目"),
            @Param(name = "needPage",
                    type = ApiParamType.BOOLEAN,
                    desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "circleList",
                    type = ApiParamType.JSONARRAY,
                    explode = KnowledgeCircleVo[].class,
                    desc = "知识圈列表"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "查询知识圈")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        KnowledgeCircleVo knowledgeCircleVo = JSON.parseObject(jsonObj.toJSONString(), new TypeReference<KnowledgeCircleVo>() {
        });
        JSONObject returnObj = new JSONObject();
        if (knowledgeCircleVo.getNeedPage()) {
            int rowNum = knowledgeCircleMapper.searchKnowledgeCircleCount(knowledgeCircleVo);
            returnObj.put("pageSize", knowledgeCircleVo.getPageSize());
            returnObj.put("currentPage", knowledgeCircleVo.getCurrentPage());
            returnObj.put("rowNum", rowNum);
            returnObj.put("pageCount", PageUtil.getPageCount(rowNum, knowledgeCircleVo.getPageSize()));
        }
        List<KnowledgeCircleVo> tbodyList = knowledgeCircleMapper.searchKnowledgeCircle(knowledgeCircleVo);
        handleTbodyList(tbodyList);
        returnObj.put("circleList", tbodyList);
        return returnObj;
    }

    private void handleTbodyList(List<KnowledgeCircleVo> tbodyList) {
        if (CollectionUtils.isNotEmpty(tbodyList)) {
            for (KnowledgeCircleVo vo : tbodyList) {
                /**
                 * 计算成员数与查询审批人
                 */
                List<KnowledgeCircleUserVo> approverList = knowledgeCircleMapper.getKnowledgeCircleUserListByIdAndAuthType(vo.getId(), KnowledgeCircleUserVo.AuthType.APPROVER.getValue());
                List<KnowledgeCircleUserVo> memberList = knowledgeCircleMapper.getKnowledgeCircleUserListByIdAndAuthType(vo.getId(), KnowledgeCircleUserVo.AuthType.MEMBER.getValue());
                /** 统计审批人用户UUID */
                Set<String> approverUuidSet = getAuthUserUuidSet(approverList);
                if (checkIsContainsAllUser(memberList)) {
                    vo.setMemberCount(-1);
                } else {
                    /** 统计成员用户UUID */
                    Set<String> memberUuidSet = getAuthUserUuidSet(memberList);
                    vo.setMemberCount(memberUuidSet.size());
                }

                /** 根据筛选到的审批人UUID查询用户 */
                if (CollectionUtils.isNotEmpty(approverUuidSet)) {
                    vo.setApproverVoList(userMapper.getUserListByUuidList(approverUuidSet.stream().collect(Collectors.toList())));
                }
            }
        }
    }

    private Set<String> getAuthUserUuidSet(List<KnowledgeCircleUserVo> authList) {
        Set<String> uuidSet = new TreeSet<>();
        if (CollectionUtils.isNotEmpty(authList)) {
            List<String> teamUuidList = new ArrayList<>();
            List<String> roleUuidList = new ArrayList<>();
            for (KnowledgeCircleUserVo obj : authList) {
                if (GroupSearch.USER.getValue().equals(obj.getType())) {
                    uuidSet.add(obj.getUuid());
                } else if (GroupSearch.TEAM.getValue().equals(obj.getType())) {
                    teamUuidList.add(obj.getUuid());
                } else if (GroupSearch.ROLE.getValue().equals(obj.getType())) {
                    roleUuidList.add(obj.getUuid());
                }
            }
            if (CollectionUtils.isNotEmpty(teamUuidList)) {
                uuidSet.addAll(userMapper.getUserUuidListByTeamUuidList(teamUuidList));
            }
            if (CollectionUtils.isNotEmpty(roleUuidList)) {
                uuidSet.addAll(userMapper.getUserUuidListByRoleUuidList(roleUuidList));
            }
        }
        return uuidSet;
    }

    /**
     * 判断是否包含所有人选项
     *
     * @param memberList
     * @return
     */
    private boolean checkIsContainsAllUser(List<KnowledgeCircleUserVo> memberList) {
        if (CollectionUtils.isNotEmpty(memberList)) {
            for (KnowledgeCircleUserVo circleUserVo : memberList) {
                if (Objects.equals(circleUserVo.getType(), GroupSearch.COMMON.getValue()) && Objects.equals(circleUserVo.getUuid(), UserType.ALL.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

}
