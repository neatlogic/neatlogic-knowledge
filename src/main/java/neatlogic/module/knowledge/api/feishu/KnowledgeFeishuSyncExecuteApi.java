package neatlogic.module.knowledge.api.feishu;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.util.FileUtil;
import neatlogic.framework.file.dao.mapper.FileMapper;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.fulltextindex.core.FullTextIndexHandlerFactory;
import neatlogic.framework.fulltextindex.core.IFullTextIndexHandler;
import neatlogic.framework.knowledge.constvalue.FeiShuAlignType;
import neatlogic.framework.knowledge.constvalue.FeiShuBlockType;
import neatlogic.framework.knowledge.constvalue.KnowledgeFullTextIndexType;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import neatlogic.framework.knowledge.dto.feishu.FeishuNode;
import neatlogic.module.knowledge.dao.mapper.KnowledgeFeishuSyncMapper;
import neatlogic.framework.knowledge.dto.*;
import neatlogic.framework.knowledge.dto.feishu.KnowledgeFeishuSyncAuditVo;
import neatlogic.framework.knowledge.dto.feishu.KnowledgeFeishuSyncConfigVo;
import neatlogic.framework.knowledge.dto.feishu.KnowledgeFeishuSyncDocumentVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.framework.util.UuidUtil;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_FEISHU_SYNC_MODIFY;
import neatlogic.module.knowledge.service.KnowledgeDocumentTypeService;
import neatlogic.module.knowledge.source.FeishuSyncSource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

