package neatlogic.module.knowledge.api.feishu;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.oceanbase.jdbc.internal.util.StringCacheUtil;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.knowledge.constvalue.Status;
import neatlogic.framework.knowledge.dto.feishu.FeiShuAppCredentialsVo;
import neatlogic.framework.knowledge.dto.feishu.FeiShuNodeSearchVo;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
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
    public String getName() { return "nmkaf.searchfeishuwikinodeapi.getname"; }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize"),
            @Param(name = "status", type = ApiParamType.ENUM, member = Status.class, desc = "common.status"),
            @Param(name = "spaceId", type = ApiParamType.LONG, isRequired = true, desc = "nmkaf.searchfeishuwikinodeapi.input.param.desc.spaceid"),
            @Param(name = "parentNodeToken", type = ApiParamType.STRING, desc = "nmkaf.searchfeishuwikinodeapi.input.param.desc.parentnodetoken")
    })
    @Description(desc = "nmkaf.searchfeishuwikinodeapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        List<KnowledgeFeiShuDocumentMappingVo> tbodyList = new ArrayList<>();
        FeiShuAppCredentialsVo feiShuAppCredentials = knowledgeFeiShuService.getFeiShuAppCredentials();
        if (feiShuAppCredentials == null) {
            return TableResultUtil.getResult(tbodyList);
        }
        FeiShuNodeSearchVo feiShuNodeSearchVo = jsonObj.toJavaObject(FeiShuNodeSearchVo.class);
        if (Objects.equals(feiShuNodeSearchVo.getStatus(), Status.SUCCEED.getValue())
                || Objects.equals(feiShuNodeSearchVo.getStatus(), Status.FAILED.getValue())
                || Objects.equals(feiShuNodeSearchVo.getStatus(), Status.RUNNING.getValue())
                || Objects.equals(feiShuNodeSearchVo.getStatus(), Status.WAITING.getValue())
                || Objects.equals(feiShuNodeSearchVo.getStatus(), Status.UNSUPPORTED.getValue())
        ) {
            return searchForDB(feiShuNodeSearchVo, feiShuAppCredentials);
        } else {
            return searchForFeiShuOpenApi(feiShuNodeSearchVo, feiShuAppCredentials);
        }
    }

    private JSONObject searchForFeiShuOpenApi(FeiShuNodeSearchVo feiShuNodeSearchVo, FeiShuAppCredentialsVo feiShuAppCredentials) {
        List<KnowledgeFeiShuDocumentMappingVo> tbodyList = new ArrayList<>();
        Long spaceId = feiShuNodeSearchVo.getSpaceId();
        String parentNodeToken = feiShuNodeSearchVo.getParentNodeToken();
        String keyword = feiShuNodeSearchVo.getKeyword();
        String status = feiShuNodeSearchVo.getStatus();
        String tenantAccessToken = FeiShuOpenApiUtil.getTenantAccessToken(feiShuAppCredentials.getAppId(), feiShuAppCredentials.getAppSecret());
        List<FeiShuNodeVo> feiShuNodeVoList = loadWikiNodes(spaceId, parentNodeToken, new ArrayList<>(), tenantAccessToken);
        if (CollectionUtils.isNotEmpty(feiShuNodeVoList)) {
            List<String> nodeTokenList = feiShuNodeVoList.stream().map(FeiShuNodeVo::getNodeToken).collect(Collectors.toList());
            List<KnowledgeFeiShuDocumentMappingVo> syncDocumentList = knowledgeFeiShuMapper.getFeiShuDocumentMappingListByNodeTokenList(nodeTokenList);
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
        if (StringUtils.isNotBlank(keyword) || StringUtils.isNotBlank(status)) {
            // Filter after loading FeiShu nodes so unsynced nodes can also match keyword/status.
            tbodyList = tbodyList.stream().filter(item -> {
                boolean isKeywordMatched = StringUtils.isBlank(keyword)
                        || StringUtils.containsIgnoreCase(item.getTitle(), keyword)
                        || Objects.equals(item.getNodeToken(), keyword)
                        || Objects.equals(item.getObjToken(), keyword)
                        || StringUtils.containsIgnoreCase(item.getConfigStr(), keyword);
                boolean isStatusMatched = StringUtils.isBlank(status) || Objects.equals(item.getStatus(), status);
                return isKeywordMatched && isStatusMatched;
            }).collect(Collectors.toList());
        }
        int rowNum = tbodyList.size();
        BasePageVo basePageVo = new BasePageVo();
//        int pageSize = ((rowNum / 20) + (rowNum % 20 > 0 ? 1 : 0)) * 20;
//        basePageVo.setCurrentPage(1);
//        basePageVo.setPageSize(Math.min(pageSize, 100));
        basePageVo.setCurrentPage(feiShuNodeSearchVo.getCurrentPage());
        basePageVo.setPageSize(feiShuNodeSearchVo.getPageSize());
        basePageVo.setRowNum(rowNum);
        // Return only the current page and calculate page metadata with the filtered row count.
        int fromIndex = Math.min(basePageVo.getStartNum(), rowNum);
        int toIndex = Math.min(fromIndex + basePageVo.getPageSize(), rowNum);
        return TableResultUtil.getResult(tbodyList.subList(fromIndex, toIndex), basePageVo);
    }

    private JSONObject searchForDB(FeiShuNodeSearchVo feiShuNodeSearchVo, FeiShuAppCredentialsVo feiShuAppCredentials) {
        // DB search reads knowledge_feishu_document_mapping directly and avoids FeiShu OpenAPI calls.
        int rowNum = knowledgeFeiShuMapper.searchFeiShuDocumentMappingCount(feiShuNodeSearchVo);
        feiShuNodeSearchVo.setRowNum(rowNum);
        List<KnowledgeFeiShuDocumentMappingVo> tbodyList = knowledgeFeiShuMapper.searchFeiShuDocumentMappingList(feiShuNodeSearchVo);
        return TableResultUtil.getResult(tbodyList, feiShuNodeSearchVo);
    }

    private List<FeiShuNodeVo> loadWikiNodes(Long spaceId, String parentNodeToken, List<String> path, String tenantAccessToken) {
        List<FeiShuNodeVo> nodeList = new ArrayList<>();
        JSONObject result = FeiShuOpenApiUtil.getFeiShuWikiNodes(spaceId, parentNodeToken, tenantAccessToken);
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
