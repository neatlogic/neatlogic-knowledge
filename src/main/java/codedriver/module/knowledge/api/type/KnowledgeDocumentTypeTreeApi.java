package codedriver.module.knowledge.api.type;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.dto.AuthenticationInfoVo;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.service.AuthenticationInfoService;
import codedriver.module.knowledge.auth.label.KNOWLEDGE_BASE;
import codedriver.framework.knowledge.dao.mapper.KnowledgeCircleMapper;
import codedriver.framework.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import codedriver.framework.knowledge.dto.KnowledgeDocumentTypeVo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentTypeTreeApi extends PrivateApiComponentBase{

	@Autowired
	private KnowledgeCircleMapper knowledgeCircleMapper;

	@Autowired
	private KnowledgeDocumentTypeMapper knowledgeDocumentTypeMapper;

	@Resource
	private AuthenticationInfoService authenticationInfoService;

	@Override
	public String getToken() {
		return "knowledge/document/type/tree";
	}

	@Override
	public String getName() {
		return "获取知识圈知识分类树";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({})
	@Output({@Param( name = "typeList", explode = KnowledgeDocumentTypeVo[].class, desc = "知识类型架构集合")})
	@Description(desc = "获取知识圈知识分类树")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		JSONArray result = new JSONArray();
		List<String> uuidList = new ArrayList<String>();
		/** 获取当前用户所在组和角色 */
		AuthenticationInfoVo authenticationInfoVo = authenticationInfoService.getAuthenticationInfo(UserContext.get().getUserUuid());
		uuidList.addAll(authenticationInfoVo.getTeamUuidList());
		uuidList.addAll(authenticationInfoVo.getRoleUuidList());
		uuidList.add(UserContext.get().getUserUuid());
		/** 获取当前用户所有的圈子ID集合 */
		List<Long> circleIdList = knowledgeCircleMapper.getCircleIdListByUserUuidList(uuidList);
		if(CollectionUtils.isNotEmpty(circleIdList)){
			Set<Long> circleIdSet = circleIdList.stream().collect(Collectors.toSet());
			/** 根据圈子ID查询分类 */
			for(Long id : circleIdSet){
				KnowledgeDocumentTypeVo type = new KnowledgeDocumentTypeVo();
				type.setParentUuid(KnowledgeDocumentTypeVo.ROOT_UUID);
				type.setKnowledgeCircleId(id);
				List<KnowledgeDocumentTypeVo> typeList = knowledgeDocumentTypeMapper.searchType(type);
				/** 计算每个分类及其子类的文档数 */
                /*if(CollectionUtils.isNotEmpty(typeList)){
                	for(KnowledgeDocumentTypeVo vo : typeList){
                		int count = 0;
                		List<KnowledgeDocumentTypeVo> childAndSelf = knowledgeDocumentTypeMapper.getChildAndSelfByLftRht(vo.getLft(), vo.getRht(), id);
                		for(KnowledgeDocumentTypeVo obj : childAndSelf){
                			count += knowledgeDocumentTypeMapper.getDocumentCountByUuid(obj.getUuid());
                		}
                		vo.setDocumentCount(count);
                	}
                }*/
				JSONObject circle = new JSONObject();
				circle.put("id",id);
				circle.put("name",knowledgeCircleMapper.getKnowledgeCircleById(id).getName());
				circle.put("disabled",true);
				circle.put("children",typeList);
				result.add(circle);
			}
		}
		return result;
	}

}
