package com.vaadin.integration.eclipse.flow.service;

import com.google.gson.JsonObject;

public class NotificationAnalytic {
    private static final String SHOW_NOTIFICATION_EVENT_TYPE = "Show notification";
    private static final String HIDE_NOTIFICATION_EVENT_TYPE = "Hide notification";
    private static final String LINK_CLICK_NOTIFICATION_EVENT_TYPE = "Notification link click";

    private static final String linkPropParam = "Link";

    public static boolean trackShowNotification(boolean visible) {
        String data = AmplitudeService
                .generateEventData(visible ? SHOW_NOTIFICATION_EVENT_TYPE
                        : HIDE_NOTIFICATION_EVENT_TYPE, null);
        return AmplitudeService.sendTracking(data);
    }

    public static boolean trackNotificationLinkClick(String link) {
        JsonObject eventProps = null;
        if (link != null) {
            JsonObject props = new JsonObject();
            props.addProperty(linkPropParam, link);
            eventProps = props;
        }
        String data = AmplitudeService.generateEventData(
                LINK_CLICK_NOTIFICATION_EVENT_TYPE, eventProps);
        return AmplitudeService.sendTracking(data);
    }
}
