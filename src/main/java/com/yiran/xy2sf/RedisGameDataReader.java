package com.yiran.xy2sf;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class RedisGameDataReader {
    private static final ObjectMapper msgpackMapper = new ObjectMapper(new MessagePackFactory());
    private static JedisPool jedisPool;

    // Redis 连接配置（根据你的 SF 配置修改）
    private static final String REDIS_HOST = "127.0.0.1";
    private static final int REDIS_PORT = 6379;
    private static final String REDIS_PASSWORD = null; // 如果无密码，设为 null

    static {
        int databaseIndex = 1;

        // 初始化 Redis 连接池
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10); // 最大连接数
        poolConfig.setMaxIdle(5);   // 最大空闲连接

        if (REDIS_PASSWORD != null && !REDIS_PASSWORD.isEmpty()) {
            jedisPool = new JedisPool(poolConfig, REDIS_HOST, REDIS_PORT, 2000, REDIS_PASSWORD);
        } else {
            jedisPool = new JedisPool(poolConfig, REDIS_HOST, REDIS_PORT, 2000, null, databaseIndex);
        }
    }
//3	4	kUI5HNQdOD6ESS45EAsDV		1784287658	0	0	0	1784450923	127.0.0.1	缘定今生
//4	4	uxzl2SstULpcekrLF9MoJ		1784287832	0	0	0	1784450921	127.0.0.1	易之然
//5	4	vjesGRT3PfZSNeWA3IFdn		1784287880	0	0	0	1784450920	127.0.0.1	滴滴答答
//6	4	hGhiuscx-FsoEOOGNPPuf		1784287910	0	0	0	1784450921	127.0.0.1	月不常圆
//7	4	Wl1DmQz1p9f56I1CarkdR		1784287931	0	0	0	1784450921	127.0.0.1	小命不保
    public static void main(String[] args) {
        // 假设 Redis 中角色的 Key 是 "player:10001"
//        String playerKey = "USER:zNSFZDHWxyJ9hVd3H9-Kt";

        System.out.println(JSON.toJSONString(getAllKeys(), JSONWriter.Feature.PrettyFormat));
        String playerKey = "USER:kUI5HNQdOD6ESS45EAsDV";
//        String playerKey = "日常活动限制";

        try {
            System.out.println("1. 正在从 Redis 读取玩家数据...");
            Object playerData = loadPlayerData(playerKey);
            System.out.println(JSON.toJSONString(playerData, JSONWriter.Feature.PrettyFormat));

            deleteKey(playerKey);




        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭连接池
            if (jedisPool != null) {
                jedisPool.close();
            }
        }
    }

    /**
     * 使用 SCAN 命令无阻塞地获取当前 1 号库的所有 Key
     */
    public static Set<String> getAllKeys() {
        Set<String> keys = new HashSet<>();

        try (Jedis jedis = jedisPool.getResource()) {
            String cursor = ScanParams.SCAN_POINTER_START; // 初始游标 "0"

            // 每次拉取 100 条（可以根据数据量调整，通常 100 到 500 比较合适）
            ScanParams scanParams = new ScanParams().count(100);

            do {
                // 执行 SCAN
                ScanResult<String> scanResult = jedis.scan(cursor, scanParams);

                // 将这一批拿到的 Key 存入我们的结果集（Set 还会自动去重）
                keys.addAll(scanResult.getResult());

                // 更新游标，继续下一次循环
                cursor = scanResult.getCursor();

            } while (!cursor.equals(ScanParams.SCAN_POINTER_START)); // 当游标重新回到 "0" 时，代表全部遍历完毕
        }

        return keys;
    }

    /**
     * 从 Redis 中读取并解析玩家数据（自动兼容二进制与十六进制文本）
     */
    public static Object loadPlayerData(String key) throws IOException {
        try (Jedis jedis = jedisPool.getResource()) {
            // 1. 使用 byte[] 键读取原始数据，这样既能拿到二进制，也能拿到 String 文本的字节
            byte[] rawBytes = jedis.get(key.getBytes(StandardCharsets.UTF_8));

            if (rawBytes == null || rawBytes.length == 0) {
                return null;
            }

            // 2. 自动判定存储格式
            byte[] msgpackBytes;
            if (isHexString(rawBytes)) {
                // 如果是 "0x..." 格式的十六进制文本，将其转为 byte[]
                String hexStr = new String(rawBytes, StandardCharsets.UTF_8).trim();
                if (hexStr.startsWith("0x") || hexStr.startsWith("0X")) {
                    hexStr = hexStr.substring(2);
                }
                msgpackBytes = hexStringToByteArray(hexStr);
            } else {
                // 如果已经是原始二进制，直接使用
                msgpackBytes = rawBytes;
            }

            // 3. 使用 MsgPack 反序列化为 Java Map
            return loadAndParseCompatible(msgpackBytes);
        }
    }

    public static Object deleteKey(String... key) throws IOException {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.del(key);
        }
    }

    /**
     * 兼容性解析方法：自动识别返回的是 Map 还是 List
     */
    public static Object loadAndParseCompatible(byte[] msgpackBytes) throws IOException {
        if (msgpackBytes == null || msgpackBytes.length == 0) {
            return null;
        }

        // 1. 先反序列化成最通用的 Object
        Object parsedData = msgpackMapper.readValue(msgpackBytes, Object.class);

        // 2. 判断实际类型并处理
        if (parsedData instanceof Map) {
            System.out.println("-> 解析成功：数据类型为 [Map] (键值对)");
            @SuppressWarnings("unchecked")
            Map<String, Object> mapData = (Map<String, Object>) parsedData;
            return mapData;

        } else if (parsedData instanceof List) {
            System.out.println("-> 解析成功：数据类型为 [List] (数组)");
            @SuppressWarnings("unchecked")
            List<Object> listData = (List<Object>) parsedData;
            return listData;

        } else {
            // 可能是基础类型（String, Integer 等）
            System.out.println("-> 解析成功：数据类型为 [基础类型]: " + parsedData.getClass().getSimpleName());
            return parsedData;
        }
    }

    /**
     * 将修改后的 Map 序列化并存回 Redis
     */
    public static void savePlayerData(String key, Map<String, Object> playerData) throws IOException {
        // 1. 将数据转换为 MsgPack 的 byte[]
        byte[] msgpackBytes = msgpackMapper.writeValueAsBytes(playerData);

        try (Jedis jedis = jedisPool.getResource()) {
            // 2. 写入 Redis
            // 提示：如果你发现你的 Redis 必须存 Hex 文本（0xDE...），请解开下面两行代码：
            /*
            String hexStr = "0x" + byteArrayToHexString(msgpackBytes);
            jedis.set(key, hexStr);
            */

            // 默认推荐以最节省空间的二进制存入：
            jedis.set(key.getBytes(StandardCharsets.UTF_8), msgpackBytes);
        }
    }

    // --- 辅助工具函数 ---

    private static boolean isHexString(byte[] bytes) {
        if (bytes.length < 2) return false;
        // 判断前两个字节是否为 '0' 和 'x' / 'X' 的 ASCII 码
        return bytes[0] == 48 && (bytes[1] == 120 || bytes[1] == 88);
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

    private static String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
