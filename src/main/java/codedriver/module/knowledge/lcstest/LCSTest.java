package codedriver.module.knowledge.lcstest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import codedriver.module.knowledge.lcs.Node;
import codedriver.module.knowledge.lcs.SegmentPair;
import codedriver.module.knowledge.lcs.SegmentRange;

public class LCSTest {
    private final static String BASE_PATH = "src/main/java/codedriver/module/knowledge/lcstest/";
    
    public static void main(String[] args) {
        List<String> oldDataList = readFileData(BASE_PATH + "oldData.txt");
        List<String> newDataList = readFileData(BASE_PATH + "newData.txt");
        List<String> oldResultList = new ArrayList<>();
        List<String> newResultList = new ArrayList<>();
        List<SegmentPair> segmentPairList = longestCommonSequence(oldDataList, newDataList).getSegmentPairList();
        for(SegmentPair segmentPair : segmentPairList) {
            test(oldDataList, newDataList, oldResultList, newResultList, segmentPair);
        }
        writeFileData(oldResultList, BASE_PATH + "oldResult.txt");
        writeFileData(newResultList, BASE_PATH + "newResult.txt");
    }
    
    private static List<String> readFileData(String filePath) {
        List<String> resultList = new ArrayList<>();
        try (
            FileInputStream fis = new FileInputStream(filePath); 
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            ) {
            String str = null;
            while((str = br.readLine()) != null) {
                resultList.add(str);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultList;
    }
    private static void writeFileData(List<String> list, String filePath) {
        try (
            FileOutputStream fos = new FileOutputStream(filePath); 
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            BufferedWriter bw = new BufferedWriter(osw);
            ) {
            for(String str : list) {
                bw.write(str);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
   
    private static void test(List<String> oldDataList, List<String> newDataList, List<String> oldResultList, List<String> newResultList, SegmentPair segmentMapping) {
//      System.out.println(segmentMapping);
      SegmentRange oldSegmentRange = segmentMapping.getOldSegmentRange();
      SegmentRange newSegmentRange = segmentMapping.getNewSegmentRange();
      List<String> oldSubList = oldDataList.subList(oldSegmentRange.getBeginIndex(), oldSegmentRange.getEndIndex());
      List<String> newSubList = newDataList.subList(newSegmentRange.getBeginIndex(), newSegmentRange.getEndIndex());
      if(segmentMapping.isMatch()) {
          oldResultList.addAll(oldSubList);
          newResultList.addAll(newSubList);
      }else {
          if(CollectionUtils.isEmpty(newSubList)) {
              for(String str : oldSubList) {
                  oldResultList.add("--" + str);
                  newResultList.add("==");
              }
          }else if(CollectionUtils.isEmpty(oldSubList)) {
              for(String str :newSubList) {
                  oldResultList.add("==");
                  newResultList.add("++" + str);
              }
          }else if(oldSubList.size() == 1 && newSubList.size() == 1) {
              String oldStr = oldSubList.get(0);
              String newStr = newSubList.get(0);
              if(oldStr.length() > 0 && newStr.length() > 0) {
                  List<SegmentRange> oldSegmentRangeList = new ArrayList<>();
                  List<SegmentRange> newSegmentRangeList = new ArrayList<>();
                  for(SegmentPair segmentPair : longestCommonSequence(oldStr, newStr).getSegmentPairList()) {
                      oldSegmentRangeList.add(segmentPair.getOldSegmentRange());
                      newSegmentRangeList.add(segmentPair.getNewSegmentRange());
                  }
                  oldResultList.add("--" + wrapChangePlace(oldStr, oldSegmentRangeList, "<->", "</->"));
                  newResultList.add("++" + wrapChangePlace(newStr, newSegmentRangeList, "<+>", "</+>"));
              }else {
                  oldResultList.add("--" + oldStr);
                  newResultList.add("++" + newStr);
              }
          }else {
              List<SegmentPair> segmentMappingList = longestCommonSequence2(oldSubList, newSubList);
              for(SegmentPair segmentMap : segmentMappingList) {
                  test(oldSubList, newSubList, oldResultList, newResultList, segmentMap);
              }
          }
      }
    }
    private static Node longestCommonSequence(String oldStr, String newStr) {
        char[] x = oldStr.toCharArray();
        char[] y = newStr.toCharArray();
        Node[][] lcs = new Node[x.length][y.length];
        
        for(int i = 0; i < x.length; i++) {
            for(int j = 0; j < y.length; j++) {
                Node currentNode = new Node(i, j);
                lcs[i][j] = currentNode;
                if(x[i] == y[j]) {
                    currentNode.setTotalMatchLength(1).setMatch(true);
                    Node upperLeftNode = null;
                    if(i > 0 && j > 0) {
                        upperLeftNode = lcs[i-1][j-1];
                    }
                    if(upperLeftNode != null) {
                        currentNode.setTotalMatchLength(upperLeftNode.getTotalMatchLength() + 1).setPrevious(upperLeftNode);
                    }
                }else {
                    int left = 0;
                    int top = 0;
                    Node leftNode = null;
                    if(j > 0) {
                        leftNode = lcs[i][j-1];
                    }
                    if(leftNode != null) {
                        left = leftNode.getTotalMatchLength();
                    }
                    Node topNode = null;
                    if(i > 0) {
                        topNode = lcs[i-1][j];
                    }
                    if(topNode != null) {
                        top = topNode.getTotalMatchLength();
                    }
                    if(top >= left) {
                        currentNode.setTotalMatchLength(top).setPrevious(topNode);
                    }else {
                        currentNode.setTotalMatchLength(left).setPrevious(leftNode);
                    }
                }
            }
        }
        
//        for(int i = 0; i < x.length; i++) {
//            for(int j = 0; j < y.length; j++) {
//                System.out.print(lcs[i][j]);
//                System.out.print("\t");
//            }
//            System.out.println();
//        }
//        System.out.println(oldStr);
//        System.out.println(newStr);
        return lcs[x.length-1][y.length-1];
    }
    
    private static Node longestCommonSequence(List<String> oldList, List<String> newList) {
        Node[][] lcs = new Node[oldList.size()][newList.size()];       
        for(int i = 0; i < oldList.size(); i++) {
            for(int j = 0; j < newList.size(); j++) {
                Node currentNode = new Node(i, j);
                lcs[i][j] = currentNode;
                if(oldList.get(i).equals(newList.get(j))) {
                    currentNode.setTotalMatchLength(1).setMatch(true);
                    Node upperLeftNode = null;
                    if(i > 0 && j > 0) {
                        upperLeftNode = lcs[i-1][j-1];
                    }
                    if(upperLeftNode != null) {
                        currentNode.setTotalMatchLength(upperLeftNode.getTotalMatchLength() + 1).setPrevious(upperLeftNode);
                    }
                }else {
                    int left = 0;
                    int top = 0;
                    Node leftNode = null;
                    if(j > 0) {
                        leftNode = lcs[i][j-1];
                    }
                    if(leftNode != null) {
                        left = leftNode.getTotalMatchLength();
                    }
                    Node topNode = null;
                    if(i > 0) {
                        topNode = lcs[i-1][j];
                    }
                    if(topNode != null) {
                        top = topNode.getTotalMatchLength();
                    }
                    if(top >= left) {
                        currentNode.setTotalMatchLength(top).setPrevious(topNode);
                    }else {
                        currentNode.setTotalMatchLength(left).setPrevious(leftNode);
                    }
                }
            }
        }
        
//        for(int i = 0; i < oldList.size(); i++) {
//            for(int j = 0; j < newList.size(); j++) {
//                System.out.print(lcs[i][j]);
//                System.out.print("\t");
//            }
//            System.out.println();
//        }
        
        return lcs[oldList.size()-1][newList.size()-1];
    }
    
    private static List<SegmentPair> longestCommonSequence2(List<String> oldList, List<String> newList) {
        List<SegmentPair> segmentMappingList = new ArrayList<>();
        List<Node> resultList = new ArrayList<>();
        PriorityQueue<Node> priorityQueue = new PriorityQueue<>(oldList.size() * newList.size(), (e1, e2) -> Integer.compare(e2.getTotalMatchLength(), e1.getTotalMatchLength()));
//        Node[][] lcs = new Node[oldList.size()][newList.size()];
        for(int i = 0; i < oldList.size(); i++) {
            for(int j = 0; j < newList.size(); j++) {
                Node currentNode = new Node(i, j);
//                lcs[i][j] = currentNode;
                String oldStr = oldList.get(i);
                String newStr = newList.get(j);
                if(StringUtils.isBlank(oldStr) || StringUtils.isBlank(newStr)) {
                    currentNode.setTotalMatchLength(0);
                }else {
                    Node node = longestCommonSequence(oldStr, newStr);
                    int maxLength = Math.max(StringUtils.length(oldStr), StringUtils.length(newStr));
                    int matchPercentage = (node.getTotalMatchLength() * 1000) / maxLength;
                    currentNode.setTotalMatchLength(matchPercentage);
                }
                priorityQueue.add(currentNode);
            }
        }
//        System.out.println("===================================================================");
//        for(int i = 0; i < oldList.size(); i++) {
//            for(int j = 0; j < newList.size(); j++) {
//                System.out.print(lcs[i][j]);
//                System.out.print("\t");
//            }
//            System.out.println();
//        }
        Node e = null;
        while((e = priorityQueue.poll()) != null) {          
//            System.out.println(e);
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
//        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        resultList.sort((e1, e2) -> Integer.compare(e1.getOldIndex(), e2.getOldIndex()));
//        resultList.forEach(System.out::println);
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
//        System.out.println("------------------------------------------------------------------------");
//        segmentMappingList.forEach(System.out::println);
        return segmentMappingList;
    }
    
    private static String wrapChangePlace(String str, List<SegmentRange> segmentList, String startMark, String endMark) {
        int count = 0;
        for(SegmentRange segmentRange : segmentList) {
            if(!segmentRange.isMatch()) {
                count++;
            }
        }
        StringBuilder stringBuilder = new StringBuilder(str.length() + count * (startMark.length() + endMark.length()));
        for(SegmentRange segmentRange : segmentList) {
            if(segmentRange.getSize() > 0) {
                if(!segmentRange.isMatch()) {
                    stringBuilder.append(startMark);
                }
                stringBuilder.append(str.substring(segmentRange.getBeginIndex(), segmentRange.getEndIndex()));
                if(!segmentRange.isMatch()) {
                    stringBuilder.append(endMark);
                }
            }           
        }
        return stringBuilder.toString();
    }

}
