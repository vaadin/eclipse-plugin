package com.amplitude;

import com.google.gson.JsonObject;

public class IngestionMetadata {
    /**
     * The source name, e.g. "ampli"
     */
    private String sourceName;
    /**
     * The source version, e.g. "2.0.0"
     */
    private String sourceVersion;

    private JsonObject jsonObject;

    /**
     * Set the ingestion metadata source name information.
     *
     * @param sourceName
     *            source name for ingestion metadata
     * @return the same IngestionMetadata object
     */
    public IngestionMetadata setSourceName(String sourceName) {
        this.sourceName = sourceName;
        this.jsonObject = null;
        return this;
    }

    /**
     * Set the ingestion metadata source version information.
     *
     * @param sourceVersion
     *            source version for ingestion metadata
     * @return the same IngestionMetadata object
     */
    public IngestionMetadata setSourceVersion(String sourceVersion) {
        this.sourceVersion = sourceVersion;
        this.jsonObject = null;
        return this;
    }

    /**
     * Get JsonObject of current ingestion metadata
     *
     * @return JsonObject including ingestion metadata information
     */
    protected JsonObject toJsonObject() {
        if (this.jsonObject != null) {
            return this.jsonObject;
        }

        JsonObject jsonObject = new JsonObject();
        if (!Utils.isEmptyString(sourceName)) {
            jsonObject.addProperty(Constants.AMP_INGESTION_METADATA_SOURCE_NAME, sourceName);
        }
        if (!Utils.isEmptyString(sourceVersion)) {
            jsonObject.addProperty(Constants.AMP_INGESTION_METADATA_SOURCE_VERSION, sourceVersion);
        }
        this.jsonObject = jsonObject;
        return jsonObject;
    }
}
