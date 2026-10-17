package com.leaksense.leaksense_backend.controller;

import com.leaksense.leaksense_backend.entity.Reading;
import com.leaksense.leaksense_backend.repository.ReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/readings")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class ReadingController {

    private final ReadingRepository readingRepository;

    @GetMapping("/latest")
    public List<Reading> getLatestReadings() {
        return readingRepository.findTop50ByOrderByRecordedAtDesc();
    }

    @GetMapping("/node/{nodeId}")
    public List<Reading> getReadingsByNode(@PathVariable Long nodeId) {
        return readingRepository.findTop20ByNodeIdOrderByRecordedAtDesc(nodeId);
    }
}
