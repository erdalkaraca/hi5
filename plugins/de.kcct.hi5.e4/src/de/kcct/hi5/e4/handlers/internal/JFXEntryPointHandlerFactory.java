//package de.kcct.hi5.e4.handlers.internal;
//
//import java.net.URL;
//
//import org.osgi.service.component.annotations.Component;
//
//import de.kcct.hi5.e4.EntryPointHandler;
//import de.kcct.hi5.e4.EntryPointHandlerFactory;
//import javafx.application.Application;
//import javafx.scene.Scene;
//import javafx.scene.layout.StackPane;
//import javafx.scene.web.WebView;
//import javafx.stage.Stage;
//
//@Component(property = EntryPointHandlerFactory.KEY + "=" + JFXEntryPointHandlerFactory.JFX)
//public class JFXEntryPointHandlerFactory implements EntryPointHandlerFactory {
//	public static final String JFX = "jfx-webview";
//	private static URL url;
//	private static String title;
//
//	@Override
//	public EntryPointHandler create(Context context) {
//		return new EntryPointHandler() {
//
//			@Override
//			public void start() {
//				URL url = calcURL(context);
//				JFXEntryPointHandlerFactory.url = url;
//				Application.launch(JFXApp.class);
//			}
//
//			@Override
//			public void stop() {
//			}
//		};
//	}
//
//	public static class JFXApp extends Application {
//
//		@Override
//		public void start(final Stage primaryStage) throws Exception {
//			primaryStage.setTitle(title);
//			StackPane root = new StackPane();
//			WebView webview = new WebView();
//			webview.getEngine().load(url.toString());
//			root.getChildren().add(webview);
//			primaryStage.setScene(new Scene(root, 800, 600));
//			primaryStage.show();
//		}
//	}
//}
