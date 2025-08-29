package com.lgcms.streaming.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class FfmpegUtils {

    public double getM3u8Duration(String m3u8Path) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
            "ffprobe", "-v", "quiet", "-print_format", "json", "-show_format", m3u8Path
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode json = objectMapper.readTree(output.toString());
            return json.path("format").path("duration").asDouble();
        }
    }
}
