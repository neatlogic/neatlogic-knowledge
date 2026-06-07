package neatlogic.module.knowledge.api.feishu;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.knowledge.constvalue.Status;
import neatlogic.framework.knowledge.dto.feishu.FeiShuAppCredentialsVo;
import neatlogic.framework.knowledge.dto.feishu.FeiShuNodeVo;
import neatlogic.framework.knowledge.dto.feishu.KnowledgeFeiShuDocumentMappingVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_FEISHU_SYNC_MODIFY;
import neatlogic.module.knowledge.dao.mapper.KnowledgeFeiShuMapper;
import neatlogic.module.knowledge.service.KnowledgeFeiShuService;
import neatlogic.module.knowledge.utils.FeiShuOpenApiUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = KNOWLEDGE_FEISHU_SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchFeiShuWikiNodeApi extends PrivateApiComponentBase {
    @Resource
    private KnowledgeFeiShuService knowledgeFeiShuService;
    @Resource
    private KnowledgeFeiShuMapper knowledgeFeiShuMapper;
    @Override
    public String getToken() { return "knowledge/feishu/wiki/node/list"; }
    @Override
    public String getName() { return "获取飞书Wiki节点列表"; }

    @Input({
            @Param(name = "spaceId", type = ApiParamType.LONG, isRequired = true, desc = "Wiki空间ID"),
            @Param(name = "parentNodeToken", type = ApiParamType.STRING, desc = "父节点nodeToken")
    })
    @Description(desc = "获取飞书Wiki节点列表")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        List<KnowledgeFeiShuDocumentMappingVo> tbodyList = new ArrayList<>();
        FeiShuAppCredentialsVo feiShuAppCredentials = knowledgeFeiShuService.getFeiShuAppCredentials();
        if (feiShuAppCredentials == null) {
            return TableResultUtil.getResult(tbodyList);
        }
        Long spaceId = jsonObj.getLong("spaceId");
        String parentNodeToken = jsonObj.getString("parentNodeToken");
        String tenantAccessToken = FeiShuOpenApiUtil.getTenantAccessToken(feiShuAppCredentials.getAppId(), feiShuAppCredentials.getAppSecret());
        List<FeiShuNodeVo> feiShuNodeVoList = loadWikiNodes(spaceId, parentNodeToken, new ArrayList<>(), tenantAccessToken);
        if (CollectionUtils.isNotEmpty(feiShuNodeVoList)) {
            List<String> nodeTokenList = feiShuNodeVoList.stream().map(FeiShuNodeVo::getNodeToken).collect(Collectors.toList());
            List<KnowledgeFeiShuDocumentMappingVo> syncDocumentList = knowledgeFeiShuMapper.getSyncDocumentListByNodeTokenList(nodeTokenList);
            Map<String, KnowledgeFeiShuDocumentMappingVo> map = syncDocumentList.stream().collect(Collectors.toMap(KnowledgeFeiShuDocumentMappingVo::getNodeToken, e -> e));
            for (FeiShuNodeVo feiShuNodeVo : feiShuNodeVoList) {
                KnowledgeFeiShuDocumentMappingVo knowledgeFeiShuDocumentMappingVo = map.get(feiShuNodeVo.getNodeToken());
                if (knowledgeFeiShuDocumentMappingVo != null) {
                    knowledgeFeiShuDocumentMappingVo.setTitle(feiShuNodeVo.getTitle());
                    knowledgeFeiShuDocumentMappingVo.setObjToken(feiShuNodeVo.getObjToken());
                    knowledgeFeiShuDocumentMappingVo.setObjType(feiShuNodeVo.getObjType());
                    knowledgeFeiShuDocumentMappingVo.setUpdateTime(feiShuNodeVo.getUpdateTime());
                    knowledgeFeiShuDocumentMappingVo.setHasChild(feiShuNodeVo.getHasChild());
                    if (knowledgeFeiShuDocumentMappingVo.getKnowledgeDocumentVersionId() == null) {
                        knowledgeFeiShuDocumentMappingVo.setKnowledgeDocumentVersionId(1L);
                    }
                    if (knowledgeFeiShuDocumentMappingVo.getLcd() == null) {
                        knowledgeFeiShuDocumentMappingVo.setLcd(new Date());
                    }
                    if (knowledgeFeiShuDocumentMappingVo.getLcu() == null) {
                        knowledgeFeiShuDocumentMappingVo.setLcu(UserContext.get().getUserUuid());
                    }
                    if (knowledgeFeiShuDocumentMappingVo.getStatus() == null) {
                        knowledgeFeiShuDocumentMappingVo.setStatus(Status.NOT_SYNCED.getValue());
                        knowledgeFeiShuDocumentMappingVo.setStatusText(Status.NOT_SYNCED.getText());
                    }
                    tbodyList.add(knowledgeFeiShuDocumentMappingVo);
                } else {
                    tbodyList.add(new KnowledgeFeiShuDocumentMappingVo(feiShuAppCredentials.getAppId(), feiShuNodeVo));
                }
            }
        }
        int rowNum = tbodyList.size();
        int pageSize = ((rowNum / 20) + (rowNum % 20 > 0 ? 1 : 0)) * 20;
        BasePageVo basePageVo = new BasePageVo();
        basePageVo.setCurrentPage(1);
        basePageVo.setPageSize(Math.min(pageSize, 100));
        basePageVo.setRowNum(rowNum);
        return TableResultUtil.getResult(tbodyList, basePageVo);
    }

    private List<FeiShuNodeVo> loadWikiNodes(Long spaceId, String parentNodeToken, List<String> path, String tenantAccessToken) {
        List<FeiShuNodeVo> nodeList = new ArrayList<>();
        JSONObject result = FeiShuOpenApiUtil.getFeishuWikiNodes(spaceId, parentNodeToken, tenantAccessToken);
        JSONObject data = result.getJSONObject("data");
        if (data == null) {
            return nodeList;
        }
        JSONArray items = data.getJSONArray("items");
        if (CollectionUtils.isNotEmpty(items)) {
            for (int i = 0; i < items.size(); i++) {
                JSONObject item = items.getJSONObject(i);
                FeiShuNodeVo node = new FeiShuNodeVo(item);
                node.getPath().addAll(path);
                if (!isDocumentNode(node)) {
                    node.getPath().add(node.getTitle());
                }
                nodeList.add(node);
//                if (Objects.equals(item.getBoolean("has_child"), true)) {
//                    List<FeishuNode> children = loadWikiNodes(spaceId, node.getNodeToken(), node.getPath(), tenantAccessToken);
//                    node.setChildren(children);
////                    nodeList.addAll(children);
//                }
            }
        }
        return nodeList;
    }

    private boolean isDocumentNode(FeiShuNodeVo node) {
        return "docx".equals(node.getObjType()) || "doc".equals(node.getObjType());
    }
}
