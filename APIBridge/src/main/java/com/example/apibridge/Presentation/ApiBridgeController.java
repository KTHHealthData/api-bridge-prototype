package com.example.apibridge.Presentation;

import com.example.apibridge.Application.IApiBridgeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
public class ApiBridgeController {
    private final IApiBridgeService apiBridgeService;
    private Collection<String> datasets;
    private Map<String, Map<String, Collection<String>>> parameters;

    public ApiBridgeController(IApiBridgeService apiBridgeService) {
        this.apiBridgeService = apiBridgeService;
    }

    @GetMapping("/")
    public String loadDatasets(Model model) {
        try {
            datasets = apiBridgeService.getDatasets();
            model.addAttribute("datasets", datasets);
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "Failed to retrieve datasets from database.");
            model.addAttribute("datasets", new ArrayList<>());
        }
        finally {
            model.addAttribute("parameters", new HashMap<>());
        }
        return "select-parameters";
    }

    @PostMapping("/")
    public String loadParameters(@RequestParam(required = false) Collection<String> selectedDatasets, Model model) {
        try {
            model.addAttribute("datasets", datasets);
            model.addAttribute("selectedDatasets", selectedDatasets);
            parameters = formatParametersToView(apiBridgeService.getParametersByDatasets(selectedDatasets));
            model.addAttribute("parameters", parameters);
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "Failed to retrieve parameters from database for the selected datasets.");
            model.addAttribute("parameters", new HashMap<>());
        }
        finally {
            model.addAttribute("datasets", datasets);
            model.addAttribute("selectedDatasets", selectedDatasets);
        }
        return "select-parameters";
    }

    private Map<String, Map<String, Collection<String>>> formatParametersToView(Collection<Map<String, Object>> parameters) {
        Map<String, Map<String, Collection<String>>> formattedParameters = new LinkedHashMap<>();

        for (Map<String, Object> parameter : parameters) {
            String dataset = (String) parameter.get("dataset");
            String type = (String) parameter.get("type");
            List<String> values = (List<String>) parameter.get("values");

            formattedParameters.putIfAbsent(type, new HashMap<>());

            Map<String, Collection<String>> valuesAndDatasets = formattedParameters.get(type);

            valuesAndDatasets.putIfAbsent("datasets", new LinkedHashSet<>());
            valuesAndDatasets.putIfAbsent("values", new LinkedHashSet<>());

            valuesAndDatasets.get("datasets").add(dataset);
            valuesAndDatasets.get("values").addAll(values);
        }

        return formattedParameters;
    }

    @PostMapping("/download")
    public ResponseEntity<?> handleDownload(@RequestParam MultiValueMap<String, String> selectedParameters) {
        try {
            byte[] csvFile = apiBridgeService.getData(formatParametersFromView(selectedParameters));

            if (csvFile == null || csvFile.length == 0) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=data_" +
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csvFile);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("errorMessage", e.getMessage()));
        }
    }

    private Map<String, Map<String, Collection<String>>> formatParametersFromView(MultiValueMap<String, String> selectedParameters) {
        Map<String, Map<String, Collection<String>>> formattedSelectedParameters = new HashMap<>();

        for (Map.Entry<String, Map<String, Collection<String>>> parameter : parameters.entrySet()) {
            String type = parameter.getKey();
            Collection<String> datasets = parameter.getValue().get("datasets");

            Collection<String> values = selectedParameters.get(type);

            if (values == null) {
                continue;
            }

            for (String dataset : datasets) {
                formattedSelectedParameters.computeIfAbsent(dataset, key -> new HashMap<>()).put(type, new ArrayList<>(values));
            }
        }

        return formattedSelectedParameters;
    }
}