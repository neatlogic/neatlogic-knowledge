package neatlogic.module.knowledge.service;

import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentTypeVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeDocumentTypeServiceImpl implements KnowledgeDocumentTypeService {

	@Autowired
	private KnowledgeDocumentTypeMapper knowledgeDocumentTypeMapper;

	@Override
	public void rebuildLeftRightCode(Long knowledgeCircleId) {
		rebuildLeftRightCode(KnowledgeDocumentTypeVo.ROOT_UUID, 1,knowledgeCircleId);
	}
	
	private Integer rebuildLeftRightCode(String parentUuid, Integer parentLft,Long knowledgeCircleId) {
		List<KnowledgeDocumentTypeVo> knowledgeDocumentTypeVoList = knowledgeDocumentTypeMapper.getTypeByParentUuid(parentUuid,knowledgeCircleId);
		for(KnowledgeDocumentTypeVo type : knowledgeDocumentTypeVoList) {
			if(type.getChildCount() == 0) {
				knowledgeDocumentTypeMapper.updateTypeLeftRightCode(type.getUuid(), parentLft + 1, parentLft + 2);
				parentLft += 2;
			}else {
				int lft = parentLft + 1;
				parentLft = rebuildLeftRightCode(type.getUuid(), lft,knowledgeCircleId);
				knowledgeDocumentTypeMapper.updateTypeLeftRightCode(type.getUuid(), lft, parentLft + 1);
				parentLft += 1;
			}
		}
		return parentLft;
	}

	@Override
	public KnowledgeDocumentTypeVo buildRootType(Long knowledgeCircleId) {
		Integer maxRhtCode = knowledgeDocumentTypeMapper.getMaxRhtCode(knowledgeCircleId);
		KnowledgeDocumentTypeVo rootType = new KnowledgeDocumentTypeVo();
		rootType.setUuid(KnowledgeDocumentTypeVo.ROOT_UUID);
		rootType.setName("root");
		rootType.setParentUuid(KnowledgeDocumentTypeVo.ROOT_PARENTUUID);
		rootType.setLft(1);
		rootType.setRht(maxRhtCode == null ? 2 : maxRhtCode + 1);
		rootType.setKnowledgeCircleId(knowledgeCircleId);
		return rootType;
	}

	/**
	 * 统一组装知识分类父子关系，供分类选择与上层业务范围树复用。
	 */
	@Override
	public KnowledgeDocumentTypeVo buildTypeTree(Long knowledgeCircleId) {
		KnowledgeDocumentTypeVo rootType = buildRootType(knowledgeCircleId);
		List<KnowledgeDocumentTypeVo> typeList = knowledgeDocumentTypeMapper.getTypeForTree(
				rootType.getLft(), rootType.getRht(), knowledgeCircleId);
		if (typeList == null || typeList.isEmpty()) {
			return rootType;
		}
		Map<String, KnowledgeDocumentTypeVo> typeMap = new HashMap<>();
		typeMap.put(rootType.getUuid(), rootType);
		for (KnowledgeDocumentTypeVo typeVo : typeList) {
			typeMap.put(typeVo.getUuid(), typeVo);
		}
		for (KnowledgeDocumentTypeVo typeVo : typeList) {
			KnowledgeDocumentTypeVo parentVo = typeMap.get(typeVo.getParentUuid());
			if (parentVo != null) {
				typeVo.setParent(parentVo);
			}
		}
		return rootType;
	}
}