@Service
@AuthAction(action = KNOWLEDGE_FEISHU_SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class KnowledgeFeishuSyncExecuteApi extends PrivateApiComponentBase {

    private final Logger logger = LoggerFactory.getLogger(KnowledgeFeishuSyncExecuteApi.class);
    private final String TENANT_ACCESS_TOKEN_INTERNAL_URL = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
    private final String WIKI_V2_SPACES_URL = "https://open.feishu.cn/open-apis/wiki/v2/spaces";
    private final String SPACE_NODES_URL = "https://open.feishu.cn/open-apis/wiki/v2/spaces/:space_id/nodes";
    private final String DOCUMENT_BLOCKS_URL = "https://open.feishu.cn/open-apis/docx/v1/documents/:document_id/blocks";
    @Resource
    private KnowledgeFeishuSyncMapper knowledgeFeishuSyncMapper;
    @Resource
    private FileMapper fileMapper;
//    @Resource
//    private KnowledgeCircleMapper knowledgeCircleMapper;
    @Resource
    private KnowledgeDocumentTypeMapper knowledgeDocumentTypeMapper;
    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Resource
    private KnowledgeDocumentTypeService knowledgeDocumentTypeService;
    @Override
    public String getToken() { return "knowledge/feishu/sync/execute"; }
    @Override
    public String getName() { return "执行飞书云文档同步"; }
    @Override
    public String getConfig() { return null; }
    @Input({
            @Param(name = "configId", type = ApiParamType.LONG, isRequired = true, desc = "配置 ID"),
//            @Param(name = "direction", type = ApiParamType.ENUM, rule = "from_feishu,to_feishu", desc = "同步方向"),
            @Param(name = "knowledgeDocumentId", type = ApiParamType.LONG, desc = "知识文档 ID")
    })
    @Description(desc = "执行飞书云文档同步")
    @Override
    public Object myDoService(JSONObject jsonObj) {
//        String direction = jsonObj.getString("direction");
//        if ("to_feishu".equals(direction)) {
//            return knowledgeFeishuSyncService.syncToFeishu(jsonObj.getLong("configId"), jsonObj.getLong("knowledgeDocumentId"));
//        }
        return syncFromFeishu(jsonObj.getLong("configId"));
    }

//    @Override
//    @Transactional
    public KnowledgeFeishuSyncAuditVo syncFromFeishu(Long configId) {
        KnowledgeFeishuSyncConfigVo config = getRequiredConfig(configId);
        Long knowledgeCircleId = config.getKnowledgeCircleId();
        String tenantAccessToken = getTenantAccessToken(config);
        System.out.println("tenantAccessToken = " + tenantAccessToken);
        KnowledgeFeishuSyncAuditVo audit = startAudit(configId, "from_feishu");
        try {
            if (!Objects.equals(config.getIsActive(), 1)) {
                throw new RuntimeException("同步配置未启用");
            }
            JSONArray wikiSpaceList = feishuWikiSpaces(tenantAccessToken);
            if (CollectionUtils.isNotEmpty(wikiSpaceList)) {
                for (int i = 0; i < wikiSpaceList.size(); i++) {
                    JSONObject wikiSpaceObj = wikiSpaceList.getJSONObject(i);
                    System.out.println("wikiSpaceObj = " + wikiSpaceObj);
                    Long spaceId = wikiSpaceObj.getLong("space_id");
                    String spaceName = wikiSpaceObj.getString("name");
                    if (spaceId != null) {
//                        if (!Objects.equals(spaceId, 7644786409622113212L)) {
//                            continue;
//                        }
                        KnowledgeDocumentTypeVo knowledgeType = getOrCreateKnowledgeType(spaceName, "0", knowledgeCircleId);
                        List<FeishuNode> nodes = loadWikiNodes(config, spaceId, null, new ArrayList<>(), tenantAccessToken);
                        System.out.println("nodes = " + JSONObject.toJSONString(nodes));
                        saveNodes(spaceId, spaceName, nodes, config, knowledgeType, audit, tenantAccessToken);
                    }
                }
            }

            finishAudit(audit, audit.getFailedCount() == 0 ? "succeed" : "failed", null);
//            finishAudit(audit, failed == 0 ? "succeed" : "failed", total, success, failed, null, detailList);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            finishAudit(audit, "failed", ex.getMessage());
//            finishAudit(audit, "failed", total, success, failed == 0 ? 1 : failed, ex.getMessage(), detailList);
        }
        knowledgeDocumentTypeService.rebuildLeftRightCode(knowledgeCircleId);
        return audit;
    }

    /**
     *
     * @param config
     * @return
     */
    private String getTenantAccessToken(KnowledgeFeishuSyncConfigVo config) {
        JSONObject body = new JSONObject();
        body.put("app_id", config.getAppId());
        body.put("app_secret", config.getAppSecret());
        JSONObject result = HttpRequestUtil.post(TENANT_ACCESS_TOKEN_INTERNAL_URL)
                .setPayload(body.toJSONString())
                .sendRequest()
                .getResultJson();
        System.out.println("getTenantAccessToken result = " + result);
        /*
        {
            "msg": "ok",
            "code": 0,
            "expire": 4119,
            "tenant_access_token": "t-g10462hfSE4CYZ3Y5DD6CCQNPSH3S4GOZZBPA4FG"
        }
         */
        checkFeishuResult(result);
        return result.getString("tenant_access_token");
    }

    /**
     * {
     * 	"msg": "success",
     * 	"code": 0,
     * 	"data": {
     * 		"page_token": "0||7644786409622113212",
     * 		"has_more": false,
     * 		"items": [
     * 			            {
     * 				"open_sharing": "closed",
     * 				"visibility": "public",
     * 				"space_type": "team",
     * 				"name": "前端研发规范",
     * 				"description": "",
     * 				"space_id": "7208706433871773699"
     *            },
     *            {
     * 				"open_sharing": "closed",
     * 				"visibility": "public",
     * 				"space_type": "team",
     * 				"name": "后端研发规范",
     * 				"description": "",
     * 				"space_id": "7208734024653832196"
     *            },
     *            {
     * 				"open_sharing": "closed",
     * 				"visibility": "public",
     * 				"space_type": "team",
     * 				"name": "设计文档",
     * 				"description": "",
     * 				"space_id": "7208737988736483331"
     *            },
     *            {
     * 				"open_sharing": "closed",
     * 				"visibility": "public",
     * 				"space_type": "team",
     * 				"name": "研发管理",
     * 				"description": "",
     * 				"space_id": "7208739847777828867"
     *            },
     *            {
     * 				"open_sharing": "closed",
     * 				"visibility": "public",
     * 				"space_type": "team",
     * 				"name": "项目文档",
     * 				"description": "",
     * 				"space_id": "7208744663619223555"
     *            },
     *            {
     * 				"open_sharing": "closed",
     * 				"visibility": "public",
     * 				"space_type": "team",
     * 				"name": "技术方案",
     * 				"description": "",
     * 				"space_id": "7208747120034283523"
     *            },
     *            {
     * 				"open_sharing": "closed",
     * 				"visibility": "public",
     * 				"space_type": "team",
     * 				"name": "产品手册",
     * 				"description": "",
     * 				"space_id": "7208748423435173892"
     *            },
     *            {
     * 				"open_sharing": "closed",
     * 				"visibility": "public",
     * 				"space_type": "team",
     * 				"name": "常见问题",
     * 				"description": "",
     * 				"space_id": "7208872407434592260"
     *            },
     *            {
     * 				"open_sharing": "closed",
     * 				"visibility": "public",
     * 				"space_type": "team",
     * 				"name": "国产系统&软件适配",
     * 				"description": "",
     * 				"space_id": "7208876213417410563"
     *            },
     *            {
     * 				"open_sharing": "closed",
     * 				"visibility": "private",
     * 				"space_type": "team",
     * 				"name": "交付问题知识库",
     * 				"description": "交付问题知识库",
     * 				"space_id": "7397619128632180764"
     *            },
     *            {
     * 				"open_sharing": "closed",
     * 				"visibility": "private",
     * 				"space_type": "team",
     * 				"name": "linbq测试",
     * 				"description": "test",
     * 				"space_id": "7644786409622113212"
     *            }
     * 		]
     * 	}
     * }
     * @param pageToken
     * @param tenantAccessToken
     * @return
     */
    private JSONObject getFeishuWikiSpaces(String pageToken, String tenantAccessToken) {
        JSONObject query = new JSONObject();
        query.put("page_size", 50);// 最大值是50
        if (StringUtils.isNotBlank(pageToken)) {
            query.put("page_token", pageToken);
        }
        HttpRequestUtil request = HttpRequestUtil.get(WIKI_V2_SPACES_URL)
                .addHeader("Authorization", "Bearer " + tenantAccessToken)
                .setQueryString(query);
        JSONObject result = request.sendRequest().getResultJson();
        System.out.println("feishuWikiSpaces result = " + result);
        checkFeishuResult(result);
        return result;
    }

    /**
     * {
     * 	"msg": "success",
     * 	"code": 0,
     * 	"data": {
     * 		"page_token": "",
     * 		"has_more": false,
     * 		"items": [
     * 			            {
     * 				"owner": "ou_3ef5dcb99755ec0a091af657adca1d58",
     * 				"creator": "ou_3ef5dcb99755ec0a091af657adca1d58",
     * 				"obj_create_time": "1678407262",
     * 				"node_token": "wikcnxWQmsOGQdxLzCSmAKJ0wRe",
     * 				"origin_space_id": "7208706433871773699",
     * 				"title": "前端开发规范（新平台）",
     * 				"url": "https://lqnnbz38z5y.feishu.cn/wiki/wikcnxWQmsOGQdxLzCSmAKJ0wRe",
     * 				"obj_edit_time": "1778754590",
     * 				"node_type": "origin",
     * 				"origin_node_token": "wikcnxWQmsOGQdxLzCSmAKJ0wRe",
     * 				"node_create_time": "1678407262",
     * 				"obj_token": "YsLKdHukxojuJ0x06iBc8IClnff",
     * 				"obj_type": "docx",
     * 				"has_child": false,
     * 				"space_id": "7208706433871773699",
     * 				"parent_node_token": ""
     *            },
     *            {
     * 				"owner": "ou_3ef5dcb99755ec0a091af657adca1d58",
     * 				"creator": "ou_3ef5dcb99755ec0a091af657adca1d58",
     * 				"obj_create_time": "1678407135",
     * 				"node_token": "wikcnQQtAljD3d0I6lOctjwhaJc",
     * 				"origin_space_id": "7208706433871773699",
     * 				"title": "codedriver 新增一个模块和对应图标指南",
     * 				"url": "https://lqnnbz38z5y.feishu.cn/wiki/wikcnQQtAljD3d0I6lOctjwhaJc",
     * 				"obj_edit_time": "1719475731",
     * 				"node_type": "origin",
     * 				"origin_node_token": "wikcnQQtAljD3d0I6lOctjwhaJc",
     * 				"node_create_time": "1678407135",
     * 				"obj_token": "Y4ntdrlFWoY0pnxZLYOc4sNBn2c",
     * 				"obj_type": "docx",
     * 				"has_child": false,
     * 				"space_id": "7208706433871773699",
     * 				"parent_node_token": ""
     *            }
     * 		]
     * 	}
     * }
     * @param config
     * @param spaceId
     * @param parentNodeToken
     * @param pageToken
     * @return
     */
    private JSONObject getFeishuWikiNodes(KnowledgeFeishuSyncConfigVo config, Long spaceId, String parentNodeToken, String pageToken, String tenantAccessToken) {
        String url = SPACE_NODES_URL.replace(":space_id", spaceId.toString());
        System.out.println("url = " + url);
        JSONObject query = new JSONObject();
        query.put("page_size", 50);
        if (StringUtils.isNotBlank(parentNodeToken)) {
            query.put("parent_node_token", parentNodeToken);
        }
        if (StringUtils.isNotBlank(pageToken)) {
            query.put("page_token", pageToken);
        }
        HttpRequestUtil request = HttpRequestUtil.get(url)
                .addHeader("Authorization", "Bearer " + tenantAccessToken)
                .setQueryString(query);
        JSONObject result = request.sendRequest().getResultJson();
        System.out.println("getFeishuWikiNodes result = " + result);
        checkFeishuResult(result);
        return result;
    }

    private JSONObject getDocumentBlocks(KnowledgeFeishuSyncConfigVo config, String objToken, String tenantAccessToken) {
        JSONArray allItems = new JSONArray();
        String url = DOCUMENT_BLOCKS_URL.replace(":document_id", objToken);
        String pageToken = null;
        Boolean hasMore = false;
        do {
            JSONObject query = new JSONObject();
            query.put("page_size", 50);
            if (StringUtils.isNotBlank(pageToken)) {
                query.put("page_token", pageToken);
            }
            System.out.println("url = " + url);
            HttpRequestUtil request = HttpRequestUtil.get(url)
                    .addHeader("Authorization", "Bearer " + tenantAccessToken)
                    .setQueryString(query);
            JSONObject result = request.sendRequest().getResultJson();
//            System.out.println("getDocumentBlocks result = " + result);
            checkFeishuResult(result);
            JSONObject data = result.getJSONObject("data");
            if (MapUtils.isNotEmpty(data)) {
                hasMore = data.getBoolean("has_more");
                pageToken = data.getString("page_token");
                JSONArray items = data.getJSONArray("items");
                if (CollectionUtils.isNotEmpty(items)) {
                    allItems.addAll(items);
                }
            }
        } while (Objects.equals(hasMore, true));
        return new JSONObject().fluentPut("code", 0).fluentPut("mas", "success").fluentPut("data", new JSONObject().fluentPut("has_more", false).fluentPut("items", allItems));
    }

    /**
     * 飞书下载素材
     * @param fileToken
     * @param tenantAccessToken
     * @return
     */
    private FileVo downloadMedias(String fileToken, String tenantAccessToken) {
        String url = "https://open.feishu.cn/open-apis/drive/v1/medias/:file_token/download";
        url = url.replace(":file_token", fileToken);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HttpRequestUtil request = HttpRequestUtil.get(url)
                .addHeader("Authorization", "Bearer " + tenantAccessToken)
                // 飞书下载素材接口返回二进制流，这里把响应先写入内存，再统一走系统附件保存逻辑。
                .setOutputStream(outputStream)
                .sendRequest();
        if (StringUtils.isNotBlank(request.getError())) {
            throw new RuntimeException(request.getError());
        }
        if (request.getResponseCode() != 200 && request.getResponseCode() != 206) {
            throw new RuntimeException("Feishu media download failed, responseCode:" + request.getResponseCode());
        }
        Map<String, List<String>> responseHeaderMap = request.getResponseHeaderMap();
        String contentType = getFirstResponseHeader(responseHeaderMap, "content-type");
        String contentDisposition = getFirstResponseHeader(responseHeaderMap, "content-disposition");
        String fileName = parseFeishuMediaFileName(contentDisposition, fileToken);
        byte[] data = outputStream.toByteArray();
        try {
//            MultipartFile multipartFile = buildFeishuMediaMultipartFile(fileName, contentType, data);
            FileVo fileVo = new FileVo();
            // 飞书素材同步到知识库后按知识库附件类型保存，便于后续下载和删除校验复用现有逻辑。
            fileVo.setType("knowledge");
//            fileVo.setName(multipartFile.getOriginalFilename());
//            fileVo.setSize(multipartFile.getSize());
//            fileVo.setContentType(multipartFile.getContentType());
            fileVo.setName(fileName);
            fileVo.setSize((long) data.length);
            fileVo.setContentType(contentType);
            fileVo.setUserUuid(UserContext.get().getUserUuid(true));
            fileVo.setUploadTime(new Date());
            String tenantUuid = TenantContext.get().getTenantUuid();
//            String filePath = FileUtil.saveData(tenantUuid, multipartFile.getInputStream(), fileVo);
            String filePath = FileUtil.saveData(tenantUuid, new ByteArrayInputStream(data), fileVo);
            fileVo.setPath(filePath);
            fileMapper.insertFile(fileVo);
            return fileVo;
        } catch (Exception ex) {
            throw new RuntimeException("Feishu media save failed, fileToken:" + fileToken, ex);
        }
    }

    /**
     * 从响应头 Map 中按忽略大小写方式获取第一个 Header 值，兼容 JDK 返回的 Header 名大小写差异。
     *
     * @param responseHeaderMap 响应头 Map
     * @param headerName        Header 名称
     * @return Header 第一个值
     */
    private String getFirstResponseHeader(Map<String, List<String>> responseHeaderMap, String headerName) {
        if (MapUtils.isEmpty(responseHeaderMap) || StringUtils.isBlank(headerName)) {
            return null;
        }
        for (Map.Entry<String, List<String>> entry : responseHeaderMap.entrySet()) {
            if (entry.getKey() != null && headerName.equalsIgnoreCase(entry.getKey()) && CollectionUtils.isNotEmpty(entry.getValue())) {
                return entry.getValue().get(0);
            }
        }
        return null;
    }

    /**
     * 飞书通过 Content-Disposition 返回素材文件名，解析失败时使用 fileToken 兜底，避免附件名称为空。
     *
     * @param contentDisposition 飞书响应头 Content-Disposition
     * @param fileToken          素材 token
     * @return 素材文件名
     */
    private String parseFeishuMediaFileName(String contentDisposition, String fileToken) {
        String fileName = null;
        if (StringUtils.isNotBlank(contentDisposition)) {
            String[] partArray = contentDisposition.split(";");
            for (String part : partArray) {
                String trimPart = StringUtils.trim(part);
                // 统一转小写后判断响应头字段，避免依赖已弃用的 StringUtils 忽略大小写方法。
                String lowerPart = trimPart.toLowerCase(Locale.ROOT);
                if (lowerPart.startsWith("filename*=")) {
                    fileName = trimQuote(StringUtils.substringAfter(trimPart, "="));
                    int charsetIndex = fileName.indexOf("''");
                    if (charsetIndex >= 0) {
                        fileName = fileName.substring(charsetIndex + 2);
                    }
                    try {
                        // filename* 通常会进行 URL 编码，这里按 UTF-8 解码还原原始文件名。
                        fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8.name());
                    } catch (Exception ignored) {
                    }
                    break;
                } else if (lowerPart.startsWith("filename=")) {
                    fileName = trimQuote(StringUtils.substringAfter(trimPart, "="));
                }
            }
        }
        if (StringUtils.isBlank(fileName)) {
            // 无文件名响应头时使用 token 兜底，保证 FileVo.name 必填语义。
            fileName = fileToken;
        }
        return fileName;
    }

    /**
     * 去掉响应头文件名两侧的引号，兼容 filename="xxx" 和 filename='xxx' 两种形式。
     *
     * @param value 原始文件名
     * @return 去除引号后的文件名
     */
    private String trimQuote(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        String result = StringUtils.trim(value);
        if ((result.startsWith("\"") && result.endsWith("\"")) || (result.startsWith("'") && result.endsWith("'"))) {
            return result.substring(1, result.length() - 1);
        }
        return result;
    }

    private void checkFeishuResult(JSONObject result) {
        if (result == null) {
            throw new RuntimeException("飞书接口无返回");
        }
        Integer code = result.getInteger("code");
        if (code != null && code != 0) {
            throw new RuntimeException(result.getString("msg"));
        }
    }

    private void saveNodes(Long spaceId, String spaceName, List<FeishuNode> nodes, KnowledgeFeishuSyncConfigVo config, KnowledgeDocumentTypeVo knowledgeType, KnowledgeFeishuSyncAuditVo auditVo, String tenantAccessToken) {
        for (FeishuNode node : nodes) {
            if (!isDocumentNode(node)) {
                continue;
            }
//            if (!Objects.equals(node.getNodeToken(), "EnH5wTCBMiZibmkDu6lcmI94n8g")) {
//                continue;
//            }
            auditVo.incrementTotalCount();
            JSONObject item = new JSONObject();
            item.put("spaceId", spaceId);
            item.put("spaceName", spaceName);
            item.put("title", node.getTitle());
            item.put("nodeToken", node.getNodeToken());
            try {
                JSONObject resultObj = saveFeishuDocument(config, node, knowledgeType.getUuid(), tenantAccessToken);
                Long documentId = resultObj.getLong("knowledgeDocumentId");
                item.put("knowledgeDocumentId", documentId);
                item.put("status", "succeed");
                JSONArray unprocessedItems = resultObj.getJSONArray("unprocessedItems");
                if (CollectionUtils.isNotEmpty(unprocessedItems)) {
                    item.put("unprocessedItems", unprocessedItems);
                }
                auditVo.incrementSuccessCount();
                if (CollectionUtils.isNotEmpty(node.getChildren())) {
                    KnowledgeDocumentTypeVo childType = getOrCreateKnowledgeType(node.getTitle(), knowledgeType.getUuid(), config.getKnowledgeCircleId());
                    saveNodes(spaceId, spaceName, node.getChildren(), config, childType, auditVo, tenantAccessToken);
                }
            } catch (Exception ex) {
                item.put("status", "failed");
                item.put("error", ex.getMessage());
                auditVo.incrementFailedCount();
            }
            auditVo.addDetailItem(item);
//            break;
        }
    }

    private KnowledgeDocumentTypeVo getOrCreateKnowledgeType(String name, String parentUuid, Long knowledgeCircleId) {
        KnowledgeDocumentTypeVo exists = null;
        List<KnowledgeDocumentTypeVo> children = knowledgeDocumentTypeMapper.getTypeByParentUuid(parentUuid, knowledgeCircleId);
        if (CollectionUtils.isNotEmpty(children)) {
            for (KnowledgeDocumentTypeVo child : children) {
                if (Objects.equals(child.getName(), name)) {
                    exists = child;
                    break;
                }
            }
        }
        if (exists != null) {
            return exists;
        } else {
            List<KnowledgeDocumentTypeVo> typeListByParentUuid = knowledgeDocumentTypeMapper.getTypeByParentUuid(parentUuid, knowledgeCircleId);
            List<KnowledgeDocumentTypeVo> list = new ArrayList<>();
            KnowledgeDocumentTypeVo knowledgeDocumentTypeVo = new KnowledgeDocumentTypeVo();
            knowledgeDocumentTypeVo.setUuid(UuidUtil.randomUuid());
            knowledgeDocumentTypeVo.setParentUuid(parentUuid);
            knowledgeDocumentTypeVo.setName(name);
            knowledgeDocumentTypeVo.setKnowledgeCircleId(knowledgeCircleId);
            /** sort的用处在于重建左右编码 */
            knowledgeDocumentTypeVo.setSort(typeListByParentUuid.size());
            list.add(knowledgeDocumentTypeVo);
            knowledgeDocumentTypeMapper.batchInsertType(list);
            return knowledgeDocumentTypeVo;
        }
    }

    private KnowledgeFeishuSyncConfigVo getRequiredConfig(Long configId) {
        KnowledgeFeishuSyncConfigVo config = knowledgeFeishuSyncMapper.getConfigById(configId);
        if (config == null) {
            throw new RuntimeException("飞书同步配置不存在");
        }
        return config;
    }

    private KnowledgeFeishuSyncAuditVo startAudit(Long configId, String direction) {
        KnowledgeFeishuSyncAuditVo audit = new KnowledgeFeishuSyncAuditVo();
        audit.setConfigId(configId);
        audit.setDirection(direction);
        audit.setStatus("running");
        audit.setTotalCount(0);
        audit.setSuccessCount(0);
        audit.setFailedCount(0);
        audit.setFcu(UserContext.get().getUserUuid(true));
        knowledgeFeishuSyncMapper.insertAudit(audit);
        return audit;
    }

    private void finishAudit(KnowledgeFeishuSyncAuditVo audit, String status, String error) {// , String status, int total, int success, int failed, String error, JSONArray detailList
        audit.setStatus(status);
//        audit.setTotalCount(total);
//        audit.setSuccessCount(success);
//        audit.setFailedCount(failed);
        audit.setError(error);
//        JSONObject detail = new JSONObject();
//        detail.put("items", detailList);
//        audit.setDetail(detail);
        knowledgeFeishuSyncMapper.updateAudit(audit);
    }

    /**
     * 返回数据结构为
     * [
     * 	    {
     * 		"open_sharing": "closed",
     * 		"visibility": "public",
     * 		"space_type": "team",
     * 		"name": "前端研发规范",
     * 		"description": "",
     * 		"space_id": "7208706433871773699"
     *    },
     *    {
     * 		"open_sharing": "closed",
     * 		"visibility": "public",
     * 		"space_type": "team",
     * 		"name": "后端研发规范",
     * 		"description": "",
     * 		"space_id": "7208734024653832196"
     *    },
     *    {
     * 		"open_sharing": "closed",
     * 		"visibility": "public",
     * 		"space_type": "team",
     * 		"name": "设计文档",
     * 		"description": "",
     * 		"space_id": "7208737988736483331"
     *    },
     *    {
     * 		"open_sharing": "closed",
     * 		"visibility": "public",
     * 		"space_type": "team",
     * 		"name": "研发管理",
     * 		"description": "",
     * 		"space_id": "7208739847777828867"
     *    },
     *    {
     * 		"open_sharing": "closed",
     * 		"visibility": "public",
     * 		"space_type": "team",
     * 		"name": "项目文档",
     * 		"description": "",
     * 		"space_id": "7208744663619223555"
     *    },
     *    {
     * 		"open_sharing": "closed",
     * 		"visibility": "public",
     * 		"space_type": "team",
     * 		"name": "技术方案",
     * 		"description": "",
     * 		"space_id": "7208747120034283523"
     *    },
     *    {
     * 		"open_sharing": "closed",
     * 		"visibility": "public",
     * 		"space_type": "team",
     * 		"name": "产品手册",
     * 		"description": "",
     * 		"space_id": "7208748423435173892"
     *    },
     *    {
     * 		"open_sharing": "closed",
     * 		"visibility": "public",
     * 		"space_type": "team",
     * 		"name": "常见问题",
     * 		"description": "",
     * 		"space_id": "7208872407434592260"
     *    },
     *    {
     * 		"open_sharing": "closed",
     * 		"visibility": "public",
     * 		"space_type": "team",
     * 		"name": "国产系统&软件适配",
     * 		"description": "",
     * 		"space_id": "7208876213417410563"
     *    },
     *    {
     * 		"open_sharing": "closed",
     * 		"visibility": "private",
     * 		"space_type": "team",
     * 		"name": "交付问题知识库",
     * 		"description": "交付问题知识库",
     * 		"space_id": "7397619128632180764"
     *    },
     *    {
     * 		"open_sharing": "closed",
     * 		"visibility": "private",
     * 		"space_type": "team",
     * 		"name": "linbq测试",
     * 		"description": "test",
     * 		"space_id": "7644786409622113212"
     *    }
     * ]
     *
     * @return
     */
    private JSONArray feishuWikiSpaces(String tenantAccessToken) {
        JSONArray resultArray = new JSONArray();
        String pageToken = null;
        Boolean hasMore = false;
        do {
            JSONObject resultObj = getFeishuWikiSpaces(pageToken, tenantAccessToken);
            JSONObject data = resultObj.getJSONObject("data");
            if (MapUtils.isNotEmpty(data)) {
                pageToken = data.getString("page_token");
                hasMore = data.getBoolean("has_more");
                JSONArray items = data.getJSONArray("items");
                if (CollectionUtils.isNotEmpty(items)) {
                    resultArray.addAll(items);
                }
            }
        } while (Objects.equals(hasMore, true));
        return resultArray;
    }

    private List<FeishuNode> loadWikiNodes(KnowledgeFeishuSyncConfigVo config, Long spaceId, String parentNodeToken, List<String> path, String tenantAccessToken) {
        List<FeishuNode> nodeList = new ArrayList<>();
        String pageToken = null;
        Boolean hasMore = false;
        do {
            JSONObject result = getFeishuWikiNodes(config, spaceId, parentNodeToken, pageToken, tenantAccessToken);
            JSONObject data = result.getJSONObject("data");
            if (data == null) {
                return nodeList;
            }
            hasMore = data.getBoolean("has_more");
            pageToken = data.getString("page_token");
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
                        System.out.println("子查询");
                        List<FeishuNode> children = loadWikiNodes(config, spaceId, node.getNodeToken(), node.getPath(), tenantAccessToken);
                        node.setChildren(children);
                    }
                }
            }
        } while (Objects.equals(hasMore, true));
        return nodeList;
    }

    private boolean isDocumentNode(FeishuNode node) {
        return "docx".equals(node.getObjType()) || "doc".equals(node.getObjType());
    }

    private JSONObject saveFeishuDocument(KnowledgeFeishuSyncConfigVo config, FeishuNode node, String typeUuid, String tenantAccessToken) {
        KnowledgeFeishuSyncDocumentVo mapping = knowledgeFeishuSyncMapper.getSyncDocumentByNodeToken(config.getId(), node.getNodeToken());
        KnowledgeDocumentVo documentVo = new KnowledgeDocumentVo();
        if (mapping == null) {
            documentVo.setTitle(node.getTitle());
            documentVo.setKnowledgeCircleId(config.getKnowledgeCircleId());
            documentVo.setKnowledgeDocumentTypeUuid(typeUuid);
            documentVo.setVersion(0);
            documentVo.setFcu(UserContext.get().getUserUuid(true));
            documentVo.setSource(FeishuSyncSource.SOURCE);
            knowledgeDocumentMapper.insertKnowledgeDocument(documentVo);
            knowledgeDocumentMapper.insertKnowledgeDocumentViewCount(documentVo.getId(), 0);
        } else {
            documentVo = knowledgeDocumentMapper.getKnowledgeDocumentLockById(mapping.getKnowledgeDocumentId());
            documentVo.setTitle(node.getTitle());
            documentVo.setKnowledgeDocumentTypeUuid(typeUuid);
            knowledgeDocumentMapper.updateKnowledgeDocumentTitleById(documentVo);
            knowledgeDocumentMapper.updateKnowledgeDocumentTypeUuidById(documentVo);
        }

        KnowledgeDocumentVersionVo versionVo = new KnowledgeDocumentVersionVo();
        versionVo.setTitle(node.getTitle());
        versionVo.setKnowledgeDocumentId(documentVo.getId());
        versionVo.setKnowledgeDocumentTypeUuid(typeUuid);
        versionVo.setFromVersion(Optional.ofNullable(knowledgeDocumentMapper.getKnowledgeDocumentVersionMaxVerionByKnowledgeDocumentId(documentVo.getId())).orElse(0));
        versionVo.setStatus("passed");
        versionVo.setLcu(UserContext.get().getUserUuid(true));
        knowledgeDocumentMapper.insertKnowledgeDocumentVersion(versionVo);
        versionVo.setVersion(versionVo.getFromVersion() + 1);
        versionVo.setReviewer(UserContext.get().getUserUuid(true));
        knowledgeDocumentMapper.updateKnowledgeDocumentVersionById(versionVo);
        documentVo.setKnowledgeDocumentVersionId(versionVo.getId());
        documentVo.setVersion(versionVo.getVersion());
        knowledgeDocumentMapper.updateKnowledgeDocumentById(documentVo);

        JSONArray unprocessedItems = new JSONArray();
        List<KnowledgeDocumentLineVo> feishuDocumentLines = getFeishuDocumentLines(config, node, tenantAccessToken, unprocessedItems);
        saveLines(documentVo.getId(), versionVo.getId(), feishuDocumentLines);
        upsertMapping(config, node, typeUuid, documentVo.getId());
        IFullTextIndexHandler handler = FullTextIndexHandlerFactory.getHandler(KnowledgeFullTextIndexType.KNOW_DOCUMENT_VERSION);
        if (handler != null) {
            handler.createIndex(versionVo.getId());
        }
        return new JSONObject().fluentPut("knowledgeDocumentId", documentVo.getId()).fluentPut("unprocessedItems", unprocessedItems);
    }

    private void upsertMapping(KnowledgeFeishuSyncConfigVo config, FeishuNode node, String typeUuid, Long documentId) {
        KnowledgeFeishuSyncDocumentVo vo = new neatlogic.framework.knowledge.dto.feishu.KnowledgeFeishuSyncDocumentVo();
        vo.setConfigId(config.getId());
        vo.setNodeToken(node.getNodeToken());
        vo.setObjToken(node.getObjToken());
        vo.setObjType(node.getObjType());
        vo.setKnowledgeDocumentId(documentId);
        vo.setKnowledgeDocumentTypeUuid(typeUuid);
        vo.setTitle(node.getTitle());
        vo.setFeishuUpdateTime(node.getUpdateTime());
        if (knowledgeFeishuSyncMapper.getSyncDocumentByNodeToken(config.getId(), node.getNodeToken()) == null) {
            knowledgeFeishuSyncMapper.insertSyncDocument(vo);
        } else {
            knowledgeFeishuSyncMapper.updateSyncDocument(vo);
        }
    }

    private String getContentFromElements(JSONArray elements) {
        StringBuilder builder = new StringBuilder();
        if (CollectionUtils.isNotEmpty(elements)) {
            for (int i = 0; i < elements.size(); i++) {
                JSONObject element = elements.getJSONObject(i);
                if (MapUtils.isNotEmpty(element)) {
                    JSONObject textRun = element.getJSONObject("text_run");
                    if (MapUtils.isNotEmpty(textRun)) {
                        String content = textRun.getString("content");
                        if (StringUtils.isNotBlank(content)) {
                            JSONObject textElementStyle = textRun.getJSONObject("text_element_style");
                            if (MapUtils.isNotEmpty(textElementStyle)) {
                                JSONObject link = textElementStyle.getJSONObject("link");
                                if (MapUtils.isNotEmpty(link)) {
                                    String url = link.getString("url");
                                    if (StringUtils.isNotBlank(url)) {
                                        content = String.format("<a href=\"%s\" target=\"_blank\">%s</a>", url, content);
                                    }
                                }
                            }
                            builder.append(content);
                        }
                    }
                }
            }
        }
        return builder.toString();
    }

    private List<String> getContentListFromElements(JSONArray elements) {
        List<String> list = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(elements)) {
            for (int i = 0; i < elements.size(); i++) {
                JSONObject element = elements.getJSONObject(i);
                if (MapUtils.isNotEmpty(element)) {
                    JSONObject textRun = element.getJSONObject("text_run");
                    if (MapUtils.isNotEmpty(textRun)) {
                        String content = textRun.getString("content");
                        if (StringUtils.isNotBlank(content)) {
                            JSONObject textElementStyle = textRun.getJSONObject("text_element_style");
                            if (MapUtils.isNotEmpty(textElementStyle)) {
                                JSONObject link = textElementStyle.getJSONObject("link");
                                if (MapUtils.isNotEmpty(link)) {
                                    String url = link.getString("url");
                                    if (StringUtils.isNotBlank(url)) {
                                        content = String.format("<a href=\"%s\" target=\"_blank\">%s</a>", url, content);
                                    }
                                }
                            }
                            list.add(content);
                        }
                    }
                }
            }
        }
        return list;
    }

    private KnowledgeDocumentLineVo handleText(JSONObject item) {
        Integer blockType = item.getInteger("block_type");
        FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
        String blockId = item.getString("block_id");
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("paragraph");
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "paragraph");
        configObj.put("blockUuid", blockId);
        configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item));
        JSONObject jsonObj = item.getJSONObject(feiShuBlockType.getText());
        if (MapUtils.isNotEmpty(jsonObj)) {
            JSONArray elements = jsonObj.getJSONArray("elements");
            String content = getContentFromElements(elements);
            knowledgeDocumentLineVo.setContent(content);
//            configObj.put("content", content);
        }
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        return knowledgeDocumentLineVo;
    }

    private KnowledgeDocumentLineVo handleHeading(JSONObject item) {
        Integer blockType = item.getInteger("block_type");
        FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
        String blockId = item.getString("block_id");
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("heading");
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "heading");
        configObj.put("level", feiShuBlockType.getValue() - 2);
        configObj.put("blockUuid", blockId);
        configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item));
        JSONObject jsonObj = item.getJSONObject(feiShuBlockType.getText());
        if (MapUtils.isNotEmpty(jsonObj)) {
            JSONArray elements = jsonObj.getJSONArray("elements");
            String content = getContentFromElements(elements);
            knowledgeDocumentLineVo.setContent(content);
            configObj.put("content", content);
        }
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        return knowledgeDocumentLineVo;
    }

