package codedriver.module.knowledge.service;

import codedriver.framework.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import codedriver.framework.knowledge.dto.KnowledgeDocumentTypeVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
