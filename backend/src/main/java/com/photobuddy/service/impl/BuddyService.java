package com.photobuddy.service.impl;

import com.photobuddy.dto.buddy.BuddyMatchResponse;
import com.photobuddy.dto.buddy.BuddyRequestResponse;
import com.photobuddy.dto.buddy.BuddyUserSummary;
import com.photobuddy.entity.BuddyMatch;
import com.photobuddy.entity.BuddyRequest;
import com.photobuddy.entity.BuddyRequestStatus;
import com.photobuddy.entity.User;
import com.photobuddy.entity.UserLocation;
import com.photobuddy.repository.BuddyMatchRepository;
import com.photobuddy.repository.BuddyRequestRepository;
import com.photobuddy.repository.UserLocationRepository;
import com.photobuddy.repository.UserRepository;
import com.photobuddy.service.impl.NotificationService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BuddyService {
    private final BuddyRequestRepository requests;
    private final BuddyMatchRepository matches;
    private final UserLocationRepository locations;
    private final UserRepository users;
    private final NotificationService notifications;

    public BuddyService(BuddyRequestRepository requests, BuddyMatchRepository matches,
                        UserLocationRepository locations, UserRepository users, NotificationService notifications) {
        this.requests = requests;
        this.matches = matches;
        this.locations = locations;
        this.users = users;
        this.notifications = notifications;
    }

    @Transactional
    public BuddyRequestResponse sendRequest(Long senderId, Long receiverId) {
        if (senderId.equals(receiverId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot send a buddy request to yourself");
        }
        User sender = getEnabledUser(senderId);
        User receiver = getEnabledUser(receiverId);
        User pairLow = senderId < receiverId ? sender : receiver;
        User pairHigh = senderId < receiverId ? receiver : sender;

        BuddyRequest request = requests.findByPairLowIdAndPairHighId(pairLow.getId(), pairHigh.getId()).orElse(null);
        if (request != null) {
            if (request.getStatus() == BuddyRequestStatus.PENDING || request.getStatus() == BuddyRequestStatus.ACCEPTED
                    || matches.existsByUser1IdAndUser2Id(pairLow.getId(), pairHigh.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A pending request or match already exists");
            }
            request.reopen(sender, receiver);
        } else {
            request = new BuddyRequest(sender, receiver, pairLow, pairHigh);
        }
        BuddyRequest saved = requests.save(request);
        notifications.notifyBuddyRequest(saved.getReceiver().getId(), saved.getSender().getId(), saved.getId());
        return toRequestResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BuddyRequestResponse> getReceived(Long userId) {
        return requests.findAllByReceiverIdOrderByUpdatedAtDesc(userId).stream()
                .filter(request -> request.getStatus() == BuddyRequestStatus.PENDING)
                .map(this::toRequestResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BuddyRequestResponse> getSent(Long userId) {
        return requests.findAllBySenderIdOrderByUpdatedAtDesc(userId).stream()
                .filter(request -> request.getStatus() == BuddyRequestStatus.PENDING)
                .map(this::toRequestResponse).toList();
    }

    @Transactional
    public BuddyRequestResponse accept(Long userId, Long requestId) {
        BuddyRequest request = getPendingRequest(requestId);
        requireReceiver(request, userId);
        if (!request.getSender().isEnabled() || !request.getReceiver().isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Disabled accounts cannot become buddies");
        }
        request.accept();
        if (!matches.existsByUser1IdAndUser2Id(request.getPairLow().getId(), request.getPairHigh().getId())) {
            matches.save(new BuddyMatch(request.getPairLow(), request.getPairHigh()));
        }
        notifications.notifyBuddyAccepted(request.getSender().getId(), request.getReceiver().getId(), request.getId());
        return toRequestResponse(request);
    }

    @Transactional
    public BuddyRequestResponse reject(Long userId, Long requestId) {
        BuddyRequest request = getPendingRequest(requestId);
        requireReceiver(request, userId);
        request.reject();
        return toRequestResponse(request);
    }

    @Transactional
    public void cancel(Long userId, Long requestId) {
        BuddyRequest request = getPendingRequest(requestId);
        if (!request.getSender().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the sender can cancel this request");
        }
        request.cancel();
    }

    @Transactional(readOnly = true)
    public List<BuddyMatchResponse> getMatches(Long userId) {
        List<BuddyMatch> userMatches = matches.findAllByUser1IdOrUser2IdOrderByCreatedAtDesc(userId, userId);
        List<Long> locationUserIds = new ArrayList<>();
        locationUserIds.add(userId);
        for (BuddyMatch match : userMatches) {
            locationUserIds.add(match.getUser1().getId());
            locationUserIds.add(match.getUser2().getId());
        }
        Map<Long, UserLocation> locationsByUser = new HashMap<>();
        for (UserLocation location : locations.findAllByUserIdIn(locationUserIds)) {
            locationsByUser.put(location.getUser().getId(), location);
        }
        UserLocation ownLocation = locationsByUser.get(userId);

        return userMatches.stream().map(match -> {
            User buddy = match.getUser1().getId().equals(userId) ? match.getUser2() : match.getUser1();
            UserLocation buddyLocation = locationsByUser.get(buddy.getId());
            Double distance = null;
            if (ownLocation != null && buddyLocation != null && ownLocation.isLocationEnabled()
                    && buddyLocation.isLocationEnabled()) {
                distance = BigDecimal.valueOf(LocationService.haversineKm(
                                ownLocation.getLatitude().doubleValue(), ownLocation.getLongitude().doubleValue(),
                                buddyLocation.getLatitude().doubleValue(), buddyLocation.getLongitude().doubleValue()))
                        .setScale(1, RoundingMode.HALF_UP).doubleValue();
            }
            return new BuddyMatchResponse(match.getId(), toSummary(buddy), distance, match.getCreatedAt());
        }).toList();
    }

    private BuddyRequest getPendingRequest(Long requestId) {
        BuddyRequest request = requests.findByIdForUpdate(requestId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Buddy request was not found"));
        if (request.getStatus() != BuddyRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Buddy request is no longer pending");
        }
        return request;
    }

    private void requireReceiver(BuddyRequest request, Long userId) {
        if (!request.getReceiver().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the receiver can respond to this request");
        }
    }

    private User getEnabledUser(Long id) {
        return users.findById(id).filter(User::isEnabled).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User was not found"));
    }

    private BuddyRequestResponse toRequestResponse(BuddyRequest request) {
        return new BuddyRequestResponse(request.getId(), toSummary(request.getSender()), toSummary(request.getReceiver()),
                request.getStatus(), request.getCreatedAt(), request.getUpdatedAt());
    }

    private BuddyUserSummary toSummary(User user) {
        return new BuddyUserSummary(user.getId(), user.getUsername(), user.getFirstName(), user.getLastName(),
                user.getProfilePicture());
    }
}
