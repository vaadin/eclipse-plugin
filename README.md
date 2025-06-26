# eclipse-plugin

This repository contains a minimal Eclipse plug-in example for Vaadin.
At workbench startup the plug-in is activated via an `org.eclipse.ui.startup` extension. The `BundleActivator` starts a small REST service using the JDK `HttpServer`. The server exposes a `/api/copilot` endpoint and its URL is stored in the `vaadin.copilot.endpoint` system property.

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
