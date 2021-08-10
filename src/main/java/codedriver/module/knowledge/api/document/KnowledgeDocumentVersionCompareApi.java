package codedriver.module.knowledge.api.document;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.file.dto.FileVo;
import codedriver.framework.knowledge.exception.KnowledgeDocumentLineHandlerNotFoundException;
import codedriver.framework.knowledge.linehandler.core.ILineHandler;
import codedriver.framework.knowledge.linehandler.core.LineHandlerFactory;
import codedriver.framework.lcs.*;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.auth.label.KNOWLEDGE_BASE;
import codedriver.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.framework.knowledge.dto.KnowledgeDocumentLineVo;
import codedriver.framework.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.service.KnowledgeDocumentService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentVersionCompareApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

    @Override
    public String getToken() {
        return "knowledge/document/version/compare";
    }

    @Override
    public String getName() {
        return "比较文档两个版本内容差异";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "newVersionId", type = ApiParamType.LONG, isRequired = true, desc = "新版本id"),
            @Param(name = "oldVersionId", type = ApiParamType.LONG, desc = "旧版本id"),
    })
    @Output({
            @Param(name = "newDocument", explode = KnowledgeDocumentVo.class, desc = "文档新版本内容"),
            @Param(name = "oldDocument", explode = KnowledgeDocumentVo.class, desc = "文档旧版本内容")
    })
    @Description(desc = "比较文档两个版本内容差异")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        Long newVersionId = jsonObj.getLong("newVersionId");
        KnowledgeDocumentVo newDocumentVo = knowledgeDocumentService.getKnowledgeDocumentDetailByKnowledgeDocumentVersionId(newVersionId);
        resultObj.put("newDocumentVo", newDocumentVo);
        Long oldVersionId = jsonObj.getLong("oldVersionId");
        if (oldVersionId == null) {
            KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(newDocumentVo.getId());
            oldVersionId = knowledgeDocumentVo.getKnowledgeDocumentVersionId();
        }
        if (Objects.equals(oldVersionId, newVersionId)) {
            KnowledgeDocumentVo oldDocumentVo = cloneKnowledgeDocumentDetail(newDocumentVo);
            resultObj.put("oldDocumentVo", oldDocumentVo);
        } else {
            KnowledgeDocumentVo oldDocumentVo = knowledgeDocumentService.getKnowledgeDocumentDetailByKnowledgeDocumentVersionId(oldVersionId);
            resultObj.put("oldDocumentVo", oldDocumentVo);
//            compareTitle(oldDocumentVo, newDocumentVo);
            compareLineList(oldDocumentVo, newDocumentVo);
        }
        return resultObj;
    }
    /**
     *
     * @Time:2020年10月28日
     * @Description: 对比文档标题
     * @param oldDocumentVo
     * @param newDocumentVo
     * @return void
     */
