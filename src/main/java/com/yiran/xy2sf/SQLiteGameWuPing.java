package com.yiran.xy2sf;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SQLiteGameWuPing {

    private static final ObjectMapper msgpackMapper = new ObjectMapper(new MessagePackFactory());

    // SQLite 数据库连接 URL（替换为你实际的本地 .db 文件路径）
    private static final String DB_URL = "jdbc:sqlite:E:\\yiran\\xy2\\服务端\\sever\\data\\save.db";

    public static void main(String[] args) {
        // 假设你要修改角色 ID 为 10001 的数据
        int playerId = 1;

        try {
            System.out.println("1. 正在从 SQLite 读取玩家数据...");
            List<Map<String, Object>> playerData = loadPlayerData(playerId);

            if (playerData != null) {
                System.out.println("读取成功！当前数据摘要：");
                System.out.println("data=" + JSON.toJSONString(playerData, true));

            } else {
                System.out.println("未找到 ID 为 " + playerId + " 的玩家数据。");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 从数据库读取并解析玩家数据
     */
    public static List<Map<String, Object>> loadPlayerData(int playerId) throws SQLException, IOException {
        // 假设表名是 players，包含 id 和 data 字段（data 字段可能为 BLOB 或 TEXT）
        String sql = "SELECT 数据 FROM 物品 WHERE rid = ? and 数量 = 1";

        List<Map<String, Object>> result = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, playerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // 兼容性读取：先尝试作为二进制（BLOB）读取，如果失败则作为 String 读取
                    byte[] bytes = rs.getBytes("数据");

                    if (bytes == null || bytes.length == 0) {
                        return null;
                    }

                    // 判断是否为 16 进制字符串格式（有些 SF 的 SQLite 会把 Hex 串当成 Text 存入）
                    if (isHexString(bytes)) {
                        String hexStr = new String(bytes).trim();
                        if (hexStr.startsWith("0x") || hexStr.startsWith("0X")) {
                            hexStr = hexStr.substring(2);
                        }
                        bytes = hexStringToByteArray(hexStr);
                    }

                    // 使用 MsgPack 反序列化为 Map
                    result.add(msgpackMapper.readValue(bytes, new TypeReference<Map<String, Object>>() {})) ;
                }
            }
        }
        return result;
    }

    /**
     * 将修改后的 Map 序列化并存回 SQLite
     */
    public static void savePlayerData(int playerId, Map<String, Object> playerData) throws IOException, SQLException {
        // 将 Map 转换为 MsgPack 二进制字节流
        byte[] bytes = msgpackMapper.writeValueAsBytes(playerData);

        // 如果你的数据库字段是 TEXT 格式，需要转成 Hex 串，请启用下面两行：
        // String hexStr = "0x" + byteArrayToHexString(bytes);
        // byte[] dataToSave = hexStr.getBytes();

        // 默认推荐以二进制 BLOB 写入
        byte[] dataToSave = bytes;

        //String sql = "SELECT 数据 FROM 角色 WHERE id = ?";
        String sql = "UPDATE 角色 SET 数据 = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBytes(1, dataToSave); // 对应 BLOB
            pstmt.setInt(2, playerId);
            pstmt.executeUpdate();
        }
    }

    // --- 辅助工具函数 ---

    private static boolean isHexString(byte[] bytes) {
        if (bytes.length < 2) return false;
        // 如果前两个字符是 '0' 和 'x'，判定为 Hex 字符串
        return bytes[0] == '0' && (bytes[1] == 'x' || bytes[1] == 'X');
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private static String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
