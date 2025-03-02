package com.example.poster.service;

import com.example.poster.dto.RequestData;
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
import java.util.stream.Collectors;

@Service
public class PosterService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String RESPONSE_FOLDER = "response";

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

    public List<String> processMultipleUsers(RequestData requestData, List<String> users) {
        List<CompletableFuture<ResponseEntity<String>>> futures = users.stream()
                .map(user -> sendPostRequestAsync(
                        requestData.getUrl(),
                        requestData.getUsername(),
                        requestData.getPassword(),
                        user,
                        requestData.getWorkspace(),
                        requestData.getEnv()))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .map(ResponseEntity::getBody)
                .collect(Collectors.toList());
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
            if (!folder.exists()) folder.mkdirs();

            String filename = RESPONSE_FOLDER + "/" + user + "_" + timestamp + ".json";
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), jsonResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int cleanUpOldResponses() {
        File folder = new File(RESPONSE_FOLDER);
        if (!folder.exists() || !folder.isDirectory()) return 0;

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) return 0;

        Map<String, List<File>> userFilesMap = new HashMap<>();
        for (File file : files) {
            String filename = file.getName();
            String user = filename.split("_")[0];
            userFilesMap.putIfAbsent(user, new ArrayList<>());
            userFilesMap.get(user).add(file);
        }

        int deletedFilesCount = 0;

        for (List<File> userFiles : userFilesMap.values()) {
            userFiles.sort((f1, f2) -> f2.getName().compareTo(f1.getName()));

            for (int i = 2; i < userFiles.size(); i++) {
                if (userFiles.get(i).delete()) {
                    deletedFilesCount++;
                }
            }
        }

        return deletedFilesCount;
    }

    public Set<String> checkForUpdate() {
        File folder = new File(RESPONSE_FOLDER);
        if (!folder.exists() || !folder.isDirectory()) return Collections.emptySet();

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) return Collections.emptySet();

        Map<String, List<File>> userFilesMap = new HashMap<>();
        for (File file : files) {
            String filename = file.getName();
            String user = filename.split("_")[0];
            userFilesMap.putIfAbsent(user, new ArrayList<>());
            userFilesMap.get(user).add(file);
        }

        Set<String> usersForUpdate = new HashSet<>();
        ObjectMapper objectMapper = new ObjectMapper();

        for (Map.Entry<String, List<File>> entry : userFilesMap.entrySet()) {
            List<File> userFiles = entry.getValue();
            userFiles.sort((f1, f2) -> f2.getName().compareTo(f1.getName()));

            if (userFiles.size() < 2) continue;

            try {
                JsonNode recentJson = objectMapper.readTree(userFiles.get(0));
                JsonNode previousJson = objectMapper.readTree(userFiles.get(1));

                String recentChecksum = recentJson.path("checksum").asText();
                String previousChecksum = previousJson.path("checksum").asText();

                if (!recentChecksum.equals(previousChecksum)) {
                    usersForUpdate.add(entry.getKey());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return usersForUpdate;
    }
}
