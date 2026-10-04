package com.photobuddy.controller;

import com.photobuddy.dto.buddy.BuddyMatchResponse;
import com.photobuddy.security.AuthenticatedUser;
import com.photobuddy.service.impl.BuddyService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/matches", "/api/v1/matches"})
public class MatchController {
    private final BuddyService buddies;

    public MatchController(BuddyService buddies) { this.buddies = buddies; }

    @GetMapping
    public List<BuddyMatchResponse> getMatches(@AuthenticationPrincipal AuthenticatedUser principal) {
        return buddies.getMatches(principal.id());
    }
}
