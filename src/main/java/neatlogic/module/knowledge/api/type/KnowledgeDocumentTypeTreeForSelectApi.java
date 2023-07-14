package neatlogic.module.knowledge.api.type;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeCircleMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentTypeVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_BASE;
import neatlogic.module.knowledge.service.KnowledgeDocumentTypeService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentTypeTreeForSelectApi extends PrivateApiComponentBase{

	@Autowired
	private KnowledgeCircleMapper knowledgeCircleMapper;

	@Autowired
	private KnowledgeDocumentTypeMapper knowledgeDocumentTypeMapper;

	@Autowired
	private KnowledgeDocumentTypeService knowledgeDocumentTypeService;

	@Override
	public String getToken() {
		return "knowledge/document/type/tree/forselect";
	}

	@Override
	public String getName() {
		return "nmkat.knowledgedocumenttypetreeforselectapi.getname";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword",xss = true)})
	@Output({@Param( name = "typeList", explode = KnowledgeDocumentTypeVo[].class, desc = "common.tbodylist")})
	@Description(desc = "nmkat.knowledgedocumenttypetreeforselectapi.getname")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		JSONArray result = new JSONArray();
		String keyword = jsonObj.getString("keyword");
//		List<KnowledgeDocumentTypeVo> docTypeList = null;
		List<String> uuidList = new ArrayList<String>();
		/** 获取当前用户所在组和角色 */
		AuthenticationInfoVo authenticationInfoVo = UserContext.get().getAuthenticationInfoVo();
		uuidList.addAll(authenticationInfoVo.getTeamUuidList());
		uuidList.addAll(authenticationInfoVo.getRoleUuidList());
		uuidList.add(UserContext.get().getUserUuid());
		/** 获取当前用户所有的圈子ID集合 */
		List<Long> circleIdList = knowledgeCircleMapper.getCircleIdListByUserUuidList(uuidList);
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
						circle.put("uuid",id);
						circle.put("name",knowledgeCircleMapper.getKnowledgeCircleById(id).getName());
						circle.put("disabled",true);
						circle.put("children",root.getChildren());
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
					circle.put("uuid",id);
					circle.put("name",knowledgeCircleMapper.getKnowledgeCircleById(id).getName());
					circle.put("disabled",true);
					circle.put("children",root.getChildren());
					result.add(circle);
//					docTypeList.addAll(root.getChildren());
				}
			}
		}
		return result;
	}

}