//    private void compareTitle(KnowledgeDocumentVo oldDocumentVo, KnowledgeDocumentVo newDocumentVo) {
//        String oldTitle = oldDocumentVo.getTitle();
//        String newTitle = newDocumentVo.getTitle();
//        List<SegmentRange> oldSegmentRangeList = new ArrayList<>();
//        List<SegmentRange> newSegmentRangeList = new ArrayList<>();
//        Node node = LCSUtil.LCSCompare(oldTitle, newTitle);
//        for(SegmentPair segmentpair : node.getSegmentPairList()) {
//            oldSegmentRangeList.add(segmentpair.getOldSegmentRange());
//            newSegmentRangeList.add(segmentpair.getNewSegmentRange());
//        }
//        oldDocumentVo.setTitle(LCSUtil.wrapChangePlace(oldTitle, oldSegmentRangeList, "<span class='delete'>", "</span>"));
//        newDocumentVo.setTitle(LCSUtil.wrapChangePlace(newTitle, newSegmentRangeList, "<span class='insert'>", "</span>"));
//    }

    /**
     * @param oldDocumentVo
     * @param newDocumentVo
     * @return void
     * @Time:2020年10月28日
     * @Description: 对比文档每行数据
     */
    private void compareLineList(KnowledgeDocumentVo oldDocumentVo, KnowledgeDocumentVo newDocumentVo) {
        List<KnowledgeDocumentLineVo> oldLineList = oldDocumentVo.getLineList();
        List<KnowledgeDocumentLineVo> newLineList = newDocumentVo.getLineList();
        List<KnowledgeDocumentLineVo> oldResultList = new ArrayList<>();
        List<KnowledgeDocumentLineVo> newResultList = new ArrayList<>();
        List<SegmentPair> segmentPairList = LCSUtil.LCSCompare(oldLineList, newLineList);
        for (SegmentPair segmentPair : segmentPairList) {
            regroupLineList(oldLineList, newLineList, oldResultList, newResultList, segmentPair);
        }
        oldDocumentVo.setLineList(oldResultList);
        newDocumentVo.setLineList(newResultList);
    }

    /**
     * @param source
     * @return KnowledgeDocumentVo
     * @Time:2020年10月28日
     * @Description: 复制文档详细信息
     */
    private KnowledgeDocumentVo cloneKnowledgeDocumentDetail(KnowledgeDocumentVo source) {
        KnowledgeDocumentVo cloneVo = new KnowledgeDocumentVo();
        cloneVo.setId(source.getId());
        cloneVo.setKnowledgeDocumentVersionId(source.getKnowledgeDocumentVersionId());
        cloneVo.setVersion(source.getVersion());
        cloneVo.setKnowledgeDocumentTypeUuid(source.getKnowledgeDocumentTypeUuid());
        cloneVo.setKnowledgeCircleId(source.getKnowledgeCircleId());
        cloneVo.setTitle(source.getTitle());
        cloneVo.getFileIdList().addAll(source.getFileIdList());
        cloneVo.setIsEditable(source.getIsEditable());
        cloneVo.setIsDeletable(source.getIsDeletable());
        cloneVo.setIsReviewable(source.getIsReviewable());
        for (KnowledgeDocumentLineVo line : source.getLineList()) {
            KnowledgeDocumentLineVo lineVo = new KnowledgeDocumentLineVo();
            lineVo.setUuid(line.getUuid());
            lineVo.setHandler(line.getHandler());
            lineVo.setChangeType(line.getChangeType());
            lineVo.setLineNumber(line.getLineNumber());
            lineVo.setConfig(JSON.toJSONString(line.getConfig()));
            lineVo.setContent(line.getContent());
            cloneVo.getLineList().add(lineVo);
        }
        for (FileVo file : source.getFileList()) {
            FileVo fileVo = new FileVo();
            fileVo.setId(file.getId());
            fileVo.setName(file.getName());
            fileVo.setSize(file.getSize());
            fileVo.setUserUuid(file.getUserUuid());
            fileVo.setUploadTime(file.getUploadTime());
            fileVo.setType(file.getType());
            fileVo.setPath(file.getPath());
            fileVo.setContentType(file.getContentType());
            cloneVo.getFileList().add(fileVo);
        }

        cloneVo.getTagList().addAll(source.getTagList());
        cloneVo.getPath().addAll(source.getPath());
        return cloneVo;
    }

    /**
     * @param oldDataList   旧数据列表
     * @param newDataList   新数据列表
     * @param oldResultList 重组后旧数据列表
     * @param newResultList 重组后新数据列表
     * @param segmentPair
     * @return void
     * @Time:2020年10月22日
     * @Description: 根据LCS算法比较结果，进行新旧数据的重组，体现两份数据的差异处
     */
    private void regroupLineList(List<KnowledgeDocumentLineVo> oldDataList, List<KnowledgeDocumentLineVo> newDataList, List<KnowledgeDocumentLineVo> oldResultList, List<KnowledgeDocumentLineVo> newResultList, SegmentPair segmentPair) {
        List<KnowledgeDocumentLineVo> oldSubList = oldDataList.subList(segmentPair.getOldBeginIndex(), segmentPair.getOldEndIndex());
        List<KnowledgeDocumentLineVo> newSubList = newDataList.subList(segmentPair.getNewBeginIndex(), segmentPair.getNewEndIndex());
        if (segmentPair.isMatch()) {
            /** 分段对匹配时，行数据不能做标记，直接添加到重组后的数据列表中 **/
            oldResultList.addAll(oldSubList);
            newResultList.addAll(newSubList);
        } else {
            /** 分段对不匹配时，分成下列四种情况 **/
            if (CollectionUtils.isEmpty(newSubList)) {
                /** 删除行 **/
                for (KnowledgeDocumentLineVo lineVo : oldSubList) {
                    lineVo.setChangeType("delete");
                    oldResultList.add(lineVo);
                    newResultList.add(createFillBlankLine(lineVo));
                }
            } else if (CollectionUtils.isEmpty(oldSubList)) {
                /** 插入行 **/
                for (KnowledgeDocumentLineVo lineVo : newSubList) {
                    oldResultList.add(createFillBlankLine(lineVo));
                    lineVo.setChangeType("insert");
                    newResultList.add(lineVo);
                }
            } else if (oldSubList.size() == 1 && newSubList.size() == 1) {
                /** 修改一行 **/
                KnowledgeDocumentLineVo oldLine = oldSubList.get(0);
                KnowledgeDocumentLineVo newLine = newSubList.get(0);
                if (oldLine.getHandler().equals(newLine.getHandler())) {
                    /** 行组件相同，才是修改行数据 **/
                    oldLine.setChangeType("update");
                    newLine.setChangeType("update");
                    String handler = oldLine.getHandler();
                    ILineHandler lineHandler = LineHandlerFactory.getHandler(handler);
                    if (lineHandler == null) {
                        throw new KnowledgeDocumentLineHandlerNotFoundException(handler);
                    }
                    String oldMainBody = lineHandler.getMainBody(oldLine);
                    String newMainBody = lineHandler.getMainBody(newLine);
                    if (lineHandler.needCompare()) {
                        if (StringUtils.length(oldMainBody) == 0) {
                            lineHandler.setMainBody(newLine, "<span class='insert'>" + newMainBody + "</span>");
                        } else if (StringUtils.length(newMainBody) == 0) {
                            lineHandler.setMainBody(oldLine, "<span class='delete'>" + oldMainBody + "</span>");
                        } else {
                            List<SegmentRange> oldSegmentRangeList = new ArrayList<>();
                            List<SegmentRange> newSegmentRangeList = new ArrayList<>();
                            List<SegmentPair> segmentPairList = LCSUtil.LCSCompare(oldMainBody, newMainBody);
                            for (SegmentPair segmentpair : segmentPairList) {
                                oldSegmentRangeList.add(new SegmentRange(segmentpair.getOldBeginIndex(), segmentpair.getOldEndIndex(), segmentpair.isMatch()));
                                newSegmentRangeList.add(new SegmentRange(segmentpair.getNewBeginIndex(), segmentpair.getNewEndIndex(), segmentpair.isMatch()));
                            }
                            lineHandler.setMainBody(oldLine, LCSUtil.wrapChangePlace(oldMainBody, oldSegmentRangeList, "<span class='delete'>", "</span>"));
                            lineHandler.setMainBody(newLine, LCSUtil.wrapChangePlace(newMainBody, newSegmentRangeList, "<span class='insert'>", "</span>"));
                        }
                    }
                    oldResultList.add(oldLine);
                    newResultList.add(newLine);
                } else {
                    /** 行组件不相同，说明删除一行，再添加一行，根据行号大小判断加入重组后数据列表顺序 **/
                    if (oldLine.getLineNumber() <= newLine.getLineNumber()) {
                        oldLine.setChangeType("delete");
                        oldResultList.add(oldLine);
                        newResultList.add(createFillBlankLine(oldLine));

                        oldResultList.add(createFillBlankLine(newLine));
                        newLine.setChangeType("insert");
                        newResultList.add(newLine);
                    } else {
                        oldResultList.add(createFillBlankLine(newLine));
                        newLine.setChangeType("insert");
                        newResultList.add(newLine);

                        oldLine.setChangeType("delete");
                        oldResultList.add(oldLine);
                        newResultList.add(createFillBlankLine(oldLine));
                    }
                }

            } else {
                /** 修改多行，多行间需要做最优匹配 **/
                List<SegmentPair> segmentPairList = differenceBestMatch(oldSubList, newSubList);
                for (SegmentPair segmentpair : segmentPairList) {
                    /** 递归 **/
                    regroupLineList(oldSubList, newSubList, oldResultList, newResultList, segmentpair);
                }
            }
        }
    }

    private KnowledgeDocumentLineVo createFillBlankLine(KnowledgeDocumentLineVo line) {
        KnowledgeDocumentLineVo fillBlankLine = new KnowledgeDocumentLineVo();
        fillBlankLine.setChangeType("fillblank");
        fillBlankLine.setHandler(line.getHandler());
        fillBlankLine.setConfig(line.getConfigStr());
        fillBlankLine.setContent(line.getContent());
        return fillBlankLine;
    }

    /**
     * @Description: 通过最短编辑距离算法，对不匹配段之间进行最佳匹配
     * @Author: linbq
     * @Date: 2021/3/5 17:32
     * @Params:[source, target]
     * @Returns:java.util.List<codedriver.module.knowledge.lcs.SegmentPair>
     **/
    private List<SegmentPair> differenceBestMatch(List<KnowledgeDocumentLineVo> source, List<KnowledgeDocumentLineVo> target) {
        int sourceCount = source.size();
        int targetCount = target.size();
        NodePool nodePool = new NodePool(sourceCount, targetCount);
        for (int i = sourceCount - 1; i >= 0; i--) {
            for (int j = targetCount - 1; j >= 0; j--) {
                Node currentNode = new Node(i, j);
                KnowledgeDocumentLineVo oldLine = source.get(i);
                KnowledgeDocumentLineVo newLine = target.get(j);
                String oldHandler = oldLine.getHandler();
                ILineHandler oldLineHandler = LineHandlerFactory.getHandler(oldHandler);
                if (oldLineHandler == null) {
                    throw new KnowledgeDocumentLineHandlerNotFoundException(oldHandler);
                }
                String newHandler = newLine.getHandler();
                ILineHandler newLineHandler = LineHandlerFactory.getHandler(newHandler);
                if (newLineHandler == null) {
                    throw new KnowledgeDocumentLineHandlerNotFoundException(newHandler);
                }
                String oldMainBody = oldLineHandler.getMainBody(oldLine);
                String newMainBody = newLineHandler.getMainBody(newLine);
                int oldLineContentLength = StringUtils.length(oldMainBody);
                int newLineContentLength = StringUtils.length(newMainBody);
                int minEditDistance = 0;
                if (oldLine.getHandler().equals(newLine.getHandler())) {
                    if (oldLineHandler.needCompare() && oldLineContentLength > 0 && newLineContentLength > 0) {
                        minEditDistance = LCSUtil.minEditDistance(oldMainBody, newMainBody);
                    } else {
                        minEditDistance = oldLineContentLength + newLineContentLength;
                    }
                } else {
                    minEditDistance = oldLineContentLength + newLineContentLength;
                }
                currentNode.setMinEditDistance(minEditDistance);
                int left = 0;
                int top = 0;
                int upperLeft = 0;
                Node upperLeftNode = nodePool.getOldNode(i + 1, j + 1);
                if (upperLeftNode != null) {
                    upperLeft = upperLeftNode.getTotalMatchLength();
                }
                Node leftNode = nodePool.getOldNode(i, j + 1);
                if (leftNode != null) {
                    left = leftNode.getTotalMatchLength();
                }
                Node topNode = nodePool.getOldNode(i + 1, j);
                if (topNode != null) {
                    top = topNode.getTotalMatchLength();
                }
                if (i + 1 == sourceCount && j + 1 == targetCount) {
                    currentNode.setTotalMatchLength(minEditDistance);
                } else if (i + 1 == sourceCount) {
                    currentNode.setTotalMatchLength(minEditDistance + left);
                    currentNode.setNext(leftNode);
                } else if (j + 1 == targetCount) {
                    currentNode.setTotalMatchLength(minEditDistance + top);
                    currentNode.setNext(topNode);
                } else {
                    if (upperLeft <= left) {
                        if (upperLeft <= top) {
                            currentNode.setTotalMatchLength(minEditDistance + upperLeft);
                            currentNode.setNext(upperLeftNode);
                        } else {
                            currentNode.setTotalMatchLength(minEditDistance + top);
                            currentNode.setNext(topNode);
                        }
                    } else if (top <= left) {
                        currentNode.setTotalMatchLength(minEditDistance + top);
                        currentNode.setNext(topNode);
                    } else {
                        currentNode.setTotalMatchLength(minEditDistance + left);
                        currentNode.setNext(leftNode);
                    }
                }

                nodePool.addNode(currentNode);
            }
        }
        List<Node> nodeList = new ArrayList<>();
        Node previous = null;
        Node node = nodePool.getOldNode(0, 0);
        while (node != null) {
            if (previous != null) {
                if (previous.getOldIndex() == node.getOldIndex() || previous.getNewIndex() == node.getNewIndex()) {
                    if (previous.getMinEditDistance() > node.getMinEditDistance()) {
                        previous = node;
                    }
                } else {
                    nodeList.add(previous);
                    previous = node;
                }
            } else {
                previous = node;
            }
            node = node.getNext();
        }
        if (previous != null) {
            nodeList.add(previous);
        }
        List<SegmentPair> segmentPairList = new ArrayList<>();
        int lastOldEndIndex = 0;
        int lastNewEndIndex = 0;
        for (Node n : nodeList) {
            if (n.getOldIndex() != lastOldEndIndex || n.getNewIndex() != lastNewEndIndex) {
                segmentPairList.add(new SegmentPair(lastOldEndIndex, n.getOldIndex(), lastNewEndIndex, n.getNewIndex(), false));
            }
            lastOldEndIndex = n.getOldIndex() + 1;
            lastNewEndIndex = n.getNewIndex() + 1;
            segmentPairList.add(new SegmentPair(n.getOldIndex(), lastOldEndIndex, n.getNewIndex(), lastNewEndIndex, false));
        }
        if (lastOldEndIndex != sourceCount || lastNewEndIndex != targetCount) {
            segmentPairList.add(new SegmentPair(lastOldEndIndex, sourceCount, lastNewEndIndex, targetCount, false));
        }
        return segmentPairList;
    }
}
