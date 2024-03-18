/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.knowledge.api.document;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.exception.type.PermissionDeniedException;
import neatlogic.framework.fulltextindex.core.FullTextIndexHandlerFactory;
import neatlogic.framework.fulltextindex.core.IFullTextIndexHandler;
import neatlogic.framework.lcs.exception.LineHandlerNotFoundException;
import neatlogic.framework.lcs.linehandler.core.ILineHandler;
import neatlogic.framework.lcs.linehandler.core.LineHandlerFactory;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.UuidUtil;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_BASE;
import neatlogic.framework.knowledge.constvalue.KnowledgeDocumentOperate;
import neatlogic.framework.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeTagMapper;
import neatlogic.framework.knowledge.dto.*;
import neatlogic.framework.knowledge.exception.*;
import neatlogic.framework.knowledge.constvalue.KnowledgeFullTextIndexType;
import neatlogic.module.knowledge.service.KnowledgeDocumentService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
@Transactional
public class KnowledgeDocumentDraftSaveApi extends PrivateApiComponentBase {

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;
    @Resource
    private KnowledgeDocumentTypeMapper knowledgeDocumentTypeMapper;
    @Resource
    private KnowledgeTagMapper knowledgeTagMapper;

    @Override
    public String getToken() {
        return "knowledge/document/draft/save";
    }

