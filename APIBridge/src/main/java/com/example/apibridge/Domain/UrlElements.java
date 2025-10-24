package com.example.apibridge.Domain;

import java.util.Collection;
import java.util.Map;

public class UrlElements {
    Api api;
    String path;
    Map<String, Map<String, Map.Entry<String, String>>> standardizedParameters;
    Collection<Parameter> parameters;

    public UrlElements(Api api, String path, Map<String, Map<String, Map.Entry<String, String>>> standardizedParameters, Collection<Parameter> parameters) {
        this.api = api;
        this.path = path;
        this.standardizedParameters = standardizedParameters;
        this.parameters = parameters;
    }

    public Api getApi() {
        return api;
    }

    public String getPath() {
        return path;
    }

    public Map<String, Map<String, Map.Entry<String, String>>> getStandardizedParameters() {
        return standardizedParameters;
    }

    public Collection<Parameter> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return "UrlElements{" +
                "api=" + api +
                ", path='" + path + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}