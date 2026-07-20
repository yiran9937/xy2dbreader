package com.yiran.xy2sf;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckFileName {
    // 正则表达式：兼容 "名称 = 'xxx'" 以及 "name = 'xxx'"
    private static final Pattern NAME_PATTERN = Pattern.compile("(?:名称|name)\\s*=\\s*'([^']+)'");

    public static void main(String[] args) {
        // 替换成你的文件真实路径
        String filePathA = "E:\\yiran\\dh2\\scripts\\掉落池new.lua";
        String filePathB = "E:\\yiran\\dh2\\scripts\\make\\物品库.lua";

        // 1. 读取 B 文件中的所有名称并放入 Set (用于 O(1) 快速查找)
        Set<String> setB = new HashSet<>();
        try (BufferedReader brB = new BufferedReader(new FileReader(filePathB))) {
            String line;
            while ((line = brB.readLine()) != null) {
                String name = extractName(line);
                if (name != null) {
                    setB.add(name);
                }
            }
        } catch (IOException e) {
            System.err.println("读取 B 文件失败：" + e.getMessage());
            return;
        }

        // 2. 遍历 A 文件，查询不在 setB 中的名称 (用 LinkedHashSet 保持查找顺序并自动去重)
        Set<String> missingInB = new LinkedHashSet<>();
        int totalACount = 0;

        try (BufferedReader brA = new BufferedReader(new FileReader(filePathA))) {
            String line;
            while ((line = brA.readLine()) != null) {
                String name = extractName(line);
                if (name != null) {
                    totalACount++;
                    // 如果 B 中没有这个名称，记录下来
                    if (!setB.contains(name)) {
                        missingInB.add(name);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("读取 A 文件失败：" + e.getMessage());
            return;
        }

        // 3. 打印结果
        System.out.println("====== 统计结果 ======");
        System.out.println("A文件总解析项数：" + totalACount);
        System.out.println("B文件唯一名称数：" + setB.size());
        System.out.println("在 A 中存在，但在 B 中缺失的名称（共 " + missingInB.size() + " 个，已自动去重）：\n");

        for (String missingName : missingInB) {
            System.out.println(missingName);
        }
    }

    /**
     * 从一行文本中提取单引号内的名称
     */
    private static String extractName(String line) {
        Matcher matcher = NAME_PATTERN.matcher(line);
        if (matcher.find()) {
            return matcher.group(1); // 返回正则分组1匹配到的文字（如：随机宝石）
        }
        return null;
    }
}
