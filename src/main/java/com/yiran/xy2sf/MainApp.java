package com.yiran.xy2sf;

import com.yiran.xy2sf.ui.MainViewController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainViewController controller = new MainViewController();
        Scene scene = new Scene(controller.getView(), 1024, 600);

        primaryStage.setTitle("通用 SQLite MsgPack 数据编辑器");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}