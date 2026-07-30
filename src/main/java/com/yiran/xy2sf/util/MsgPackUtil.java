package com.yiran.xy2sf.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.io.IOException;
import java.util.Map;

public class MsgPackUtil {

    private static final ObjectMapper msgpackMapper = new ObjectMapper(new MessagePackFactory());

    /**
     * 将数据库读取的字节流（支持 Hex 兼容）解码为 Pretty 格式的 JSON 字符串
     */
    public static String bytesToJson(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0) {
            return "{}";
        }

        // Hex 兼容处理
        if (isHexString(bytes)) {
            String hexStr = new String(bytes).trim();
            if (hexStr.startsWith("0x") || hexStr.startsWith("0X")) {
                hexStr = hexStr.substring(2);
            }
            bytes = hexStringToByteArray(hexStr);
        }

        // 先还原为 Map，再序列化为 Pretty JSON 供前端展示修改
        Map<String, Object> map = msgpackMapper.readValue(bytes, new TypeReference<Map<String, Object>>() {});
        return JSON.toJSONString(map, JSONWriter.Feature.PrettyFormat);
    }

    /**
     * 将修改后的 JSON 字符串重新打包为 MsgPack 字节流
     */
    public static byte[] jsonToBytes(String jsonStr) throws IOException {
        // 先解析为 Map
        Map<String, Object> map = msgpackMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
        // 再写为 MsgPack 字节流
        return msgpackMapper.writeValueAsBytes(map);
    }

    /**
     * 验证字符串是否为标准的合法 JSON 格式
     */
    public static boolean isValidJson(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return false;
        }
        return JSON.isValid(jsonStr);
    }

    private static boolean isHexString(byte[] bytes) {
        if (bytes.length < 2) return false;
        return bytes[0] == '0' && (bytes[1] == 'x' || bytes[1] == 'X');
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
