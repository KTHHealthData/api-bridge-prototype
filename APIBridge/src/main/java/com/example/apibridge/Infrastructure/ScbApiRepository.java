package com.example.apibridge.Infrastructure;

import com.example.apibridge.Domain.Parameter;
import com.example.apibridge.Domain.UrlElements;
import com.github.dannil.scbjavaclient.client.SCBClient;
import com.github.dannil.scbjavaclient.model.GenericModel;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ScbApiRepository implements IScbApiRepository {
    private final SCBClient scbClient;

    public ScbApiRepository() {
        this.scbClient = new SCBClient();
    }

    private Collection<Map<String, String>> parseResponse(Collection<Map<String, Object>> response, Map<String, Map<String, Map.Entry<String, String>>> standardizedParameters) {
        Collection<Map<String, String>> result = new ArrayList<>();
        rowLoop:
        for (Map<String, Object> row : response) {
            Map<String, String> newRow = new LinkedHashMap<>();

            Map<String, String> variables = (Map<String, String>) row.get("Variables");

            for (Map.Entry<String, String> entry : variables.entrySet()) {
                Map.Entry<String, String> standardizedParameter = standardizedParameters.get(entry.getKey()).get(entry.getValue());
                if (standardizedParameter == null) {
                    continue rowLoop;
                }
                newRow.put(standardizedParameter.getKey(), standardizedParameter.getValue());
            }

            List<Map<String, String>> values = (List<Map<String, String>>) row.get("Values");

            for (Map<String, String> valueMap : values) {
                String text = valueMap.get("Text");
                String value = valueMap.get("Value");
                newRow.put(text, value);
            }

            result.add(newRow);
        }
        return result;
    }

    private Map<String, Collection<?>> formatParameters(Collection<Parameter> parameters) {
        Map<String, Collection<?>> formattedParameters = parameters.stream()
                .collect(Collectors.toMap(
                        Parameter::getKey,
                        Parameter::getValues
                ));
        return formattedParameters;
    }

    @Override
    public Collection<Map<String, String>> sendQuery(UrlElements urlElements) {
        Map<String, Collection<?>> formattedParameters = formatParameters(urlElements.getParameters());
        String responseJson = scbClient.getRawData(urlElements.getPath(), formattedParameters);
        Collection<Map<String, Object>> responseMap = new GenericModel(responseJson).getEntries();
        return parseResponse(responseMap, urlElements.getStandardizedParameters());
    }
}