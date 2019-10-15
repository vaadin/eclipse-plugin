package com.vaadin.integration.eclipse.flow.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

public class Tracker {

    public static final String INSTALL_EVENT_TYPE = "Install";
    private static final String CREATE_EVENT_TYPE = "Create project";

    private static final String starterPropParam = "Starter";
    private static final String stackPropParam = "Tech stack";

    public static final String UTM_TRACKING_PARAM = "?utm_source=eclipse&utm_medium=notification&utm_campaign=eclipse_notifications";

    private static final String SHOW_NOTIFICATION_EVENT_TYPE = "Show notification";
    private static final String HIDE_NOTIFICATION_EVENT_TYPE = "Hide notification";
    private static final String LINK_CLICK_NOTIFICATION_EVENT_TYPE = "Notification link click";

    private static final String linkPropParam = "Link";

    public static boolean track(String eventType) {
        return AmplitudeService.sendTracking(eventType, null);
    }

    public static boolean trackProjectCreate(String starter, String techStack) {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(starterPropParam, starter));
        params.add(new BasicNameValuePair(stackPropParam, techStack));
        return AmplitudeService.sendTracking(CREATE_EVENT_TYPE, params);
    }

    public static boolean trackShowNotification(boolean visible) {
        return AmplitudeService
                .sendTracking(visible ? SHOW_NOTIFICATION_EVENT_TYPE
                        : HIDE_NOTIFICATION_EVENT_TYPE, null);
    }

    public static boolean trackNotificationLinkClick(String link) {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(linkPropParam, link));
        return AmplitudeService.sendTracking(LINK_CLICK_NOTIFICATION_EVENT_TYPE,
                params);
    }

}
