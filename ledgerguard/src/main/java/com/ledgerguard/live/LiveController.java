package com.ledgerguard.live;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class LiveController {

    private final LiveStreamService liveStreamService;

    public LiveController(LiveStreamService liveStreamService) {
        this.liveStreamService = liveStreamService;
    }

    @GetMapping(path = "/api/live/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(
            Authentication authentication,
            @RequestHeader(name = "Last-Event-ID", required = false) String lastEventId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (request.getParameter("access_token") != null || request.getParameter("token") != null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token in query string is not allowed");
        }
        return liveStreamService.openStream(authentication, lastEventId, response);
    }
}