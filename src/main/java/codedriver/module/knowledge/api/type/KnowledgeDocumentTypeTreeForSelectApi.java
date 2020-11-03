package codedriver.module.knowledge.api.type;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dao.mapper.KnowledgeCircleMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentTypeVo;
import codedriver.module.knowledge.service.KnowledgeDocumentTypeService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentTypeTreeForSelectApi extends PrivateApiComponentBase{

	@Autowired
	private KnowledgeCircleMapper knowledgeCircleMapper;

	@Autowired
	private KnowledgeDocumentTypeMapper knowledgeDocumentTypeMapper;

	@Autowired
	private KnowledgeDocumentTypeService knowledgeDocumentTypeService;

	@Autowired
	private UserMapper userMapper;

	@Autowired
	private TeamMapper teamMapper;

	@Override
	public String getToken() {
		return "knowledge/document/type/tree/forselect";
	}

	@Override
	public String getName() {
		return "获取知识圈知识分类树_下拉框";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字",xss = true)})
	@Output({@Param( name = "typeList", explode = KnowledgeDocumentTypeVo[].class, desc = "知识类型架构集合")})
	@Description(desc = "获取知识圈知识分类树_下拉框")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		JSONArray result = new JSONArray();
		String keyword = jsonObj.getString("keyword");
//		List<KnowledgeDocumentTypeVo> docTypeList = null;
		List uuidList = new ArrayList();
		/** 获取当前用户所在组和角色 */
		List<String> teamUuidList = teamMapper.getTeamUuidListByUserUuid(UserContext.get().getUserUuid());
		List<String> roleUuidList = userMapper.getRoleUuidListByUserUuid(UserContext.get().getUserUuid());
		uuidList.addAll(teamUuidList);
		uuidList.addAll(roleUuidList);
		uuidList.add(UserContext.get().getUserUuid());
		/** 获取当前用户所有的圈子ID集合 */
		List<Long> circleIdList = knowledgeCircleMapper.getCircleIdListByMemberUuidList(uuidList);
		/** 根据圈子ID查询分类 */
		if(CollectionUtils.isNotEmpty(circleIdList)){
			Set<Long> circleIdSet = circleIdList.stream().collect(Collectors.toSet());
//			docTypeList = new ArrayList<>();
			for(Long id : circleIdSet){
				/** 搜索模式下，根据圈子ID与关键词搜索文档类型与其所有父类型 */
				if(StringUtils.isNotBlank(keyword)){
					Map<String, KnowledgeDocumentTypeVo> typeMap = new HashMap<>();
					List<String> typeUuidList = new ArrayList<>();
					List<KnowledgeDocumentTypeVo> typeVoList = new ArrayList<>();

					KnowledgeDocumentTypeVo keywordType = new KnowledgeDocumentTypeVo();
					keywordType.setKeyword(keyword);
					keywordType.setKnowledgeCircleId(id);
					/** 查询符合条件的类型 */
					List<KnowledgeDocumentTypeVo> typeVos = knowledgeDocumentTypeMapper.searchType(keywordType);
					if(CollectionUtils.isNotEmpty(typeVos)){
						for(KnowledgeDocumentTypeVo vo : typeVos){
							List<KnowledgeDocumentTypeVo> ancestorsAndSelf = knowledgeDocumentTypeMapper.getAncestorsAndSelfByLftRht(vo.getLft(), vo.getRht(), id);
							for(KnowledgeDocumentTypeVo as : ancestorsAndSelf){
								if(!typeUuidList.contains(as.getUuid())){
									typeMap.put(as.getUuid(),as);
									typeUuidList.add(as.getUuid());
									typeVoList.add(as);
								}
							}
						}
					}
					/** 计算每个类型的子类型数量&&设置每个类型的父类型 */
					if(CollectionUtils.isNotEmpty(typeVoList)){
						KnowledgeDocumentTypeVo root = knowledgeDocumentTypeService.buildRootType(id);
						typeMap.put(root.getUuid(),root);
						List<KnowledgeDocumentTypeVo> childCountList = knowledgeDocumentTypeMapper.getTypeChildCountListByUuidList(typeUuidList);
						Map<String,KnowledgeDocumentTypeVo> childCountMap = new HashMap<>();
						if(CollectionUtils.isNotEmpty(childCountList)){
							for(KnowledgeDocumentTypeVo vo : childCountList){
								childCountMap.put(vo.getUuid(),vo);
							}
						}
						for(KnowledgeDocumentTypeVo vo : typeVoList){
							KnowledgeDocumentTypeVo parent = typeMap.get(vo.getParentUuid());
							if(parent != null) {
								vo.setParent(parent);
							}
							KnowledgeDocumentTypeVo childCount = childCountMap.get(vo.getUuid());
							if(childCount != null) {
								vo.setChildCount(childCount.getChildCount());
							}
						}
						JSONObject circle = new JSONObject();
						circle.put("id",id);
						circle.put("name",knowledgeCircleMapper.getKnowledgeCircleById(id).getName());
						circle.put("typeList",root.getChildren());
						result.add(circle);
//						docTypeList.addAll(root.getChildren());
					}

				}else{
					/** 非搜索模式下获取所有文档类型 */
					KnowledgeDocumentTypeVo root = knowledgeDocumentTypeService.buildRootType(id);
					List<KnowledgeDocumentTypeVo> typeList = knowledgeDocumentTypeMapper.getTypeForTree(root.getLft(), root.getRht(),id);
					if(CollectionUtils.isNotEmpty(typeList)){
						Map<String, KnowledgeDocumentTypeVo> idMap = new HashMap<>();
						typeList.add(root);
						for(KnowledgeDocumentTypeVo vo : typeList){
							idMap.put(vo.getUuid(),vo);
						}
						for(KnowledgeDocumentTypeVo vo : typeList){
							String parentUuid = vo.getParentUuid();
							KnowledgeDocumentTypeVo parent = idMap.get(parentUuid);
							if(parent != null){
								vo.setParent(parent);
							}
						}
					}
					JSONObject circle = new JSONObject();
					circle.put("id",id);
					circle.put("name",knowledgeCircleMapper.getKnowledgeCircleById(id).getName());
					circle.put("typeList",root.getChildren());
					result.add(circle);
//					docTypeList.addAll(root.getChildren());
				}
			}
		}
		return result;
	}

}
