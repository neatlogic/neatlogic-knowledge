package codedriver.module.knowledge.api.circle;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.dao.mapper.RoleMapper;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dao.mapper.KnowledgeCircleMapper;
import codedriver.module.knowledge.dto.KnowledgeCircleUserVo;
import codedriver.module.knowledge.dto.KnowledgeCircleVo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeCircleSearchApi extends PrivateApiComponentBase{

	@Autowired
	private KnowledgeCircleMapper knowledgeCircleMapper;

	@Autowired
	private TeamMapper teamMapper;

	@Autowired
	private RoleMapper roleMapper;

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
			@Param( name = "keyword",
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
		KnowledgeCircleVo knowledgeCircleVo = JSON.parseObject(jsonObj.toJSONString(), new TypeReference<KnowledgeCircleVo>(){});
		JSONObject returnObj = new JSONObject();
		if(knowledgeCircleVo.getNeedPage()){
			int rowNum = knowledgeCircleMapper.searchKnowledgeCircleCount(knowledgeCircleVo);
			returnObj.put("pageSize", knowledgeCircleVo.getPageSize());
			returnObj.put("currentPage", knowledgeCircleVo.getCurrentPage());
			returnObj.put("rowNum", rowNum);
			returnObj.put("pageCount", PageUtil.getPageCount(rowNum, knowledgeCircleVo.getPageSize()));
		}
		List<KnowledgeCircleVo> circleList = knowledgeCircleMapper.searchKnowledgeCircle(knowledgeCircleVo);
		if(CollectionUtils.isNotEmpty(circleList)){
			for(KnowledgeCircleVo vo : circleList){
				/** 计算成员数与查询审批人
				 * 先根据知识圈ID查询所有关联的对象
				 * 然后通过authType分别统计成员数和审批人
				 */
				List<KnowledgeCircleUserVo> authObjList = knowledgeCircleMapper.getKnowledgeCircleUserList(vo.getId());
				if(CollectionUtils.isNotEmpty(authObjList)){
					Set<String> memberUuidSet = new TreeSet<>();
					Set<String> approverUuidSet = new TreeSet<>();
					for(KnowledgeCircleUserVo obj : authObjList){
						if(GroupSearch.USER.getValue().equals(obj.getType())){
							if(KnowledgeCircleUserVo.AuthType.MEMBER.getValue().equals(obj.getAuthType())){
								memberUuidSet.add(obj.getUuid());
							}else if(KnowledgeCircleUserVo.AuthType.APPROVER.getValue().equals(obj.getAuthType())){
								approverUuidSet.add(obj.getUuid());
							}
						}else if(GroupSearch.TEAM.getValue().equals(obj.getType())){
							List<String> uuidList = teamMapper.getUserUuidListByTeamUuid(obj.getUuid());
							if(KnowledgeCircleUserVo.AuthType.MEMBER.getValue().equals(obj.getAuthType())){
								memberUuidSet.addAll(uuidList);
							}else if(KnowledgeCircleUserVo.AuthType.APPROVER.getValue().equals(obj.getAuthType())){
								approverUuidSet.addAll(uuidList);
							}
						}else if(GroupSearch.ROLE.getValue().equals(obj.getType())){
							List<String> uuidList = roleMapper.getUserUuidListByRoleUuid(obj.getUuid());
							if(KnowledgeCircleUserVo.AuthType.MEMBER.getValue().equals(obj.getAuthType())){
								memberUuidSet.addAll(uuidList);
							}else if(KnowledgeCircleUserVo.AuthType.APPROVER.getValue().equals(obj.getAuthType())){
								approverUuidSet.addAll(uuidList);
							}
						}
					}
					vo.setMemberCount(memberUuidSet.size());
					/** 根据筛选到的审批人UUID查询用户名 */
					if(CollectionUtils.isNotEmpty(approverUuidSet)){
						List<String> approverNameList = new ArrayList<>();
						for(String uuid : approverUuidSet){
							approverNameList.add(userMapper.getUserBaseInfoByUuid(uuid).getUserName());
						}
						vo.setApproverNameList(approverNameList);
					}
				}
			}
		}

		returnObj.put("circleList", circleList);
		return returnObj;
	}

}
