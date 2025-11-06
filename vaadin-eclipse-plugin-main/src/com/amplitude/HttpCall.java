package com.amplitude;

import java.io.*;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import com.amplitude.exception.AmplitudeInvalidAPIKeyException;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class HttpCall {
    private final String apiKey;
    private final String serverUrl;
    private final Options options;
    private final Proxy proxy;
    private final Gson gson = new Gson();

    protected HttpCall(String apiKey, String serverUrl) {
        this(apiKey, serverUrl, null, Proxy.NO_PROXY);
    }

    protected HttpCall(String apiKey, String serverUrl, Options options, Proxy proxy) {
        this.apiKey = apiKey;
        this.serverUrl = serverUrl;
        this.options = options;
        this.proxy = proxy;
    }

    protected String getApiUrl() {
        return this.serverUrl;
    }

    protected Response makeRequest(List<Event> events) throws AmplitudeInvalidAPIKeyException {
        String apiUrl = getApiUrl();
        HttpsURLConnection connection;
        InputStream inputStream = null;
        int responseCode = 500;
        Response responseBody = new Response();
        try {
            connection = (HttpsURLConnection) new URL(apiUrl).openConnection(proxy);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(Constants.NETWORK_TIMEOUT_MILLIS);
            connection.setReadTimeout(Constants.NETWORK_TIMEOUT_MILLIS);
            connection.setDoOutput(true);

            if (this.options != null && this.options.headers != null && !this.options.headers.isEmpty()) {
                this.options.headers.forEach(connection::setRequestProperty);
            }

            JsonObject bodyJson = new JsonObject();
            bodyJson.addProperty("api_key", this.apiKey);
            if (options != null)
                bodyJson.add("options", options.toJsonObject());

            JsonArray eventsArr = new JsonArray();
            for (Event event : events) {
                eventsArr.add(event.toJsonObject());
            }
            bodyJson.add("events", eventsArr);

            String bodyString = gson.toJson(bodyJson);
            OutputStream os = connection.getOutputStream();
            byte[] input = bodyString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);

            responseCode = connection.getResponseCode();
            boolean isErrorCode = responseCode >= Constants.HTTP_STATUS_BAD_REQ;
            if (!isErrorCode) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                sb.append(output);
            }
            JsonObject responseJson = JsonParser.parseString(sb.toString()).getAsJsonObject();
            responseBody = Response.populateResponse(responseJson);
        } catch (IOException e) {
            // This handles UnknownHostException, when there is no internet connection.
            // SocketTimeoutException will be triggered when the HTTP request times out.
            JsonObject timesOutResponse = new JsonObject();
            timesOutResponse.addProperty("status", Status.TIMEOUT.toString());
            timesOutResponse.addProperty("code", 408);
            responseBody = Response.populateResponse(timesOutResponse);
        } catch (JsonSyntaxException e) {
            // Some error responses from load balancers and reverse proxies may have
            // response bodies that are not JSON (e.g. HTML, XML).
            JsonObject decodeFailureResponse = new JsonObject();
            decodeFailureResponse.addProperty("code", responseCode);
            responseBody = Response.populateResponse(decodeFailureResponse);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        }
        return responseBody;
    }
}
