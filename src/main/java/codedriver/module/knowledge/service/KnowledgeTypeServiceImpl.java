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
	public void rebuildLeftRightCode() {
		rebuildLeftRightCode(KnowledgeTypeVo.ROOT_ID, 1);
	}
	
	private Integer rebuildLeftRightCode(Long parentId, Integer parentLft) {
		List<KnowledgeTypeVo> knowledgeTypeVoList = knowledgeTypeMapper.getKnowledgeTypeByParentId(parentId);
		for(KnowledgeTypeVo knowledgeType : knowledgeTypeVoList) {
			if(knowledgeType.getChildCount() == 0) {
				knowledgeTypeMapper.updateKnowledgeTypeLeftRightCode(knowledgeType.getId(), parentLft + 1, parentLft + 2);
				parentLft += 2;
			}else {
				int lft = parentLft + 1;
				parentLft = rebuildLeftRightCode(knowledgeType.getId(), lft);
				knowledgeTypeMapper.updateKnowledgeTypeLeftRightCode(knowledgeType.getId(), lft, parentLft + 1);
				parentLft += 1;
			}
		}
		return parentLft;
	}

	@Override
	public KnowledgeTypeVo buildRootKnowledgeType() {
		Integer maxRhtCode = knowledgeTypeMapper.getMaxRhtCode();
		KnowledgeTypeVo rootknowledgeType = new KnowledgeTypeVo();
		rootknowledgeType.setId(KnowledgeTypeVo.ROOT_ID);
		rootknowledgeType.setName("root");
		rootknowledgeType.setParentId(KnowledgeTypeVo.ROOT_PARENTID);
		rootknowledgeType.setLft(1);
		rootknowledgeType.setRht(maxRhtCode == null ? 2 : maxRhtCode + 1);
		return rootknowledgeType;
	}
}
