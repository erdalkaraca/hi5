# hi5
HTML5 Renderer for Eclipse 4 applications

![Hi5 Architecture Overview](docs/overview-architecture.png "Hi5 Architecture Overview")

# Server side
On the server side (backend) the infrastructure of the Eclipse RCP platform is handling requests that the web client doing. This includes collecting all available E4 fragments and merging them into the application model. Next, the merged application model is transformed into a HTML DOM representation which then sent to the client via HTTP.

With the Eclipse Communication Framework (ECF) there is also great support for a service driven development approach as OSGi services can serve as web services, for example, using ECF's JAX-RS integration.

# Client side
On the client side you can use your favorite framework/library to provide a smooth and reactive UI. By default, jQuery, jQuery UI and jQuery mobile are used.

# Deployment modes
The application can be deployed in several modes:
typical server/client mode: Eclipse RCP backend runs on a server and the client runs in a web browser
local mode: Eclipse RCP backend runs locally and the client runs in a JavaFX WebView instance

# Demo
The following screenshot is a demo showing the various jQuery UI widgets running in a JavaFX WebView instance.

![jQuery UI widgets](plugins/de.metadocks.hi5.demo/screenshots/hi5-jquery-widgets-demo.png "jQuery UI widgets")

# Current Status
[x] means supported, [ ] means not yet implemented

| E4 API | Hi5 |
| --- | --- |
| App model, fragments | [x] |
| Addons | [x] |
| Window Menu | [ ] |
| IEclipseContext/Scoping | [ ] |
| DI | [ ] |
| Data Binding | [ ] |
| Key Bindings | [ ] |
| ECommandService/EHandlerService (commands, handlers) | [ ] |
| ESelectionService (part selections) | [ ] |
| EMenuService (context menus) | [ ] |

# Dependencies

| Project | Licence | Used for | Required? |
| --- | --- | --- | --- |
| [Eclipse 4](https://wiki.eclipse.org/Eclipse4) | EPL | defining/managing contributions of the single-page web application | yes |
| [Eclipse Modeling Framework](https://eclipse.org/modeling/emf/) | EPL | used by Eclipse 4 to model the workbench | yes |
| [GoldenLayout](https://www.golden-layout.com) | MIT | docking framework on client side | yes |
| [jQuery](https://jquery.com) | MIT | client side DOM manipulation | yes |
| [jQuery UI](https://jqueryui.com) | MIT | client side UI widgets | no |
| [jQuery mobile](https://jquerymobile.com) | MIT | optimizations for mobiledevices | no |
| [Eclipse Communications Framework](https://www.eclipse.org/ecf/) | EPL | web services support | no |
| [Eclipse Texo](https://wiki.eclipse.org/Texo) | EPL | de/serialization of EMF models from/to server/client in JSON format | no |


