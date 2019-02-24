package de.kcct.hi5.swt.cef;

import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.chromium.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.osgi.service.component.annotations.Component;

import de.kcct.hi5.e4.EntryPointHandler;
import de.kcct.hi5.e4.EntryPointHandlerFactory;

@Component(property = EntryPointHandlerFactory.KEY + "=" + CEFSWTEntryPointHandlerFactory.CEFSWT)
public class CEFSWTEntryPointHandlerFactory implements EntryPointHandlerFactory {
	public static final String CEFSWT = "swt-cef";

	@Override
	public EntryPointHandler create(Context context) {
		return new EntryPointHandler() {
			private boolean exit = false;

			@Override
			public void start() {
				URL url = calcURL(context);
				Display display = new Display();
				Shell shell = new Shell(display);
				shell.setSize(1366, 768);
				shell.setLayout(new FillLayout());

				Browser browser = new Browser(shell, SWT.None);
				browser.setUrl(url.toString());
				browser.addTitleListener(ev -> {
					shell.setText(ev.title);
				});

				shell.open();
				while (!shell.isDisposed() && !exit) {
					if (!display.readAndDispatch())
						display.sleep();
				}
				display.dispose();
			}

			@Override
			public void stop() {
				exit = true;
			}
		};
	}
}
