package com.example.apibridge.Application;

import com.example.apibridge.Domain.Api;
import com.example.apibridge.Domain.Parameter;
import com.example.apibridge.Domain.UrlElements;
import com.example.apibridge.Infrastructure.INeo4jRepository;
import com.example.apibridge.Infrastructure.IScbApiRepository;
import com.example.apibridge.Infrastructure.ISocApiRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;
import org.neo4j.driver.types.Node;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;

@Service
public class ApiBridgeService implements IApiBridgeService {
    private final INeo4jRepository neo4jRepository;
    private final IScbApiRepository scbApiRepository;
    private final ISocApiRepository socApiRepository;

    public ApiBridgeService(INeo4jRepository neo4jRepository, IScbApiRepository scbApiRepository, ISocApiRepository socApiRepository) {
        this.neo4jRepository = neo4jRepository;
        this.scbApiRepository = scbApiRepository;
        this.socApiRepository = socApiRepository;
    }

    @Override
    public Collection<String> getDatasets() {
        Collection<String> datasets = neo4jRepository.matchDatasetNodes();
        return datasets;
    }

    @Override
    public Collection<Map<String, Object>> getParametersByDatasets(Collection<String> datasets) {
        Collection<Map<String, Object>> parameters = neo4jRepository.matchParameterNodesByDatasets(datasets);
        return parameters;
    }

    @Override
    public byte[] getData(Map<String, Map<String, Collection<String>>> parameters) {
        Collection<Map<String, Object>> queryResult;
        try {
            queryResult = neo4jRepository.matchNodesByDatasetsAndParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to retrieve URL elements from database for the selected datasets and parameters.");
        }

        if (queryResult.isEmpty()) {
            return new byte[0];
        }

        Collection<UrlElements> mappedUrls = mapToUrlElements(queryResult);

        Collection<Map<String, String>> result;
        try {
            result = searchApis(mappedUrls);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch data from APIs for the retrieved URL elements.");
        }

        if (result.isEmpty()) {
            return new byte[0];
        }

        return generateCsv(result);
    }

    private Collection<UrlElements> mapToUrlElements(Collection<Map<String, Object>> queryResults) {
        Collection<UrlElements> urlElementsList = new ArrayList<>();

        for (Map<String, Object> result : queryResults) {
            Node datasetNode = (Node) result.get("dataset");

            Map<String, Object> datasetProps = datasetNode.asMap();
            String apiStr = String.valueOf(datasetProps.get("api"));
            Api api = Api.valueOf(apiStr);
            apiStr = apiStr.toLowerCase();
            String path = String.valueOf(datasetProps.get("path"));

            Map<String, Map<String, Map.Entry<String, String>>> standardizedParameters = new HashMap<>();
            Collection<Parameter> parameters = new ArrayList<>();

            for (String groupKey : result.keySet()) {
                if (groupKey.endsWith("Group")) {
                    String leafKey = groupKey.substring(0, groupKey.length() - 5) + "Leaf";

                    List<Node> groupNodes = (List<Node>) result.get(groupKey);
                    List<Node> leafNodes = (List<Node>) result.get(leafKey);

                    for (Node groupNode : groupNodes) {
                        Map<String, Object> groupProps = groupNode.asMap();
                        String groupCode = String.valueOf(groupProps.get(apiStr + "Code"));
                        String groupName = String.valueOf(groupProps.get("name"));

                        Map<String, Map.Entry<String, String>> standardizedValueParameters = new HashMap<>();
                        Collection<String> leafValues = new HashSet<>();

                        for (Node leafNode : leafNodes) {
                            Map<String, Object> leafProps = leafNode.asMap();
                            String leafCode = String.valueOf(leafProps.get(apiStr + "Code"));
                            String leafName = String.valueOf(leafProps.get("name"));

                            leafValues.add(leafCode);
                            standardizedValueParameters.put(leafCode, Map.entry(groupName, leafName));
                        }

                        standardizedParameters.put(groupCode, standardizedValueParameters);

                        if (!groupKey.contains("additional")) {
                            parameters.add(new Parameter(groupCode, leafValues));
                        }
                    }
                }
            }

            List<Node> additionalGroups = (List<Node>) result.get("additionalGroup");
            List<Node> additionalLeafs = (List<Node>) result.get("additionalLeaf");

            Set<String> resultKeys = new HashSet<>(result.keySet());

            if (additionalGroups != null && additionalLeafs != null) {
                Map<String, List<Node>> leafsByLabel = new HashMap<>();

                for (Node leafNode : additionalLeafs) {
                    String leafLabel = leafNode.labels().iterator().next();
                    leafsByLabel.computeIfAbsent(leafLabel, k -> new ArrayList<>()).add(leafNode);
                }

                for (Node groupNode : additionalGroups) {
                    String groupLabel = groupNode.labels().iterator().next();
                    Map<String, Object> groupProps = groupNode.asMap();

                    String additionalGroupCode = String.valueOf(groupProps.get(apiStr + "Code"));
                    String additionalGroupName = String.valueOf(groupProps.get("name"));

                    List<Node> matchingLeafNodes = leafsByLabel.getOrDefault(groupLabel, new ArrayList<>());

                    if (!resultKeys.contains(groupLabel) && !matchingLeafNodes.isEmpty()) {
                        Map<String, Map.Entry<String, String>> additionalValueParameters = new HashMap<>();

                        for (Node leafNode : matchingLeafNodes) {
                            Map<String, Object> leafProps = leafNode.asMap();
                            String additionalLeafCode = String.valueOf(leafProps.get(apiStr + "Code"));
                            String additionalLeafName = String.valueOf(leafProps.get("name"));

                            additionalValueParameters.put(additionalLeafCode, Map.entry(additionalGroupName, additionalLeafName));
                        }

                        standardizedParameters.put(additionalGroupCode, additionalValueParameters);
                    }
                }
            }
            urlElementsList.add(new UrlElements(api, path, standardizedParameters, parameters));
        }
        return urlElementsList;
    }

