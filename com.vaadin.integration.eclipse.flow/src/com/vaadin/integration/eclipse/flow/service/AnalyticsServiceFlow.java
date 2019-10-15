package com.vaadin.integration.eclipse.flow.service;

import java.util.Optional;

import com.google.gson.JsonObject;

public class AnalyticsServiceFlow {

    public static final String INSTALL_EVENT_TYPE = "Install";
    private static final String CREATE_EVENT_TYPE = "Create project";

    private static final String starterPropParam = "Starter";
    private static final String stackPropParam = "Tech stack";

    public static boolean track(String eventType) {
        return AmplitudeService
                .sendTracking(createEventData(eventType, null, null));
    }

    public static boolean trackProjectCreate(String starter, String techStack) {
        return AmplitudeService.sendTracking(
                createEventData(CREATE_EVENT_TYPE, starter, techStack));
    }

    private static String createEventData(String eventType, String starter,
            String techStack) {
        Optional<JsonObject> eventProps = Optional.empty();
        if (starter != null && techStack != null) {
            JsonObject props = new JsonObject();
            props.addProperty(starterPropParam, starter);
            props.addProperty(stackPropParam, techStack);
            eventProps = Optional.of(props);
        }
        return AmplitudeService.generateEventData(eventType, eventProps);
    }

}
