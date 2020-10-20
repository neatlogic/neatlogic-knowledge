package codedriver.module.knowledge.service;

import codedriver.module.knowledge.dao.mapper.KnowledgeTypeMapper;
import codedriver.module.knowledge.dto.KnowledgeTypeVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeTypeServiceImpl implements KnowledgeTypeService {

	@Autowired
	private KnowledgeTypeMapper knowledgeTypeMapper;

	@Override
	public void rebuildLeftRightCode(Long knowledgeCircleId) {
		rebuildLeftRightCode(KnowledgeTypeVo.ROOT_UUID, 1,knowledgeCircleId);
	}
	
	private Integer rebuildLeftRightCode(String parentUuid, Integer parentLft,Long knowledgeCircleId) {
		List<KnowledgeTypeVo> knowledgeTypeVoList = knowledgeTypeMapper.getKnowledgeTypeByParentUuid(parentUuid,knowledgeCircleId);
		for(KnowledgeTypeVo knowledgeType : knowledgeTypeVoList) {
			if(knowledgeType.getChildCount() == 0) {
				knowledgeTypeMapper.updateKnowledgeTypeLeftRightCode(knowledgeType.getUuid(), parentLft + 1, parentLft + 2);
				parentLft += 2;
			}else {
				int lft = parentLft + 1;
				parentLft = rebuildLeftRightCode(knowledgeType.getUuid(), lft,knowledgeCircleId);
				knowledgeTypeMapper.updateKnowledgeTypeLeftRightCode(knowledgeType.getUuid(), lft, parentLft + 1);
				parentLft += 1;
			}
		}
		return parentLft;
	}

	@Override
	public KnowledgeTypeVo buildRootKnowledgeType(Long knowledgeCircleId) {
		Integer maxRhtCode = knowledgeTypeMapper.getMaxRhtCode(knowledgeCircleId);
		KnowledgeTypeVo rootknowledgeType = new KnowledgeTypeVo();
		rootknowledgeType.setUuid(KnowledgeTypeVo.ROOT_UUID);
		rootknowledgeType.setName("root");
		rootknowledgeType.setParentUuid(KnowledgeTypeVo.ROOT_PARENTUUID);
		rootknowledgeType.setLft(1);
		rootknowledgeType.setRht(maxRhtCode == null ? 2 : maxRhtCode + 1);
		rootknowledgeType.setKnowledgeCircleId(knowledgeCircleId);
		return rootknowledgeType;
	}
}
