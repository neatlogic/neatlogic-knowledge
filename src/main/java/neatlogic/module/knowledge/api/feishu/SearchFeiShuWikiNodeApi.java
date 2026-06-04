package neatlogic.module.knowledge.api.feishu;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.knowledge.dto.feishu.FeishuNode;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_FEISHU_SYNC_MODIFY;
import neatlogic.module.knowledge.service.KnowledgeFeishuSyncService;
import neatlogic.module.knowledge.utils.FeiShuOpenApiUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AuthAction(action = KNOWLEDGE_FEISHU_SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchFeiShuWikiNodeApi extends PrivateApiComponentBase {
    @Resource
    private KnowledgeFeishuSyncService knowledgeFeishuSyncService;
    @Override
    public String getToken() { return "knowledge/feishu/wiki/node/list"; }
    @Override
    public String getName() { return "获取飞书Wiki节点列表"; }

    @Input({
            @Param(name = "spaceId", type = ApiParamType.LONG, isRequired = true, desc = "Wiki空间ID")
    })
    @Description(desc = "获取飞书Wiki节点列表")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        Long spaceId = jsonObj.getLong("spaceId");
        JSONObject feiShuAppCredentials = knowledgeFeishuSyncService.getFeiShuAppCredentials();
        String tenantAccessToken = FeiShuOpenApiUtil.getTenantAccessToken(feiShuAppCredentials.getString("appId"), feiShuAppCredentials.getString("appSecret"));
        List<FeishuNode> feishuNodeList = loadWikiNodes(spaceId, null, new ArrayList<>(), tenantAccessToken);
        return TableResultUtil.getResult(feishuNodeList);
    }

    private List<FeishuNode> loadWikiNodes(Long spaceId, String parentNodeToken, List<String> path, String tenantAccessToken) {
        List<FeishuNode> nodeList = new ArrayList<>();
        JSONObject result = FeiShuOpenApiUtil.getFeishuWikiNodes(spaceId, parentNodeToken, tenantAccessToken);
        JSONObject data = result.getJSONObject("data");
        if (data == null) {
            return nodeList;
        }
        JSONArray items = data.getJSONArray("items");
        if (CollectionUtils.isNotEmpty(items)) {
            for (int i = 0; i < items.size(); i++) {
                JSONObject item = items.getJSONObject(i);
                FeishuNode node = new FeishuNode(item);
                node.getPath().addAll(path);
                if (!isDocumentNode(node)) {
                    node.getPath().add(node.getTitle());
                }
                nodeList.add(node);
                if (Objects.equals(item.getBoolean("has_child"), true)) {
                    List<FeishuNode> children = loadWikiNodes(spaceId, node.getNodeToken(), node.getPath(), tenantAccessToken);
//                    node.setChildren(children);
                    nodeList.addAll(children);
                }
            }
        }
        return nodeList;
    }

    private boolean isDocumentNode(FeishuNode node) {
        return "docx".equals(node.getObjType()) || "doc".equals(node.getObjType());
    }
}
