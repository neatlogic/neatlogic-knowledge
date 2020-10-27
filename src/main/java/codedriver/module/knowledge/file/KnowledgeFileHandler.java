package codedriver.module.knowledge.file;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.file.core.FileTypeHandlerBase;
import codedriver.framework.file.dto.FileVo;

@Component
public class KnowledgeFileHandler extends FileTypeHandlerBase {

	@Override
	public boolean valid(String userUuid, JSONObject jsonObj) {
		return true;
	}

	@Override
	public String getDisplayName() {
		return "知识库附件";
	}

	@Override
	public void afterUpload(FileVo fileVo, JSONObject jsonObj) {
	}

	@Override
	public String getName() {
		return "KNOWLEDGE";
	}

}
