package com.lgcms.streaming.controller;

import com.lgcms.streaming.service.StreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/streaming")
@RequiredArgsConstructor @Slf4j
public class StreamingController {
    private final StreamingService streamingService;
    @PostMapping
    public ResponseEntity<?> testEncoding(){
        streamingService.downloadOriginFile(null);
        return ResponseEntity.ok("성공");
    }
}
