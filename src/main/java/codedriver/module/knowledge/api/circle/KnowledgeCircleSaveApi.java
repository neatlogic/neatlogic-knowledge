package codedriver.module.knowledge.api.circle;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.dto.AuthorityVo;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dao.mapper.KnowledgeCircleMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import codedriver.module.knowledge.dto.KnowledgeCircleUserVo;
import codedriver.module.knowledge.dto.KnowledgeCircleVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentTypeVo;
import codedriver.module.knowledge.exception.KnowledgeCircleNameRepeatException;
import codedriver.module.knowledge.exception.KnowledgeCircleNotFoundException;
import codedriver.module.knowledge.service.KnowledgeDocumentTypeService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 关于知识圈中的知识类型说明：
 * 1、知识类型会在每一次保存知识圈时全部删除，再重新插入
 * 2、前端传入包含所有知识类型的JSON(knowledgeType)，此JSON保持树形结构，结构形如：
 * {"children":[{"children":[],"name":"test2","parentUuid":"0","uuid":"b1"},{"children":[{"children":[{"children":[],"name":"aaa","parentUuid":"b3","uuid":"b5"},{"children":[],"name":"bbb","parentUuid":"b3","uuid":"b4"}],"name":"wqeqw","parentUuid":"b2","uuid":"b3"}],"name":"test1","parentUuid":"0","uuid":"b2"}]}
 * 后端需要据此解析出每一个知识类型，并根据顺序构建左右编码
 *
 */

@AuthAction(name = "KNOWLEDGE_CIRCLE_MODIFY")
@Service
@Transactional
@OperationType(type = OperationTypeEnum.CREATE)
public class KnowledgeCircleSaveApi extends PrivateApiComponentBase{

	@Autowired
	private KnowledgeCircleMapper knowledgeCircleMapper;

	@Autowired
	private KnowledgeDocumentTypeMapper knowledgeDocumentTypeMapper;

	@Autowired
	private KnowledgeDocumentTypeService knowledgeDocumentTypeService;

	@Override
	public String getToken() {
		return "knowledge/circle/save";
	}

