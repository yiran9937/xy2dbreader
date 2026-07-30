# 🛠️ XY2 DB Editor - 通用 SQLite MsgPack/JSON 可视化编辑器

一款基于 **JavaFX 19** 开发的桌面端 SQLite 数据库可视编辑工具，专为处理包含 **MsgPack 二进制压缩（BLOB）** 数据的表结构而设计。

![Java Version](https://img.shields.io/badge/Java-19%2B-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-19-blue)
![License](https://img.shields.io/badge/License-MIT-green)

---

## 🌟 核心特性

* **📦 二进制数据解包/打包**：自动提取数据库中的 MsgPack BLOB 字节流，反序列化为 Pretty 格式的 JSON 供可视化修改；保存时自动验证 JSON 语法并重新打包存入数据库。
* **🔍 Excel 风格智能表头筛选**：
    * **文本列**：支持实时字符串模糊匹配。
    * **数值列**：自动提供“起 - 止”范围筛选。
    * 多列筛选条件自动取交集（AND 关系）。
* **📊 交互式表格与排序**：支持点击表头正逆序排列，正在修改的数据行高亮（深色模式）显示，方便定位。
* **⚙️ 高度抽象与多表通用**：支持在界面动态配置数据库路径、目标表名、主键字段名及 BLOB 字段名，一套软件轻松适配多个数据表（如 `物品`、`角色` 等）。
* **💾 路径持久化记忆**：使用 JDK Preferences API，自动记住上一次选择的数据库路径，开箱即用。
* **⚡ 局部无刷新重载**：修改保存数据后，仅局部更新数据库内容，自动保持当前的筛选条件、排序状态及选中行。

---

## 🛠️ 技术栈

* **GUI 框架**：JavaFX 19 (`javafx-controls`, `javafx-fxml`)
* **数据库**：SQLite 3 (`sqlite-jdbc`)
* **数据序列化**：MessagePack (`msgpack-core`, `jackson-dataformat-msgpack`) + Fastjson2
* **构建工具**：Maven 3.x
* **运行环境**：OpenJDK 19

---

## 🚀 快速启动

### 前置要求
* JDK 19+
* Maven 3.8+

### 1. 克隆项目
```bash
git clone [https://github.com/yiran9937/xy2dbreader.git](https://github.com/yiran9937/xy2dbreader.git)
cd xy2dbreader