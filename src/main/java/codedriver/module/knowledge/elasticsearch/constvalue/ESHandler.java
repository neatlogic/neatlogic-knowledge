package codedriver.module.knowledge.elasticsearch.constvalue;

public enum ESHandler {
	KNOWLEDGE("knowledge","知识"),
	KNOWLEDGE_VERSION("knowledgeversion","知识版本");

	private String value;
	private String text;

	private ESHandler(String _value, String _text) {
		this.value = _value;
		this.text = _text;
	}

	public String getValue() {
		return value;
	}

	public String getText() {
		return text;
	}

	public static String getValue(String _status) {
		for (ESHandler s : ESHandler.values()) {
			if (s.getValue().equals(_status)) {
				return s.getValue();
			}
		}
		return null;
	}

	public static String getText(String _status) {
		for (ESHandler s : ESHandler.values()) {
			if (s.getValue().equals(_status)) {
				return s.getText();
			}
		}
		return "";
	}

}
