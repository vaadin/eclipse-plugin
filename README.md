# eclipse-plugin

This repository contains a minimal Eclipse plug-in example for Vaadin.
At workbench startup the plug-in is activated via an `org.eclipse.ui.startup` extension. The `BundleActivator` registers a servlet using the OSGi `HttpService`. The servlet exposes a unique `/api/{serviceName}` endpoint on the container's HTTP server, and the full endpoint URL is stored in the `vaadin.copilot.endpoint` system property.

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