//    private KnowledgeDocumentLineVo handleIframe(JSONObject item) {
//        Integer blockType = item.getInteger("block_type");
//        FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
//        String blockId = item.getString("block_id");
//        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
//        knowledgeDocumentLineVo.setHandler("image");
//        JSONObject configObj = new JSONObject();
//        configObj.put("blockType", "image");
//        configObj.put("blockUuid", blockId);
//        configObj.put("name", "");
//        configObj.put("uploading", false);
//        configObj.put("align", "left");
//        configObj.put("title", "");
//        configObj.put("value", "");
//        configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item));
//        JSONObject jsonObj = item.getJSONObject(feiShuBlockType.getText());
//        if (MapUtils.isNotEmpty(jsonObj)) {
//            JSONObject component = jsonObj.getJSONObject("component");
//            if (MapUtils.isNotEmpty(component)) {
//                Integer iframeType = component.getInteger("iframe_type");
//                String url = component.getString("url");
//                configObj.put("url", url);
//                configObj.put("src", url);
//            }
//        }
//        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
//        return knowledgeDocumentLineVo;
//    }

    private KnowledgeDocumentLineVo handleOrderedList(List<JSONObject> orderedList) {
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("orderedList");
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "orderedList");
        configObj.put("className", "decimal");
        configObj.put("feiShuBlockList", orderedList);
        if (CollectionUtils.isNotEmpty(orderedList)) {
            Integer start = null;
            List<String> blockIdList = new ArrayList<>();
            List<String> contentList = new ArrayList<>();
            for (JSONObject item : orderedList) {
                String blockId = item.getString("block_id");
                blockIdList.add(blockId);
                Integer blockType = item.getInteger("block_type");
                FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
                JSONObject jsonObj = item.getJSONObject(feiShuBlockType.getText());
                if (MapUtils.isNotEmpty(jsonObj)) {
                    JSONArray elements = jsonObj.getJSONArray("elements");
                    String content = getContentFromElements(elements);
                    contentList.add("<li>" + content + "</li>");
                    if (start == null) {
                        JSONObject style = jsonObj.getJSONObject("style");
                        if (MapUtils.isNotEmpty(style)) {
                            String sequence = style.getString("sequence");
                            if (StringUtils.isNumeric(sequence)) {
                                start = Integer.parseInt(sequence);
                            }
                        }
                    }
                }
            }
            configObj.put("start", start != null ? start : 1);
            knowledgeDocumentLineVo.setContent(String.join("", contentList));
            configObj.put("content", String.join("", contentList));
            configObj.put("blockUuid", String.join(",", blockIdList));
        }
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        return knowledgeDocumentLineVo;
    }

    private KnowledgeDocumentLineVo handleBulletList(List<JSONObject> bulletList) {
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("bulletList");
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "bulletList");
        configObj.put("className", "disc");
        configObj.put("feiShuBlockList", bulletList);
        if (CollectionUtils.isNotEmpty(bulletList)) {
            Integer start = null;
            List<String> blockIdList = new ArrayList<>();
            List<String> contentList = new ArrayList<>();
            for (JSONObject item : bulletList) {
                String blockId = item.getString("block_id");
                blockIdList.add(blockId);
                Integer blockType = item.getInteger("block_type");
                FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
                JSONObject jsonObj = item.getJSONObject(feiShuBlockType.getText());
                if (MapUtils.isNotEmpty(jsonObj)) {
                    JSONArray elements = jsonObj.getJSONArray("elements");
                    String content = getContentFromElements(elements);
                    contentList.add("<li>" + content + "</li>");
//                    if (start == null) {
//                        JSONObject style = jsonObj.getJSONObject("style");
//                        if (MapUtils.isNotEmpty(style)) {
//                            String sequence = style.getString("sequence");
//                            if (StringUtils.isNumeric(sequence)) {
//                                start = Integer.parseInt(sequence);
//                            }
//                        }
//                    }
                }
            }
//            configObj.put("start", start != null ? start : 1);
            knowledgeDocumentLineVo.setContent(String.join("", contentList));
            configObj.put("content", String.join("", contentList));
            configObj.put("blockUuid", String.join(",", blockIdList));
        }
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        return knowledgeDocumentLineVo;
    }

    private KnowledgeDocumentLineVo handleTodoList(List<JSONObject> todoList) {
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("taskList");
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "taskList");
//        configObj.put("className", "disc");
        configObj.put("feiShuBlockList", todoList);
        if (CollectionUtils.isNotEmpty(todoList)) {
            Integer start = null;
            List<String> blockIdList = new ArrayList<>();
            List<String> contentList = new ArrayList<>();
            List<JSONObject> contentObjList = new ArrayList<>();
            for (JSONObject item : todoList) {
                String blockId = item.getString("block_id");
                blockIdList.add(blockId);
                Integer blockType = item.getInteger("block_type");
                FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
                JSONObject jsonObj = item.getJSONObject(feiShuBlockType.getText());
                if (MapUtils.isNotEmpty(jsonObj)) {
                    JSONArray elements = jsonObj.getJSONArray("elements");
                    String content = getContentFromElements(elements);
                    contentList.add("<li>" + content + "</li>");
                    Boolean done = null;
                    JSONObject style = jsonObj.getJSONObject("style");
                    if (MapUtils.isNotEmpty(style)) {
                        done = style.getBoolean("done");
                    }
                    contentObjList.add(new JSONObject().fluentPut("content", content).fluentPut("checked", done));
                }
            }
//            configObj.put("start", start != null ? start : 1);
            knowledgeDocumentLineVo.setContent(String.join("", contentList));
            configObj.put("contentList", contentObjList);
            configObj.put("blockUuid", String.join(",", blockIdList));
        }
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        return knowledgeDocumentLineVo;
    }

    private KnowledgeDocumentLineVo handleCode(JSONObject item) {
        Integer blockType = item.getInteger("block_type");
        FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
        String blockId = item.getString("block_id");
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("codeBlock");
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "codeBlock");
        configObj.put("blockUuid", blockId);
        configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item));
        JSONObject jsonObj = item.getJSONObject(feiShuBlockType.getText());
        if (MapUtils.isNotEmpty(jsonObj)) {
            JSONArray elements = jsonObj.getJSONArray("elements");
            List<String> contentList = getContentListFromElements(elements);
            knowledgeDocumentLineVo.setContent(String.join("\n", contentList));
//            configObj.put("content", content);
            configObj.put("value", String.join("\n", contentList));
            JSONObject style = jsonObj.getJSONObject("style");
            if (MapUtils.isNotEmpty(style)) {
                Integer language = style.getInteger("language");
                if (language != null) {
                    configObj.put("codeMode", language); // TODO
                }
            }
        }
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        return knowledgeDocumentLineVo;
    }

    private KnowledgeDocumentLineVo handleCallOut(JSONObject item, List<JSONObject> childItemList) {
        String blockId = item.getString("block_id");
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("highlightBlock");
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "highlightBlock");
        configObj.put("blockUuid", blockId);
        configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item).fluentAddAll(childItemList));
        List<String> list = new ArrayList<>();
        for (JSONObject childItem : childItemList) {
            Integer blockType = childItem.getInteger("block_type");
            FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
            JSONObject jsonObj = childItem.getJSONObject(feiShuBlockType.getText());
            if (MapUtils.isNotEmpty(jsonObj)) {
                JSONArray elements = jsonObj.getJSONArray("elements");
                List<String> contentList = getContentListFromElements(elements);
                for (String content : contentList) {
                    list.add("<p>" + content + "</p>");
                }
            }
        }
        knowledgeDocumentLineVo.setContent(String.join("", list));
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        return knowledgeDocumentLineVo;
    }

    private KnowledgeDocumentLineVo handleQuoteContainer(JSONObject item, List<JSONObject> childItemList) {
        String blockId = item.getString("block_id");
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("blockquote");
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "blockquote");
        configObj.put("blockUuid", blockId);
        configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item).fluentAddAll(childItemList));
        List<String> list = new ArrayList<>();
        for (JSONObject childItem : childItemList) {
            Integer blockType = childItem.getInteger("block_type");
            FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
            JSONObject jsonObj = childItem.getJSONObject(feiShuBlockType.getText());
            if (MapUtils.isNotEmpty(jsonObj)) {
                JSONArray elements = jsonObj.getJSONArray("elements");
                List<String> contentList = getContentListFromElements(elements);
                for (String content : contentList) {
                    list.add("<p>" + content + "</p>");
                }
            }
        }
        knowledgeDocumentLineVo.setContent(String.join("", list));
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        return knowledgeDocumentLineVo;
    }

    private KnowledgeDocumentLineVo handleView(JSONObject item, List<JSONObject> childItemList, String tenantAccessToken) {
        String handler = "paragraph";
        String blockId = item.getString("block_id");
        {
            Integer blockType = item.getInteger("block_type");
            FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
            Integer viewType = null;
            JSONObject jsonObj = item.getJSONObject(feiShuBlockType.getText());
            if (MapUtils.isNotEmpty(jsonObj)) {
                viewType = jsonObj.getInteger("view_type");
                if (Objects.equals(viewType, 1)) {
                    handler = "file";
                } else if (Objects.equals(viewType, 2)) {
                    handler = "video";
                } else if (Objects.equals(viewType, 3)) {
//                    handler = "image";
                }
            }
        }
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler(handler);
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", handler);
        configObj.put("blockUuid", blockId);
        configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item).fluentAddAll(childItemList));
        for (JSONObject childItem : childItemList) {
            Integer blockType = childItem.getInteger("block_type");
            FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
            JSONObject jsonObj = childItem.getJSONObject(feiShuBlockType.getText());
            if (MapUtils.isNotEmpty(jsonObj)) {
                String name = jsonObj.getString("name");
                String token = jsonObj.getString("token");
                FileVo fileVo = downloadMedias(token, tenantAccessToken);
                if (fileVo != null) {
                    if (Objects.equals(handler, "video")) {
                        configObj.put("src", "api/binary/file/download?id=" + fileVo.getId());
                    } else if (Objects.equals(handler, "file")) {
                        configObj.put("name", fileVo.getName());
                        configObj.put("size", fileVo.getSize());
                    }
                    configObj.put("url", "api/binary/file/download?id=" + fileVo.getId());
//                    configObj.put("uploading", false);
                    configObj.put("align", "left");
                }
            }
        }
        if (Objects.equals(handler, "paragraph")) {
            knowledgeDocumentLineVo.setContent(configObj.toJSONString());
        }
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        return knowledgeDocumentLineVo;
    }

    private KnowledgeDocumentLineVo handleImage(JSONObject item, String tenantAccessToken) {
        String blockId = item.getString("block_id");
        Integer blockType = item.getInteger("block_type");
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("image");
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "image");
        configObj.put("blockUuid", blockId);
        configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item));
        FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
        JSONObject jsonObj = item.getJSONObject(feiShuBlockType.getText());
        if (MapUtils.isNotEmpty(jsonObj)) {
            Integer width = jsonObj.getInteger("width");
            Integer height = jsonObj.getInteger("height");
            Integer scale = jsonObj.getInteger("scale");
            Integer align = jsonObj.getInteger("align");
            configObj.put("align", FeiShuAlignType.getFeiShuAlignText(align));
            String token = jsonObj.getString("token");
            FileVo fileVo = downloadMedias(token, tenantAccessToken);
            if (fileVo != null) {
                configObj.put("url", "api/binary/file/download?id=" + fileVo.getId());
            }
        }
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        return knowledgeDocumentLineVo;
    }

    private KnowledgeDocumentLineVo handleTable(JSONObject item, List<JSONObject> childItemList) {
        String blockId = item.getString("block_id");
        KnowledgeDocumentLineVo knowledgeDocumentLineVo = new KnowledgeDocumentLineVo();
        knowledgeDocumentLineVo.setHandler("table");
        Map<String, JSONObject> childItemMap = new HashMap<>();
        for (JSONObject childItem : childItemList) {
            childItemMap.put(childItem.getString("block_id"), childItem);
        }
        JSONObject tableObj = item.getJSONObject(FeiShuBlockType.TABLE.getText());
        JSONObject property = tableObj.getJSONObject("property");
        Integer columnSize = property.getInteger("column_size");
        Integer rowSize = property.getInteger("row_size");
        JSONArray mergeInfo = property.getJSONArray("merge_info");
        List<JSONObject> headerList = new ArrayList<>(columnSize);
        for (int i = 0; i < columnSize; i++) {
            headerList.add(new JSONObject().fluentPut("width", 100));
        }
        List<JSONObject> lefterList = new ArrayList<>(rowSize);
        for (int i = 0; i < rowSize; i++) {
            lefterList.add(new JSONObject().fluentPut("height", 45));
        }
        List<JSONObject> tableList = new ArrayList<>(columnSize * rowSize);
        JSONArray tableChildren = item.getJSONArray("children");
        if (CollectionUtils.isNotEmpty(tableChildren)) {
            for (int i = 0; i < tableChildren.size(); i++) {
                int rowIndex = i / columnSize;
                int colIndex = i % columnSize;
                JSONObject mergeInfoElement = mergeInfo.getJSONObject(i);
                JSONObject tableElement = new JSONObject();
                tableElement.put("col", colIndex);
                tableElement.put("row", rowIndex);
                tableElement.put("colspan", mergeInfoElement.getInteger("colspan"));
                tableElement.put("rowspan", mergeInfoElement.getInteger("rowspan"));
                tableElement.put("verticalAlign", "top");
                tableElement.put("isHeader", 0);
                tableElement.put("cellStyle", "border-bottom:1px solid #dfe1e5");
                tableElement.put("content", "");
                String tableChildBlockId = tableChildren.getString(i);
                JSONObject childItem = childItemMap.get(tableChildBlockId);
                JSONArray tableCellChildren = childItem.getJSONArray("children");
                if (CollectionUtils.isNotEmpty(tableCellChildren)) {
                    for (String tableCellChildBlockId : tableCellChildren.toJavaList(String.class)) {
                        JSONObject tableCellChildItem = childItemMap.get(tableCellChildBlockId);
                        if (MapUtils.isNotEmpty(tableCellChildItem)) {
                            Integer blockType = tableCellChildItem.getInteger("block_type");
                            FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
                            if (feiShuBlockType == FeiShuBlockType.TEXT) {
                                JSONObject jsonObj = tableCellChildItem.getJSONObject(FeiShuBlockType.TEXT.getText());
                                if (MapUtils.isNotEmpty(jsonObj)) {
                                    JSONArray elements = jsonObj.getJSONArray("elements");
                                    String content = getContentFromElements(elements);
                                    tableElement.put("content", content);
                                }
                            }
                        }
                    }
                }
                tableList.add(tableElement);
            }
        }
        JSONObject configObj = new JSONObject();
        configObj.put("blockType", "table");
        configObj.put("blockUuid", blockId);
        configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item).fluentAddAll(childItemList));
        configObj.put("col", columnSize);
        configObj.put("row", rowSize);
        configObj.put("headerList", headerList);
        configObj.put("lefterList", lefterList);
        configObj.put("tableList", tableList);
        configObj.put("tableStyle", new JSONObject().fluentPut("td", "border-bottom:1px solid #dfe1e5").fluentPut("tr", "height:42px").fluentPut("table", "table-layout:fixed;border-collapse:collapse;width:100%;text-align:left;border:none;"));
        configObj.put("style", "table-layout:fixed;border-collapse:collapse;text-align:left;border:none");
        knowledgeDocumentLineVo.setConfig(configObj.toJSONString());
        return knowledgeDocumentLineVo;
    }

    private List<KnowledgeDocumentLineVo> getFeishuDocumentLines(KnowledgeFeishuSyncConfigVo config, FeishuNode node, String tenantAccessToken, JSONArray unprocessedItems) {
        List<KnowledgeDocumentLineVo> lineList = new ArrayList<>();
        try {
            System.out.println("node.title = " + node.getTitle());
            JSONObject blockResult = getDocumentBlocks(config, node.getObjToken(), tenantAccessToken);
            System.out.println("blockResult = " + blockResult);
            JSONArray items = blockResult.getJSONObject("data") == null ? null : blockResult.getJSONObject("data").getJSONArray("items");
            if (CollectionUtils.isNotEmpty(items)) {
                JSONObject pageItem = null;
                Map<String, JSONObject> itemMap = new LinkedHashMap<>();
                for (int i = 0; i < items.size(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    if (MapUtils.isNotEmpty(item)) {
                        String blockId = item.getString("block_id");
                        itemMap.put(blockId, item);
                        String parentId = item.getString("parent_id");
                        Integer blockType = item.getInteger("block_type");
                        if (Objects.equals(blockType, FeiShuBlockType.PAGE.getValue()) && StringUtils.isBlank(parentId)) {
                            pageItem = item;
                        }
                    }
                }
                if (pageItem != null) {
                    String title = null;
                    JSONObject pageObj = pageItem.getJSONObject(FeiShuBlockType.PAGE.getText());
                    if (MapUtils.isNotEmpty(pageObj)) {
                        JSONArray elements = pageObj.getJSONArray("elements");
                        title = getContentFromElements(elements);
                    }
                    JSONArray children = pageItem.getJSONArray("children");
                    if (CollectionUtils.isNotEmpty(children)) {
                        List<String> handledBlockIdList = new ArrayList<>();
                        List<String> blockIdList = children.toJavaList(String.class);
                        for (String blockId : blockIdList) {
                            JSONObject item = itemMap.get(blockId);
                            String parentId = item.getString("parent_id");
                            Integer blockType = item.getInteger("block_type");
//                            System.out.println("blockType = " + blockType);
                            FeiShuBlockType feiShuBlockType = FeiShuBlockType.getFeiShuBlockType(blockType);
//                            System.out.println("feiShuBlockType = " + feiShuBlockType.getText());
                            if (handledBlockIdList.contains(blockId)) {
                                continue;
                            }
                            handledBlockIdList.add(blockId);
                            item.put("block_type_text", feiShuBlockType.getText());
                            item.put("block_type_description", feiShuBlockType.getDescription());
                            if (feiShuBlockType == FeiShuBlockType.TEXT) {
                                KnowledgeDocumentLineVo line = handleText(item);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.HEADING1) {
                                KnowledgeDocumentLineVo line = handleHeading(item);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.HEADING2) {
                                KnowledgeDocumentLineVo line = handleHeading(item);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.HEADING3) {
                                KnowledgeDocumentLineVo line = handleHeading(item);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.HEADING4) {
                                KnowledgeDocumentLineVo line = handleHeading(item);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.HEADING5) {
                                KnowledgeDocumentLineVo line = handleHeading(item);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.HEADING6) {
                                KnowledgeDocumentLineVo line = handleHeading(item);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.HEADING7) {
                                KnowledgeDocumentLineVo line = handleHeading(item);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.HEADING8) {
                                KnowledgeDocumentLineVo line = handleHeading(item);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.HEADING9) {
                                KnowledgeDocumentLineVo line = handleHeading(item);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.BULLET) {
                                List<JSONObject> bulletList = new ArrayList<>();
                                boolean isStart = false;
                                for (Map.Entry<String, JSONObject> entry : itemMap.entrySet()) {
                                    JSONObject value = entry.getValue();
                                    String key = entry.getKey();
                                    if (!isStart && Objects.equals(key, blockId)) {
                                        bulletList.add(value);
                                        isStart = true;
                                    } else if (isStart) {
                                        if (Objects.equals(value.getInteger("block_type"), feiShuBlockType.getValue())
                                                && Objects.equals(value.getString("parent_id"), parentId)) {
                                            bulletList.add(value);
                                            handledBlockIdList.add(value.getString("block_id"));
                                        } else {
                                            break;
                                        }
                                    }
                                }
                                if (CollectionUtils.isNotEmpty(bulletList)) {
                                    KnowledgeDocumentLineVo line = handleBulletList(bulletList);
                                    lineList.add(line);
                                }
                            } else if (feiShuBlockType == FeiShuBlockType.ORDERED) {
                                List<JSONObject> orderedList = new ArrayList<>();
                                boolean isStart = false;
                                for (Map.Entry<String, JSONObject> entry : itemMap.entrySet()) {
                                    JSONObject value = entry.getValue();
                                    String key = entry.getKey();
                                    if (!isStart && Objects.equals(key, blockId)) {
                                        orderedList.add(value);
                                        isStart = true;
                                    } else if (isStart) {
                                        if (Objects.equals(value.getInteger("block_type"), feiShuBlockType.getValue())
                                                && Objects.equals(value.getString("parent_id"), parentId)) {
                                            orderedList.add(value);
                                            handledBlockIdList.add(value.getString("block_id"));
                                        } else {
                                            break;
                                        }
                                    }
                                }
                                if (CollectionUtils.isNotEmpty(orderedList)) {
                                    KnowledgeDocumentLineVo line = handleOrderedList(orderedList);
                                    lineList.add(line);
                                }
                            } else if (feiShuBlockType == FeiShuBlockType.CODE) {
                                KnowledgeDocumentLineVo line = handleCode(item);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.QUOTE_CONTAINER) {
                                List<JSONObject> childItemList = new ArrayList<>();
                                JSONArray array = item.getJSONArray("children");
                                if (CollectionUtils.isNotEmpty(array)) {
                                    List<String> list = array.toJavaList(String.class);
                                    for (String str : list) {
                                        JSONObject childItem = itemMap.get(str);
                                        if (MapUtils.isNotEmpty(childItem)) {
                                            childItemList.add(childItem);
                                            handledBlockIdList.add(str);
                                        }
                                    }
                                }
                                KnowledgeDocumentLineVo line = handleQuoteContainer(item, childItemList);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.TODO) {
                                List<JSONObject> todoList = new ArrayList<>();
                                boolean isStart = false;
                                for (Map.Entry<String, JSONObject> entry : itemMap.entrySet()) {
                                    JSONObject value = entry.getValue();
                                    String key = entry.getKey();
                                    if (!isStart && Objects.equals(key, blockId)) {
                                        todoList.add(value);
                                        isStart = true;
                                    } else if (isStart) {
                                        if (Objects.equals(value.getInteger("block_type"), feiShuBlockType.getValue())
                                                && Objects.equals(value.getString("parent_id"), parentId)) {
                                            todoList.add(value);
                                            handledBlockIdList.add(value.getString("block_id"));
                                        } else {
                                            break;
                                        }
                                    }
                                }
                                if (CollectionUtils.isNotEmpty(todoList)) {
                                    KnowledgeDocumentLineVo line = handleTodoList(todoList);
                                    lineList.add(line);
                                }
                            } else if (feiShuBlockType == FeiShuBlockType.CALLOUT) {
                                List<JSONObject> childItemList = new ArrayList<>();
                                JSONArray array = item.getJSONArray("children");
                                if (CollectionUtils.isNotEmpty(array)) {
                                    List<String> list = array.toJavaList(String.class);
                                    for (String str : list) {
                                        JSONObject childItem = itemMap.get(str);
                                        if (MapUtils.isNotEmpty(childItem)) {
                                            childItemList.add(childItem);
                                            handledBlockIdList.add(str);
                                        }
                                    }
                                }
                                KnowledgeDocumentLineVo line = handleCallOut(item, childItemList);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.DIVIDER) {
                                KnowledgeDocumentLineVo line = new KnowledgeDocumentLineVo();
                                JSONObject configObj = new JSONObject();
                                configObj.put("blockType", "divider");
                                configObj.put("blockUuid", blockId);
                                line.setConfig(configObj.toJSONString());
                                line.setHandler("divider");
                                lineList.add(line);
//                            } else if (feiShuBlockType == FeiShuBlockType.IFRAME) {
//                                KnowledgeDocumentLineVo line = handleIframe(item);
//                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.VIEW) {
                                List<JSONObject> childItemList = new ArrayList<>();
                                JSONArray array = item.getJSONArray("children");
                                if (CollectionUtils.isNotEmpty(array)) {
                                    List<String> list = array.toJavaList(String.class);
                                    for (String str : list) {
                                        JSONObject childItem = itemMap.get(str);
                                        if (MapUtils.isNotEmpty(childItem)) {
                                            childItemList.add(childItem);
                                            handledBlockIdList.add(str);
                                        }
                                    }
                                }
                                KnowledgeDocumentLineVo line = handleView(item, childItemList, tenantAccessToken);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.IMAGE) {
                                KnowledgeDocumentLineVo line = handleImage(item, tenantAccessToken);
                                lineList.add(line);
                            } else if (feiShuBlockType == FeiShuBlockType.TABLE) {
                                List<JSONObject> childItemList = new ArrayList<>();
                                JSONArray array = item.getJSONArray("children");
                                if (CollectionUtils.isNotEmpty(array)) {
                                    List<String> list = array.toJavaList(String.class);
                                    for (String str : list) {
                                        JSONObject childItem = itemMap.get(str);
                                        if (MapUtils.isNotEmpty(childItem)) {
                                            childItemList.add(childItem);
                                            handledBlockIdList.add(str);
                                            Integer childBlockType = childItem.getInteger("block_type");
                                            FeiShuBlockType childFeiShuBlockType = FeiShuBlockType.getFeiShuBlockType(childBlockType);
                                            if (childFeiShuBlockType == FeiShuBlockType.TABLE_CELL) {
                                                JSONArray tableCellChildArray = childItem.getJSONArray("children");
                                                if (CollectionUtils.isNotEmpty(tableCellChildArray)) {
                                                    List<String> tableCellChildList = tableCellChildArray.toJavaList(String.class);
                                                    for (String tableCellChild : tableCellChildList) {
                                                        JSONObject tableCellChildItem = itemMap.get(tableCellChild);
                                                        if (MapUtils.isNotEmpty(tableCellChildItem)) {
                                                            childItemList.add(tableCellChildItem);
                                                            handledBlockIdList.add(tableCellChild);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                KnowledgeDocumentLineVo line = handleTable(item, childItemList);
                                lineList.add(line);
                            } else {
                                unprocessedItems.add(item);
                                JSONArray childItemList = collectChildItemList(item, itemMap);
                                if (CollectionUtils.isNotEmpty(childItemList)) {
                                    unprocessedItems.addAll(childItemList);
                                    for (int i = 0; i < childItemList.size(); i++) {
                                        JSONObject childItem = childItemList.getJSONObject(i);
                                        handledBlockIdList.add(childItem.getString("block_id"));
                                    }
                                }
                                KnowledgeDocumentLineVo line = new KnowledgeDocumentLineVo();
                                JSONObject configObj = new JSONObject();
                                configObj.put("blockType", feiShuBlockType.getText());
                                configObj.put("blockUuid", blockId);
                                configObj.put("feiShuBlockList", new JSONArray().fluentAdd(item));
                                line.setConfig(configObj.toJSONString());
                                line.setHandler("paragraph");
                                line.setContent(item.toJSONString());
                                lineList.add(line);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
//        if (CollectionUtils.isEmpty(lineList)) {
//            lineList.add(newLine("p", "<a href=\"" + config.getBaseUrl() + "/wiki/" + node.getNodeToken() + "\">" + node.getTitle() + "</a>"));
//        }
        return lineList;
    }

    private JSONArray collectChildItemList(JSONObject item, Map<String, JSONObject> itemMap) {
        JSONArray resultList = new JSONArray();
        JSONArray children = item.getJSONArray("children");
        if (CollectionUtils.isNotEmpty(children)) {
            for (int i = 0; i < children.size(); i++) {
                String childBlockId = children.getString(i);
                JSONObject childItem = itemMap.get(childBlockId);
                if (MapUtils.isNotEmpty(childItem)) {
                    resultList.add(childItem);
                    JSONArray childItemList = collectChildItemList(childItem, itemMap);
                    resultList.addAll(childItemList);
                }
            }
        }
        return resultList;
    }

    private void saveLines(Long documentId, Long versionId, List<KnowledgeDocumentLineVo> lineList) {
        int size = 0;
        int lineNumber = 0;
        for (KnowledgeDocumentLineVo lineVo : lineList) {
            lineVo.setLineNumber(++lineNumber);
            lineVo.setKnowledgeDocumentId(documentId);
            lineVo.setKnowledgeDocumentVersionId(versionId);
            lineVo.setUuid(UuidUtil.randomUuid());
            if (lineVo.getConfig() != null) {
                KnowledgeDocumentLineConfigVo knowledgeDocumentLineConfigVo = new KnowledgeDocumentLineConfigVo(lineVo.getConfigStr());
                lineVo.setConfigHash(knowledgeDocumentLineConfigVo.getHash());
                if (knowledgeDocumentMapper.checkKnowledgeDocumentLineConfigHashIsExists(knowledgeDocumentLineConfigVo.getHash()) == 0) {
                    knowledgeDocumentMapper.insertKnowledgeDocumentLineConfig(knowledgeDocumentLineConfigVo);
                }
            }
            if (StringUtils.isNotBlank(lineVo.getContent())) {
                size += lineVo.getContent().getBytes(StandardCharsets.UTF_8).length;
                KnowledgeDocumentLineContentVo contentVo = new KnowledgeDocumentLineContentVo(lineVo.getContent());
                lineVo.setContentHash(contentVo.getHash());
                if (knowledgeDocumentMapper.checkKnowledgeDocumentLineContentHashIsExists(contentVo.getHash()) == 0) {
                    knowledgeDocumentMapper.insertKnowledgeDocumentLineContent(contentVo);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(lineList)) {
            knowledgeDocumentMapper.insertKnowledgeDocumentLineList(lineList);
        }
        KnowledgeDocumentVersionVo updateVo = new KnowledgeDocumentVersionVo();
        updateVo.setId(versionId);
        updateVo.setSize(size);
        knowledgeDocumentMapper.updateKnowledgeDocumentVersionById(updateVo);
    }
}
