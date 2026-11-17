package neatlogic.module.knowledge.api.document;

import java.util.List;
import java.util.Map;
import neatlogic.framework.knowledge.generation.IKnowledgeGenerationSource;
import neatlogic.framework.knowledge.generation.KnowledgeGenerationSources;
import java.util.Objects;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.knowledge.crossover.IKnowledgeDocumentCrossoverService;
import neatlogic.framework.knowledge.source.ISyncSource;
import neatlogic.framework.knowledge.source.SyncSourceFactory;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_BASE;
import neatlogic.module.knowledge.service.KnowledgeDocumentService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeCircleMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import neatlogic.framework.knowledge.dto.KnowledgeCircleVo;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentTypeVo;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVersionVo;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVo;

import javax.annotation.Resource;

/** 查询可见的创建来源关联，并保留手动转换入口的权限和单文档响应。 */
@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentInvokeGetApi extends PrivateApiComponentBase {

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;
    @Resource
    private KnowledgeCircleMapper knowledgeCircleMapper;
    @Resource
    private KnowledgeDocumentTypeMapper knowledgeDocumentTypeMappper;

    @Override
    public String getToken() {
        return "knowledge/document/invoke/get";
    }

    @Override
    public String getName() {
        return "knowledgeorigin.list";
    }

    @Override
    public String getConfig() {
        return null;
    }

    /** 工单按归属汇总各来源，其他旧入口仅展示各自的直接关联。 */
    @Input({
        @Param(name = "invokeId", type = ApiParamType.LONG, isRequired = true, desc = "nmkad.knowledgedocumentdraftsaveapi.invokeid"),
        @Param(name = "source", type = ApiParamType.STRING, isRequired = true, desc = "common.source")
    })
    @Output({
        @Param(name = "isTransferKnowledge", type = ApiParamType.INTEGER, desc = "knowledgeorigin.cantransfer"),
        @Param(name = "versionList", type = ApiParamType.JSONARRAY, desc = "knowledgeorigin.list"),
        @Param(name = "knowledgeDocumentVersion", explode = KnowledgeDocumentVersionVo.class, desc = "knowledgeorigin.versioninfo")
    })
    @Description(desc = "knowledgeorigin.list")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        String source = jsonObj.getString("source");
        Long invokeId = jsonObj.getLong("invokeId");
        Map<String, List<Long>> origins;
        ISyncSource syncSource = SyncSourceFactory.getProvider(source);
        if (syncSource != null) {
            origins = syncSource.listOrigins(source, invokeId);
        } else {
            IKnowledgeGenerationSource provider = KnowledgeGenerationSources.get(source);
            if (provider != null) {
                provider.checkOriginView(invokeId);
            }
            origins = Map.of(source, List.of(invokeId));
        }
        IKnowledgeDocumentCrossoverService documents = CrossoverServiceFactory.getApi(IKnowledgeDocumentCrossoverService.class);
        List<JSONObject> versions = documents.listOrigins(origins);
        resultObj.put("versionList", versions);
        Long directId = knowledgeDocumentMapper.getDocumentIdByOrigin(source, invokeId);
        if (directId != null) {
            resultObj.put("isTransferKnowledge", 0);
            // 只有可见的直接关联才填充旧版单条响应，不能以最新版本替换已绑定版本。
            Long versionId = versions.stream().filter(row -> directId.equals(row.getLong("documentId"))
                    && source.equals(row.getString("source")) && invokeId.equals(row.getLong("invokeId")))
                    .map(row -> row.getLong("versionId")).filter(Objects::nonNull).findFirst().orElse(null);
            KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = versionId == null ? null : knowledgeDocumentMapper.getKnowledgeDocumentVersionById(versionId);
            if (knowledgeDocumentVersionVo != null) {
                KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(directId);
                KnowledgeCircleVo knowledgeCircleVo = knowledgeCircleMapper.getKnowledgeCircleById(knowledgeDocumentVo.getKnowledgeCircleId());
                if(knowledgeCircleVo != null) {
                    knowledgeDocumentVersionVo.getPath().add(knowledgeCircleVo.getName());
                }
                KnowledgeDocumentTypeVo knowledgeDocumentTypeVo = knowledgeDocumentTypeMappper.getTypeByUuid(knowledgeDocumentVo.getKnowledgeDocumentTypeUuid());
                if(knowledgeDocumentTypeVo != null) {
                    List<String> typeNameList = knowledgeDocumentTypeMappper.getAncestorsAndSelfNameByLftRht(knowledgeDocumentTypeVo.getLft(), knowledgeDocumentTypeVo.getRht(), knowledgeDocumentTypeVo.getKnowledgeCircleId());
                    if(CollectionUtils.isNotEmpty(typeNameList)) {
                        knowledgeDocumentVersionVo.getPath().addAll(typeNameList);
                    }
                }
                resultObj.put("knowledgeDocumentVersion", knowledgeDocumentVersionVo);               
            }
        }else {
            if(knowledgeDocumentService.isMember(null) == 0) {
                resultObj.put("isTransferKnowledge", 0);
            }else {
                resultObj.put("isTransferKnowledge", 1);
            }
        }
        return resultObj;
    }

}
