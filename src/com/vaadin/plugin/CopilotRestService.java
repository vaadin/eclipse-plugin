package com.vaadin.plugin;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.UUID;

/**
 * Registers a simple servlet for Vaadin Copilot integration using the OSGi
 * {@link HttpService}. The servlet is bound to a unique "/api/{serviceName}"
 * alias and the full endpoint URL can be retrieved via {@link #getEndpoint()}.
 */
public class CopilotRestService {
    private HttpService httpService;
    private ServiceReference<HttpService> serviceRef;
    private String serviceName;
    private String endpoint;

    public void start(BundleContext context) throws ServletException, NamespaceException {
        serviceName = "copilot-" + UUID.randomUUID();
        serviceRef = context.getServiceReference(HttpService.class);
        if (serviceRef == null) {
            throw new IllegalStateException("HttpService not available");
        }
        httpService = context.getService(serviceRef);
        String alias = "/api/" + serviceName;
        httpService.registerServlet(alias, new CopilotServlet(), null, null);
        String port = context.getProperty("org.osgi.service.http.port");
        if (port == null) {
            port = "8080";
        }
        endpoint = "http://localhost:" + port + alias;
        System.out.println("Copilot REST service registered at " + endpoint);
    }

    public void stop(BundleContext context) {
        if (httpService != null) {
            String alias = "/api/" + serviceName;
            httpService.unregister(alias);
            context.ungetService(serviceRef);
            httpService = null;
            serviceRef = null;
        }
    }

    public String getEndpoint() {
        return endpoint;
    }

}
