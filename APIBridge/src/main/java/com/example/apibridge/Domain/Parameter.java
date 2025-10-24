package com.example.apibridge.Domain;

import java.util.Collection;

public class Parameter {
    private String key;
    private Collection<String> values;

    public Parameter(String key, Collection<String> values) {
        this.key = key;
        this.values = values;
    }

    public String getKey() {
        return key;
    }

    public Collection<String> getValues() {
        return values;
    }

    @Override
    public String toString() {
        return "Parameter{" +
                ", key='" + key + '\'' +
                ", values=" + values +
                '}';
    }
}
