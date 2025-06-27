# eclipse-plugin

This repository contains a minimal Eclipse plug-in example for Vaadin.
At workbench startup the plug-in is activated via an `org.eclipse.ui.startup` extension. The `BundleActivator` starts a small REST service using the JDK `HttpServer`. The server exposes a `/api/copilot` endpoint and its URL is stored in the `vaadin.copilot.endpoint` system property.

## Building the project

Building requires Maven 3.9 or newer and a JDK 17 installation. Once those prerequisites are available, run the following command:

```bash
mvn install
```

This will compile the plug-in and create the P2 metadata in the `target` folder.

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
