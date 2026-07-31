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
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

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

    // --- 将 TextField 替换为 ComboBox 下拉列表 ---
    private final ComboBox<String> tableNameComboBox = new ComboBox<>();

    private final TextField pkColumnField = new TextField("nid");
    private final TextField blobColumnField = new TextField("数据");

    private GenericTableDAO dao;
    private Map<String, Object> selectedRow;

    // 记录最后一次在左侧表格中编辑的行及其原主键
    private Map<String, Object> lastEditedRow = null;
    Object lastEditedPkValue = null;

    // 数据源与筛选/排序容器
    private final ObservableList<Map<String, Object>> masterData = FXCollections.observableArrayList();
    private final FilteredList<Map<String, Object>> filteredData = new FilteredList<>(masterData, p -> true);
    private final SortedList<Map<String, Object>> sortedData = new SortedList<>(filteredData);
    private final Map<String, Predicate<Map<String, Object>>> columnFilters = new HashMap<>();

    public Parent getView() {
        String darkSelectedRowCss = "data:text/css," +
                ".table-row-cell:selected {" +
                "    -fx-background-color: #1E293B !important;" +
                "}" +
                ".table-row-cell:selected .table-cell {" +
                "    -fx-text-fill: #F8FAFC !important;" +
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
        dbPathField.setPrefWidth(260);

        tableNameComboBox.setPrefWidth(120);
        tableNameComboBox.setPromptText("选择表名");

        Button browseBtn = new Button("选择数据库");
        browseBtn.setOnAction(e -> selectDatabaseFile());

        Button loadBtn = new Button("加载数据");
        loadBtn.setOnAction(e -> loadData());

        topBar.getChildren().addAll(
                new Label("数据库:"), dbPathField, browseBtn,
                new Label("表名:"), tableNameComboBox,
                new Label("主键:"), pkColumnField,
                new Label("BLOB列:"), blobColumnField,
                loadBtn
        );
        root.setTop(topBar);

        // 界面初始化时，若上一次有保存路径，自动扫描一次表名
        if (!lastPath.isEmpty() && new File(lastPath).exists()) {
            scanTableNames(lastPath);
        }

        // --- 2. 主体左右布局 ---
        HBox mainContent = new HBox(15);
        mainContent.setPadding(new Insets(10));

        tableView.setEditable(true);

        Button saveTableBtn = new Button("保存表格修改 (仅保存最后编辑行)");
        saveTableBtn.setMaxWidth(Double.MAX_VALUE);
        saveTableBtn.setStyle("-fx-background-color: #0284C7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");
        saveTableBtn.setOnAction(e -> saveTableChanges());

        VBox leftBox = new VBox(8, new Label("数据列表（双击单元格可直接修改）："), tableView, saveTableBtn);
        HBox.setHgrow(leftBox, Priority.ALWAYS);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        sortedData.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedData);

        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedRow = newSel;
                displayJsonForSelectedRow();
            }
        });

        // 右侧 JSON 区域
        jsonTextArea.setWrapText(true);
        jsonTextArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 13px;");
        jsonTextArea.setMinWidth(420);
        jsonTextArea.setPrefWidth(650);
        jsonTextArea.setMaxWidth(1000);
        VBox.setVgrow(jsonTextArea, Priority.ALWAYS);

        Button saveJsonBtn = new Button("验证并保存二进制数据");
        saveJsonBtn.setMaxWidth(Double.MAX_VALUE);
        saveJsonBtn.setStyle("-fx-background-color: #16A34A; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");
        saveJsonBtn.setOnAction(e -> saveJsonChanges());

        VBox rightBox = new VBox(8, new Label("解包 JSON 数据（可直接修改）："), jsonTextArea, saveJsonBtn);
        VBox.setVgrow(rightBox, Priority.ALWAYS);

        mainContent.getChildren().addAll(leftBox, rightBox);
        root.setCenter(mainContent);

        return root;
    }

    /**
     * 选择数据库文件并自动扫描里面的表名
     */
    private void selectDatabaseFile() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite DB", "*.db", "*.sqlite"));

        String currentPath = dbPathField.getText().trim();
        if (!currentPath.isEmpty()) {
            File currentFile = new File(currentPath);
            File initialDir = null;

            if (currentFile.exists()) {
                initialDir = currentFile.isDirectory() ? currentFile : currentFile.getParentFile();
            } else {
                File parent = currentFile.getParentFile();
                if (parent != null && parent.exists() && parent.isDirectory()) {
                    initialDir = parent;
                }
            }

            if (initialDir != null && initialDir.exists()) {
                chooser.setInitialDirectory(initialDir);
            }
        }

        File file = chooser.showOpenDialog(root.getScene().getWindow());
        if (file != null) {
            String path = file.getAbsolutePath();
            dbPathField.setText(path);
            prefs.put(KEY_DB_PATH, path);

            // 选择文件后自动扫描表名
            scanTableNames(path);
        }
    }

    /**
     * 自动扫描数据库中的所有表名并填充到 ComboBox 中
     */
    private void scanTableNames(String dbPath) {
        try {
            dao = new GenericTableDAO(dbPath);
            List<String> tables = dao.getAllTableNames();

            tableNameComboBox.getItems().clear();
            if (tables.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "提示", "该数据库文件中未找到任何可用数据表！");
            } else {
                tableNameComboBox.getItems().addAll(tables);
                tableNameComboBox.getSelectionModel().selectFirst(); // 默认选中第一个表
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "扫描表名失败", e.getMessage());
        }
    }

    private void loadData() {
        String dbPath = dbPathField.getText().trim();
        String tableName = tableNameComboBox.getValue();

        if (dbPath.isEmpty() || tableName == null || tableName.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "错误", "请选择有效的数据库文件和表名！");
            return;
        }

        try {
            if (dao == null) {
                dao = new GenericTableDAO(dbPath);
            }
            List<Map<String, Object>> list = dao.executeQuery(tableName, "");

            masterData.setAll(list);
            columnFilters.clear();
            lastEditedRow = null;
            lastEditedPkValue = null;
            applyFilters();

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

    private void reloadTableDataOnly() {
        if (dao == null) return;
        try {
            String tableName = tableNameComboBox.getValue();
            String pkCol = pkColumnField.getText().trim();
            Object currentPkValue = (selectedRow != null) ? selectedRow.get(pkCol) : null;

            List<Map<String, Object>> list = dao.executeQuery(tableName, "");
            masterData.setAll(list);

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

    private TableColumn<Map<String, Object>, Object> createFilterableColumn(String colName, boolean isNumeric) {
        TableColumn<Map<String, Object>, Object> col = new TableColumn<>();
        col.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().get(colName)));
        col.setSortable(true);

        String blobCol = blobColumnField.getText().trim();
        boolean isBlob = colName.equalsIgnoreCase(blobCol);

        if (!isBlob) {
            col.setEditable(true);
            col.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<Object>() {
                @Override
                public String toString(Object object) {
                    return object == null ? "" : object.toString();
                }

                @Override
                public Object fromString(String string) {
                    return string;
                }
            }));

            col.setOnEditCommit(event -> {
                Map<String, Object> row = event.getRowValue();
                String pkCol = pkColumnField.getText().trim();

                if (lastEditedRow != row) {
                    lastEditedRow = row;
                    lastEditedPkValue = row.get(pkCol);
                }

                row.put(colName, event.getNewValue());
            });
        } else {
            col.setEditable(false);
        }

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
            minField.setOnMouseClicked(Event::consume);

            TextField maxField = new TextField();
            maxField.setPromptText("止");
            maxField.setPrefWidth(45);
            maxField.setStyle("-fx-font-size: 10px; -fx-padding: 2;");
            maxField.setOnMouseClicked(Event::consume);

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
            filterField.setOnMouseClicked(Event::consume);

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

    private void saveTableChanges() {
        if (lastEditedRow == null || lastEditedPkValue == null) {
            showAlert(Alert.AlertType.INFORMATION, "提示", "当前没有在左侧表格中编辑过任何数据！");
            return;
        }

        try {
            String tableName = tableNameComboBox.getValue();
            String pkCol = pkColumnField.getText().trim();
            String blobCol = blobColumnField.getText().trim();

            dao.updateRowData(tableName, pkCol, lastEditedPkValue, lastEditedRow, blobCol);

            showAlert(Alert.AlertType.INFORMATION, "成功", "表格最后编辑行数据保存成功！");

            lastEditedRow = null;
            lastEditedPkValue = null;
            reloadTableDataOnly();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "保存失败", e.getMessage());
        }
    }

    private void saveJsonChanges() {
        if (selectedRow == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请先选择左侧表中的一条数据！");
            return;
        }

        String jsonText = jsonTextArea.getText();

        if (!MsgPackUtil.isValidJson(jsonText)) {
            showAlert(Alert.AlertType.ERROR, "JSON 验证错误", "修改后的内容不是合法的 JSON 格式，请检查语法！");
            return;
        }

        String formattedJsonText;
        try {
            Object parsedJson = JSON.parse(jsonText);
            formattedJsonText = JSON.toJSONString(parsedJson, JSONWriter.Feature.PrettyFormat);
            jsonTextArea.setText(formattedJsonText);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "JSON 格式化失败", "格式化 JSON 报错: " + e.getMessage());
            return;
        }

        try {
            String tableName = tableNameComboBox.getValue();
            String pkCol = pkColumnField.getText().trim();
            String blobCol = blobColumnField.getText().trim();
            Object pkValue = selectedRow.get(pkCol);

            dao.updateBlobData(tableName, pkCol, pkValue, blobCol, formattedJsonText);

            showAlert(Alert.AlertType.INFORMATION, "成功", "二进制数据打包并保存至数据库成功！");
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