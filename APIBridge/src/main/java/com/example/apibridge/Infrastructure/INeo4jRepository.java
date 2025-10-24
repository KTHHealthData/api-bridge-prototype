package com.example.apibridge.Infrastructure;

import java.util.Collection;
import java.util.Map;

public interface INeo4jRepository {
    Collection<String> matchDatasetNodes();

    Collection<Map<String, Object>> matchParameterNodesByDatasets(Collection<String> datasets);

    Collection<Map<String, Object>> matchNodesByDatasetsAndParameters(Map<String, Map<String, Collection<String>>> searchedDatasets);
}