package com.yiran.xy2sf.ui;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.yiran.xy2sf.dao.GenericTableDAO;
import com.yiran.xy2sf.util.MsgPackUtil;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.prefs.Preferences;

public class MainViewController {

    private final Preferences prefs = Preferences.userNodeForPackage(MainViewController.class);
    private static final String KEY_DB_PATH = "last_db_path";

    private final BorderPane root = new BorderPane();
    private final TableView<Map<String, Object>> tableView = new TableView<>();
    private final TextArea jsonTextArea = new TextArea();
    private final TextField dbPathField = new TextField();
    private final TextField tableNameField = new TextField("物品");
    private final TextField pkColumnField = new TextField("nid");
    private final TextField blobColumnField = new TextField("数据");

    private GenericTableDAO dao;
    private Map<String, Object> selectedRow;

    // --- 数据源与筛选/排序容器 ---
    private final ObservableList<Map<String, Object>> masterData = FXCollections.observableArrayList();
    private final FilteredList<Map<String, Object>> filteredData = new FilteredList<>(masterData, p -> true);
    private final SortedList<Map<String, Object>> sortedData = new SortedList<>(filteredData);
    private final Map<String, Predicate<Map<String, Object>>> columnFilters = new HashMap<>();

    public Parent getView() {
        // --- 0. 动态添加高亮选中行（正在修改行）的深色 CSS 样式 ---
        String darkSelectedRowCss = "data:text/css," +
                ".table-row-cell:selected {" +
                "    -fx-background-color: #1E293B !important;" + // 正在修改的数据行：深蓝色/黑灰色背景
                "}" +
                ".table-row-cell:selected .table-cell {" +
                "    -fx-text-fill: #F8FAFC !important;" + // 亮白色加粗文字
                "    -fx-font-weight: bold;" +
                "}";
        root.getStylesheets().add(darkSelectedRowCss);

        // --- 1. 顶部配置栏 ---
        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(10));
        topBar.setAlignment(Pos.CENTER_LEFT);

        String lastPath = prefs.get(KEY_DB_PATH, "");
        dbPathField.setText(lastPath);
        dbPathField.setPromptText("请选择 SQLite 数据库文件 (.db)");
        dbPathField.setPrefWidth(280);

        Button browseBtn = new Button("选择数据库");
        browseBtn.setOnAction(e -> selectDatabaseFile());

        Button loadBtn = new Button("加载数据");
        loadBtn.setOnAction(e -> loadData());

        topBar.getChildren().addAll(
                new Label("数据库:"), dbPathField, browseBtn,
                new Label("表名:"), tableNameField,
                new Label("主键:"), pkColumnField,
                new Label("BLOB列:"), blobColumnField,
                loadBtn
        );
        root.setTop(topBar);

        // --- 2. 主体左右布局 ---
        HBox mainContent = new HBox(15);
        mainContent.setPadding(new Insets(10));

