package neatlogic.module.knowledge.api.feishu;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_FEISHU_SYNC_MODIFY;
import neatlogic.module.knowledge.service.KnowledgeFeishuSyncService;
import neatlogic.module.knowledge.utils.FeiShuOpenApiUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = KNOWLEDGE_FEISHU_SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListFeiShuWikiSpaceApi extends PrivateApiComponentBase {
    @Resource
    private KnowledgeFeishuSyncService knowledgeFeishuSyncService;
    @Override
    public String getToken() { return "knowledge/feishu/wiki/space/list"; }
    @Override
    public String getName() { return "获取飞书 Wiki 空间列表"; }
    @Input({})
    @Output({})
    @Description(desc = "获取飞书 Wiki 空间列表")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        JSONObject feiShuAppCredentials = knowledgeFeishuSyncService.getFeiShuAppCredentials();
        String tenantAccessToken = FeiShuOpenApiUtil.getTenantAccessToken(feiShuAppCredentials.getString("appId"), feiShuAppCredentials.getString("appSecret"));
        JSONObject resultObj = FeiShuOpenApiUtil.getFeishuWikiSpaces(tenantAccessToken);
        JSONArray wikiSpaceList = new JSONArray();
        JSONObject data = resultObj.getJSONObject("data");
        if (MapUtils.isNotEmpty(data)) {
            JSONArray items = data.getJSONArray("items");
            if (CollectionUtils.isNotEmpty(items)) {
                wikiSpaceList.addAll(items);
            }
        }
        return TableResultUtil.getResult(wikiSpaceList);
    }
}
