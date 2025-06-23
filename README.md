# eclipse-plugin

This repository contains a minimal Eclipse plugin example for Vaadin.
The plug-in registers a servlet using the OSGi `HttpService` when the workbench starts. The servlet exposes a unique `/api/{serviceName}` endpoint on the container's HTTP server, and the full endpoint URL is available in the `vaadin.copilot.endpoint` system property.

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