        // 左侧：数据列表（支持筛选 + 点击表头排序）
        VBox leftBox = new VBox(8, new Label("数据列表："), tableView);
        HBox.setHgrow(leftBox, Priority.ALWAYS);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        // 绑定 SortedList 以支持点击表头正序/逆序排列
        sortedData.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedData);

        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedRow = newSel;
                displayJsonForSelectedRow();
            }
        });

        // 右侧：JSON 编辑区（支持 Pretty 格式化显示/修改）
        jsonTextArea.setWrapText(true);
        jsonTextArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 13px;");
        jsonTextArea.setMinWidth(420);
        jsonTextArea.setPrefWidth(650);
        jsonTextArea.setMaxWidth(1000);
        VBox.setVgrow(jsonTextArea, Priority.ALWAYS);

        Button saveBtn = new Button("验证并保存修改");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");
        saveBtn.setOnAction(e -> saveJsonChanges());

        VBox rightBox = new VBox(8, new Label("解包 JSON 数据（可直接修改）："), jsonTextArea, saveBtn);
        VBox.setVgrow(rightBox, Priority.ALWAYS);

        mainContent.getChildren().addAll(leftBox, rightBox);
        root.setCenter(mainContent);

        return root;
    }

    private void selectDatabaseFile() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite DB", "*.db", "*.sqlite"));
        File file = chooser.showOpenDialog(root.getScene().getWindow());
        if (file != null) {
            String path = file.getAbsolutePath();
            dbPathField.setText(path);
            prefs.put(KEY_DB_PATH, path);
        }
    }

    /**
     * 完全加载/重置表（重新生成列和表头组件）
     */
    private void loadData() {
        String dbPath = dbPathField.getText().trim();
        String tableName = tableNameField.getText().trim();

        if (dbPath.isEmpty() || tableName.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "错误", "请配置有效的数据库路径和表名！");
            return;
        }

        try {
            dao = new GenericTableDAO(dbPath);
            List<Map<String, Object>> list = dao.executeQuery(tableName, "");

            // 1. 更新主数据源并清空筛选
            masterData.setAll(list);
            columnFilters.clear();
            applyFilters();

            // 2. 动态生成带筛选和排序能力的列
            tableView.getColumns().clear();
            if (!list.isEmpty()) {
                Map<String, Object> firstRow = list.get(0);
                for (String colName : firstRow.keySet()) {
                    boolean isNumeric = isColumnNumeric(list, colName);
                    TableColumn<Map<String, Object>, Object> col = createFilterableColumn(colName, isNumeric);
                    tableView.getColumns().add(col);
                }
            }

            prefs.put(KEY_DB_PATH, dbPath);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "读取失败", e.getMessage());
        }
    }

    /**
     * 局部重新加载数据库数据（保存成功后调用）：不重建UI，保持过滤条件、排序状态及当前选中行
     */
    private void reloadTableDataOnly() {
        if (dao == null) return;
        try {
            String tableName = tableNameField.getText().trim();
            String pkCol = pkColumnField.getText().trim();
            Object currentPkValue = (selectedRow != null) ? selectedRow.get(pkCol) : null;

            // 重新查询最新的数据列表
            List<Map<String, Object>> list = dao.executeQuery(tableName, "");
            masterData.setAll(list); // FilteredList 和 SortedList 会自动平滑响应更新

            // 重新定位并高亮选中原数据行
            if (currentPkValue != null) {
                for (Map<String, Object> row : tableView.getItems()) {
                    if (Objects.equals(row.get(pkCol), currentPkValue)) {
                        tableView.getSelectionModel().select(row);
                        selectedRow = row;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "数据刷新失败", e.getMessage());
        }
    }

    /**
     * 创建带 Excel 筛选框且支持表头正逆序排列的 TableColumn
     */
    private TableColumn<Map<String, Object>, Object> createFilterableColumn(String colName, boolean isNumeric) {
        TableColumn<Map<String, Object>, Object> col = new TableColumn<>();
        col.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().get(colName)));
        col.setSortable(true);

        // 设置自定义智能比较器，支持数值与字符串排序
        col.setComparator((o1, o2) -> {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            if (o1 instanceof Number && o2 instanceof Number) {
                return Double.compare(((Number) o1).doubleValue(), ((Number) o2).doubleValue());
            }
            try {
                double d1 = Double.parseDouble(o1.toString());
                double d2 = Double.parseDouble(o2.toString());
                return Double.compare(d1, d2);
            } catch (NumberFormatException ignored) {}
            return o1.toString().compareTo(o2.toString());
        });

        VBox headerBox = new VBox(4);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(4, 2, 4, 2));

        Label titleLabel = new Label(colName);
        titleLabel.setStyle("-fx-font-weight: bold;");

        if (isNumeric) {
            HBox rangeBox = new HBox(2);
            rangeBox.setAlignment(Pos.CENTER);

            TextField minField = new TextField();
            minField.setPromptText("起");
            minField.setPrefWidth(45);
            minField.setStyle("-fx-font-size: 10px; -fx-padding: 2;");
            minField.setOnMouseClicked(Event::consume); // 阻止事件冒泡，防止点击输入框触发表头排序

            TextField maxField = new TextField();
            maxField.setPromptText("止");
            maxField.setPrefWidth(45);
            maxField.setStyle("-fx-font-size: 10px; -fx-padding: 2;");
            maxField.setOnMouseClicked(Event::consume); // 阻止事件冒泡

            Runnable updateNumericFilter = () -> {
                String minStr = minField.getText().trim();
                String maxStr = maxField.getText().trim();

                if (minStr.isEmpty() && maxStr.isEmpty()) {
                    columnFilters.remove(colName);
                } else {
                    columnFilters.put(colName, row -> {
                        Object valObj = row.get(colName);
                        if (valObj == null) return false;
                        try {
                            double val = Double.parseDouble(valObj.toString());
                            if (!minStr.isEmpty()) {
                                double min = Double.parseDouble(minStr);
                                if (val < min) return false;
                            }
                            if (!maxStr.isEmpty()) {
                                double max = Double.parseDouble(maxStr);
                                if (val > max) return false;
                            }
                            return true;
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    });
                }
                applyFilters();
            };

            minField.textProperty().addListener((obs, o, n) -> updateNumericFilter.run());
            maxField.textProperty().addListener((obs, o, n) -> updateNumericFilter.run());

            rangeBox.getChildren().addAll(minField, new Label("-"), maxField);
            headerBox.getChildren().addAll(titleLabel, rangeBox);
            col.setPrefWidth(110);
        } else {
            TextField filterField = new TextField();
            filterField.setPromptText("筛选...");
            filterField.setStyle("-fx-font-size: 10px; -fx-padding: 2;");
            filterField.setOnMouseClicked(Event::consume); // 阻止事件冒泡，防止点击输入框触发表头排序

            filterField.textProperty().addListener((obs, o, n) -> {
                String filterText = (n == null) ? "" : n.trim().toLowerCase();
                if (filterText.isEmpty()) {
                    columnFilters.remove(colName);
                } else {
                    columnFilters.put(colName, row -> {
                        Object valObj = row.get(colName);
                        if (valObj == null) return false;
                        return valObj.toString().toLowerCase().contains(filterText);
                    });
                }
                applyFilters();
            });

            headerBox.getChildren().addAll(titleLabel, filterField);
            col.setPrefWidth(120);
        }

        col.setGraphic(headerBox);
        return col;
    }

    private void applyFilters() {
        filteredData.setPredicate(row -> {
            for (Predicate<Map<String, Object>> predicate : columnFilters.values()) {
                if (!predicate.test(row)) {
                    return false;
                }
            }
            return true;
        });
    }

    private boolean isColumnNumeric(List<Map<String, Object>> list, String colName) {
        for (Map<String, Object> row : list) {
            Object val = row.get(colName);
            if (val != null) {
                if (val instanceof Number) return true;
                try {
                    Double.parseDouble(val.toString());
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
        return false;
    }

    private void displayJsonForSelectedRow() {
        if (selectedRow == null) return;
        String blobCol = blobColumnField.getText().trim();

        try {
            Object blobData = selectedRow.get(blobCol);
            if (blobData instanceof byte[]) {
                String jsonStr = MsgPackUtil.bytesToJson((byte[]) blobData);
                jsonTextArea.setText(jsonStr);
            } else {
                jsonTextArea.setText("// 该列为空或不是二进制数据");
            }
        } catch (Exception e) {
            jsonTextArea.setText("// 解码失败: " + e.getMessage());
        }
    }

    private void saveJsonChanges() {
        if (selectedRow == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请先选择左侧表中的一条数据！");
            return;
        }

        String jsonText = jsonTextArea.getText();

        // 1. JSON 格式合法性校验
        if (!MsgPackUtil.isValidJson(jsonText)) {
            showAlert(Alert.AlertType.ERROR, "JSON 验证错误", "修改后的内容不是合法的 JSON 格式，请检查语法！");
            return;
        }

        // 2. 格式化 JSON (Pretty Print) 并回显至文本框
        String formattedJsonText;
        try {
            Object parsedJson = JSON.parse(jsonText);
            formattedJsonText = JSON.toJSONString(parsedJson, JSONWriter.Feature.PrettyFormat);
            jsonTextArea.setText(formattedJsonText); // 回显美化后的 JSON 字符串
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "JSON 格式化失败", "格式化 JSON 报错: " + e.getMessage());
            return;
        }

        // 3. 执行打包并保存到数据库
        try {
            String tableName = tableNameField.getText().trim();
            String pkCol = pkColumnField.getText().trim();
            String blobCol = blobColumnField.getText().trim();
            Object pkValue = selectedRow.get(pkCol);

            dao.updateBlobData(tableName, pkCol, pkValue, blobCol, formattedJsonText);

            showAlert(Alert.AlertType.INFORMATION, "成功", "二进制数据打包并保存至数据库成功！");

            // 4. 局部刷新数据：不重建 UI，保留已有的筛选条件、排序和选中行
            reloadTableDataOnly();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "保存失败", e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}