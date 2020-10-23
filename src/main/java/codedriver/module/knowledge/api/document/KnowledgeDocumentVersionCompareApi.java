package codedriver.module.knowledge.api.document;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dao.mapper.TagMapper;
import codedriver.framework.dto.TagVo;
import codedriver.framework.file.dao.mapper.FileMapper;
import codedriver.framework.file.dto.FileVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentFileVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentLineVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentTagVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotFoundException;
import codedriver.module.knowledge.exception.KnowledgeDocumentVersionNotFoundException;
import codedriver.module.knowledge.lcs.LCSUtil;
import codedriver.module.knowledge.lcs.Node;
import codedriver.module.knowledge.lcs.SegmentPair;
import codedriver.module.knowledge.lcs.SegmentRange;
//@Service
//@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentVersionCompareApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Autowired
    private FileMapper fileMapper;
    @Autowired
    private TagMapper tagMapper;
    
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
        KnowledgeDocumentVo newDocumentVo = getKnowledgeDocumentDetailByKnowledgeDocumentVersionId(newVersionId);
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
            KnowledgeDocumentVo oldDocumentVo = getKnowledgeDocumentDetailByKnowledgeDocumentVersionId(oldVersionId);       
            resultObj.put("oldDocumentVo", oldDocumentVo);
            compareTitle(oldDocumentVo, newDocumentVo);
            compareLineList(oldDocumentVo, newDocumentVo);           
        }       
        return resultObj;
    }
    
    private void compareTitle(KnowledgeDocumentVo oldDocumentVo, KnowledgeDocumentVo newDocumentVo) {
        String oldTitle = oldDocumentVo.getTitle();
        String newTitle = newDocumentVo.getTitle();
        List<SegmentRange> oldSegmentRangeList = new ArrayList<>();
        List<SegmentRange> newSegmentRangeList = new ArrayList<>();
        List<Character> oldCharList = new ArrayList<>();
        for(char c : oldTitle.toCharArray()) {
            oldCharList.add(c);
        }
        List<Character> newCharList = new ArrayList<>();
        for(char c : newTitle.toCharArray()) {
            newCharList.add(c);
        }
        Node node = LCSUtil.LCSCompare(oldCharList, newCharList, (c1, c2) -> c1.equals(c2));
        for(SegmentPair segmentpair : node.getSegmentPairList()) {
            oldSegmentRangeList.add(segmentpair.getOldSegmentRange());
            newSegmentRangeList.add(segmentpair.getNewSegmentRange());
        }
        oldDocumentVo.setTitle(LCSUtil.wrapChangePlace(oldTitle, oldSegmentRangeList, "<span class='delete'>", "</span>"));
        newDocumentVo.setTitle(LCSUtil.wrapChangePlace(newTitle, newSegmentRangeList, "<span class='insert'>", "</span>"));
    }
    
    private void compareLineList(KnowledgeDocumentVo oldDocumentVo, KnowledgeDocumentVo newDocumentVo) {
        List<KnowledgeDocumentLineVo> oldLineList = oldDocumentVo.getLineList();
        List<KnowledgeDocumentLineVo> newLineList = newDocumentVo.getLineList();
        List<KnowledgeDocumentLineVo> oldResultList = new ArrayList<>();
        List<KnowledgeDocumentLineVo> newResultList = new ArrayList<>();
        Node node = LCSUtil.LCSCompare(oldLineList, newLineList, (e1, e2) -> e1.getContent().equals(e2.getContent()));
        for(SegmentPair segmentPair : node.getSegmentPairList()) {
            regroupLineList(oldLineList, newLineList, oldResultList, newResultList, segmentPair);
        }
        oldDocumentVo.setLineList(oldResultList);
        newDocumentVo.setLineList(newResultList);
    }
    private KnowledgeDocumentVo cloneKnowledgeDocumentDetail(KnowledgeDocumentVo source) {
        KnowledgeDocumentVo cloneVo = new KnowledgeDocumentVo();
        cloneVo.setId(source.getId());
        cloneVo.setKnowledgeDocumentVersionId(source.getKnowledgeDocumentVersionId());
        cloneVo.setVersion(source.getVersion());
        cloneVo.setKnowledgeDocumentTypeUuid(source.getKnowledgeDocumentTypeUuid());
        cloneVo.setKnowledgeCircleId(source.getKnowledgeCircleId());
        cloneVo.setTitle(source.getTitle());
        cloneVo.getFileIdList().addAll(source.getFileIdList());
        cloneVo.getTagIdList().addAll(source.getTagIdList());
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
        for(FileVo file : cloneVo.getFileList()) {
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
        for(TagVo tag : cloneVo.getTagList()) {
            TagVo tagVo = new TagVo();
            tagVo.setId(tag.getId());
            tagVo.setName(tag.getName());
            cloneVo.getTagList().add(tagVo);
        }
        return cloneVo;
    }

    private KnowledgeDocumentVo getKnowledgeDocumentDetailByKnowledgeDocumentVersionId(Long knowledgeDocumentVersionId) {
        KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
        if(knowledgeDocumentVersionVo == null) {
            throw new KnowledgeDocumentVersionNotFoundException(knowledgeDocumentVersionId);
        }
        KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        if(knowledgeDocumentVo == null) {
            throw new KnowledgeDocumentNotFoundException(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        }else {
            knowledgeDocumentVo.setKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
        }
        knowledgeDocumentVo.setTitle(knowledgeDocumentVersionVo.getTitle());
        List<KnowledgeDocumentLineVo> lineList = knowledgeDocumentMapper.getKnowledgeDocumentLineListByKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
        knowledgeDocumentVo.setLineList(lineList);
        List<Long> fileIdList = knowledgeDocumentMapper.getKnowledgeDocumentFileIdListByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentFileVo(knowledgeDocumentVo.getId(), knowledgeDocumentVersionId));
        if(CollectionUtils.isNotEmpty(fileIdList)) {
            List<FileVo> fileList = fileMapper.getFileListByIdList(fileIdList);
            knowledgeDocumentVo.setFileIdList(fileIdList);
            knowledgeDocumentVo.setFileList(fileList);
        }
        List<Long> tagIdList = knowledgeDocumentMapper.getKnowledgeDocumentTagIdListByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentTagVo(knowledgeDocumentVo.getId(), knowledgeDocumentVersionId));
        if(CollectionUtils.isNotEmpty(tagIdList)) {
            List<TagVo> tagList = tagMapper.getTagListByIdList(tagIdList);
            knowledgeDocumentVo.setTagIdList(tagIdList);
            knowledgeDocumentVo.setTagList(tagList);
        }
        knowledgeDocumentVo.setIsEditable(0);
        knowledgeDocumentVo.setIsDeletable(0);
        knowledgeDocumentVo.setIsReviewable(0);
        
        int isReviewable = knowledgeDocumentMapper.checkUserIsApprover(UserContext.get().getUserUuid(true), knowledgeDocumentVo.getKnowledgeCircleId());
        if(KnowledgeDocumentVersionStatus.DRAFT.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            if(UserContext.get().getUserUuid(true).equals(knowledgeDocumentVersionVo.getLcu())) {
                knowledgeDocumentVo.setIsEditable(1);
                knowledgeDocumentVo.setIsDeletable(1);
            }
            knowledgeDocumentVo.setIsReviewable(isReviewable);
        }else if(KnowledgeDocumentVersionStatus.SUBMITED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            knowledgeDocumentVo.setIsReviewable(isReviewable);
        }else if(KnowledgeDocumentVersionStatus.PASSED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            knowledgeDocumentVo.setIsEditable(1);
        }else if(KnowledgeDocumentVersionStatus.REJECTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            if(Objects.equals(knowledgeDocumentVo.getVersion(), knowledgeDocumentVersionVo.getVersion())) {
                knowledgeDocumentVo.setIsEditable(1);
                knowledgeDocumentVo.setIsDeletable(1);
                knowledgeDocumentVo.setIsReviewable(isReviewable);
            }
        }
        return knowledgeDocumentVo;
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
          oldResultList.addAll(oldSubList);
          newResultList.addAll(newSubList);
      }else {
          if(CollectionUtils.isEmpty(newSubList)) {
              for(KnowledgeDocumentLineVo lineVo : oldSubList) {
                  lineVo.setChangeType("delete");
                  oldResultList.add(lineVo);
                  KnowledgeDocumentLineVo fillBlankLine = new KnowledgeDocumentLineVo();
                  fillBlankLine.setChangeType("fillblank");
                  fillBlankLine.setHandler(lineVo.getHandler());
                  newResultList.add(fillBlankLine);
              }
          }else if(CollectionUtils.isEmpty(oldSubList)) {
              for(KnowledgeDocumentLineVo lineVo :newSubList) {
                  KnowledgeDocumentLineVo fillBlankLine = new KnowledgeDocumentLineVo();
                  fillBlankLine.setChangeType("fillblank");
                  fillBlankLine.setHandler(lineVo.getHandler());
                  oldResultList.add(fillBlankLine);
                  lineVo.setChangeType("insert");
                  newResultList.add(lineVo);
              }
          }else if(oldSubList.size() == 1 && newSubList.size() == 1) {
              KnowledgeDocumentLineVo oldLine = oldSubList.get(0);
              KnowledgeDocumentLineVo newLine = newSubList.get(0);
              oldLine.setChangeType("delete");
              newLine.setChangeType("insert");
              if(StringUtils.length(oldLine.getContent()) > 0 && StringUtils.length(newLine.getContent()) > 0) {
                  List<SegmentRange> oldSegmentRangeList = new ArrayList<>();
                  List<SegmentRange> newSegmentRangeList = new ArrayList<>();
                  List<Character> oldCharList = new ArrayList<>();
                  for(char c : oldLine.getContent().toCharArray()) {
                      oldCharList.add(c);
                  }
                  List<Character> newCharList = new ArrayList<>();
                  for(char c : newLine.getContent().toCharArray()) {
                      newCharList.add(c);
                  }
                  Node node = LCSUtil.LCSCompare(oldCharList, newCharList, (c1, c2) -> c1.equals(c2));
                  for(SegmentPair segmentpair : node.getSegmentPairList()) {
                      oldSegmentRangeList.add(segmentpair.getOldSegmentRange());
                      newSegmentRangeList.add(segmentpair.getNewSegmentRange());
                  }
                  oldLine.setContent(LCSUtil.wrapChangePlace(oldLine.getContent(), oldSegmentRangeList, "<span class='delete'>", "</span>"));
                  oldResultList.add(oldLine);
                  newLine.setContent(LCSUtil.wrapChangePlace(newLine.getContent(), newSegmentRangeList, "<span class='insert'>", "</span>"));
                  newResultList.add(newLine);
              }else {
                  oldResultList.add(oldLine);
                  newResultList.add(newLine);
              }
          }else {
              List<SegmentPair> segmentPairList = differenceBestMatch(oldSubList, newSubList);
              for(SegmentPair segmentpair : segmentPairList) {
                  regroupLineList(oldSubList, newSubList, oldResultList, newResultList, segmentpair);
              }
          }
      }
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
                KnowledgeDocumentLineVo oldStr = oldList.get(i);
                KnowledgeDocumentLineVo newStr = newList.get(j);
                int oldLineContentLength = StringUtils.length(oldStr.getContent());
                int newLineContentLength = StringUtils.length(newStr.getContent());
                if(oldLineContentLength == 0 || newLineContentLength == 0) {
                    currentNode.setTotalMatchLength(0);
                }else {
                    List<Character> oldCharList = new ArrayList<>();
                    for(char c : oldStr.getContent().toCharArray()) {
                        oldCharList.add(c);
                    }
                    List<Character> newCharList = new ArrayList<>();
                    for(char c : newStr.getContent().toCharArray()) {
                        newCharList.add(c);
                    }
                    Node node = LCSUtil.LCSCompare(oldCharList, newCharList, (c1, c2) -> c1.equals(c2));
                    int maxLength = Math.max(oldLineContentLength, newLineContentLength);
                    int matchPercentage = (node.getTotalMatchLength() * 1000) / maxLength;
                    currentNode.setTotalMatchLength(matchPercentage);
                }
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
