package com.Ram.uber.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.Ram.uber.model.Ride;

@Repository
public interface RideRepository extends MongoRepository<Ride, String> {
}
