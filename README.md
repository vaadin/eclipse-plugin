# eclipse-plugin

This repository contains a minimal Eclipse plug-in example for Vaadin.
At workbench startup the plug-in is activated via an `org.eclipse.ui.startup` extension. The `BundleActivator` starts a small REST service using the JDK `HttpServer`. The server exposes a `/api/copilot` endpoint and its URL is stored in the `vaadin.copilot.endpoint` system property.

## Building the project

Building requires Maven 3.9 or newer and a JDK 17 installation. Once those prerequisites are available, run the following command:

```bash
mvn install
```

This will compile the plug-in and create the P2 metadata in the `target` folder.

## Debug the project

- Install Eclipse JavaEE (better than RCP to run vaadin application in the same environment)
- Create empty workspace
- Optional - Install current Vaadin plugin from market place to get familiar with it and check that everything works. This plugin is part of the "Vaadin IDE integration" package, you can select only this one. 
- Import this repo as maven project
- Debug the vaadin-eclipse-plugin module as Eclipse Application
- In the opened IDE create a new project (Vaadin project should appear in the list)
- Debug the application (Hotswap or regular debug from Debug as... menu) 


## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
