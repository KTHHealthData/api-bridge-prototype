package com.example.apibridge.Infrastructure;

import com.example.apibridge.Domain.Parameter;
import com.example.apibridge.Domain.UrlElements;
import io.netty.resolver.DefaultAddressResolverGroup;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.json.*;
import reactor.netty.http.client.HttpClient;

import java.util.*;

@Service
public class SocApiRepository implements ISocApiRepository {

    private final WebClient webClient;
    private final String baseUrl = "https://sdb.socialstyrelsen.se/api/v1/sv/";
    private String path;
    private String measurementName;

    public SocApiRepository() {
        HttpClient httpClient = HttpClient.create()
                .resolver(DefaultAddressResolverGroup.INSTANCE);
        var builder = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
                .clientConnector(new ReactorClientHttpConnector(httpClient));
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    private String getValuesString(Collection<String> values) {
        StringBuilder result = new StringBuilder();
        values.forEach(value -> {
            result.append(value).append(",");
        });
        result.deleteCharAt(result.length() - 1);
        return result.toString();
    }

    @Override
    public Collection<Map<String, String>> sendQuery(UrlElements urlElements) {
        path = urlElements.getPath();
        ArrayList<JSONArray> dataArray = new ArrayList<>();
        path += getParameterString(urlElements.getParameters());
        measurementName = getMeasurementName();
        int i = 1;
        String currentResult;
        JSONObject currentResultObj;
        do {
            currentResult = webClient.get()
                    .uri(path + "?sida=" + i)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            i++;
            currentResultObj = new JSONObject(currentResult);
            dataArray.add(currentResultObj.getJSONArray("data"));
        } while (currentResultObj.has("nasta_sida") && !currentResultObj.isNull("nasta_sida"));
        return parseAllResponses(dataArray, urlElements.getStandardizedParameters());
    }

    private String getParameterString(Collection<Parameter> parameters) {
        StringBuilder result = new StringBuilder();
        for (Parameter parameter : parameters) {
            result.append(parameter.getKey()).append("/").append(getValuesString(parameter.getValues())).append("/");
        }
        return result.toString();
    }

    private Collection<Map<String, String>> parseResponse(JSONArray response, Map<String, Map<String, Map.Entry<String, String>>> parameters) {
        Collection<Map<String, String>> result = new ArrayList<>();
        var names = new JSONArray();
        if (!response.isEmpty()) {
            names = ((JSONObject) response.get(0)).names();
        }
        for (var obj : response) {
            Map<String, String> newRow = new LinkedHashMap<>();
            JSONObject row = (JSONObject) obj;
            boolean addRow = true;
            for (var name : names) {
                String key = name.toString();
                String value = row.get(key).toString();
                if (name.equals("varde")) {
                    newRow.put(measurementName, value);
                    continue;
                }
                if (parameters.containsKey(getQueryName(key)) && parameters.get(getQueryName(key)).containsKey(value)) {
                    Map.Entry<String, String> standardizedEntry = parameters.get(getQueryName(key)).get(value);
                    newRow.put(standardizedEntry.getKey(), standardizedEntry.getValue());
                } else {
                    addRow = false;
                    break;
                }
            }
            if (addRow) result.add(newRow);
        }
        return result;
    }

    private String getQueryName(String resultName) {
        int index;
        for (index = 0; index < resultName.length(); index++) {
            if (Character.isUpperCase(resultName.charAt(index))) {
                break;
            }
        }
        return resultName.substring(0, index);
    }

    private Collection<Map<String, String>> parseAllResponses(ArrayList<JSONArray> responses, Map<String, Map<String, Map.Entry<String, String>>> parameters) {
        Collection<Map<String, String>> result = new ArrayList<>();
        for (JSONArray response : responses) {
            result.addAll(parseResponse(response, parameters));
        }
        return result;
    }

    private String extractTableNameId() {
        return path.split("/")[0];
    }

    private String extractMeasurementId() {
        var result = path.split("/");
        for (int i = 0; i < result.length; i++) {
            if (result[i].equals("matt")) {
                return (i + 1) >= result.length ? "-1" : result[i + 1];
            }
        }
        return "-1";
    }

    private String getMeasurementName() {
        String measurmentName = "";
        String result = webClient.get()
                .uri(extractTableNameId() + "/matt/")
                .retrieve()
                .bodyToMono(String.class)
                .block();
        var measurementNames = new JSONArray(result);
        measurmentName = findMeasurementName(measurementNames);
        return measurmentName.isEmpty() ? "Value" : measurmentName;
    }

    private String findMeasurementName(JSONArray measurementNames) {
        for (var obj : measurementNames) {
            var measurementName = (JSONObject) obj;
            if (((JSONObject) obj).getInt("id") == Integer.parseInt(extractMeasurementId())) {
                return measurementName.getString("text");
            }
        }
        return "";
    }
}