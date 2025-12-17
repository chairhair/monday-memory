package com.monday.monday_backend.analytics;

import com.monday.shared.analytics.AnalyticsEvent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/analytics/event")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Value("${internal.analytics.secret}")
    private String analyticsSecret;

    @PostMapping("/event")
    public ResponseEntity<Void> emitEvent(
            @RequestHeader("X-MM-Internal-Secret") String secret,
            @Valid @RequestBody AnalyticsEvent event
    ) {
        if (!analyticsSecret.equals(secret)) {
            return ResponseEntity.status(403).build();
        }

        analyticsService.emit(event);
        return ResponseEntity.accepted().build();
    }

}
