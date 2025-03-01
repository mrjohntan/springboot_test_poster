package com.example.poster.controller;

import com.example.poster.dto.RequestData;
import com.example.poster.service.PosterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class PosterController {

    private final PosterService posterService;

    public PosterController(PosterService posterService) {
        this.posterService = posterService;
    }

    @PostMapping("/send-request")
    public ResponseEntity<String> sendRequest(@RequestBody RequestData requestData) {
        return posterService.sendPostRequest(
                requestData.getUrl(),
                requestData.getUsername(),
                requestData.getPassword(),
                requestData.getUser(),
                requestData.getWorkspace(),
                requestData.getEnv()
        );
    }

    @PostMapping("/send-multiple-requests")
    public ResponseEntity<List<String>> sendMultipleRequests(
            @RequestBody RequestData requestData,
            @RequestParam List<String> users
    ) {
        List<CompletableFuture<ResponseEntity<String>>> futures = users.stream()
                .map(user -> posterService.sendPostRequestAsync(
                        requestData.getUrl(),
                        requestData.getUsername(),
                        requestData.getPassword(),
                        user,
                        requestData.getWorkspace(),
                        requestData.getEnv()))
                .collect(Collectors.toList());

        List<String> responses = futures.stream()
                .map(CompletableFuture::join)
                .map(ResponseEntity::getBody)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    // New housekeeping endpoint
    @PostMapping("/housekeep")
    public ResponseEntity<String> housekeep() {
        int deletedFiles = posterService.cleanUpOldResponses();
        return ResponseEntity.ok("Deleted " + deletedFiles + " old response files.");
    }
}
