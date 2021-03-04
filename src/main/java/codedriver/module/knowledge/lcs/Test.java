package codedriver.module.knowledge.lcs;

import org.apache.tools.ant.taskdefs.Java;
import sun.management.resources.agent;
import sun.tools.jar.resources.jar;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @Title: Test
 * @Package codedriver.module.knowledge.lcs
 * @Description: 测试算法性能
 * @Author: linbq
 * @Date: 2021/2/21 12:26
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 **/
public class Test {
    private final static String BASE_PATH = "codedriver-knowledge/src/main/java/codedriver/module/knowledge/lcs/";
    private final static long M = 1024 * 1024;
    public static void main(String[] args) {
//        Runtime runTime = Runtime.getRuntime();
//        long maxMemory = runTime.maxMemory();//1888485376(1801M)
//        long freeMemory = runTime.freeMemory();//119349000(113M)
//        long totalMemory = runTime.totalMemory();//128974848(123M)
//        System.out.println("可以获得的最大内存：" + maxMemory + "(" + (maxMemory / M) + "M)");
//        System.out.println("所分配的剩余内存大小：" + freeMemory + "(" + (freeMemory / M) + "M)");
//        System.out.println("已经分配内存的大小：" + totalMemory + "(" + (totalMemory / M) + "M)");
//        long sum = 0;
//        List<String> oldDataList = readFileData(BASE_PATH + "source.txt");
//        List<String> newDataList = readFileData(BASE_PATH + "target.txt");
//        List<String> oldDataList = readFileData(BASE_PATH + "A.txt");
//        List<String> newDataList = readFileData(BASE_PATH + "B.txt");
        List<String> oldDataList = readFileData(BASE_PATH + "对比A.txt");
        List<String> newDataList = readFileData(BASE_PATH + "对比B.txt");
//        List<String> oldDataList = readFileData(BASE_PATH + "oldData1.txt");
//        List<String> newDataList = readFileData(BASE_PATH + "newData1.txt");
        List<String> oldResultList = new ArrayList<>();
        List<String> newResultList = new ArrayList<>();
        int min = Math.min(oldDataList.size(), newDataList.size());
        min = min > 20 ? 20 : min;
        for(int i = 0; i < min; i++){
            String source = oldDataList.get(i);
            String target =newDataList.get(i);
            long startTime = System.currentTimeMillis();
//            String[] resultArray = LCSUtil.LCSCompare(source, target);
            List<SegmentPair> segmentPairList = LCSUtil.LCSCompare(source, target);
            List<SegmentRange> oldSegmentRangeList = new ArrayList<>();
            List<SegmentRange> newSegmentRangeList = new ArrayList<>();
            for(SegmentPair segmentpair : segmentPairList) {
                System.out.println(segmentpair);
                oldSegmentRangeList.add(new SegmentRange(segmentpair.getOldBeginIndex(), segmentpair.getOldEndIndex(), segmentpair.isMatch()));
                newSegmentRangeList.add(new SegmentRange(segmentpair.getNewBeginIndex(), segmentpair.getNewEndIndex(), segmentpair.isMatch()));
            }
            String oldResult = LCSUtil.wrapChangePlace(source, oldSegmentRangeList, LCSUtil.SPAN_CLASS_DELETE, LCSUtil.SPAN_END);
            String newResult = LCSUtil.wrapChangePlace(target, newSegmentRangeList, LCSUtil.SPAN_CLASS_INSERT, LCSUtil.SPAN_END);
            long cost = System.currentTimeMillis() - startTime;
            System.out.println(source.length() + "*" + target.length() + "=" + (source.length() * target.length()) + "-耗时：" + cost);
//            sum += cost;
//            System.out.println(resultArray[0]);
//            System.out.println(resultArray[1]);
            oldResultList.add(oldResult);
            newResultList.add(newResult);
//            freeMemory = runTime.freeMemory();//200313272(191M)
//            totalMemory = runTime.totalMemory();//795344896(758M)
//            System.out.println("所分配的剩余内存大小：" + freeMemory + "(" + (freeMemory / M) + "M)");
//            System.out.println("已经分配内存的大小：" + totalMemory + "(" + (totalMemory / M) + "M)");
        }
//        System.out.println("总耗时：" + sum);
//        maxMemory = runTime.maxMemory();//1888485376(1801M)
//        freeMemory = runTime.freeMemory();//200313272(191M)
//        totalMemory = runTime.totalMemory();//795344896(758M)
//        System.out.println("可以获得的最大内存：" + maxMemory + "(" + (maxMemory / M) + "M)");
//        System.out.println("所分配的剩余内存大小：" + freeMemory + "(" + (freeMemory / M) + "M)");
//        System.out.println("已经分配内存的大小：" + totalMemory + "(" + (totalMemory / M) + "M)");
        writeFileData(oldResultList, BASE_PATH + "oldResult.txt");
        writeFileData(newResultList, BASE_PATH + "newResult.txt");
//        可以获得的最大内存：1888485376(1801M)
//        所分配的剩余内存大小：119349000(113M)
//        已经分配内存的大小：128974848(123M)
//        7650*7651=58530150-耗时：1994
//        所分配的剩余内存大小：230490088(219M)
//        已经分配内存的大小：764411904(729M)
//        7651*8038=61498738-耗时：1725
//        所分配的剩余内存大小：334167256(318M)
//        已经分配内存的大小：769654784(734M)
//        7651*8044=61544644-耗时：1010
//        所分配的剩余内存大小：391667464(373M)
//        已经分配内存的大小：776994816(741M)
//        7651*8152=62370952-耗时：1522
//        所分配的剩余内存大小：398267776(379M)
//        已经分配内存的大小：781713408(745M)
//        7651*8152=62370952-耗时：997
//        所分配的剩余内存大小：372358576(355M)
//        已经分配内存的大小：783286272(747M)
//        7651*8169=62501019-耗时：1045
//        所分配的剩余内存大小：316437224(301M)
//        已经分配内存的大小：782237696(746M)
//        7651*8169=62501019-耗时：1107
//        所分配的剩余内存大小：223095448(212M)
//        已经分配内存的大小：786432000(750M)
//        7651*8231=62975381-耗时：1052
//        所分配的剩余内存大小：660571280(629M)
//        已经分配内存的大小：811597824(774M)
//        7651*8231=62975381-耗时：1025
//        所分配的剩余内存大小：462842016(441M)
//        已经分配内存的大小：812122112(774M)
//        8239*8238=67872882-耗时：1130
//        所分配的剩余内存大小：199634960(190M)
//        已经分配内存的大小：794820608(758M)
//        总耗时：12607
//        可以获得的最大内存：1888485376(1801M)
//        所分配的剩余内存大小：199634960(190M)
//        已经分配内存的大小：794820608(758M)

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
}
