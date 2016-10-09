package de.metadocks.hi5.e4.internal;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class JFXBrowserApp {
	private static String url;
	private static String title;

	public static class JFXApp extends Application {

		@Override
		public void start(final Stage primaryStage) throws Exception {
			primaryStage.setTitle(title);
			StackPane root = new StackPane();
			WebView webview = new WebView();
			webview.getEngine().load(url);
			root.getChildren().add(webview);
			primaryStage.setScene(new Scene(root, 800, 600));
			primaryStage.show();
		}
	}

	public static void run(String title, String url) {
		JFXBrowserApp.title = title;
		JFXBrowserApp.url = url;
		Application.launch(JFXApp.class);
	}
}
