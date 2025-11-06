package com.amplitude;

import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class Event {
    /** A unique identifier for your event. */
    public String eventType;

    /**
     * A readable ID specified by you. Must have a minimum length of 5 characters. Required unless device_id is present.
     */
    public String userId;

    /**
     * A device-specific identifier, such as the Identifier for Vendor on iOS. Required unless user_id is present. If a
     * device_id is not sent with the event, it will be set to a hashed version of the user_id.
     */
    public String deviceId;

    /**
     * The timestamp of the event in milliseconds since epoch. If time is not sent with the event, it will be set to the
     * request upload time.
     */
    public long timestamp = System.currentTimeMillis();

    /** The current Latitude of the user. */
    public double locationLat;

    /** The current Longitude of the user. */
    public double locationLng;

    /** The current version of your application. */
    public String appVersion;

    /**
     * Legacy SDK API, only for use with older API. The application's version name.
     */
    public String versionName;

    /** Platform of the device. */
    public String platform;

    /**
     * The name of the mobile operating system or browser that the user is using.
     */
    public String osName;

    /** The version of the mobile operating system or browser the user is using. */
    public String osVersion;

    /** The device brand that the user is using. */
    public String deviceBrand;

    /** The device manufacturer that the user is using. */
    public String deviceManufacturer;

    /** The device model that the user is using. */
    public String deviceModel;

    /** The carrier that the user is using. */
    public String carrier;

    /** The current country of the user. */
    public String country;

    /** The current region of the user. */
    public String region;

    /** The current city of the user. */
    public String city;

    /** The current Designated Market Area of the user. */
    public String dma;

    /** (iOS) Identifier for Advertiser. */
    public String idfa;

    /** (iOS) Identifier for Vendor. */
    public String idfv;

    /** (Android) Google Play Services advertising ID */
    public String adid;

    /** (Android) Android ID (not the advertising ID) */
    public String androidId;

    /** The (human) language set by the user. */
    public String language;

    /** The partner id of event */
    public String partnerId;

    /**
     * The IP address of the user. Use "$remote" to use the IP address on the upload request. We will use the IP address
     * to reverse lookup a user's location (city, country, region, and DMA). Amplitude has the ability to drop the
     * location and IP address from events once it reaches our servers. You can submit a request to our platform
     * specialist team here to configure this for you.
     */
    public String ip;

    /**
     * A dictionary of key-value pairs that represent additional data to be sent along with the event. You can store
     * property values in an array. Date values are transformed into string values. Object depth may not exceed 40
     * layers.
     */
    public JsonObject eventProperties;

    /**
     * A dictionary of key-value pairs that represent additional data tied to the user. You can store property values in
     * an array. Date values are transformed into string values. Object depth may not exceed 40 layers.
     */
    public JsonObject userProperties;

    /**
     * The price of the item purchased. Required for revenue data if the revenue field is not sent. You can use negative
     * values to indicate refunds.
     */
    public Double price;

    /** The quantity of the item purchased. Defaults to 1 if not specified. */
    public int quantity;

    /**
     * revenue = price * quantity. If you send all 3 fields of price, quantity, and revenue, then (price * quantity)
     * will be used as the revenue value. You can use negative values to indicate refunds.
     */
    public Double revenue;

    /**
     * An identifier for the item purchased. You must send a price and quantity or revenue with this field.
     */
    public String productId;

    /**
     * The type of revenue for the item purchased. You must send a price and quantity or revenue with this field.
     */
    public String revenueType;

    /**
     * The 3 letter revenue currency code for the item purchased.
     */
    public String currency;

    /**
     * An incrementing counter to distinguish events with the same user_id and timestamp from each other. We recommend
     * you send an event_id, increasing over time, especially if you expect events to occur simultanenously.
     */
    public int eventId;

    /**
     * The start time of the session in milliseconds since epoch (Unix Timestamp), necessary if you want to associate
     * events with a particular system. A session_id of -1 is the same as no session_id specified.
     */
    public long sessionId = -1;

    /**
     * A unique identifier for the event. We will deduplicate subsequent events sent with an insert_id we have already
     * seen before within the past 7 days. We recommend generation a UUID or using some combination of device_id,
     * user_id, event_type, event_id, and time.
     */
    public String insertId = UUID.randomUUID().toString();

    /**
     * This feature is only available to Enterprise customers who have purchased the Accounts add-on. This field adds a
     * dictionary of key-value pairs that represent groups of users to the event as an event-level group. You can only
     * track up to 5 groups.
     */
    public JsonObject groups;

    /**
     * This feature is only available to Enterprise customers who have purchased the Accounts add-on. A dictionary of
     * key-value pairs that represent data tied to the groups.
     */
    public JsonObject groupProperties;

    /**
     * The tracking plan.
     */
    public Plan plan;

    /**
     * The ingestion metadata.
     */
    public IngestionMetadata ingestionMetadata;

    /**
     * Callback for Event
     */
    protected AmplitudeCallbacks callback;

    /**
     * Create a new event with the required items. `eventType` is required, and at least one of `userId` or `deviceId`
     * is required. This method sets deviceId to null.
     *
     * @param eventType
     *            A unique identifier for your event
     * @param userId
     *            A readable ID specified by you. Must have a minimum length of 5 characters. Required unless device_id
     *            is present.
     */
    public Event(String eventType, String userId) {
        this(eventType, userId, null);
    }

    /**
     * Create a new event with the required items. `eventType` is required, and at least one of `userId` or `deviceId`
     * is required.
     *
     * @param eventType
     *            A unique identifier for your event
     * @param userId
     *            A readable ID specified by you. Must have a minimum length of 5 characters. Required unless device_id
     *            is present.
     * @param deviceId
     *            A device-specific identifier, such as the Identifier for Vendor on iOS. Required unless user_id is
     *            present. If a device_id is not sent with the event, it will be set to a hashed version of the user_id.
     */
    public Event(String eventType, String userId, String deviceId) {
        this.eventType = eventType;
        if (userId == null && deviceId == null) {
            throw new IllegalArgumentException("Event must have one defined userId and/or deviceId");
        }
        this.userId = userId;
        this.deviceId = deviceId;
    }

    /** @return the JsonObject that represents the event data of this event */
    public JsonObject toJsonObject() {
        JsonObject event = new JsonObject();
        try {
            event.addProperty("event_type", eventType);
            event.add("user_id", replaceWithJsonNull(userId));
            event.add("device_id", replaceWithJsonNull(deviceId));
            event.addProperty("time", timestamp);
            event.addProperty("location_lat", locationLat);
            event.addProperty("location_lng", locationLng);
            event.addProperty("app_version", appVersion);
            event.add("version_name", replaceWithJsonNull(versionName));
            event.addProperty("library", Constants.SDK_LIBRARY + "/" + Constants.SDK_VERSION);
            event.add("platform", replaceWithJsonNull(platform));
            event.add("os_name", replaceWithJsonNull(osName));
            event.add("os_version", replaceWithJsonNull(osVersion));
            event.add("device_brand", replaceWithJsonNull(deviceBrand));
            event.add("device_manufacturer", replaceWithJsonNull(deviceManufacturer));
            event.add("device_model", replaceWithJsonNull(deviceModel));
            event.add("carrier", replaceWithJsonNull(carrier));
            event.add("country", replaceWithJsonNull(country));
            event.add("region", replaceWithJsonNull(region));
            event.add("city", replaceWithJsonNull(city));
            event.add("dma", replaceWithJsonNull(dma));
            event.add("idfa", replaceWithJsonNull(idfa));
            event.add("idfv", replaceWithJsonNull(idfv));
            event.add("adid", replaceWithJsonNull(adid));
            event.add("android_id", replaceWithJsonNull(androidId));
            event.add("language", replaceWithJsonNull(language));
            event.add("partner_id", replaceWithJsonNull(partnerId));
            event.add("ip", replaceWithJsonNull(ip));
            event.add("event_properties", (eventProperties == null) ? new JsonObject() : truncate(eventProperties));
            event.add("user_properties", (userProperties == null) ? new JsonObject() : truncate(userProperties));

            boolean shouldLogRevenueProps = (revenue != null || price != null);
            if (shouldLogRevenueProps) {
                int eventQuantity = quantity > 0 ? quantity : 1;
                event.add("price", replaceWithJsonNull(price));
                event.addProperty("quantity", eventQuantity);
                event.add("revenue", replaceWithJsonNull(revenue));
                event.addProperty("productId", productId);
                event.addProperty("revenueType", revenueType);
                event.addProperty("currency", currency);
            }

            event.add("event_id", replaceWithJsonNull(eventId));
            event.addProperty("session_id", sessionId); // session_id = -1 if outOfSession = true;
            event.addProperty("insert_id", insertId);
            event.add("groups", (groups == null) ? new JsonObject() : truncate(groups));
            event.add("group_properties", (groupProperties == null) ? new JsonObject() : truncate(groupProperties));

            if (plan != null) {
                event.add("plan", plan.toJsonObject());
            }

            if (ingestionMetadata != null) {
                event.add("ingestion_metadata", ingestionMetadata.toJsonObject());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return event;
    }

    /** internal method */
    protected JsonElement replaceWithJsonNull(Object obj) {
        return obj == null ? JsonNull.INSTANCE : new JsonPrimitive(obj.toString());
    }

    protected JsonElement replaceWithJsonNull(String str) {
        return str == null ? JsonNull.INSTANCE : new JsonPrimitive(str);
    }

    protected JsonElement replaceWithJsonNull(Double d) {
        return d == null ? JsonNull.INSTANCE : new JsonPrimitive(d);
    }

    protected JsonElement replaceWithJsonNull(Integer i) {
        return i == null ? JsonNull.INSTANCE : new JsonPrimitive(i);
    }

    protected JsonObject truncate(JsonObject object) {
        if (object == null) {
            return new JsonObject();
        }

        if (object.size() > Constants.MAX_PROPERTY_KEYS) {
            throw new IllegalArgumentException(
                    "Too many properties (more than " + Constants.MAX_PROPERTY_KEYS + ") in JSON");
        }

        JsonObject truncatedObject = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();

            try {
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                    truncatedObject.addProperty(key, truncate(value.getAsString()));
                } else if (value.isJsonObject()) {
                    truncatedObject.add(key, truncate(value.getAsJsonObject()));
                } else if (value.isJsonArray()) {
                    truncatedObject.add(key, truncate(value.getAsJsonArray()));
                } else {
                    truncatedObject.add(key, value);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "JSON parsing error. Too long (>" + Constants.MAX_STRING_LENGTH + " chars) or invalid JSON");
            }
        }

        return truncatedObject;
    }

    protected JsonArray truncate(JsonArray array) {
        if (array == null) {
            return new JsonArray();
        }

        JsonArray truncatedArray = new JsonArray();
        for (int i = 0; i < array.size(); i++) {
            JsonElement value = array.get(i);
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                truncatedArray.add(truncate(value.getAsString()));
            } else if (value.isJsonObject()) {
                truncatedArray.add(truncate(value.getAsJsonObject()));
            } else if (value.isJsonArray()) {
                truncatedArray.add(truncate(value.getAsJsonArray()));
            } else {
                truncatedArray.add(value);
            }
        }
        return truncatedArray;
    }

    public String toString() {
        return this.toJsonObject().toString();
    }

    protected String truncate(String value) {
        return value.length() <= Constants.MAX_STRING_LENGTH ? value : value.substring(0, Constants.MAX_STRING_LENGTH);
    }
}