    @Override
    public String getName() {
        return "保存文档草稿";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "knowledgeDocumentVersionId", type = ApiParamType.LONG, desc = "版本id"),
            @Param(name = "knowledgeDocumentTypeUuid", type = ApiParamType.STRING, isRequired = true, desc = "类型id"),
            @Param(name = "title", type = ApiParamType.STRING, isRequired = true, minLength = 1, desc = "标题"),
            @Param(name = "lineList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "行数据列表"),
            @Param(name = "fileIdList", type = ApiParamType.JSONARRAY, desc = "附件id列表"),
            @Param(name = "tagList", type = ApiParamType.JSONARRAY, desc = "标签列表"),
            @Param(name = "invokeId", type = ApiParamType.LONG, desc = "调用者id"),
            @Param(name = "source", type = ApiParamType.STRING, desc = "来源"),
            @Param(name = "isSubmit", type = ApiParamType.INTEGER, desc = "是否提交")
    })
    @Output({
            @Param(name = "knowledgeDocumentId", type = ApiParamType.LONG, desc = "文档id"),
            @Param(name = "knowledgeDocumentVersionId", type = ApiParamType.LONG, desc = "版本id"),
            @Param(name = "isReviewable", type = ApiParamType.INTEGER, desc = "是否能审批"),
    })
    @Description(desc = "保存文档草稿")
    //@ResubmitInterval(value = 5) //无需限制，用户可能存在快速修改保存的情况。
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long knowledgeDocumentVersionId = jsonObj.getLong("knowledgeDocumentVersionId");
        KnowledgeDocumentVo documentVo = JSON.toJavaObject(jsonObj, KnowledgeDocumentVo.class);
        KnowledgeDocumentTypeVo knowledgeDocumentTypeVo = knowledgeDocumentTypeMapper.getTypeByUuid(documentVo.getKnowledgeDocumentTypeUuid());
        if (knowledgeDocumentTypeVo == null) {
            throw new KnowledgeDocumentTypeNotFoundException(documentVo.getKnowledgeDocumentTypeUuid());
        }
        if (knowledgeDocumentService.isMember(knowledgeDocumentTypeVo.getKnowledgeCircleId()) == 0) {
            throw new PermissionDeniedException();
        }
        documentVo.setKnowledgeCircleId(knowledgeDocumentTypeVo.getKnowledgeCircleId());
        documentVo.setId(null);
        JSONObject resultObj = new JSONObject();
        Integer isSubmit = jsonObj.getInteger("isSubmit");
        int isReviewable = 0;
        String status = KnowledgeDocumentVersionStatus.DRAFT.getValue();
        if (Objects.equals(isSubmit, 1)) {
            status = KnowledgeDocumentVersionStatus.SUBMITTED.getValue();
            isReviewable = knowledgeDocumentService.isReviewer(knowledgeDocumentTypeVo.getKnowledgeCircleId());
        }
        resultObj.put("isReviewable", isReviewable);

        boolean needSaveDocument = true;
        KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = new KnowledgeDocumentVersionVo();
        knowledgeDocumentVersionVo.setTitle(documentVo.getTitle());
        knowledgeDocumentVersionVo.setKnowledgeDocumentTypeUuid(documentVo.getKnowledgeDocumentTypeUuid());
        knowledgeDocumentVersionVo.setLcu(UserContext.get().getUserUuid(true));
        knowledgeDocumentVersionVo.setStatus(status);
        if (knowledgeDocumentVersionId == null) {
            /* 没有版本id，则是首次创建文档 **/
            if (knowledgeDocumentMapper.getKnowledgeDocumentByTitle(documentVo.getTitle()) != null) {
                throw new KnowledgeDocumentTitleRepeatException(documentVo.getTitle());
            }
            documentVo.setFcu(UserContext.get().getUserUuid(true));
            documentVo.setVersion(0);
            knowledgeDocumentMapper.insertKnowledgeDocument(documentVo);
            if (StringUtils.isNotBlank(documentVo.getSource())) {
                if (documentVo.getInvokeId() == null) {
                    throw new ParamNotExistsException("invokeId");
                }
                KnowledgeDocumentInvokeVo knowledgeDocumentInvokeVo = new KnowledgeDocumentInvokeVo(documentVo.getId(), documentVo.getInvokeId(), documentVo.getSource());
                if (knowledgeDocumentMapper.getKnowledgeDocumentIdByInvokeIdAndSource(knowledgeDocumentInvokeVo) != null) {
                    throw new KnowledgeDocumentRepeatInvokeException();
                }
                knowledgeDocumentMapper.insertKnowledgeDocumentInvoke(knowledgeDocumentInvokeVo);
            }
            knowledgeDocumentMapper.insertKnowledgeDocumentViewCount(documentVo.getId(), 0);
            knowledgeDocumentVersionVo.setKnowledgeDocumentId(documentVo.getId());
            knowledgeDocumentVersionVo.setFromVersion(0);
            knowledgeDocumentMapper.insertKnowledgeDocumentVersion(knowledgeDocumentVersionVo);
            documentVo.setKnowledgeDocumentVersionId(knowledgeDocumentVersionVo.getId());
            resultObj.put("knowledgeDocumentId", documentVo.getId());
            resultObj.put("knowledgeDocumentVersionId", knowledgeDocumentVersionVo.getId());
        } else {
            /* 有版本id，则是在已有文档上修改 **/
            KnowledgeDocumentVersionVo oldKnowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
            if (oldKnowledgeDocumentVersionVo == null) {
                throw new KnowledgeDocumentVersionNotFoundException(knowledgeDocumentVersionId);
            }
            /* 获取文档锁 **/
            KnowledgeDocumentVo oldDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentLockById(oldKnowledgeDocumentVersionVo.getKnowledgeDocumentId());
            if (oldDocumentVo == null) {
                throw new KnowledgeDocumentNotFoundException(oldKnowledgeDocumentVersionVo.getKnowledgeDocumentId());
            }
            if (status.equals(KnowledgeDocumentVersionStatus.SUBMITTED.getValue())) {
                if (knowledgeDocumentMapper.checkIFThereIsSubmittedDraftByKnowDocumentId(oldKnowledgeDocumentVersionVo.getKnowledgeDocumentId()) > 0) {
                    throw new KnowledgeDocumentDraftSubmitFailedExecption();
                }
            }
            documentVo.setId(oldDocumentVo.getId());
            if(knowledgeDocumentMapper.checkKnowledgeDocumentTitleIsRepeat(documentVo) > 0){
                throw new KnowledgeDocumentTitleRepeatException(documentVo.getTitle());
            }
            resultObj.put("knowledgeDocumentId", documentVo.getId());
            KnowledgeDocumentVo before = knowledgeDocumentService.getKnowledgeDocumentDetailByKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
            oldKnowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
            if (knowledgeDocumentVersionId.equals(oldDocumentVo.getKnowledgeDocumentVersionId())) {
                if (!checkDocumentIsModify(before, documentVo)) {
                    throw new KnowledgeDocumentUnmodifiedCannotBeSavedException();
                }
                /* 如果入参版本id是文档当前版本id，说明该操作是当前版本上修改首次存草稿 **/
                knowledgeDocumentVersionVo.setKnowledgeDocumentId(documentVo.getId());
                knowledgeDocumentVersionVo.setFromVersion(oldKnowledgeDocumentVersionVo.getVersion());
                if (oldKnowledgeDocumentVersionVo.getIsDelete() == 1) {
                    knowledgeDocumentVersionVo.setFromVersion(0);
                } else {
                    knowledgeDocumentVersionVo.setFromVersion(oldKnowledgeDocumentVersionVo.getVersion());
                }
                knowledgeDocumentMapper.insertKnowledgeDocumentVersion(knowledgeDocumentVersionVo);
                documentVo.setKnowledgeDocumentVersionId(knowledgeDocumentVersionVo.getId());
                resultObj.put("knowledgeDocumentVersionId", knowledgeDocumentVersionVo.getId());
            } else {
                /* 如果入参版本id不是文档当前版本id，说明该操作是在已有草稿上再次保存 **/
                if (KnowledgeDocumentVersionStatus.PASSED.getValue().equals(oldKnowledgeDocumentVersionVo.getStatus())) {
                    throw new KnowledgeDocumentDraftPublishedCannotBeModifiedException();
                } else if (KnowledgeDocumentVersionStatus.SUBMITTED.getValue().equals(oldKnowledgeDocumentVersionVo.getStatus())) {
                    throw new KnowledgeDocumentDraftSubmittedCannotBeModifiedException();
                }
                if (!oldKnowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid(true))) {
                    throw new KnowledgeDocumentCurrentUserNotOwnerException();
                }
                knowledgeDocumentVersionVo.setId(knowledgeDocumentVersionId);
                documentVo.setKnowledgeDocumentVersionId(knowledgeDocumentVersionVo.getId());
                resultObj.put("knowledgeDocumentVersionId", knowledgeDocumentVersionVo.getId());
                needSaveDocument = checkDocumentIsModify(before, documentVo);
                if (needSaveDocument) {
                    knowledgeDocumentMapper.deleteKnowledgeDocumentLineByKnowledgeDocumentVersionId(knowledgeDocumentVersionVo.getId());
                    knowledgeDocumentMapper.deleteKnowledgeDocumentFileByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentFileVo(documentVo.getId(), knowledgeDocumentVersionVo.getId()));
                    knowledgeDocumentMapper.deleteKnowledgeDocumentTagByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentTagVo(documentVo.getId(), knowledgeDocumentVersionVo.getId()));
                }
                /* 覆盖旧草稿时，更新标题、修改用户、修改时间，删除行数据、附件、标签数据，后面再重新插入 **/
                if (oldKnowledgeDocumentVersionVo.getIsDelete() == 1) {
                    knowledgeDocumentVersionVo.setFromVersion(0);
                }
                knowledgeDocumentMapper.updateKnowledgeDocumentVersionById(knowledgeDocumentVersionVo);
            }
        }
        if (needSaveDocument) {
            saveDocument(documentVo);
            //创建全文检索索引
            IFullTextIndexHandler handler = FullTextIndexHandlerFactory.getHandler(KnowledgeFullTextIndexType.KNOW_DOCUMENT_VERSION);
            if (handler != null) {
                handler.createIndex(documentVo.getKnowledgeDocumentVersionId());
            }
        }
        if (status.equals(KnowledgeDocumentVersionStatus.SUBMITTED.getValue())) {
            knowledgeDocumentService.audit(documentVo.getId(), documentVo.getKnowledgeDocumentVersionId(), KnowledgeDocumentOperate.SUBMIT, null);
        }

        return resultObj;
    }

    public IValid title() {
        return value -> {
            String title = value.getString("title");
            if (knowledgeDocumentMapper.getKnowledgeDocumentByTitle(title) != null) {
                return new FieldValidResultVo(new KnowledgeDocumentTitleRepeatException(title));
            }
            return new FieldValidResultVo();
        };
    }

    /**
     * @Description: 保存文档内容
     * @Author: linbq
     * @Date: 2021/1/25 12:14
     * @Params: [documentVo]
     * @Returns:void
     **/
    private void saveDocument(KnowledgeDocumentVo documentVo) {
        /* 保存附件 **/
        KnowledgeDocumentFileVo knowledgeDocumentFileVo = new KnowledgeDocumentFileVo();
        knowledgeDocumentFileVo.setKnowledgeDocumentId(documentVo.getId());
        knowledgeDocumentFileVo.setKnowledgeDocumentVersionId(documentVo.getKnowledgeDocumentVersionId());
        for (Long fileId : documentVo.getFileIdList()) {
            knowledgeDocumentFileVo.setFileId(fileId);
            knowledgeDocumentMapper.insertKnowledgeDocumentFile(knowledgeDocumentFileVo);
        }
        /* 保存标签 **/
        KnowledgeDocumentTagVo knowledgeDocumentTagVo = new KnowledgeDocumentTagVo();
        knowledgeDocumentTagVo.setKnowledgeDocumentId(documentVo.getId());
        knowledgeDocumentTagVo.setKnowledgeDocumentVersionId(documentVo.getKnowledgeDocumentVersionId());
        for (String tagName : documentVo.getTagList()) {
            Long tagId = knowledgeTagMapper.getKnowledgeTagIdByName(tagName);
            if (tagId == null) {
                KnowledgeTagVo knowledgeTagVo = new KnowledgeTagVo(tagName);
                knowledgeTagMapper.insertKnowledgeTag(knowledgeTagVo);
                tagId = knowledgeTagVo.getId();
            }
            knowledgeDocumentTagVo.setTagId(tagId);
            knowledgeDocumentMapper.insertKnowledgeDocumentTag(knowledgeDocumentTagVo);
        }
        /* 保存每行内容，计算文档大小 **/
        int size = 0;
        int lineNumber = 0;
        List<KnowledgeDocumentLineVo> knowledgeDocumentLineList = new ArrayList<>(100);
        for (KnowledgeDocumentLineVo knowledgeDocumentLineVo : documentVo.getLineList()) {
            knowledgeDocumentLineVo.setLineNumber(++lineNumber);
            knowledgeDocumentLineVo.setKnowledgeDocumentId(documentVo.getId());
            knowledgeDocumentLineVo.setKnowledgeDocumentVersionId(documentVo.getKnowledgeDocumentVersionId());
            knowledgeDocumentLineVo.setUuid(UuidUtil.randomUuid());
            if (knowledgeDocumentLineVo.getConfig() != null) {
                KnowledgeDocumentLineConfigVo knowledgeDocumentLineConfigVo = new KnowledgeDocumentLineConfigVo(knowledgeDocumentLineVo.getConfigStr());
                knowledgeDocumentLineVo.setConfigHash(knowledgeDocumentLineConfigVo.getHash());
                if (knowledgeDocumentMapper.checkKnowledgeDocumentLineConfigHashIsExists(knowledgeDocumentLineConfigVo.getHash()) == 0) {
                    knowledgeDocumentMapper.insertKnowledgeDocumentLineConfig(knowledgeDocumentLineConfigVo);
                }
            }
            if (StringUtils.isNotBlank(knowledgeDocumentLineVo.getContent())) {
                size += knowledgeDocumentLineVo.getContent().getBytes(StandardCharsets.UTF_8).length;
                KnowledgeDocumentLineContentVo knowledgeDocumentLineContentVo = new KnowledgeDocumentLineContentVo(knowledgeDocumentLineVo.getContent());
                knowledgeDocumentLineVo.setContentHash(knowledgeDocumentLineContentVo.getHash());
                if (knowledgeDocumentMapper.checkKnowledgeDocumentLineContentHashIsExists(knowledgeDocumentLineContentVo.getHash()) == 0) {
                    knowledgeDocumentMapper.insertKnowledgeDocumentLineContent(knowledgeDocumentLineContentVo);
                }
            }
            knowledgeDocumentLineList.add(knowledgeDocumentLineVo);
            if (knowledgeDocumentLineList.size() >= 100) {
                knowledgeDocumentMapper.insertKnowledgeDocumentLineList(knowledgeDocumentLineList);
                knowledgeDocumentLineList.clear();
            }
        }
        if (CollectionUtils.isNotEmpty(knowledgeDocumentLineList)) {
            knowledgeDocumentMapper.insertKnowledgeDocumentLineList(knowledgeDocumentLineList);
        }
        /* 更新文档大小 **/
        KnowledgeDocumentVersionVo updateSizeVo = new KnowledgeDocumentVersionVo();
        updateSizeVo.setId(documentVo.getKnowledgeDocumentVersionId());
        updateSizeVo.setSize(size);
        knowledgeDocumentMapper.updateKnowledgeDocumentVersionById(updateSizeVo);
    }

    /*
     * @Description: 检查文档内容信息是否被修改
     * @Author: linbq
     * @Date: 2021/2/26 3:56 下午
     * @Params: [before, after]
     * @Returns: boolean
     **/
    private boolean checkDocumentIsModify(KnowledgeDocumentVo before, KnowledgeDocumentVo after) {
        //knowledgeDocumentTypeUuid
        if (!Objects.equals(before.getKnowledgeDocumentTypeUuid(), after.getKnowledgeDocumentTypeUuid())) {
            return true;
        }
        //title
        if (!Objects.equals(before.getTitle(), after.getTitle())) {
            return true;
        }
        //fileIdList
        if (!SetUtils.isEqualSet(new HashSet<>(before.getFileIdList()), new HashSet<>(after.getFileIdList()))) {
            return true;
        }
        //tagList
        if (!SetUtils.isEqualSet(new HashSet<>(before.getTagList()), new HashSet<>(after.getTagList()))) {
            return true;
        }
        //lineList
        List<KnowledgeDocumentLineVo> beforeLineList = before.getLineList();
        List<KnowledgeDocumentLineVo> afterLineList = after.getLineList();
        if (beforeLineList.size() != afterLineList.size()) {
            return true;
        }
        Iterator<KnowledgeDocumentLineVo> beforeLineIterator = beforeLineList.iterator();
        Iterator<KnowledgeDocumentLineVo> afterLineIterator = afterLineList.iterator();
        while (beforeLineIterator.hasNext() && afterLineIterator.hasNext()) {
            KnowledgeDocumentLineVo beforeLine = beforeLineIterator.next();
            KnowledgeDocumentLineVo afterLine = afterLineIterator.next();
            if (!Objects.equals(beforeLine.getHandler(), afterLine.getHandler())) {
                return true;
            }
            String handler = beforeLine.getHandler();
            ILineHandler lineHandler = LineHandlerFactory.getHandler(handler);
            if (lineHandler == null) {
                throw new LineHandlerNotFoundException(handler);
            }
            String beforeMainBody = lineHandler.getMainBody(beforeLine);
            String afterMainBody = lineHandler.getMainBody(afterLine);
            if (!Objects.equals(beforeMainBody, afterMainBody)) {
//                System.out.println(beforeMainBody);
//                System.out.println(afterMainBody);
                return true;
            }
            if (!Objects.equals(beforeLine.getConfigStr(), afterLine.getConfigStr())) {
                return true;
            }
        }
        return false;
    }
}
