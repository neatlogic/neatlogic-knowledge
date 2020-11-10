package codedriver.module.knowledge.dto;

import java.util.Arrays;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.module.knowledge.constvalue.KnowledgeType;

public class KnowledgeTypeVo {

    private String value;
    private String text;
    private int count;
    private JSONObject defaultCondition;
    public KnowledgeTypeVo(KnowledgeType type) {
        this.value = type.getValue();
        this.text = type.getText();
    }
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public int getCount() {
        return count;
    }
    public void setCount(int count) {
        this.count = count;
    }
    public JSONObject getDefaultCondition() {
        if(defaultCondition == null) {
            defaultCondition = new JSONObject();
            if(KnowledgeType.WAITINGFORREVIEW.getValue() == this.value) {
                defaultCondition.put("reviewerList", Arrays.asList(GroupSearch.USER.getValuePlugin()+UserContext.get().getUserUuid()));
            }else if(KnowledgeType.SHARE.getValue() == this.value){
                defaultCondition.put("lcuList", Arrays.asList(GroupSearch.USER.getValuePlugin()+UserContext.get().getUserUuid()));
                defaultCondition.put("statusList", Arrays.asList(KnowledgeDocumentVersionStatus.SUBMITTED));
            }else if(KnowledgeType.COLLECT.getValue() == this.value){
                defaultCondition.put("collector", GroupSearch.USER.getValuePlugin()+UserContext.get().getUserUuid());
            }else if(KnowledgeType.DRAFT.getValue() == this.value){
                defaultCondition.put("lcuList",  Arrays.asList(GroupSearch.USER.getValuePlugin()+UserContext.get().getUserUuid()));
                defaultCondition.put("statusList", Arrays.asList(KnowledgeDocumentVersionStatus.DRAFT));
            }
        }
        return defaultCondition;
    }

}
