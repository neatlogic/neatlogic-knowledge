package codedriver.module.knowledge.api.document;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.util.ExportUtil;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentLineHandler;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentLineVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.exception.KnowledgeDocumentEmptyException;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotFoundException;
import codedriver.module.knowledge.exception.KnowledgeDocumentVersionNotFoundException;
import codedriver.module.knowledge.service.KnowledgeDocumentService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentExportApi extends PrivateBinaryStreamApiComponentBase {

    private static final Log logger = LogFactory.getLog(KnowledgeDocumentExportApi.class);

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    @Override
    public String getToken() {
        return "knowledge/document/export";
    }

    @Override
    public String getName() {
        return "导出文档内容";
    }

    @Override
    public String getConfig() {
        return null;
    }
    
    @Input({
        @Param(name = "knowledgeDocumentId", type = ApiParamType.LONG, isRequired = true, desc = "文档id"),
        @Param(name = "knowledgeDocumentVersionId", type = ApiParamType.LONG, desc = "版本id"),
        @Param(name = "type", type = ApiParamType.ENUM, rule = "pdf,word", isRequired = true, desc = "文件类型")
    })
    @Description(desc = "导出文档内容")
    @Override
    public Object myDoService(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        OutputStream os = null;
        String userUuid = UserContext.get().getUserUuid(true);
        Long knowledgeDocumentId = jsonObj.getLong("knowledgeDocumentId");
        KnowledgeDocumentVo documentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(knowledgeDocumentId);
        if(documentVo == null) {
            throw new KnowledgeDocumentNotFoundException(knowledgeDocumentId);
        }

        boolean isLcu = false;
        boolean isReviewer = false;
        Long currentVersionId = documentVo.getKnowledgeDocumentVersionId();
        Long knowledgeDocumentVersionId = jsonObj.getLong("knowledgeDocumentVersionId");
        if(knowledgeDocumentVersionId != null) {
            currentVersionId = knowledgeDocumentVersionId;
            KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
            if (knowledgeDocumentVersionVo == null) {
                throw new KnowledgeDocumentVersionNotFoundException(knowledgeDocumentVersionId);
            }
            if(knowledgeDocumentVersionVo.getLcu().equals(userUuid)){
                isLcu = true;
            }
            if(userUuid.equals(knowledgeDocumentVersionVo.getReviewer())){
                isReviewer = true;
            }
        }
        /** 如果当前用户不是成员，但是该版本的作者或者审核人，可以有查看权限 **/
        if(!isLcu && !isReviewer && knowledgeDocumentService.isMember(documentVo.getKnowledgeCircleId()) == 0) {
            throw new PermissionDeniedException();
        }

        KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentService.getKnowledgeDocumentDetailByKnowledgeDocumentVersionId(currentVersionId);
        if(knowledgeDocumentVo == null){
            throw new KnowledgeDocumentNotFoundException(knowledgeDocumentId);
        }
        if(CollectionUtils.isEmpty(knowledgeDocumentVo.getLineList())){
            throw new KnowledgeDocumentEmptyException(knowledgeDocumentId);
        }
        try {
            os = response.getOutputStream();
            response.setContentType("application/x-download");
            response.setHeader("Content-Disposition",
                    "attachment;filename=\"" + URLEncoder.encode(knowledgeDocumentVo.getTitle(), "utf-8") + ".docx\"");
            StringBuilder sb = new StringBuilder();
            sb.append("<html xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:o=\"urn:schemas-microsoft-com:office:office\" xmlns:x=\"urn:schemas-microsoft-com:office:excel\" xmlns=\"http://www.w3.org/TR/REC-html40\">\n");
            sb.append("<head>\n");
            sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></meta>\n");
            sb.append("</head>\n");
            sb.append("<body>\n");
            sb.append("<span></span>");//暂时解决首行乱码
            for(KnowledgeDocumentLineVo line : knowledgeDocumentVo.getLineList()){
                if(KnowledgeDocumentLineHandler.IMG.getValue().equals(line.getHandler())){

                }else{
                    sb.append(KnowledgeDocumentLineHandler.convertContentToHtml(line));
                }
            }
            sb.append("\n</body>\n</html>");
            ExportUtil.getWordFileByHtml(sb.toString(), true, os);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            if (os != null) {
                os.flush();
                os.close();
            }
        }

        return null;
    }

}
