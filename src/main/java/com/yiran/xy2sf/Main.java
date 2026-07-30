package com.yiran.xy2sf;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    // 终极强力手机号正则
    private static final String REGEX = "(?<!\\d)1[^\\d\\n\\r]{0,4}[3-9](?:[^\\d\\n\\r]{0,4}\\d){9}(?!\\d)";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    public static void main(String[] args) {
        String[] validSamples = {
                "我的联系方式是 138-1234-5678，非诚勿扰。",
                "13812345678",
                "138-1234-5678",
                "138 1234 5678",
                "138(电话)1234(号)5678",
                "1 3 8 1 2 3 4 5 6 7 8",
                "1--#a3812345678",
                "电1 3 2 88 9131-48",
                "联系方式：+86 138-1234-5678",
                "那么第4点我df电1 3 2 88 9131-48 第2点"
        };

        String[] invalidSamples = {
                "23812345678",
                "1381234567",
                "订单号1381234567890",
                "1-----3812345678",
                "1(联系电话是)3812345678",
                "10012345678",
                "1381234567\n89012345"
        };

        System.out.println("=== 测试 正向有效样本 ===");
        for (String sample : validSamples) {
            Matcher matcher = PATTERN.matcher(sample);
            if (matcher.find()) {
                String raw = matcher.group();
                String clean = raw.replaceAll("\\D", "");
                System.out.println("【PASS】原文: \"" + sample + "\" -> 提取: " + clean);
            } else {
                System.err.println("【FAIL】原文: \"" + sample + "\" 未能匹配！");
            }
        }

        System.out.println("\n=== 测试 反向干扰样本 ===");
        for (String sample : invalidSamples) {
            Matcher matcher = PATTERN.matcher(sample);
            if (matcher.find()) {
                System.err.println("【FAIL】原文: \"" + sample + "\" 误匹配到了: " + matcher.group());
            } else {
                System.out.println("【PASS】原文: \"" + sample + "\" 成功拦截");
            }
        }
    }

    public static void main111(String[] args) {
//        String testText = "请联系张三：138-1234-5678，或者李四：电1 3 2 88 9131-48，"
//                + "不要打10012345678，也不要查单号1381234567890。";

//        String testText = "那么第4点我电1 3 2 88 9131-48 第2点";
//        List<String> results = extractPhoneNumbers(testText);
//        System.out.println("提取到的有效手机号：" + results);
//        // 输出: 提取到的有效手机号：[13812345678, 13288913148]

        String text = "那么第4点我电1 3 2 88 9131-48 第2点";

        // 优化后的正则：精准提取 1 开头的 11 位数字块
        // 允许数字间有 0-4 个非数字字符
        String regex = "1[3-9](?:[^\\d\\n\\r]{0,4}\\d){9}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        List<String> results = new ArrayList<>();
        while (matcher.find()) {
            String raw = matcher.group();
            // 剔除干扰字符，还原纯数字
            String clean = raw.replaceAll("\\D", "");

            // 校验：只要纯数字正好是 11 位就成功拿到
            if (clean.length() == 11) {
                results.add(clean);
                System.out.println("成功提取原始串: " + raw);
                System.out.println("清洗后手机号: " + clean);
            }
        }
    }

    // 强力手机号匹配正则
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("1[3-9](?:[^\\d\\n\\r]{0,4}\\d){9}");

    /**
     * 从文本中提取所有符合条件的手机号（已自动清洗为纯11位数字）
     */
    public static List<String> extractPhoneNumbers(String text) {
        List<String> phones = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return phones;
        }

        Matcher matcher = PHONE_PATTERN.matcher(text);
        while (matcher.find()) {
            String rawMatch = matcher.group();
            // 剔除所有非数字字符，还原为 11 位标准手机号
            String cleanPhone = rawMatch.replaceAll("\\D", "");
            phones.add(cleanPhone);
        }
        return phones;
    }


}