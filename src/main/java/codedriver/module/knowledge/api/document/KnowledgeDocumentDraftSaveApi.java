package codedriver.module.knowledge.api.document;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.label.NO_AUTH;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.framework.fulltextindex.core.FullTextIndexHandlerFactory;
import codedriver.framework.fulltextindex.core.IFullTextIndexHandler;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.UuidUtil;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentLineHandler;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentOperate;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeTagMapper;
import codedriver.module.knowledge.dto.*;
import codedriver.module.knowledge.exception.*;
import codedriver.module.knowledge.fulltextindex.FullTextIndexType;
import codedriver.module.knowledge.service.KnowledgeDocumentService;
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
@AuthAction(action = NO_AUTH.class)
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
    @ResubmitInterval(value = 5)
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
                    throw new ParamNotExistsException("参数：“invokeId”不能为空");
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
            IFullTextIndexHandler handler = FullTextIndexHandlerFactory.getComponent(FullTextIndexType.KNOW_DOCUMENT_VERSION);
            if (handler != null) {
                handler.createIndex(documentVo.getKnowledgeDocumentVersionId());
            }
        }
        if (status.equals(KnowledgeDocumentVersionStatus.SUBMITTED.getValue())) {
            knowledgeDocumentService.audit(documentVo.getId(), documentVo.getKnowledgeDocumentVersionId(), KnowledgeDocumentOperate.SUBMIT, null);
        }

        return resultObj;
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
            String beforeMainBody = KnowledgeDocumentLineHandler.getMainBody(beforeLine);
            String afterMainBody = KnowledgeDocumentLineHandler.getMainBody(afterLine);
            if (!Objects.equals(beforeMainBody, afterMainBody)) {
//                System.out.println(beforeMainBody);
//                System.out.println(afterMainBody);
                return true;
            }
        }
        return false;
    }
}
