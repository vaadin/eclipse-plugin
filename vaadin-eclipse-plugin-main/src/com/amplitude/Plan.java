package com.amplitude;

import com.google.gson.JsonObject;

public class Plan {
    /**
     * The tracking plan branch name e.g. "main"
     */
    private String branch;
    /**
     * The tracking plan source e.g. "web", "mobile"
     */
    private String source;
    /**
     * The tracking plan version e.g. "1", "15"
     */
    private String version;
    /**
     * The tracking plan version Id e.g. "9ec23ba0-275f-468f-80d1-66b88bff9529"
     */
    private String versionId;

    private JsonObject jsonPlan;

    /**
     * Set the tracking plan branch information.
     *
     * @param branch
     *            The tracking plan branch name e.g. "main"
     * @return the same Plan object
     */
    public Plan setBranch(String branch) {
        this.branch = branch;
        this.jsonPlan = null;
        return this;
    }

    /**
     * Set the tracking plan source information.
     *
     * @param source
     *            The tracking plan source e.g. "web", "mobile"
     * @return the same Plan object
     */
    public Plan setSource(String source) {
        this.source = source;
        this.jsonPlan = null;
        return this;
    }

    /**
     * Set the tracking plan version information.
     *
     * @param version
     *            The tracking plan version e.g. "1", "15"
     * @return the same Plan object
     */
    public Plan setVersion(String version) {
        this.version = version;
        this.jsonPlan = null;
        return this;
    }

    /**
     * Set the tracking plan version Id.
     *
     * @param versionId
     *            The tracking plan versionId e.g. "9ec23ba0-275f-468f-80d1-66b88bff9529"
     * @return the same Plan object
     */
    public Plan setVersionId(String versionId) {
        this.versionId = versionId;
        this.jsonPlan = null;
        return this;
    }

    /**
     * Get JsonObject of current tacking plan
     *
     * @return JsonObject including plan information
     */
    protected JsonObject toJsonObject() {
        if (this.jsonPlan != null) {
            return this.jsonPlan;
        }

        JsonObject plan = new JsonObject();
        if (!Utils.isEmptyString(branch)) {
            plan.addProperty(Constants.AMP_PLAN_BRANCH, branch);
        }
        if (!Utils.isEmptyString(source)) {
            plan.addProperty(Constants.AMP_PLAN_SOURCE, source);
        }
        if (!Utils.isEmptyString(version)) {
            plan.addProperty(Constants.AMP_PLAN_VERSION, version);
        }
        if (!Utils.isEmptyString(versionId)) {
            plan.addProperty(Constants.AMP_PLAN_VERSION_ID, versionId);
        }
        this.jsonPlan = plan;
        return plan;
    }
}
