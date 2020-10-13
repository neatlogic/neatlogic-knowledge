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

import org.apache.commons.collections4.CollectionUtils;

public class LCSTest {
    private final static String BASE_PATH = "src/main/java/codedriver/module/knowledge/lcstest/";
    
    public static void main(String[] args) {
        List<String> oldDataList = new ArrayList<>();
        List<String> newDataList = new ArrayList<>();
        readFileData(oldDataList, BASE_PATH + "oldData.txt", newDataList, BASE_PATH + "newData.txt");
        List<SegmentMapping> segmentMappingList = longestCommonSequence(oldDataList, newDataList).getSegmentMappingList();
        List<String> oldResultList = new ArrayList<>();
        List<String> newResultList = new ArrayList<>();
        for(SegmentMapping segmentMapping : segmentMappingList) {
            SegmentRange oldSegmentRange = segmentMapping.getOldSegmentRange();
            SegmentRange newSegmentRange = segmentMapping.getNewSegmentRange();
            List<String> oldSubList = new ArrayList<>();
            List<String> newSubList = new ArrayList<>();
            if(oldSegmentRange.getSize() > 0) {
                oldSubList = oldDataList.subList(oldSegmentRange.getBeginIndex(), oldSegmentRange.getEndIndex());
            }
            if(newSegmentRange.getSize() > 0) {
                newSubList = newDataList.subList(newSegmentRange.getBeginIndex(), newSegmentRange.getEndIndex());
            }
            if(segmentMapping.isMatch()) {
                if(CollectionUtils.isNotEmpty(oldSubList)) {
                    oldResultList.addAll(oldSubList);
                }
                if(CollectionUtils.isNotEmpty(newSubList)) {
                    newResultList.addAll(newSubList);
                }
            }else {
                int minSize = Math.min(oldSegmentRange.getSize(), newSegmentRange.getSize());
                for(int i = 0; i < minSize; i++) {
                    String oldStr = oldSubList.get(i);
                    String newStr = newSubList.get(i);
                    List<SegmentRange> oldSegmentRangeList = new ArrayList<>();
                    List<SegmentRange> newSegmentRangeList = new ArrayList<>();
                    for(SegmentMapping segmentmapping : longestCommonSequence(oldStr, newStr).getSegmentMappingList()) {
                        oldSegmentRangeList.add(segmentmapping.getOldSegmentRange());
                        newSegmentRangeList.add(segmentmapping.getNewSegmentRange());
                    }
                    oldResultList.add("--" + wrapChangePlace(oldStr, oldSegmentRangeList, "<->", "</->"));
                    newResultList.add("++" + wrapChangePlace(newStr, newSegmentRangeList, "<+>", "</+>"));
                }
                if(oldSegmentRange.getSize() > newSegmentRange.getSize()) {
                    for(int i = newSegmentRange.getSize(); i < oldSegmentRange.getSize(); i++) {
                        oldResultList.add("--" + oldSubList.get(i));
                        newResultList.add("==");
                    }
                }else if(oldSegmentRange.getSize() < newSegmentRange.getSize()) {
                    for(int i = oldSegmentRange.getSize(); i < newSegmentRange.getSize(); i++) {
                        oldResultList.add("==");
                        newResultList.add("++" + newSubList.get(i));
                    }
                }
            }
        }
        writeFileData(oldResultList, BASE_PATH + "oldResult.txt", newResultList, BASE_PATH + "newResult.txt");
    }
    
    private static void readFileData(List<String> oldList, String oldFilePath, List<String> newList, String newFilePath) {
        try (
            FileInputStream fis = new FileInputStream(oldFilePath); 
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            ) {
            String str = null;
            while((str = br.readLine()) != null) {
                oldList.add(str);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (
            FileInputStream fis = new FileInputStream(newFilePath); 
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            ) {
            String str = null;
            while((str = br.readLine()) != null) {
                newList.add(str);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void writeFileData(List<String> oldList, String oldFilePath, List<String> newList, String newFilePath) {
        try (
            FileOutputStream fos = new FileOutputStream(oldFilePath); 
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            BufferedWriter bw = new BufferedWriter(osw);
            ) {
            for(String str : oldList) {
                bw.write(str);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (
            FileOutputStream fos = new FileOutputStream(newFilePath); 
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            BufferedWriter bw = new BufferedWriter(osw);
            ) {
            for(String str : newList) {
                bw.write(str);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static Node longestCommonSequence(String oldStr, String newStr) {
        char[] x = oldStr.toCharArray();
        char[] y = newStr.toCharArray();
        Node[][] lcs = new Node[x.length][y.length];
        
        for(int i = 0; i < x.length; i++) {
            for(int j = 0; j < y.length; j++) {
                Node currentNode = new Node(i, j);
                if(x[i] == y[j]) {
                    currentNode.setTotalMatchLength(1).setMatch(true);
                    Node upperLeftNode = null;
                    if(i > 0 && j > 0) {
                        upperLeftNode = lcs[i-1][j-1];
                    }
                    if(upperLeftNode != null) {
                        currentNode.setTotalMatchLength(upperLeftNode.getTotalMatchLength() + 1).setPrevious(upperLeftNode);
                    }
                    lcs[i][j] = currentNode;
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
                    if(left >= top) {
                        currentNode.setTotalMatchLength(left).setPrevious(leftNode);
                        lcs[i][j] = currentNode;
                    }else {
                        currentNode.setTotalMatchLength(top).setPrevious(topNode);
                        lcs[i][j] = currentNode;
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
        
        return lcs[x.length-1][y.length-1];
    }
    private static Node longestCommonSequence(List<String> oldList, List<String> newList) {
        Node[][] lcs = new Node[oldList.size()][newList.size()];       
        for(int i = 0; i < oldList.size(); i++) {
            for(int j = 0; j < newList.size(); j++) {
                Node currentNode = new Node(i, j);
                if(oldList.get(i).equals(newList.get(j))) {
                    currentNode.setTotalMatchLength(1).setMatch(true);
                    Node upperLeftNode = null;
                    if(i > 0 && j > 0) {
                        upperLeftNode = lcs[i-1][j-1];
                    }
                    if(upperLeftNode != null) {
                        currentNode.setTotalMatchLength(upperLeftNode.getTotalMatchLength() + 1).setPrevious(upperLeftNode);
                    }
                    lcs[i][j] = currentNode;
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
                    if(left >= top) {
                        currentNode.setTotalMatchLength(left).setPrevious(leftNode);
                        lcs[i][j] = currentNode;
                    }else {
                        currentNode.setTotalMatchLength(top).setPrevious(topNode);
                        lcs[i][j] = currentNode;
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
    
    private static String wrapChangePlace(String str, List<SegmentRange> segmentList, String startMark, String endMark) {
        int count = 0;
        for(SegmentRange segmentRange : segmentList) {
            if(!segmentRange.isMatch()) {
                count++;
            }
        }
        StringBuilder stringBuilder = new StringBuilder(str.length() + count * (startMark.length() + endMark.length()));
        for(SegmentRange segmentRange : segmentList) {
            if(!segmentRange.isMatch()) {
                stringBuilder.append(startMark);
            }
            stringBuilder.append(str.substring(segmentRange.getBeginIndex(), segmentRange.getEndIndex()));
            if(!segmentRange.isMatch()) {
                stringBuilder.append(endMark);
            }
        }
        return stringBuilder.toString();
    }

}
