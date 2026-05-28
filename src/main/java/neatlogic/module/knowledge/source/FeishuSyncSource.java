package neatlogic.module.knowledge.source;

import neatlogic.framework.knowledge.dto.SyncSourceVo;
import neatlogic.framework.knowledge.source.ISyncSource;

import java.util.Collections;
import java.util.List;

public enum FeishuSyncSource implements ISyncSource {
    INSTANCE;

    public static final String SOURCE = "feishu";

    @Override
    public List<SyncSourceVo> getSyncSource() {
        SyncSourceVo vo = new SyncSourceVo();
        vo.setSource(SOURCE);
        vo.setSourceName("飞书云文档");
        return Collections.singletonList(vo);
    }
}
