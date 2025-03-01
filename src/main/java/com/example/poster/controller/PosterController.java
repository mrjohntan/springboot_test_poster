package com.example.poster.controller;

import com.example.poster.dto.RequestData;
import com.example.poster.service.PosterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PosterController {

    private final PosterService posterService;

    public PosterController(PosterService posterService) {
        this.posterService = posterService;
    }

    @PostMapping("/send-request")
    public ResponseEntity<String> sendRequest(@RequestBody RequestData requestData) {
        System.out.println("Received URL: " + requestData.getUrl());  // Debugging
        return posterService.sendPostRequest(
                requestData.getUrl(),
                requestData.getUsername(),
                requestData.getPassword(),
                requestData.getUser(),
                requestData.getWorkspace(),
                requestData.getEnv()
        );
    }
}
