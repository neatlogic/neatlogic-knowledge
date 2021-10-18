package codedriver.module.knowledge.api.document;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.util.FileUtil;
import codedriver.framework.file.dao.mapper.FileMapper;
import codedriver.framework.file.dto.FileVo;
import codedriver.framework.knowledge.linehandler.core.ILineHandler;
import codedriver.framework.knowledge.linehandler.core.LineHandlerFactory;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.util.DocType;
import codedriver.framework.util.ExportUtil;
import codedriver.module.knowledge.auth.label.KNOWLEDGE_BASE;
import codedriver.framework.knowledge.constvalue.KnowledgeDocumentLineHandler;
import codedriver.framework.knowledge.dto.KnowledgeDocumentLineVo;
import codedriver.framework.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.service.KnowledgeDocumentService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.util.Base64;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URLEncoder;

@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentExportApi extends PrivateBinaryStreamApiComponentBase {

    private static final Log logger = LogFactory.getLog(KnowledgeDocumentExportApi.class);

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    @Resource
    private FileMapper fileMapper;

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

        String type = jsonObj.getString("type");
        Long knowledgeDocumentId = jsonObj.getLong("knowledgeDocumentId");
        Long knowledgeDocumentVersionId = jsonObj.getLong("knowledgeDocumentVersionId");
        Long currentVersionId = knowledgeDocumentService.checkViewPermissionByDocumentIdAndVersionId(knowledgeDocumentId, knowledgeDocumentVersionId);

        KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentService.getKnowledgeDocumentContentByKnowledgeDocumentVersionId(currentVersionId);
        OutputStream os = null;
        try {
            os = response.getOutputStream();
            String content = getHtmlContent(knowledgeDocumentVo);
            if (DocType.WORD.getValue().equals(type)) {
                response.setContentType("application/x-download");
                response.setHeader("Content-Disposition",
                        " attachment; filename=\"" + URLEncoder.encode(knowledgeDocumentVo.getTitle(), "utf-8") + ".docx\"");
                ExportUtil.getWordFileByHtml(content, true, os);
            } else if (DocType.PDF.getValue().equals(type)) {
                response.setContentType("application/pdf");
                response.setHeader("Content-Disposition",
                        " attachment; filename=\"" + URLEncoder.encode(knowledgeDocumentVo.getTitle(), "utf-8") + ".pdf\"");
                ExportUtil.getPdfFileByHtml(content, true, os);
            }
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

    private String getHtmlContent(KnowledgeDocumentVo knowledgeDocumentVo) throws Exception {
        InputStream in = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StringWriter out = new StringWriter();
        out.write("<html xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:o=\"urn:schemas-microsoft-com:office:office\" xmlns:x=\"urn:schemas-microsoft-com:office:excel\" xmlns=\"http://www.w3.org/TR/REC-html40\">\n");
        out.write("<head>\n");
        out.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></meta>\n");
        out.write("<style>\n" + style + "\n</style>\n");
        out.write("</head>\n");
        out.write("<body>\n");
        for (KnowledgeDocumentLineVo line : knowledgeDocumentVo.getLineList()) {
            if (!KnowledgeDocumentLineHandler.IMG.getValue().equals(line.getHandler())) {
                ILineHandler lineHandler = LineHandlerFactory.getHandler(line.getHandler());
                if (lineHandler != null) {
                    out.write(lineHandler.convertContentToHtml(line));
                }
            } else {
                bos.reset();
                String url = line.getConfig().getString("url");
                String value = line.getConfig().getString("value");
                if (StringUtils.isNotBlank(url)) {
                    String id = url.split("=")[1];
                    FileVo fileVo = fileMapper.getFileById(Long.valueOf(id));
                    if (fileVo != null) {
                        in = FileUtil.getData(fileVo.getPath());
                        IOUtils.copyLarge(in, bos);
                        out.write("<div><img src=\"data:image/png;base64," + Base64.encodeBase64String(bos.toByteArray()) + "\">");
                        if (StringUtils.isNotBlank(value)) {
                            out.write("<br/><span>备注：" + value + "</span>");
                        }
                        out.write("</div>");
                    }
                }
            }
        }
        if (in != null) {
            in.close();
        }
        bos.close();
        out.write("\n</body>\n</html>");
        out.flush();
        out.close();
        return out.toString();
    }


    private static final String style = ".tstable-container {\n" +
            "  position: relative;\n" +
            "  overflow: hidden;\n" +
            "}\n" +
            ".tstable-container.tstable-small .tstable-body th,\n" +
            ".tstable-container.tstable-small .tstable-body td {\n" +
            "  padding: 4px;\n" +
            "}\n" +
            ".tstable-container.tstable-small .tstable-body.table-top th {\n" +
            "  height: 28px;\n" +
            "}\n" +
            "\n" +
            ".tstable-container.tstable-card {\n" +
            "  border-top: 0 none;\n" +
            "}\n" +
            ".tstable-container.tstable-card .tstable-body th {\n" +
            "  border: 0 none !important;\n" +
            "}\n" +
            ".tstable-container.tstable-card .tstable-body td {\n" +
            "  opacity: 1;\n" +
            "  border: 0 none !important;\n" +
            "  position: relative;\n" +
            "  padding-top: 12px;\n" +
            "  padding-bottom: 12px;\n" +
            "}\n" +
            ".tstable-container.tstable-card .tstable-body td:before {\n" +
            "  position: absolute;\n" +
            "  content: '';\n" +
            "  top: 6px;\n" +
            "  bottom: 6px;\n" +
            "  left: 0;\n" +
            "  right: 0;\n" +
            "}\n" +
            ".tstable-container.tstable-card .tstable-body td > div {\n" +
            "  position: relative;\n" +
            "}\n" +
            ".tstable-container.tstable-card.tstable-nohover .tstable-body tr td .action-div {\n" +
            "  top: 6px;\n" +
            "  bottom: 6px;\n" +
            "}\n" +
            ".tstable-container.tstable-noborder td {\n" +
            "  border-bottom: 0 none;\n" +
            "}\n" +
            ".tstable-container:hover .btn-setting {\n" +
            "  opacity: 1;\n" +
            "}\n" +
            ".tstable-container .btn-setting {\n" +
            "  position: absolute;\n" +
            "  top: 0px;\n" +
            "  right: 0;\n" +
            "  z-index: 9;\n" +
            "}\n" +
            ".tstable-container .btn-setting .icon-setting {\n" +
            "  padding: 9px 9px;\n" +
            "  padding-right: 25px;\n" +
            "  cursor: pointer;\n" +
            "  display: block;\n" +
            "}\n" +
            ".tstable-container .tstable-main {\n" +
            "  overflow: auto;\n" +
            "  min-height: 40px;\n" +
            "}\n" +
            ".tstable-container .table-top {\n" +
            "  position: relative;\n" +
            "  z-index: 9;\n" +
            "}\n" +
            ".tstable-container .table-top > tbody > tr > td,\n" +
            ".tstable-container .table-top > tbody > tr > th {\n" +
            "  height: 0;\n" +
            "  overflow: hidden;\n" +
            "  padding-top: 0 !important;\n" +
            "  padding-bottom: 0 !important;\n" +
            "  border-bottom: 0 none !important;\n" +
            "  line-height: 0;\n" +
            "}\n" +
            ".tstable-container .table-top > tbody > tr > td > *,\n" +
            ".tstable-container .table-top > tbody > tr > th > * {\n" +
            "  height: 0 !important;\n" +
            "  overflow: hidden;\n" +
            "  margin-top: 0 !important;\n" +
            "  margin-bottom: 0 !important;\n" +
            "  border-top: 0 none !important;\n" +
            "  border-bottom: 0 none !important;\n" +
            "}\n" +
            ".tstable-container .table-main > thead > tr > th {\n" +
            "  height: 0;\n" +
            "  overflow: hidden;\n" +
            "  padding-top: 0 !important;\n" +
            "  padding-bottom: 0 !important;\n" +
            "  border-bottom: 0 none !important;\n" +
            "  line-height: 0;\n" +
            "}\n" +
            ".tstable-container .table-main > thead > tr > th > * {\n" +
            "  height: 0 !important;\n" +
            "  overflow: hidden;\n" +
            "  margin-top: 0 !important;\n" +
            "  margin-bottom: 0 !important;\n" +
            "  border-top: 0 none !important;\n" +
            "  border-bottom: 0 none !important;\n" +
            "}\n" +
            ".tstable-container .tstable-body {\n" +
            "  min-width: 100%;\n" +
            "  text-align: left;\n" +
            "  border-collapse: collapse;\n" +
            "  border-spacing: 0;\n" +
            "}\n" +
            ".tstable-container .tstable-body th,\n" +
            ".tstable-container .tstable-body td {\n" +
            "  padding: 9px;\n" +
            "  font-weight: normal;\n" +
            "  line-height: inherit;\n" +
            "}\n" +
            ".tstable-container .tstable-body th {\n" +
            "  white-space: nowrap;\n" +
            "  word-break: keep-all;\n" +
            "  height: 38px;\n" +
            "  position: relative;\n" +
            "  -webkit-backface-visibility: hidden;\n" +
            "  backface-visibility: hidden;\n" +
            "  -webkit-perspective: 1000px;\n" +
            "  -moz-perspective: 1000px;\n" +
            "  -ms-perspective: 1000px;\n" +
            "  transition: none;\n" +
            "  perspective: 1000px;\n" +
            "  will-change: transform;\n" +
            "}\n" +
            ".tstable-container .tstable-body th .btn-resize {\n" +
            "  position: absolute;\n" +
            "  top: 0;\n" +
            "  right: 0;\n" +
            "  width: 8px;\n" +
            "  height: 100%;\n" +
            "  cursor: col-resize;\n" +
            "}\n" +
            ".tstable-container .tstable-body th .btn-resize:after {\n" +
            "  content: '';\n" +
            "  position: absolute;\n" +
            "  top: 0;\n" +
            "  right: 0;\n" +
            "  width: 1px;\n" +
            "  height: 100%;\n" +
            "}\n" +
            ".tstable-container .tstable-body td {\n" +
            "  white-space: nowrap;\n" +
            "  word-break: keep-all;\n" +
            "}\n" +
            ".tstable-container .tstable-body tbody tr {\n" +
            "  transition: opacity ease 0.3s;\n" +
            "}\n" +
            "\n" +
            ".tstable-container .tstable-body .tstable-selection {\n" +
            "  width: 16px;\n" +
            "  height: 16px;\n" +
            "  display: block;\n" +
            "  margin-right: 4px;\n" +
            "  margin-left: 4px;\n" +
            "  position: relative;\n" +
            "  display: inline-block;\n" +
            "}\n" +
            ".tstable-container .tstable-body .tstable-selection:hover {\n" +
            "  cursor: pointer;\n" +
            "}\n" +
            ".tstable-container .tstable-body .tstable-selection.selected:after {\n" +
            "  content: '';\n" +
            "  width: 11px;\n" +
            "  height: 6px;\n" +
            "  position: absolute;\n" +
            "  top: 50%;\n" +
            "  left: 50%;\n" +
            "  border-radius: 2px;\n" +
            "  transform: rotate(-45deg);\n" +
            "  margin-top: -5px;\n" +
            "  margin-left: -6px;\n" +
            "}\n" +
            ".tstable-container .tstable-body .tstable-selection.disabled {\n" +
            "  opacity: 0.9;\n" +
            "}\n" +
            ".tstable-container .tstable-body .tstable-selection.disabled:hover {\n" +
            "  cursor: not-allowed;\n" +
            "}\n" +
            ".tstable-container .tstable-body .tstable-selection.some:after {\n" +
            "  content: '';\n" +
            "  width: 8px;\n" +
            "  height: 2px;\n" +
            "  position: absolute;\n" +
            "  top: 50%;\n" +
            "  left: 50%;\n" +
            "  margin-top: -1px;\n" +
            "  margin-left: -4px;\n" +
            "  border: 0 none;\n" +
            "  transform: none;\n" +
            "}\n" +
            "\n" +
            ".tableaction-container {\n" +
            "  width: 0;\n" +
            "}\n" +
            ".tableaction-container .table-dropdown {\n" +
            "  padding: 0;\n" +
            "  margin: 0;\n" +
            "  box-shadow: none;\n" +
            "  top: 0;\n" +
            "  right: 100%;\n" +
            "  display: none;\n" +
            "}\n" +
            ".tableaction-container .table-dropdown .ivu-dropdown-menu {\n" +
            "  word-break: keep-all;\n" +
            "  white-space: nowrap;\n" +
            "  display: block;\n" +
            "  min-width: auto;\n" +
            "}\n" +
            ".tableaction-container .table-dropdown .ivu-dropdown-menu .ivu-dropdown-item {\n" +
            "  display: inline-block;\n" +
            "  padding: 0 10px;\n" +
            "  position: relative;\n" +
            "}\n" +
            ".tableaction-container .table-dropdown .ivu-dropdown-menu .ivu-dropdown-item:not(:last-of-type):after {\n" +
            "  content: '';\n" +
            "  width: 1px;\n" +
            "  height: 14px;\n" +
            "  top: 50%;\n" +
            "  margin-top: -7px;\n" +
            "  right: 0;\n" +
            "  position: absolute;\n" +
            "}\n" +
            ".tableaction-container .table-dropdown .ivu-dropdown-menu .ivu-dropdown-item:hover {\n" +
            "  background: transparent;\n" +
            "}\n" +
            ".ck-content {\n" +
            "  min-height: 130px;\n" +
            "}\n" +
            "html .ck.ck-reset_all,\n" +
            "html .ck.ck-reset_all * {\n" +
            "  color: #212121;\n" +
            "}\n" +
            "html .ck.ck-button:not(.ck-disabled):hover,\n" +
            "html a.ck.ck-button:not(.ck-disabled):hover {\n" +
            "  background-color: #fff;\n" +
            "}\n" +
            "html .ck.ck-button.ck-on,\n" +
            "html a.ck.ck-button.ck-on {\n" +
            "  background-color: #fff;\n" +
            "}\n" +
            "html .ck.ck-list {\n" +
            "  background-color: #fff;\n" +
            "}\n" +
            "html .ck.ck-list__item .ck-button:hover:not(.ck-disabled) {\n" +
            "  background-color: #E7F3FF;\n" +
            "}\n" +
            "html .ck.ck-list__item .ck-button.ck-on {\n" +
            "  background: #E7F3FF;\n" +
            "  color: #00bcd4;\n" +
            "}\n" +
            "html .ck.ck-dropdown__panel {\n" +
            "  background: #fff;\n" +
            "  border-color: #DBDBDB;\n" +
            "}\n" +
            "html .ck-content .table table td,\n" +
            "html .ck-content .table table th,\n" +
            "html .ck-content .table table {\n" +
            "  border-color: #DBDBDB !important;\n" +
            "  border: 1px solid;\n" +
            "}\n" +
            "\n" +
            "section {\n" +
            "  position: relative;\n" +
            "}\n" +
            " .sheet-table {\n" +
            "  width: 100%;\n" +
            "  border-collapse: collapse;\n" +
            "  border-spacing: 0px;\n" +
            "  table-layout: fixed;\n" +
            "  outline: none;\n" +
            "}\n" +
            " .sheet-table thead {\n" +
            "  height: 0;\n" +
            "}\n" +
            " .sheet-table tbody tr td {\n" +
            "  border: 1px solid;\n" +
            "  vertical-align: middle;\n" +
            "  padding: 3px;\n" +
            "  height: 40px;\n" +
            "  position: relative;\n" +
            "  word-break: break-all;\n" +
            "}\n" +
            " .sheet-table tbody tr td.text-right {\n" +
            "  padding-right: 12px;\n" +
            "}\n" +
            ".tstable-container .tstable-body tbody tr:hover {\n" +
            "  background: transparent !important;\n" +
            "}\n" +
            ".table-color .tstable-container {\n" +
            "  overflow: auto;\n" +
            "  border-top: 0px !important;\n" +
            "}\n" +
            ".table-color .tstable-container .table-list {\n" +
            "  width: 100%;\n" +
            "  border-top: none;\n" +
            "  border-collapse: collapse;\n" +
            "  table-layout: fixed;\n" +
            "}\n" +
            ".table-color .tstable-container .table-list > thead,\n" +
            ".table-color .tstable-container .table-list > thead > tr > th {\n" +
            "  visibility: visible !important;\n" +
            "  border: none !important;\n" +
            "  vertical-align: middle;\n" +
            "  height: 38px;\n" +
            "  padding-top: 0px;\n" +
            "  padding-bottom: 0px;\n" +
            "  text-align: left;\n" +
            "}\n" +
            ".table-color .tstable-container .table-list > tbody > tr > td {\n" +
            "  border-left: none !important;\n" +
            "  border-right: none !important;\n" +
            "  border-bottom: none !important;\n" +
            "  border-top: none !important;\n" +
            "  vertical-align: top;\n" +
            "}\n" +
            "\n" +
            ".ck-content .table table td,\n" +
            ".ck-content .table table th,\n" +
            ".ck-content .table table {\n" +
            "  border-color: #DBDBDB !important;\n" +
            "  border: 1px solid;\n" +
            "}\n" +
            "\n" +
            "ul li {\n" +
            "  list-style: disc;\n" +
            "}\n" +
            " ol {\n" +
            "  list-style: decimal inside;\n" +
            "}\n" +
            " ol li {\n" +
            "  list-style: decimal;\n" +
            "}\n" +
            "ol.cjk-ideographic li {\n" +
            "  list-style: cjk-ideographic;\n" +
            "}\n" +
            "span.line-through {\n" +
            "  text-decoration: line-through;\n" +
            "  vertical-align: baseline;\n" +
            "}\n" +
            "[class=line-through] * {\n" +
            "  text-decoration: line-through;\n" +
            "  vertical-align: baseline;\n" +
            "}\n" +
            "body{\n" +
            "  font-size:14px;\n" +
            "}\n" +
            "h2{\n" +
            "  font-size:14px;\n" +
            "}\n" +
            "h1{\n" +
            "  font-size:16px;\n" +
            "}";

}
