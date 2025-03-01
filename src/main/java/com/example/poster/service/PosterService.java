package com.example.poster.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class PosterService {

    private final RestTemplate restTemplate = new RestTemplate();

    public ResponseEntity<String> sendPostRequest(String url, String username, String password, String user, String workspace, String env) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("user", user);
        requestBody.put("workspace", workspace);
        requestBody.put("env", env);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        // Save API response to JSON file with proper formatting and timestamp in filename
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String filename = "response/response_" + timestamp + ".json";
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), jsonResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }
}

