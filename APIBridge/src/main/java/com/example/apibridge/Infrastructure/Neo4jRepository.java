package com.example.apibridge.Infrastructure;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class Neo4jRepository implements INeo4jRepository {
    private final Neo4jClient neo4jClient;
    //private static final String DATABASE = "apibridge";
    private static final String DATABASE = "neo4j";

    public Neo4jRepository(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    @Override
    public Collection<String> matchDatasetNodes() {
        return neo4jClient.query(
                        "USE " + DATABASE + " " +
                                "MATCH (dataset:Dataset) " +
                                "ORDER BY dataset.name " +
                                "RETURN dataset.name AS dataset"
                )
                .fetchAs(String.class)
                .all();
    }

    @Override
    public Collection<Map<String, Object>> matchParameterNodesByDatasets(Collection<String> datasets) {
        return neo4jClient.query(
                        "USE " + DATABASE + " " +
                                "MATCH (dataset:Dataset) " +
                                "WHERE dataset.name IN $datasets " +
                                "MATCH (dataset)-[:CONTAINS]->(parameterType) " +
                                "WITH dataset.name AS dataset, parameterType, parameterType.name AS type " +
                                "MATCH (parameterType)-[:CONTAINS]->(parameterValue) " +
                                "WITH dataset, type, parameterValue.name AS value " +
                                "ORDER BY type, value " +
                                "RETURN dataset, type, COLLECT(value) AS values"
                )
                .bind(datasets).to("datasets")
                .fetch()
                .all();
    }

    @Override
    public Collection<Map<String, Object>> matchNodesByDatasetsAndParameters(Map<String, Map<String, Collection<String>>> searchedDatasets) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (Map.Entry<String, Map<String, Collection<String>>> datasetEntry : searchedDatasets.entrySet()) {
            String datasetName = datasetEntry.getKey();
            Map<String, Collection<String>> searchParameters = datasetEntry.getValue();

            StringBuilder queryBuilder = new StringBuilder("USE " + DATABASE + " ");

            Map<String, Object> parameters = new HashMap<>();
            List<String> matchClauses = new ArrayList<>();
            List<String> whereClauses = new ArrayList<>();
            List<String> returnClauses = new ArrayList<>();
            Set<String> searchedTypes = new HashSet<>(searchParameters.keySet());
            int index = 0;

            for (Map.Entry<String, Collection<String>> parametersEntry : searchParameters.entrySet()) {
                String type = Arrays.stream(parametersEntry.getKey().split("\\s+"))
                        .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                        .collect(Collectors.joining(""));

                String groupAlias = type + "Group";
                String specificAlias = type + "Leaf";

                matchClauses.add("(dataset)-[:CONTAINS]->(" + groupAlias + ":" + type + ")");
                matchClauses.add("(" + groupAlias + ")-[:CONTAINS]->(" + specificAlias + ":" + type + ")");

                String paramKey = "param" + index;
                whereClauses.add(specificAlias + ".name IN $" + paramKey);
                parameters.put(paramKey, parametersEntry.getValue());

                returnClauses.add("COLLECT(DISTINCT " + groupAlias + ") AS " + groupAlias);
                returnClauses.add("COLLECT(DISTINCT " + specificAlias + ") AS " + specificAlias);

                index++;
            }

            queryBuilder.append("\nMATCH (dataset:Dataset {name: $datasetName}), ");
            queryBuilder.append("\n      ").append(String.join(",\n      ", matchClauses));
            queryBuilder.append("\nWHERE ").append(String.join("\n      AND ", whereClauses));

            queryBuilder.append("\nOPTIONAL MATCH (dataset)-[:CONTAINS]->(otherGroup)");
            queryBuilder.append("\nWHERE NOT labels(otherGroup)[0] IN $searchedTypes");

            queryBuilder.append("\nOPTIONAL MATCH (otherGroup)-[:CONTAINS]->(otherLeaf)");

            queryBuilder.append("\nRETURN dataset,\n      ").append(String.join(",\n      ", returnClauses));
            queryBuilder.append(",\n      COLLECT(DISTINCT otherGroup) AS additionalGroup");
            queryBuilder.append(",\n      COLLECT(DISTINCT otherLeaf) AS additionalLeaf");

            parameters.put("datasetName", datasetName);
            parameters.put("searchedTypes", new ArrayList<>(searchedTypes));

            String cypherQuery = queryBuilder.toString();

            Collection<Map<String, Object>> queryResults = neo4jClient.query(cypherQuery)
                    .bindAll(parameters)
                    .fetch()
                    .all();

            results.addAll(queryResults);
        }

        return results;
    }
}