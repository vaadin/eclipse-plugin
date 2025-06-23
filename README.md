# eclipse-plugin

This repository contains a minimal Eclipse plugin example for Vaadin.
The plugin now starts a small REST service on a random TCP port during startup to allow communication with Vaadin Copilot. The service exposes a unique `/api/{serviceName}` endpoint and the full endpoint URL is available in the `vaadin.copilot.endpoint` system property.

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
