package com.yiran.xy2sf.dao;

import com.yiran.xy2sf.util.MsgPackUtil;

import java.sql.*;
import java.util.*;

public class GenericTableDAO {

    private final String dbUrl;

    public GenericTableDAO(String dbPath) {
        this.dbUrl = "jdbc:sqlite:" + dbPath;
    }

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
     * 更新指定行的 BLOB 数据（右侧按钮使用）
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

    /**
     * 新增：更新指定行的普通字段（左侧按钮使用，自动排除 BLOB 列）
     */
    public void updateRowData(String tableName, String pkColumn, Object pkValue, Map<String, Object> rowData, String blobColumn) throws Exception {
        List<String> setCols = new ArrayList<>();
        List<Object> setValues = new ArrayList<>();

        for (Map.Entry<String, Object> entry : rowData.entrySet()) {
            String colName = entry.getKey();
            // 排除 BLOB 字段，只更新普通列
            if (!colName.equalsIgnoreCase(blobColumn)) {
                setCols.add(colName + " = ?");
                setValues.add(entry.getValue());
            }
        }

        if (setCols.isEmpty()) return;

        String sql = String.format("UPDATE %s SET %s WHERE %s = ?",
                tableName, String.join(", ", setCols), pkColumn);

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < setValues.size(); i++) {
                pstmt.setObject(i + 1, setValues.get(i));
            }
            pstmt.setObject(setValues.size() + 1, pkValue);
            pstmt.executeUpdate();
        }
    }

    /**
     * 获取当前 SQLite 数据库中所有的用户表名
     */
    public List<String> getAllTableNames() throws SQLException {
        List<String> tables = new ArrayList<>();
        // 查询 sqlite_master 表，过滤掉 sqlite_ 开头的系统内置表
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name;";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tables.add(rs.getString("name"));
            }
        }
        return tables;
    }
}