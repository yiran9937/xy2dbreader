package com.yiran.xy2sf;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import com.alibaba.fastjson2.JSONWriter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.io.IOException;
import java.sql.*;
import java.util.*;

public class SQLiteGameWuPing {

    private static final ObjectMapper msgpackMapper = new ObjectMapper(new MessagePackFactory());

    // SQLite 数据库连接 URL（替换为你实际的本地 .db 文件路径）
//    private static final String DB_URL = "jdbc:sqlite:D:\\games\\dh24\\服务端\\sever\\data\\save.db";
    private static final String DB_URL = "jdbc:sqlite:I:\\java\\ideaIU-2018.2.6.win\\workspace\\xy2dbreader\\db\\save.db";

    public static void main(String[] args) {
        // 假设你要修改角色 ID 为 10001 的数据
//         queryAllWuPing(4);
        modifyData();

    }

    private static void modifyData(){
        try {
            int rid = 3;
            int index = 268;
            Map<String, Object> data = selectOne(rid, index);
            System.out.println("data=" + JSON.toJSONString(data, JSONWriter.Feature.PrettyFormat));
            // Fastjson2 解析成 JSONObject
            JSONObject jo = JSON.parseObject(JSON.toJSONString(data));

            // 灵活配置所有的修改规则
            List<ModRule> rules = new ArrayList<>();

            // 1. 修改最外层 Key-Value (单值)
//            rules.add(new ModRule("$.默契值", 500));
            // 2. 修改基本属性 (找到抗风的子数组，直接替换整个子数组，解决 1 到 2 个值的问题)
//            rules.add(new ModRule("$.属性要求", new Object[]{"根骨", 10}));
            rules.add(new ModRule("$.基本属性[?(@[0] == '增加强克效果')]", new Object[]{"无属性伤害", 2, 2}));
            rules.add(new ModRule("$.基本属性[?(@[0] == '抗遗忘上限')]", new Object[]{"抗混乱上限", 2, 2}));
//            rules.add(new ModRule("$.基本属性[?(@[0] == '力量')]", new Object[]{"根骨", 10}));
//            rules.add(new ModRule("$.基本属性[?(@[0] == '根骨')]", new Object[]{"根骨", 10}));
//            rules.add(new ModRule("$.基本属性[?(@[0] == '抗水')]", new Object[]{"抗封印", 16}));
//            rules.add(new ModRule("$.基本属性[?(@[0] == '抗封印')]", new Object[]{"抗封印", 24}));
//            rules.add(new ModRule("$.基本属性[?(@[0] == '抗水')]", new Object[]{"抗封印", 16}));
//            rules.add(new ModRule("$.基本属性[?(@[0] == '抗混乱')]", new Object[]{"抗混乱", 24}));
//            rules.add(new ModRule("$.基本属性[?(@[0] == '抗雷')]", new Object[]{"抗封印", 16}));
//            rules.add(new ModRule("$.基本属性[?(@[0] == '附加攻击')]", new Object[]{"附加气血", 600}));
//            rules.add(new ModRule("$.基本属性[?(@[0] == '四抗上限')]", new Object[]{"抗混乱上限", 14, 2}));
            // 3. 修改附加属性 (有两个或多个值的数组，直接覆盖)
            rules.add(new ModRule("$.附加属性[?(@[0] == '忽视抗遗忘')]", new Object[]{"加强三尸虫", 400, 400}));
            rules.add(new ModRule("$.附加属性[?(@[0] == '强力克土')]", new Object[]{"加强三尸虫回血程度", 2, 2}));
//            rules.add(new ModRule("$.附加属性[?(@[0] == '抗火')]", new Object[]{"抗封印", 0.1, 4.2}));
//            rules.add(new ModRule("$.附加属性[?(@[0] == '抗火')]", new Object[]{"抗昏睡", 5.1, 5.9}));
//            rules.add(new ModRule("$.附加属性[?(@[0] == '抗雷')]", new Object[]{"抗震慑", 0.1, 3}));
//            rules.add(new ModRule("$.附加属性[?(@[0] == '抗封印')]", new Object[]{"抗震慑", 0.1, 3}));

            // 直接将整条克木属性数组替换为 ["加强三尸虫", 2400]
//            rules.add(new ModRule("$.精炼属性.*[?(@[0] == '强力克木')]", new Object[]{"加强三尸虫", 2400}));
//            rules.add(new ModRule("$.精炼属性.*[?(@[0] == '敏捷')]", new Object[]{"加强三尸虫回血程度", 16}));

            // 4. 修改复杂的 Key -> 数组 -> Key 嵌套结构
            //rules.add(new ModRule("$.内丹列表[?(@.名称 == '神兽丹')].经验", 99999));

            // 遍历并执行修改
            for (ModRule rule : rules) {
                // Fastjson2 的 JSONPath.set 直接支持标准复杂过滤，并且会自动转换底层数组格式
                JSONPath.set(jo, rule.path, rule.value);
            }
//            jo.getJSONArray("附加属性").add(new Object[]{"加强混乱", 5.6, 0.8});
//            jo.getJSONArray("附加属性").add(new Object[]{"加强仙法", 8.4, 1.2});
//            jo.getJSONArray("基本属性").add(new Object[]{"抗风", 75});
//            jo.getJSONArray("附加属性").remove(2);
//            jo.put("精炼属性", new JSONObject(){{
//                put("1", new Object[]{"灵性", 30});
//                put("2", new Object[]{"根骨", 20});
//                put("重复", true);
//            }});

//            jo.put("属性要求", new Object[]{"根骨", 60});

            // 打印修改后的标准 JSON 字符串
            System.out.println("修改完成后的结果：\n" + JSON.toJSONString(jo, JSONWriter.Feature.PrettyFormat));
//            saveWupingData(jo);
/*
{
		"等级需求": [
			60,
			0
		],
		"位置": 564,
		"丢弃时间": 0,
		"获得时间": 0,
		"耐久": 300,
		"默契值": 1,
		"名称": "草莺斗篷",
		"属性要求": [
			"根骨",
			80
		],
		"基本属性": [
			[
				"灵性",
				6
			],
			[
				"附加气血",
				600
			],
			[
				"抗遗忘",
				16
			]
		],
		"默契值上限": 1000,
		"附加属性": [
			[
				"速度",
				1,
				17
			],
			[
				"抗混乱",
				0.1,
				4.2
			]
		],
		"数量": 1
	}
 */


        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void queryAllWuPing(int playerId){
        try {
            System.out.println("1. 正在从 SQLite 读取玩家数据...");
            List<Map<String, Object>> playerData = loadPlayerData(playerId);

            if (playerData != null) {
                System.out.println("读取成功！当前数据摘要：");
//                System.out.println("data=" + JSON.toJSONString(playerData, true));
                printData(playerData);

            } else {
                System.out.println("未找到 ID 为 " + playerId + " 的玩家数据。");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void printData(List<Map<String, Object>> playerData){
        List<Integer> indexArr = new ArrayList<>();
        Map<Integer, Map<String, Object>> dataMap = new HashMap<>();
        for (Map<String, Object> playerDatum : playerData) {
            // System.out.println("位置:" + playerDatum.get("位置"));
            Integer key = Integer.valueOf(playerDatum.get("位置").toString());
            if (key > 256 && key < 512){
//            if (key > 1 && key < 1111512){
                indexArr.add(key);
                dataMap.put(key, playerDatum);
            }
        }
        Collections.sort(indexArr);
        System.out.println(JSON.toJSONString(dataMap, JSONWriter.Feature.PrettyFormat));
    }

    public static Map<String, Object> selectOne(int rid, int index) throws SQLException, IOException{
        String sql = "SELECT nid, 数据 FROM 物品 WHERE rid = ? and 位置 = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, rid);
            pstmt.setInt(2, index);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // 兼容性读取：先尝试作为二进制（BLOB）读取，如果失败则作为 String 读取
                    byte[] bytes = rs.getBytes("数据");

                    String nid = rs.getString("nid");

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

                    Map<String, Object> data = msgpackMapper.readValue(bytes, new TypeReference<Map<String, Object>>() {});
                    // 使用 MsgPack 反序列化为 Map
                    data.put("nid", nid);
                    return data;
                }
            }
        }

        return null;
    }

    public static class ModRule {
        String path;   // JSONPath 表达式
        Object value;  // 替换成什么值（可以是单个数字、数组、甚至整个JSONObject）

        public ModRule(String path, Object value) {
            this.path = path;
            this.value = value;
        }
    }



    /**
     * 从数据库读取并解析玩家数据
     */
    public static List<Map<String, Object>> loadPlayerData(int playerId) throws SQLException, IOException {
        // 假设表名是 players，包含 id 和 data 字段（data 字段可能为 BLOB 或 TEXT）
        String sql = "SELECT nid, 数据 FROM 物品 WHERE rid = ? and 数量 = 1";

        List<Map<String, Object>> result = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, playerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // 兼容性读取：先尝试作为二进制（BLOB）读取，如果失败则作为 String 读取
                    byte[] bytes = rs.getBytes("数据");

                    String nid = rs.getString("nid");

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

                    Map<String, Object> data = msgpackMapper.readValue(bytes, new TypeReference<Map<String, Object>>() {});
                    // 使用 MsgPack 反序列化为 Map
                    data.put("nid", nid);
                    result.add(data) ;
                }
            }
        }
        return result;
    }

    /**
     * 将修改后的 Map 序列化并存回 SQLite
     */
    public static void saveWupingData(Map<String, Object> wuPingData) throws IOException, SQLException {
        String nid = wuPingData.get("nid").toString();
        wuPingData.remove("nid");
        // 将 Map 转换为 MsgPack 二进制字节流
        byte[] bytes = msgpackMapper.writeValueAsBytes(wuPingData);

        // 如果你的数据库字段是 TEXT 格式，需要转成 Hex 串，请启用下面两行：
        // String hexStr = "0x" + byteArrayToHexString(bytes);
        // byte[] dataToSave = hexStr.getBytes();

        // 默认推荐以二进制 BLOB 写入
        byte[] dataToSave = bytes;

        //String sql = "SELECT 数据 FROM 角色 WHERE id = ?";
        String sql = "UPDATE 物品 SET 数据 = ? WHERE nid = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBytes(1, dataToSave); // 对应 BLOB
            pstmt.setString(2, nid);
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
