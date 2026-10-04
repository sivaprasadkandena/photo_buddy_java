package com.photobuddy.controller;

import com.photobuddy.dto.buddy.BuddyRequestResponse;
import com.photobuddy.security.AuthenticatedUser;
import com.photobuddy.service.impl.BuddyService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/buddy-requests", "/api/v1/buddy-requests"})
public class BuddyRequestController {
    private final BuddyService buddies;

    public BuddyRequestController(BuddyService buddies) { this.buddies = buddies; }

    @PostMapping("/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public BuddyRequestResponse send(@AuthenticationPrincipal AuthenticatedUser principal,
                                     @PathVariable Long userId) {
        return buddies.sendRequest(principal.id(), userId);
    }

    @GetMapping("/received")
    public List<BuddyRequestResponse> received(@AuthenticationPrincipal AuthenticatedUser principal) {
        return buddies.getReceived(principal.id());
    }

    @GetMapping("/sent")
    public List<BuddyRequestResponse> sent(@AuthenticationPrincipal AuthenticatedUser principal) {
        return buddies.getSent(principal.id());
    }

    @PutMapping("/{id}/accept")
    public BuddyRequestResponse accept(@AuthenticationPrincipal AuthenticatedUser principal,
                                       @PathVariable Long id) {
        return buddies.accept(principal.id(), id);
    }

    @PutMapping("/{id}/reject")
    public BuddyRequestResponse reject(@AuthenticationPrincipal AuthenticatedUser principal,
                                       @PathVariable Long id) {
        return buddies.reject(principal.id(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long id) {
        buddies.cancel(principal.id(), id);
    }
}