	@Override
	public String getName() {
		return "保存知识圈";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({
			@Param( name = "id", type = ApiParamType.LONG, desc = "知识圈ID"),
			@Param( name = "name", type = ApiParamType.REGEX, rule = "^[A-Za-z_\\d\\u4e00-\\u9fa5]+$", desc = "名称", isRequired = true,xss=true),
			@Param( name = "approver", type = ApiParamType.JSONARRAY, desc = "审批人，可多选，格式[\"user#userUuid\",\"team#teamUuid\",\"role#roleUuid\"]", isRequired = true),
			@Param( name = "member", type = ApiParamType.JSONARRAY, desc = "成员，可多选，格式[\"user#userUuid\",\"team#teamUuid\",\"role#roleUuid\"]"),
			@Param( name = "knowledgeType", type = ApiParamType.JSONOBJECT, desc = "知识类型",isRequired = true)
	})
	@Output({@Param(name = "id", type = ApiParamType.INTEGER,desc = "知识圈ID")})
	@Description(desc = "保存知识圈")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {

		Long id = jsonObj.getLong("id");
		String name = jsonObj.getString("name");
		JSONArray approver = jsonObj.getJSONArray("approver");
		JSONArray member = jsonObj.getJSONArray("member");
		JSONObject knowledgeType = jsonObj.getJSONObject("knowledgeType");

		KnowledgeCircleVo knowledgeCircleVo = new KnowledgeCircleVo();
		knowledgeCircleVo.setId(id);
		knowledgeCircleVo.setName(name);

		if(id != null){
			if(knowledgeCircleMapper.checkKnowledgeCircleExistsById(id) == 0){
				throw new KnowledgeCircleNotFoundException(id);
			}
			if(knowledgeCircleMapper.checkNameIsRepeat(knowledgeCircleVo) > 0){
				throw new KnowledgeCircleNameRepeatException(name);
			}
			knowledgeCircleMapper.updateKnowledgeCircle(knowledgeCircleVo);
			/** 先删除知识圈用户与知识圈中的知识类型，最后重新插入 */
			knowledgeDocumentTypeMapper.deleteTypeByCircleId(knowledgeCircleVo.getId());
			knowledgeCircleMapper.deleteKnowledgeCircleUserById(id);
		}else{
			if(knowledgeCircleMapper.checkNameIsRepeat(knowledgeCircleVo) > 0){
				throw new KnowledgeCircleNameRepeatException(name);
			}
			knowledgeCircleMapper.insertKnowledgeCircle(knowledgeCircleVo);
		}

		/** 解析知识类型JSON*/
		List<KnowledgeDocumentTypeVo> typeList = new ArrayList<>();
		parseKnowledgeTypeJson(knowledgeType,typeList,knowledgeCircleVo.getId());
		/** 解析知识圈用户，包括审批人与成员 */
		List<KnowledgeCircleUserVo> circleUserList = getKnowledgeCircleUserList(approver, member, knowledgeCircleVo.getId());
		/** 插入知识类型与用户 */
		if(CollectionUtils.isNotEmpty(typeList)){
			knowledgeDocumentTypeMapper.batchInsertType(typeList);
			/** knowledgeType中并不包含左右编码，故需要根据parentId与sort重建左右编码 */
			knowledgeDocumentTypeService.rebuildLeftRightCode(knowledgeCircleVo.getId());
		}
		if(CollectionUtils.isNotEmpty(circleUserList)){
			knowledgeCircleMapper.batchInsertKnowledgeCircleUser(circleUserList);
		}
		JSONObject result = new JSONObject();
		result.put("id",knowledgeCircleVo.getId());
		return result;
	}

	/**
	 * 组装知识圈用户列表
	 * @param approver
	 * @param member
	 * @param knowledgeCircleId
	 * @return
	 */
	private List<KnowledgeCircleUserVo> getKnowledgeCircleUserList(JSONArray approver, JSONArray member, Long knowledgeCircleId) {
		List<KnowledgeCircleUserVo> circleUserList = new ArrayList<>();
		/** 组装审批人 */
		if(CollectionUtils.isNotEmpty(approver)){
			List<String> approvers = approver.toJavaList(String.class);
			List<AuthorityVo> approverList = getAuthList(approvers);
			if(CollectionUtils.isNotEmpty(approverList)){
				approverList.stream().forEach(vo -> {
					KnowledgeCircleUserVo user = new KnowledgeCircleUserVo();
					user.setKnowledgeCircleId(knowledgeCircleId);
					user.setUuid(vo.getUuid());
					user.setType(vo.getType());
					user.setAuthType(KnowledgeCircleUserVo.AuthType.APPROVER.getValue());
					circleUserList.add(user);
				});
			}
		}
		/** 组装成员 */
		if(CollectionUtils.isNotEmpty(member)){
			List<String> members = member.toJavaList(String.class);
			List<AuthorityVo> memberList = getAuthList(members);
			if(CollectionUtils.isNotEmpty(memberList)){
				memberList.stream().forEach(vo -> {
					KnowledgeCircleUserVo user = new KnowledgeCircleUserVo();
					user.setKnowledgeCircleId(knowledgeCircleId);
					user.setUuid(vo.getUuid());
					user.setType(vo.getType());
					user.setAuthType(KnowledgeCircleUserVo.AuthType.MEMBER.getValue());
					circleUserList.add(user);
				});
			}
		}

		return circleUserList;
	}

	/**
	 * 转换授权列表
	 * @param authList
	 * @return
	 */
	private List<AuthorityVo> getAuthList(List<String> authList){
		List<AuthorityVo> list = new ArrayList<>();
		if(CollectionUtils.isNotEmpty(authList)) {
			for(String authority : authList) {
				String[] split = authority.split("#");
				if(GroupSearch.getGroupSearch(split[0]) != null) {
					AuthorityVo authorityVo = new AuthorityVo();
					authorityVo.setType(split[0]);
					authorityVo.setUuid(split[1]);
					list.add(authorityVo);
				}
			}
		}
		return list;
	}

	/**
	 * 解析知识类型JSON
	 * @param objJson
	 * @param list
	 * @param knowledgeCircleId
	 * @return
	 */
	private List<KnowledgeDocumentTypeVo> parseKnowledgeTypeJson(Object objJson, List<KnowledgeDocumentTypeVo> list, Long knowledgeCircleId) {
		if (objJson instanceof JSONArray) {
			JSONArray objArray = JSONArray.parseArray(objJson.toString());
			for (int i = 0; i < objArray.size(); i++) {
				JSONObject obj = objArray.getJSONObject(i);
				KnowledgeDocumentTypeVo knowledgeDocumentTypeVo = new KnowledgeDocumentTypeVo();
				knowledgeDocumentTypeVo.setUuid(obj.getString("uuid"));
				knowledgeDocumentTypeVo.setParentUuid(obj.getString("parentUuid"));
				knowledgeDocumentTypeVo.setName(obj.getString("name"));
				knowledgeDocumentTypeVo.setKnowledgeCircleId(knowledgeCircleId);
				/** sort的用处在于重建左右编码 */
				knowledgeDocumentTypeVo.setSort(i);
				list.add(knowledgeDocumentTypeVo);
				parseKnowledgeTypeJson(objArray.get(i),list,knowledgeCircleId);
			}
		}else if (objJson instanceof JSONObject) {
			JSONObject jsonObject = JSONObject.parseObject(objJson.toString());
			Iterator it = jsonObject.keySet().iterator();
			while (it.hasNext()) {
				String key = it.next().toString();
				Object object = jsonObject.get(key);
				if (object instanceof JSONArray) {
					JSONArray objArray = JSONArray.parseArray(object.toString());
					parseKnowledgeTypeJson(objArray,list,knowledgeCircleId);
				} else if (object instanceof JSONObject) {
					parseKnowledgeTypeJson(object,list,knowledgeCircleId);
				}
			}
		}
		return list;
	}

}
