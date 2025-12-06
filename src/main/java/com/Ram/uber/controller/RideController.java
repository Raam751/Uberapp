package com.Ram.uber.controller;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Ram.uber.dto.CreateRideRequest;
import com.Ram.uber.exception.BadRequestException;
import com.Ram.uber.exception.NotFoundException;
import com.Ram.uber.model.Ride;
import com.Ram.uber.model.User;
import com.Ram.uber.repository.RideRepository;
import com.Ram.uber.repository.UserRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class RideController {

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private UserRepository userRepository;

    private String getLoggedInUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    private User getLoggedInUser() {
        String username = getLoggedInUsername();
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        return user;
    }

    // USER: create ride
    @PostMapping("/rides")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Ride> createRide(@Valid @RequestBody CreateRideRequest request) {
        User user = getLoggedInUser();

        Ride ride = new Ride();
        ride.setUserId(user.getId());
        ride.setPickupLocation(request.getPickupLocation());
        ride.setDropLocation(request.getDropLocation());
        ride.setStatus("REQUESTED");
        ride.setCreatedAt(new Date());

        Ride saved = rideRepository.save(ride);
        return ResponseEntity.ok(saved);
    }

    // DRIVER: view pending ride requests
    @GetMapping("/driver/rides/requests")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<List<Ride>> getPendingRides() {
        List<Ride> rides = rideRepository.findByStatus("REQUESTED");
        return ResponseEntity.ok(rides);
    }

    // DRIVER: accept ride
    @PostMapping("/driver/rides/{rideId}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<Ride> acceptRide(@PathVariable String rideId) {
        User driver = getLoggedInUser();

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new NotFoundException("Ride not found"));

        if (!"REQUESTED".equals(ride.getStatus())) {
            throw new BadRequestException("Ride must be in REQUESTED status to accept");
        }

        ride.setDriverId(driver.getId());
        ride.setStatus("ACCEPTED");

        Ride saved = rideRepository.save(ride);
        return ResponseEntity.ok(saved);
    }

    // USER/DRIVER: complete ride
    @PostMapping("/rides/{rideId}/complete")
    @PreAuthorize("hasAnyRole('USER','DRIVER')")
    public ResponseEntity<Ride> completeRide(@PathVariable String rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new NotFoundException("Ride not found"));

        if (!"ACCEPTED".equals(ride.getStatus())) {
            throw new BadRequestException("Ride must be in ACCEPTED status to complete");
        }

        ride.setStatus("COMPLETED");
        Ride saved = rideRepository.save(ride);
        return ResponseEntity.ok(saved);
    }

    // USER: get own rides
    @GetMapping("/user/rides")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<Ride>> getMyRides() {
        User user = getLoggedInUser();
        List<Ride> rides = rideRepository.findByUserId(user.getId());
        return ResponseEntity.ok(rides);
    }
}
