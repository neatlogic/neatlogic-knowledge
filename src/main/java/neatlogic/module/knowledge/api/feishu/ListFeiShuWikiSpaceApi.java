package neatlogic.module.knowledge.api.feishu;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.knowledge.dto.feishu.FeiShuAppCredentialsVo;
import neatlogic.framework.knowledge.dto.feishu.FeiShuSpaceVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_FEISHU_SYNC_MODIFY;
import neatlogic.module.knowledge.service.KnowledgeFeiShuService;
import neatlogic.module.knowledge.utils.FeiShuOpenApiUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = KNOWLEDGE_FEISHU_SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListFeiShuWikiSpaceApi extends PrivateApiComponentBase {
    @Resource
    private KnowledgeFeiShuService knowledgeFeiShuService;
    @Override
    public String getToken() { return "knowledge/feishu/wiki/space/list"; }
    @Override
    public String getName() { return "nmkaf.listfeishuwikispaceapi.getname"; }
    @Input({})
    @Output({
            @Param(name = "tbodyList", explode = FeiShuSpaceVo[].class, desc = "common.tbodylist")
    })
    @Description(desc = "nmkaf.listfeishuwikispaceapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        JSONArray wikiSpaceList = new JSONArray();
        FeiShuAppCredentialsVo feiShuAppCredentials = knowledgeFeiShuService.getFeiShuAppCredentials();
        if (feiShuAppCredentials != null) {
            List<FeiShuSpaceVo> feiShuSpaceList = knowledgeFeiShuService.getFeiShuSpaceList(feiShuAppCredentials);
            for (FeiShuSpaceVo feiShuSpaceVo : feiShuSpaceList) {
                wikiSpaceList.add(new JSONObject().fluentPut("spaceId", feiShuSpaceVo.getSpaceId().toString()).fluentPut("name", feiShuSpaceVo.getName()).fluentPut("description", feiShuSpaceVo.getDescription()));
            }
        }
        return TableResultUtil.getResult(wikiSpaceList);
    }
}
