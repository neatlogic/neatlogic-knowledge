package codedriver.module.knowledge.lcs;

/**
 * @Title: PrintSingeColorFormatUtil
 * @Package codedriver.module.knowledge.lcs
 * @Description: TODO
 * @Author: linbq
 * @Date: 2021/3/1 10:55
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 **/
public class PrintSingeColorFormatUtil {
    private static boolean SWITCH = false;
    public static void print(char c){
        if(SWITCH){
            System.out.format("\33[39;2m%c", c);
        }
    }
    public static void print(String s){
        if(SWITCH) {
            System.out.format("\33[39;2m%s", s);
        }
    }
    public static void println(String s){
        if(SWITCH) {
            System.out.format("\33[39;2m%s%n", s);
        }
    }
    public static void println(){
        if(SWITCH) {
            System.out.format("\33[39;2m%n");
        }
    }
    public static void print(char c, String startMark){
        if(SWITCH) {
            if (startMark.equals(LCSUtil.SPAN_CLASS_INSERT)) {
                System.out.format("\33[32;2m%c", c);
            } else if (startMark.equals(LCSUtil.SPAN_CLASS_DELETE)) {
                System.out.format("\33[31;2m%c", c);
            } else {
                System.out.format("\33[39;2m%c", c);
            }
        }
    }
    public static void print(String s, String startMark){
        if(SWITCH) {
            if (startMark.equals(LCSUtil.SPAN_CLASS_INSERT)) {
                System.out.format("\33[32;2m%s", s);
            } else if (startMark.equals(LCSUtil.SPAN_CLASS_DELETE)) {
                System.out.format("\33[31;2m%s", s);
            } else {
                System.out.format("\33[39;2m%s", s);
            }
        }
    }
    public static void println(String s, String startMark){
        if(SWITCH) {
            print(s, startMark);
            System.out.format("\33[39;2m%n");
        }
    }

    public static void main(String[] args){
        System.out.format("\33[32;42;4mddddddd%n");
        int font = 31;
        int background = 41;
        for (int i = 0; i <= 50; i++) {
            for(int j = 0; j <= 50; j++){
                System.out.format("\33[%d;%d;4m前景色是%d,背景色是%d------我是博主%n", font + i, background + j, font + i, background + j);
            }
        }
    }
}
