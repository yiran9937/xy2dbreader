package com.yiran.xy2sf.dao;

import com.yiran.xy2sf.util.MsgPackUtil;

import java.sql.*;
import java.util.*;

public class GenericTableDAO {

    private final String dbUrl;

    public GenericTableDAO(String dbPath) {
        this.dbUrl = "jdbc:sqlite:" + dbPath;
    }

    /**
     * 动态查询指定表的所有数据行（转换为 Map 列表，便于 TableView 动态展示）
     */
    public List<Map<String, Object>> executeQuery(String tableName, String filterSql) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT * FROM " + tableName + (filterSql.isBlank() ? "" : " WHERE " + filterSql);

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String colName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(colName, value);
                }
                result.add(row);
            }
        }
        return result;
    }

    /**
     * 更新指定表的 BLOB 数据列
     *
     * @param tableName  表名（如 "物品"）
     * @param pkColumn   主键字段名（如 "nid"）
     * @param pkValue    主键值
     * @param blobColumn BLOB 字段名（如 "数据"）
     * @param jsonContent 修改后的 JSON 字符串
     */
    public void updateBlobData(String tableName, String pkColumn, Object pkValue, String blobColumn, String jsonContent) throws Exception {
        byte[] blobBytes = MsgPackUtil.jsonToBytes(jsonContent);

        String sql = String.format("UPDATE %s SET %s = ? WHERE %s = ?", tableName, blobColumn, pkColumn);

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBytes(1, blobBytes);
            pstmt.setObject(2, pkValue);
            pstmt.executeUpdate();
        }
    }
}
