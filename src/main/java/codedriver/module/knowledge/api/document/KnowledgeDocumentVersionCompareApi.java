package codedriver.module.knowledge.api.document;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.file.dto.FileVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentLineHandler;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentLineVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.lcs.LCSUtil;
import codedriver.module.knowledge.lcs.Node;
import codedriver.module.knowledge.lcs.SegmentPair;
import codedriver.module.knowledge.lcs.SegmentRange;
import codedriver.module.knowledge.service.KnowledgeDocumentService;
@Service
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
        if(oldVersionId == null) {
            KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(newDocumentVo.getId());
            oldVersionId = knowledgeDocumentVo.getKnowledgeDocumentVersionId();
        }
        if(Objects.equals(oldVersionId, newVersionId)) {
            KnowledgeDocumentVo oldDocumentVo = cloneKnowledgeDocumentDetail(newDocumentVo);
            resultObj.put("oldDocumentVo", oldDocumentVo);
        }else {
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
     * 
    * @Time:2020年10月28日
    * @Description: 对比文档每行数据
    * @param oldDocumentVo
    * @param newDocumentVo 
    * @return void
     */
    private void compareLineList(KnowledgeDocumentVo oldDocumentVo, KnowledgeDocumentVo newDocumentVo) {
        List<KnowledgeDocumentLineVo> oldLineList = oldDocumentVo.getLineList();
        List<KnowledgeDocumentLineVo> newLineList = newDocumentVo.getLineList();
        List<KnowledgeDocumentLineVo> oldResultList = new ArrayList<>();
        List<KnowledgeDocumentLineVo> newResultList = new ArrayList<>();
        Node node = LCSUtil.LCSCompare(oldLineList, newLineList, (e1, e2) -> {
            if(e1.getHandler().equals(e2.getHandler())) {
                return Objects.equals(KnowledgeDocumentLineHandler.getMainBody(e1), KnowledgeDocumentLineHandler.getMainBody(e2));
            }
            return false;
        });
        for(SegmentPair segmentPair : node.getSegmentPairList()) {
            regroupLineList(oldLineList, newLineList, oldResultList, newResultList, segmentPair);
        }
        oldDocumentVo.setLineList(oldResultList);
        newDocumentVo.setLineList(newResultList);
    }
    /**
     * 
    * @Time:2020年10月28日
    * @Description: 复制文档详细信息 
    * @param source
    * @return KnowledgeDocumentVo
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
        for(KnowledgeDocumentLineVo line : source.getLineList()) {
            KnowledgeDocumentLineVo lineVo = new KnowledgeDocumentLineVo();
            lineVo.setUuid(line.getUuid());
            lineVo.setHandler(line.getHandler());
            lineVo.setChangeType(line.getChangeType());
            lineVo.setLineNumber(line.getLineNumber());
            lineVo.setConfig(JSON.toJSONString(line.getConfig()));
            lineVo.setContent(line.getContent());
            cloneVo.getLineList().add(lineVo);
        }
        for(FileVo file : source.getFileList()) {
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
     * 
    * @Time:2020年10月22日
    * @Description: 根据LCS算法比较结果，进行新旧数据的重组，体现两份数据的差异处 
    * @param oldDataList 旧数据列表
    * @param newDataList 新数据列表
    * @param oldResultList 重组后旧数据列表
    * @param newResultList 重组后新数据列表
    * @param  segmentPair 
    * @return void
     */
    private void regroupLineList(List<KnowledgeDocumentLineVo> oldDataList, List<KnowledgeDocumentLineVo> newDataList, List<KnowledgeDocumentLineVo> oldResultList, List<KnowledgeDocumentLineVo> newResultList, SegmentPair segmentPair) {
      SegmentRange oldSegmentRange = segmentPair.getOldSegmentRange();
      SegmentRange newSegmentRange = segmentPair.getNewSegmentRange();
      List<KnowledgeDocumentLineVo> oldSubList = oldDataList.subList(oldSegmentRange.getBeginIndex(), oldSegmentRange.getEndIndex());
      List<KnowledgeDocumentLineVo> newSubList = newDataList.subList(newSegmentRange.getBeginIndex(), newSegmentRange.getEndIndex());
      if(segmentPair.isMatch()) {
          /** 分段对匹配时，行数据不能做标记，直接添加到重组后的数据列表中 **/
          oldResultList.addAll(oldSubList);
          newResultList.addAll(newSubList);
      }else {
          /** 分段对不匹配时，分成下列四种情况 **/
          if(CollectionUtils.isEmpty(newSubList)) {
              /** 删除行 **/
              for(KnowledgeDocumentLineVo lineVo : oldSubList) {
                  lineVo.setChangeType("delete");
                  oldResultList.add(lineVo);
                  newResultList.add(createFillBlankLine(lineVo));
              }
          }else if(CollectionUtils.isEmpty(oldSubList)) {
              /** 插入行 **/
              for(KnowledgeDocumentLineVo lineVo :newSubList) {
                  oldResultList.add(createFillBlankLine(lineVo));
                  lineVo.setChangeType("insert");
                  newResultList.add(lineVo);
              }
          }else if(oldSubList.size() == 1 && newSubList.size() == 1) {
              /** 修改一行 **/
              KnowledgeDocumentLineVo oldLine = oldSubList.get(0);
              KnowledgeDocumentLineVo newLine = newSubList.get(0);
              if(oldLine.getHandler().equals(newLine.getHandler())) {
                  /** 行组件相同，才是修改行数据 **/
                  oldLine.setChangeType("update");
                  newLine.setChangeType("update");
                  String oldMainBody = KnowledgeDocumentLineHandler.getMainBody(oldLine);
                  String newMainBody = KnowledgeDocumentLineHandler.getMainBody(newLine);
                  if(KnowledgeDocumentLineHandler.getMainBodySet(oldLine.getHandler()) != null) {
                      if(StringUtils.length(oldMainBody) == 0) {
                          KnowledgeDocumentLineHandler.setMainBody(newLine, "<span class='insert'>" + newMainBody + "</span>");
                      }else if(StringUtils.length(newMainBody) == 0) {
                          KnowledgeDocumentLineHandler.setMainBody(oldLine, "<span class='delete'>" + oldMainBody + "</span>");
                      }else {
                          List<SegmentRange> oldSegmentRangeList = new ArrayList<>();
                          List<SegmentRange> newSegmentRangeList = new ArrayList<>();
                          Node node = LCSUtil.LCSCompare(oldMainBody, newMainBody);
                          for(SegmentPair segmentpair : node.getSegmentPairList()) {
                              //System.out.println(segmentpair);
                              oldSegmentRangeList.add(segmentpair.getOldSegmentRange());
                              newSegmentRangeList.add(segmentpair.getNewSegmentRange());
                          }
                          KnowledgeDocumentLineHandler.setMainBody(oldLine, LCSUtil.wrapChangePlace(oldMainBody, oldSegmentRangeList, "<span class='delete'>", "</span>"));
                          KnowledgeDocumentLineHandler.setMainBody(newLine, LCSUtil.wrapChangePlace(newMainBody, newSegmentRangeList, "<span class='insert'>", "</span>"));
                      }
                  }
                  oldResultList.add(oldLine);
                  newResultList.add(newLine);
              }else {
                  /** 行组件不相同，说明删除一行，再添加一行，根据行号大小判断加入重组后数据列表顺序 **/
                  if(oldLine.getLineNumber() <= newLine.getLineNumber()) {
                      oldLine.setChangeType("delete");
                      oldResultList.add(oldLine);
                      newResultList.add(createFillBlankLine(oldLine));
                      
                      oldResultList.add(createFillBlankLine(newLine));
                      newLine.setChangeType("insert");
                      newResultList.add(newLine);
                  }else {                     
                      oldResultList.add(createFillBlankLine(newLine));
                      newLine.setChangeType("insert");
                      newResultList.add(newLine);
                      
                      oldLine.setChangeType("delete");
                      oldResultList.add(oldLine);
                      newResultList.add(createFillBlankLine(oldLine));
                  }
              }
              
          }else {
              /** 修改多行，多行间需要做最优匹配 **/
              List<SegmentPair> segmentPairList = differenceBestMatch(oldSubList, newSubList);
              for(SegmentPair segmentpair : segmentPairList) {
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
     * 
    * @Time:2020年10月22日
    * @Description: 不匹配段的最佳匹配结果 
    * @param oldList 旧数据列表
    * @param newList 新数据列表
    * @return List<SegmentPair>
     */
    private List<SegmentPair> differenceBestMatch(List<KnowledgeDocumentLineVo> oldList, List<KnowledgeDocumentLineVo> newList) {
        List<SegmentPair> segmentMappingList = new ArrayList<>();
        List<Node> resultList = new ArrayList<>();
        PriorityQueue<Node> priorityQueue = new PriorityQueue<>(oldList.size() * newList.size(), (e1, e2) -> Integer.compare(e2.getTotalMatchLength(), e1.getTotalMatchLength()));
        for(int i = 0; i < oldList.size(); i++) {
            for(int j = 0; j < newList.size(); j++) {
                Node currentNode = new Node(i, j);
                KnowledgeDocumentLineVo oldLine = oldList.get(i);
                KnowledgeDocumentLineVo newLine = newList.get(j);
                int matchPercentage = 0;
                if(oldLine.getHandler().equals(newLine.getHandler())) {
                        String oldMainBody = KnowledgeDocumentLineHandler.getMainBody(oldLine);
                        String newMainBody = KnowledgeDocumentLineHandler.getMainBody(newLine);
                        int oldLineContentLength = StringUtils.length(oldMainBody);
                        int newLineContentLength = StringUtils.length(newMainBody);
                        if(KnowledgeDocumentLineHandler.getMainBodySet(oldLine.getHandler()) != null && oldLineContentLength > 0 && newLineContentLength > 0) {
                            Node node = LCSUtil.LCSCompare(oldMainBody, newMainBody);
                            int maxLength = Math.max(oldLineContentLength, newLineContentLength);
                            matchPercentage = (node.getTotalMatchLength() * 1000) / maxLength;
                            currentNode.setTotalMatchLength(matchPercentage);
                        }
                }
                currentNode.setTotalMatchLength(matchPercentage);
                priorityQueue.add(currentNode);
            }
        }
        Node e = null;
        while((e = priorityQueue.poll()) != null) {
            boolean flag = true;
            for(Node n : resultList) {
                if(n.getTotalMatchLength() == 0) {
                    flag = false;
                    break;
                }
                if(e.getOldIndex() >= n.getOldIndex() && e.getNewIndex() <= n.getNewIndex()) {
                    flag = false;
                    break;
                }
                if(e.getOldIndex() <= n.getOldIndex() && e.getNewIndex() >= n.getNewIndex()) {
                    flag = false;
                    break;
                }
            }
            if(flag) {
                resultList.add(e);           
            }
        }
        resultList.sort((e1, e2) -> Integer.compare(e1.getOldIndex(), e2.getOldIndex()));
        int oldIndex = 0;
        int newIndex = 0;
        for(Node node : resultList) {
            if(node.getOldIndex() > oldIndex) {
                SegmentPair segmentMapping = new SegmentPair(oldIndex, 0, false);
                segmentMapping.setEndIndex(node.getOldIndex(), 0);
                segmentMappingList.add(segmentMapping);
            }
            if(node.getNewIndex() > newIndex) {
                SegmentPair segmentMapping = new SegmentPair(0, newIndex, false);
                segmentMapping.setEndIndex(0, node.getNewIndex());
                segmentMappingList.add(segmentMapping);
            }
            oldIndex = node.getOldIndex() + 1;
            newIndex = node.getNewIndex() + 1;
            SegmentPair segmentMapping = new SegmentPair(node.getOldIndex(), node.getNewIndex(), false);
            segmentMapping.setEndIndex(oldIndex, newIndex);
            segmentMappingList.add(segmentMapping);
        }
        if(oldList.size() > oldIndex) {
            SegmentPair segmentMapping = new SegmentPair(oldIndex, 0, false);
            segmentMapping.setEndIndex(oldList.size(), 0);
            segmentMappingList.add(segmentMapping);
        }
        if(newList.size() > newIndex) {
            SegmentPair segmentMapping = new SegmentPair(0, newIndex, false);
            segmentMapping.setEndIndex(0, newList.size());
            segmentMappingList.add(segmentMapping);
        }
        return segmentMappingList;
    }
}
