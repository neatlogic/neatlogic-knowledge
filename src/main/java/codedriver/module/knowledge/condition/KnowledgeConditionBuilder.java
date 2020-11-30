package codedriver.module.knowledge.condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.FormHandlerType;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.module.knowledge.constvalue.KnowledgeType;

public class KnowledgeConditionBuilder {
     
    private JSONArray conditionArray = new JSONArray();

    public JSONArray build() {
        conditionArray.sort(Comparator.comparing(obj -> ((JSONObject)obj).getInteger("sort")));
        return conditionArray;
    }
    
    /**
    * @Author 89770
    * @Time 2020年11月9日  
    * @Description: 提交人/修改人
     */
    public KnowledgeConditionBuilder setLcu(String knowledgeType) {
        if(knowledgeType.equals(KnowledgeType.SHARE.getValue())) {
            return this;
        }
        JSONObject conditionJson = new JSONObject();
        conditionJson.put("handler", "lcuList");
        conditionJson.put("handlerName", "修改人");
        if(knowledgeType.equals(KnowledgeType.WAITINGFORREVIEW.getValue())) {
            conditionJson.put("handlerName", "提交人");
        }
        conditionJson.put("handlerType", FormHandlerType.USERSELECT.toString());
        
        JSONObject config = new JSONObject();
        config.put("type", FormHandlerType.USERSELECT.toString());
        config.put("groupList", Arrays.asList("user"));
        config.put("multiple", true);
        conditionJson.put("config", config);
        conditionJson.put("sort", 3);
        conditionArray.add(conditionJson);
        return this;
    }
    
    /**
     * @Author 89770
     * @Time 2020年11月9日  
     * @Description: 提交/修改时间
      */
     public KnowledgeConditionBuilder setLcd(String knowledgeType) {
         JSONObject conditionJson = new JSONObject();
         conditionJson.put("handler", "lcd");
         conditionJson.put("handlerName", "修改时间");
         if(knowledgeType.equals(KnowledgeType.WAITINGFORREVIEW.getValue())) {
             conditionJson.put("handlerName", "提交时间");
         }
         conditionJson.put("handlerType", FormHandlerType.TIMESELECT.toString());
         
         JSONObject config = new JSONObject();
         config.put("type", "datetimerange");
         config.put("value", "");
         config.put("defaultValue", "");
         config.put("format", "yyyy-MM-dd HH:mm:ss");
         config.put("valueType", "timestamp");
         conditionJson.put("config", config);
         conditionJson.put("sort", 4);
         conditionArray.add(conditionJson);
         return this;
     }
     
     /**
      * @Author 89770
      * @Time 2020年11月9日  
      * @Description: 标签
       */
      public KnowledgeConditionBuilder setTag() {
          JSONObject conditionJson = new JSONObject();
          conditionJson.put("handler", "tagList");
          conditionJson.put("handlerName", "标签");
          conditionJson.put("handlerType", FormHandlerType.SELECT.toString());
          
          JSONObject config = new JSONObject();
          config.put("type", FormHandlerType.SELECT.toString());
          config.put("search", true);
          config.put("dynamicUrl", "api/rest/knowledge/tag/list");
          config.put("rootName", "list");
          config.put("valueName", "value");
          config.put("textName", "text");
          config.put("multiple", true);
          config.put("value", "");
          config.put("defaultValue", "");
          
          conditionJson.put("config", config);
          conditionJson.put("sort", 5);
          conditionArray.add(conditionJson);
          return this;
      }
      
      /**
       * @Author 89770
       * @Time 2020年11月9日  
       * @Description: 来源
        */
       public KnowledgeConditionBuilder setSource() {
           JSONObject conditionJson = new JSONObject();
           conditionJson.put("handler", "sourceList");
           conditionJson.put("handlerName", "来源");
           conditionJson.put("handlerType", FormHandlerType.SELECT.toString());
           
           //TODO 后续从枚举类获取，暂时写shi
           JSONArray dataList = new JSONArray();
           dataList.add(new ValueTextVo("processtask", "工单"));
           
           JSONObject config = new JSONObject();
           config.put("type", FormHandlerType.SELECT.toString());
           config.put("search", false);
           config.put("multiple", true);
           config.put("value", "");
           config.put("defaultValue", new ArrayList<String>());
           config.put("dataList", dataList);
           conditionJson.put("config", config);
           conditionJson.put("sort", 6);
           conditionArray.add(conditionJson);
           return this;
       }
       
