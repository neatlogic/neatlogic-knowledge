/*
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

package neatlogic.module.knowledge.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.common.util.FileUtil;
import neatlogic.framework.common.util.RC4Util;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.util.HttpRequestUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FeiShuOpenApiUtil {

    private static final Logger logger = LoggerFactory.getLogger(FeiShuOpenApiUtil.class);

    private static final String TENANT_ACCESS_TOKEN_INTERNAL_URL = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
    private static final String WIKI_V2_SPACES_URL = "https://open.feishu.cn/open-apis/wiki/v2/spaces";
    private static final String SPACE_NODES_URL = "https://open.feishu.cn/open-apis/wiki/v2/spaces/:space_id/nodes";
    private static final String DOCUMENT_BLOCKS_URL = "https://open.feishu.cn/open-apis/docx/v1/documents/:document_id/blocks";
    private static final String GET_NODE_URL = "https://open.feishu.cn/open-apis/wiki/v2/spaces/get_node";
    private static final String MEDIAS_DOWNLOAD_URL = "https://open.feishu.cn/open-apis/drive/v1/medias/:file_token/download";
    private static final String GET_SPACE_URL = "https://open.feishu.cn/open-apis/wiki/v2/spaces/:space_id";

    /**
     *
     * @param appId
     * @param appSecret
     * @return
     */
    public static String getTenantAccessToken(String appId, String appSecret) {
        JSONObject body = new JSONObject();
        body.put("app_id", appId);
        body.put("app_secret", RC4Util.decrypt(appSecret));
        JSONObject result = HttpRequestUtil.post(TENANT_ACCESS_TOKEN_INTERNAL_URL)
                .setPayload(body.toJSONString())
                .sendRequest()
                .getResultJson();
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
     *
     * @param tenantAccessToken
     * @return
     */
    public static JSONObject getFeishuWikiSpaces(String tenantAccessToken) {
        JSONArray allItems = new JSONArray();
        String pageToken = null;
        Boolean hasMore = false;
        do {
            JSONObject query = new JSONObject();
            query.put("page_size", 50);// 最大值是50
            if (StringUtils.isNotBlank(pageToken)) {
                query.put("page_token", pageToken);
            }
            HttpRequestUtil request = HttpRequestUtil.get(WIKI_V2_SPACES_URL)
                    .addHeader("Authorization", "Bearer " + tenantAccessToken)
                    .setQueryString(query);
            JSONObject result = request.sendRequest().getResultJson();
            checkFeishuResult(result);
            JSONObject data = result.getJSONObject("data");
            if (MapUtils.isNotEmpty(data)) {
                pageToken = data.getString("page_token");
                hasMore = data.getBoolean("has_more");
                JSONArray items = data.getJSONArray("items");
                if (CollectionUtils.isNotEmpty(items)) {
                    allItems.addAll(items);
                }
            }
        } while (Objects.equals(hasMore, true));
        JSONObject resultObj = new JSONObject();
        resultObj.put("msg", "success");
        resultObj.put("code", 0);
        resultObj.put("data", new JSONObject().fluentPut("has_more", false).fluentPut("items", allItems));
        return resultObj;
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
     *
     * @param spaceId
     * @param parentNodeToken
     * @return
     */
    public static JSONObject getFeishuWikiNodes(Long spaceId, String parentNodeToken, String tenantAccessToken) {
        String url = SPACE_NODES_URL.replace(":space_id", spaceId.toString());
        JSONArray allItems = new JSONArray();
        String pageToken = null;
        Boolean hasMore = false;
        do {
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
            checkFeishuResult(result);
            JSONObject data = result.getJSONObject("data");
            if (MapUtils.isNotEmpty(data)) {
                hasMore = data.getBoolean("has_more");
                pageToken = data.getString("page_token");
                JSONArray items = data.getJSONArray("items");
                if (CollectionUtils.isNotEmpty(items)) {
                    if (CollectionUtils.isNotEmpty(items)) {
                        allItems.addAll(items);
                    }
                }
            }
        } while (Objects.equals(hasMore, true));
        JSONObject resultObj = new JSONObject();
        resultObj.put("msg", "success");
        resultObj.put("code", 0);
        resultObj.put("data", new JSONObject().fluentPut("has_more", false).fluentPut("items", allItems));
        return resultObj;
    }

    /**
     * {
     * 	"msg": "success",
     * 	"code": 0,
     * 	"data": {
     * 		"node": {
     * 			"owner": "ou_91b7dbc06db12a0e739bb61ef0d2ef4c",
     * 			"creator": "ou_91b7dbc06db12a0e739bb61ef0d2ef4c",
     * 			"node_creator": "ou_91b7dbc06db12a0e739bb61ef0d2ef4c",
     * 			"obj_create_time": "1779071123",
     * 			"node_token": "A1JXw9a6nisS7okNXuGciQQNnwc",
     * 			"origin_space_id": "7397619128632180764",
     * 			"title": "基础数据",
     * 			"obj_edit_time": "1779275315",
     * 			"node_type": "origin",
     * 			"origin_node_token": "A1JXw9a6nisS7okNXuGciQQNnwc",
     * 			"node_create_time": "1779071123",
     * 			"obj_token": "DKJJddRlBoQEKQxGIOncoO8inqg",
     * 			"obj_type": "docx",
     * 			"has_child": false,
     * 			"space_id": "7397619128632180764",
     * 			"parent_node_token": "Lf9ywVj7ki2H91kmCNIc0WDFnhc"
     * 		     }
     *      }
     * }
     * @param nodeToken
     * @param tenantAccessToken
     * @return
     */
    public static JSONObject getFeishuNodeInfo(String nodeToken, String tenantAccessToken) {
        JSONObject query = new JSONObject();
        query.put("token", nodeToken);
        HttpRequestUtil request = HttpRequestUtil.get(GET_NODE_URL)
                .addHeader("Authorization", "Bearer " + tenantAccessToken)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setQueryString(query);
        JSONObject result = request.sendRequest().getResultJson();
        checkFeishuResult(result);
        return result;
    }

    /**
     *
     * @param spaceId
     * @param tenantAccessToken
     * @return
     */
    public static JSONObject getFeishuSpaceInfo(Long spaceId, String tenantAccessToken) {
//        JSONObject query = new JSONObject();
//        query.put("token", nodeToken);
        String url = GET_SPACE_URL.replace(":space_id", spaceId.toString());
        HttpRequestUtil request = HttpRequestUtil.get(url)
                .addHeader("Authorization", "Bearer " + tenantAccessToken)
                .addHeader("Content-Type", "application/json; charset=utf-8")
//                .setQueryString(query)
                ;
        JSONObject result = request.sendRequest().getResultJson();
        checkFeishuResult(result);
        return result;
    }

    public static JSONObject getDocumentBlocks(String objToken, String tenantAccessToken) {
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
            HttpRequestUtil request = HttpRequestUtil.get(url)
                    .addHeader("Authorization", "Bearer " + tenantAccessToken)
                    .setQueryString(query);
            JSONObject result = request.sendRequest().getResultJson();
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
    public static FileVo downloadMedias(String fileToken, String tenantAccessToken) {
//        String url = "https://open.feishu.cn/open-apis/drive/v1/medias/:file_token/download";
        String url = MEDIAS_DOWNLOAD_URL.replace(":file_token", fileToken);
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
            FileVo fileVo = new FileVo();
            // 飞书素材同步到知识库后按知识库附件类型保存，便于后续下载和删除校验复用现有逻辑。
            fileVo.setType("knowledge");
            fileVo.setName(fileName);
            fileVo.setSize((long) data.length);
            fileVo.setContentType(contentType);
            fileVo.setUserUuid(UserContext.get().getUserUuid(true));
            fileVo.setUploadTime(new Date());
            String tenantUuid = TenantContext.get().getTenantUuid();
            String filePath = FileUtil.saveData(tenantUuid, new ByteArrayInputStream(data), fileVo);
            fileVo.setPath(filePath);
            return fileVo;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
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
    public static String getFirstResponseHeader(Map<String, List<String>> responseHeaderMap, String headerName) {
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
    public static String parseFeishuMediaFileName(String contentDisposition, String fileToken) {
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
                        logger.error(ignored.getMessage(), ignored);
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
    public static String trimQuote(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        String result = StringUtils.trim(value);
        if ((result.startsWith("\"") && result.endsWith("\"")) || (result.startsWith("'") && result.endsWith("'"))) {
            return result.substring(1, result.length() - 1);
        }
        return result;
    }

    public static void checkFeishuResult(JSONObject result) {
        if (result == null) {
            throw new RuntimeException("飞书接口无返回");
        }
        Integer code = result.getInteger("code");
        if (code != null && code != 0) {
            throw new RuntimeException(result.getString("msg"));
        }
    }

}