    private Collection<Map<String, String>> searchApis(Collection<UrlElements> urlElementsList) {
        Collection<Map<String, String>> totalResult = new ArrayList<>();
        Collection<Map<String, String>> result;
        for (UrlElements urlElements : urlElementsList) {
            result = switch (urlElements.getApi()) {
                case SCB -> scbApiRepository.sendQuery(urlElements);
                case Socialstyrelsen -> socApiRepository.sendQuery(urlElements);
            };
            totalResult.addAll(result);
        }
        return formatResult(totalResult);
    }

    private Collection<Map<String, String>> formatResult(Collection<Map<String, String>> result) {
        Map<String, Integer> columnFrequency = new HashMap<>();
        for (Map<String, String> row : result) {
            for (String column : row.keySet()) {
                columnFrequency.put(column, columnFrequency.getOrDefault(column, 0) + 1);
            }
        }

        List<String> sortedColumns = new ArrayList<>(columnFrequency.keySet());
        sortedColumns.sort((column1, column2) -> columnFrequency.get(column2) - columnFrequency.get(column1));

        Collection<Map<String, String>> formattedResult = new ArrayList<>();
        for (Map<String, String> row : result) {
            Map<String, String> formattedRow = new LinkedHashMap<>();
            for (String column : sortedColumns) {
                formattedRow.put(column, row.getOrDefault(column, ".."));
            }
            formattedResult.add(formattedRow);
        }
        return formattedResult;
    }

    private byte[] generateCsv(Collection<Map<String, String>> data) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(outputStream);
             CSVWriter csvWriter = new CSVWriter(writer)) {

            Map<String, String> firstRow = data.stream().findFirst().orElse(null);
            if (firstRow == null) {
                return new byte[0];
            }
            String[] header = firstRow.keySet().toArray(new String[0]);
            csvWriter.writeNext(header);

            for (Map<String, String> row : data) {
                String[] rowData = new String[header.length];

                for (int i = 0; i < header.length; i++) {
                    rowData[i] = row.get(header[i]);
                }
                csvWriter.writeNext(rowData);
            }

            csvWriter.flush();
            writer.flush();
            return outputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to generate CSV file for the fetched data.");
        }
    }

    private void printUrlElements(Collection<UrlElements> mappedUrls) {
        System.out.println("\nMAPPED URL ELEMENTS\n");

        for (UrlElements urlElements : mappedUrls) {
            System.out.println("UrlElements {");
            System.out.println("  api = " + urlElements.getApi() + ",");
            System.out.println("  path = '" + urlElements.getPath() + "',");

            System.out.println("  standardizedParameters = {");
            for (Map.Entry<String, Map<String, Map.Entry<String, String>>> outerEntry : urlElements.getStandardizedParameters().entrySet()) {
                String standardizedKey = outerEntry.getKey();
                Map<String, Map.Entry<String, String>> innerMap = outerEntry.getValue();

                System.out.println("    '" + standardizedKey + "' : {");
                for (Map.Entry<String, Map.Entry<String, String>> innerEntry : innerMap.entrySet()) {
                    String innerKey = innerEntry.getKey();
                    Map.Entry<String, String> values = innerEntry.getValue();
                    System.out.println("      '" + innerKey + "' : { standardizedGroup = '" + values.getKey() + "', standardizedLeaf = '" + values.getValue() + "' },");
                }
                System.out.println("    },  // End of '" + standardizedKey + "'");
            }
            System.out.println("  },  // End of standardizedParameters");

            System.out.println("  parameters = [");
            for (Parameter parameter : urlElements.getParameters()) {
                System.out.println("    Parameter {");
                System.out.println("      key = '" + parameter.getKey() + "',");
                System.out.println("      values = " + parameter.getValues());
                System.out.println("    },  // End of Parameter");
            }
            System.out.println("  ]  // End of parameters");
            System.out.println("}  // End of UrlElements");
            System.out.println("──────────────────────────────────────────────");
        }
        System.out.println("\n=========================================\n");
    }

    private void printSearchResults(Collection<Map<String, Object>> results) {
        System.out.println("Search Results from Neo4j");

        if (results.isEmpty()) {
            System.out.println("No results found.");
        } else {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                List<Map<String, Object>> formattedResults = new ArrayList<>();

                for (Map<String, Object> resultMap : results) {
                    Map<String, Object> formattedResult = new HashMap<>();

                    resultMap.forEach((key, value) -> {
                        if (value instanceof List<?> listValue) {
                            List<Map<String, Object>> nodesList = new ArrayList<>();
                            for (Object obj : listValue) {
                                if (obj instanceof Node node) {
                                    nodesList.add(Map.of(
                                            "identity", node.elementId(),
                                            "labels", node.labels(),
                                            "properties", node.asMap()
                                    ));
                                }
                            }
                            formattedResult.put(key, nodesList);
                        } else if (value instanceof Node node) {
                            if (key.equals("dataset")) {
                                formattedResult.put("dataset", Map.of(
                                        "identity", node.elementId(),
                                        "labels", node.labels(),
                                        "properties", node.asMap()
                                ));
                            } else {
                                formattedResult.put(key, node.asMap());
                            }
                        } else {
                            formattedResult.put(key, value);
                        }
                    });

                    formattedResults.add(formattedResult);
                }

                String jsonOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(formattedResults);
                System.out.println("Query Results (JSON Format):\n" + jsonOutput);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}