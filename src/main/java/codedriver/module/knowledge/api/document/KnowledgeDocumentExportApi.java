package codedriver.module.knowledge.api.document;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.util.FileUtil;
import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.framework.file.dao.mapper.FileMapper;
import codedriver.framework.file.dto.FileVo;
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
import java.net.URLEncoder;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentExportApi extends PrivateBinaryStreamApiComponentBase {

    private static final Log logger = LogFactory.getLog(KnowledgeDocumentExportApi.class);

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
            ".tstable-container.tstable-small .tstable-body .tstable-action .tstable-action-ul {\n" +
            "  margin-top: -10px;\n" +
            "}\n" +
            ".tstable-container.tstable-small .tstable-body .tstable-action .tstable-action-ul li {\n" +
            "  padding: 2px;\n" +
            "}\n" +
            ".tstable-container.tstable-small .tstable-body .tstable-action .tstable-action-ul li:not(:last-of-type):after {\n" +
            "  top: 4px;\n" +
            "}\n" +
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
            ".tstable-container .tstable-body tbody tr .action-div {\n" +
            "  position: absolute;\n" +
            "  z-index: 2;\n" +
            "  top: 0;\n" +
            "  bottom: 0;\n" +
            "  right: 24px;\n" +
            "  display: none;\n" +
            "}\n" +
            ".tstable-container .tstable-body tbody tr:hover .tstable-action {\n" +
            "  z-index: 99;\n" +
            "}\n" +
            ".tstable-container .tstable-body tbody tr:hover .tstable-action .tstable-action-ul {\n" +
            "  opacity: 1;\n" +
            "  right: 0;\n" +
            "}\n" +
            ".tstable-container .tstable-body tbody tr:hover .action-tr {\n" +
            "  z-index: 7;\n" +
            "  position: relative;\n" +
            "  opacity: 1;\n" +
            "}\n" +
            ".tstable-container .tstable-body tbody tr:hover .action-bgimg {\n" +
            "  display: block;\n" +
            "}\n" +
            ".tstable-container .tstable-body tbody tr:hover .btn-hideaction {\n" +
            "  display: block;\n" +
            "}\n" +
            ".tstable-container .tstable-body tbody tr:hover .action-div {\n" +
            "  display: block;\n" +
            "}\n" +
            ".tstable-container .tstable-body .action-tr {\n" +
            "  opacity: 0;\n" +
            "}\n" +
            ".tstable-container .tstable-body .action-tr .action-bgimg {\n" +
            "  display: none;\n" +
            "  position: absolute;\n" +
            "  top: 0;\n" +
            "  bottom: 0;\n" +
            "  right: -8px;\n" +
            "  filter: blur(4px);\n" +
            "}\n" +
            ".tstable-container .tstable-body .action-tr .action-bgimg:before {\n" +
            "  content: '';\n" +
            "  position: absolute;\n" +
            "  top: 0;\n" +
            "  left: 0;\n" +
            "  bottom: 0;\n" +
            "  right: 0;\n" +
            "}\n" +
            ".tstable-container .tstable-body .action-tr .btn-hideaction {\n" +
            "  position: absolute;\n" +
            "  top: 0;\n" +
            "  height: 100%;\n" +
            "  width: 24px;\n" +
            "  left: 0;\n" +
            "  display: none;\n" +
            "  cursor: pointer;\n" +
            "  overflow: hidden;\n" +
            "}\n" +
            ".tstable-container .tstable-body .action-tr .btn-hideaction:hover .btn-hideicon {\n" +
            "  opacity: 1;\n" +
            "}\n" +
            ".tstable-container .tstable-body .action-tr .btn-hideaction .btn-hideicon {\n" +
            "  position: absolute;\n" +
            "  top: 50%;\n" +
            "  right: 0;\n" +
            "  height: 18px;\n" +
            "  line-height: 18px;\n" +
            "  margin-top: -9px;\n" +
            "  width: 24px;\n" +
            "  text-align: center;\n" +
            "  font-size: 16px;\n" +
            "  transition: all 0.3s;\n" +
            "  opacity: 0.4;\n" +
            "}\n" +
            ".tstable-container .tstable-body .action-tr .btn-hideaction .btn-hideicon:before {\n" +
            "  margin-right: 0;\n" +
            "}\n" +
            ".tstable-container .tstable-body .action-tr.hideAction .tstable-action-ul {\n" +
            "  opacity: 0 !important;\n" +
            "  width: 0;\n" +
            "  overflow: hidden;\n" +
            "}\n" +
            ".tstable-container .tstable-body .action-tr.hideAction .btn-hideaction .btn-hideicon {\n" +
            "  transform: rotate(180deg);\n" +
            "}\n" +
            ".tstable-container .tstable-body .action-tr.hideAction .action-bgimg {\n" +
            "  width: 0 !important;\n" +
            "}\n" +
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
            ".tstable-container .tstable-body .tstable-action {\n" +
            "  width: 0;\n" +
            "  position: absolute;\n" +
            "  top: 0;\n" +
            "  bottom: 0;\n" +
            "  float: right;\n" +
            "}\n" +
            ".tstable-container .tstable-body .tstable-action .tstable-action-ul {\n" +
            "  position: absolute;\n" +
            "  top: 50%;\n" +
            "  list-style: none;\n" +
            "  margin-top: -18px;\n" +
            "  display: block;\n" +
            "  opacity: 0;\n" +
            "  right: 0px;\n" +
            "}\n" +
            ".tstable-container .tstable-body .tstable-action .tstable-action-ul li {\n" +
            "  list-style: none;\n" +
            "  padding: 8px 16px;\n" +
            "  display: inline-block;\n" +
            "  cursor: pointer;\n" +
            "  position: relative;\n" +
            "}\n" +
            ".tstable-container .tstable-body .tstable-action .tstable-action-ul li.disable {\n" +
            "  cursor: not-allowed;\n" +
            "}\n" +
            ".tstable-container .tstable-body .tstable-action .tstable-action-ul li:before {\n" +
            "  margin-right: 4px;\n" +
            "}\n" +
            ".tstable-container .tstable-body .tstable-action .tstable-action-ul li:not(:last-of-type):after {\n" +
            "  content: '|';\n" +
            "  position: absolute;\n" +
            "  top: 9px;\n" +
            "  right: 0;\n" +
            "  opacity: 0.4;\n" +
            "}\n" +
            ".tstable-container .tstable-body .tstable-action .tstable-action-ul li .icon:before {\n" +
            "  padding-right: 8px;\n" +
            "}\n" +
            ".tstable-page {\n" +
            "  text-align: right;\n" +
            "  padding-top: 10px;\n" +
            "  background-color: transparent !important;\n" +
            "}\n" +
            ".sort-container {\n" +
            "  position: relative;\n" +
            "  padding-top: 30px;\n" +
            "}\n" +
            ".sort-container .sort-thead {\n" +
            "  position: absolute;\n" +
            "  top: 0;\n" +
            "  left: 0;\n" +
            "  width: 100%;\n" +
            "}\n" +
            ".sort-container .sort-item {\n" +
            "  position: relative;\n" +
            "}\n" +
            ".sort-container .sort-item,\n" +
            ".sort-container .sort-thead {\n" +
            "  padding-left: 32px;\n" +
            "  padding-right: 34px;\n" +
            "  line-height: 30px;\n" +
            "  height: 30px;\n" +
            "}\n" +
            ".sort-container .sort-item .sort-handle,\n" +
            ".sort-container .sort-thead .sort-handle {\n" +
            "  position: absolute;\n" +
            "  left: 0;\n" +
            "  top: 0;\n" +
            "  width: 32px;\n" +
            "  text-align: center;\n" +
            "}\n" +
            ".sort-container .sort-item .sort-show,\n" +
            ".sort-container .sort-thead .sort-show {\n" +
            "  position: absolute;\n" +
            "  right: 0;\n" +
            "  top: 0;\n" +
            "  width: 34px;\n" +
            "  text-align: center;\n" +
            "}\n" +
            ".sort-container .sort-item.disabled {\n" +
            "  display: none;\n" +
            "}\n" +
            ".sort-container .sort-item .sort-handle {\n" +
            "  cursor: ns-resize;\n" +
            "}\n" +
            ".sort-container .sort-group {\n" +
            "  overflow: auto;\n" +
            "  max-height: 300px;\n" +
            "}\n" +
            ".sort-drop .ivu-poptip-body {\n" +
            "  padding: 4px;\n" +
            "}\n" +
            ".tstable-page .ivu-select-selection {\n" +
            "  border: none !important;\n" +
            "}\n" +
            ".tstable-action .ivu-table-cell {\n" +
            "  overflow: visible;\n" +
            "  position: relative;\n" +
            "  padding: 0;\n" +
            "}\n" +
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
            ".userselect-container .ivu-select-dropdown {\n" +
            "  max-height: 200px;\n" +
            "  overflow: auto;\n" +
            "}\n" +
            ".userselect-container .ivu-select-dropdown.ivu-select-dropdown-transfer {\n" +
            "  max-height: auto;\n" +
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
            "html.theme-dark .ck.ck-reset_all,\n" +
            "html.theme-dark .ck.ck-reset_all * {\n" +
            "  color: #E0E1E2;\n" +
            "}\n" +
            "html.theme-dark .ck.ck-button:not(.ck-disabled):hover,\n" +
            "html.theme-dark a.ck.ck-button:not(.ck-disabled):hover {\n" +
            "  background-color: #252833;\n" +
            "}\n" +
            "html.theme-dark .ck.ck-button.ck-on,\n" +
            "html.theme-dark a.ck.ck-button.ck-on {\n" +
            "  background-color: #252833;\n" +
            "}\n" +
            "html.theme-dark .ck.ck-list {\n" +
            "  background-color: #252833;\n" +
            "}\n" +
            "html.theme-dark .ck.ck-list__item .ck-button:hover:not(.ck-disabled) {\n" +
            "  background-color: #5B5D66;\n" +
            "}\n" +
            "html.theme-dark .ck.ck-list__item .ck-button.ck-on {\n" +
            "  background: #5B5D66;\n" +
            "  color: #00bcd4;\n" +
            "}\n" +
            "html.theme-dark .ck.ck-dropdown__panel {\n" +
            "  background: #252833;\n" +
            "  border-color: #363842;\n" +
            "}\n" +
            "html.theme-dark .ck-content .table table td,\n" +
            "html.theme-dark .ck-content .table table th,\n" +
            "html.theme-dark .ck-content .table table {\n" +
            "  border-color: #363842 !important;\n" +
            "  border: 1px solid;\n" +
            "}\n" +
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
            "}\n";

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

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
            sb.append("<style>" + style + "</style>");
            sb.append("</head>\n");
            sb.append("<body>\n");
            sb.append("<span></span>");//暂时解决首行乱码
            for(KnowledgeDocumentLineVo line : knowledgeDocumentVo.getLineList()){
                if(KnowledgeDocumentLineHandler.IMG.getValue().equals(line.getHandler())){
                    String url = line.getConfig().getString("url");
                    String value = line.getConfig().getString("value");
                    if(StringUtils.isNotEmpty(url)){
                        String id = url.split("=")[1];
                        FileVo fileVo = fileMapper.getFileById(Long.valueOf(id));
                        if(fileVo != null){
                            InputStream in = FileUtil.getData(fileVo.getPath());
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            IOUtils.copyLarge(in,out);
                            sb.append("<div><img src=\"data:image/png;base64," + Base64.encodeBase64String(out.toByteArray()) + "\">");
                            if(StringUtils.isNotBlank(value)){
                                sb.append("<p>备注：" + value + "</p>");
                            }
                            sb.append("</div>");
                        }
                    }

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