       /**
        * @Author 89770
        * @Time 2020年11月9日  
        * @Description: 审核时间
         */
        public KnowledgeConditionBuilder setReviewDate(String knowledgeType) {
            if(!knowledgeType.equals(KnowledgeType.SHARE.getValue())) {
                return this;
            }
            JSONObject conditionJson = new JSONObject();
            conditionJson.put("handler", "reviewDate");
            conditionJson.put("handlerName", "审核时间");
            conditionJson.put("handlerType", FormHandlerType.TIMESELECT.toString());
            
            JSONObject config = new JSONObject();
            config.put("type", "datetimerange");
            config.put("value", "");
            config.put("defaultValue", "");
            config.put("format", "yyyy-MM-dd HH:mm:ss");
            config.put("valueType", "timestamp");
            conditionJson.put("config", config);
            conditionJson.put("sort", 2);
            conditionArray.add(conditionJson);
            return this;
        }
        
        /**
         * @Author 89770
         * @Time 2020年11月9日  
         * @Description: 审核人
          */
         public KnowledgeConditionBuilder setReviewer(String knowledgeType) {
             if(!knowledgeType.equals(KnowledgeType.SHARE.getValue())) {
                 return this;
             }
             JSONObject conditionJson = new JSONObject();
             conditionJson.put("handler", "reviewerList");
             conditionJson.put("handlerName", "审批人");
             conditionJson.put("handlerType", FormHandlerType.USERSELECT.toString());
             
             JSONObject config = new JSONObject();
             config.put("type", FormHandlerType.USERSELECT.toString());
             config.put("groupList", Arrays.asList("user"));
             config.put("multiple", true);
             conditionJson.put("config", config);
             conditionJson.put("sort", 1);
             conditionArray.add(conditionJson);
             return this;
         }
    
         /**
          * @Author 89770
          * @Time 2020年11月9日  
          * @Description: 审核人
           */
          public KnowledgeConditionBuilder setReviewStatus(String knowledgeType) {
              if(!KnowledgeType.SHARE.getValue().equals(knowledgeType)&&!KnowledgeType.WAITINGFORREVIEW.getValue().equals(knowledgeType)) {
                  return this;
              }
              JSONObject conditionJson = new JSONObject();
              conditionJson.put("handler", "statusList");
              conditionJson.put("handlerName", "审核状态");
              conditionJson.put("handlerType", FormHandlerType.SELECT.toString());
              
              JSONArray dataList = new JSONArray();
              dataList.add(new ValueTextVo(KnowledgeDocumentVersionStatus.ALL.getValue(), KnowledgeDocumentVersionStatus.ALL.getText()));
              dataList.add(new ValueTextVo(KnowledgeDocumentVersionStatus.SUBMITTED.getValue(), KnowledgeDocumentVersionStatus.SUBMITTED.getText()));
              dataList.add(new ValueTextVo(KnowledgeDocumentVersionStatus.PASSED.getValue(), KnowledgeDocumentVersionStatus.PASSED.getText()));
              dataList.add(new ValueTextVo(KnowledgeDocumentVersionStatus.REJECTED.getValue(), KnowledgeDocumentVersionStatus.REJECTED.getText()));
              
              JSONObject config = new JSONObject();
              config.put("type", FormHandlerType.SELECT.toString());
              config.put("search", false);
              config.put("multiple", false);
              config.put("value", "");
              config.put("defaultValue", KnowledgeDocumentVersionStatus.SUBMITTED.getValue());
              config.put("dataList", dataList);
              conditionJson.put("config", config);
              conditionJson.put("sort", 7);
              conditionArray.add(conditionJson);
              return this;
          }
    
}
