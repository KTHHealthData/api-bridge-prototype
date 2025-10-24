package com.example.apibridge.Application;

import java.util.Collection;
import java.util.Map;

public interface IApiBridgeService {
    Collection<String> getDatasets();

    Collection<Map<String, Object>> getParametersByDatasets(Collection<String> datasets);

    byte[] getData(Map<String, Map<String, Collection<String>>> parameters);
}