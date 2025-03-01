package com.example.poster.service;

import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class PosterService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String RESPONSE_FOLDER = "response"; // Folder to store JSON responses

    public ResponseEntity<String> sendPostRequest(
            String url, String username, String password, String user, String workspace, String env) {

        HttpHeaders headers = createHeaders(username, password);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(createRequestBody(user, workspace, env), headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        saveResponseToFile(response.getBody(), user);

        return response;
    }

    @Async
    public CompletableFuture<ResponseEntity<String>> sendPostRequestAsync(
            String url, String username, String password, String user, String workspace, String env) {

        HttpHeaders headers = createHeaders(username, password);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(createRequestBody(user, workspace, env), headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        saveResponseToFile(response.getBody(), user);

        return CompletableFuture.completedFuture(response);
    }

    private HttpHeaders createHeaders(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);
        return headers;
    }

    private Map<String, String> createRequestBody(String user, String workspace, String env) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("user", user);
        requestBody.put("workspace", workspace);
        requestBody.put("env", env);
        return requestBody;
    }

    private void saveResponseToFile(String responseBody, String user) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File folder = new File(RESPONSE_FOLDER);
            if (!folder.exists()) folder.mkdirs(); // Ensure directory exists

            String filename = RESPONSE_FOLDER + "/" + user + "_" + timestamp + ".json";
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), jsonResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Housekeeping method to delete old JSON files
    public int cleanUpOldResponses() {
        File folder = new File(RESPONSE_FOLDER);
        if (!folder.exists() || !folder.isDirectory()) return 0;

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) return 0;

        // Group files by user
        Map<String, List<File>> userFilesMap = new HashMap<>();
        for (File file : files) {
            String filename = file.getName();
            String user = filename.split("_")[0]; // Extract username
            userFilesMap.putIfAbsent(user, new ArrayList<>());
            userFilesMap.get(user).add(file);
        }

        int deletedFilesCount = 0;

        for (List<File> userFiles : userFilesMap.values()) {
            // Sort files by timestamp (descending order)
            userFiles.sort((f1, f2) -> f2.getName().compareTo(f1.getName()));

            // Keep only the 2 most recent files, delete the rest
            for (int i = 2; i < userFiles.size(); i++) {
                if (userFiles.get(i).delete()) {
                    deletedFilesCount++;
                }
            }
        }

        return deletedFilesCount;
    }
}
