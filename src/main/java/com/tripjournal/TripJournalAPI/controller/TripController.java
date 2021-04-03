package com.tripjournal.TripJournalAPI.controller;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.tripjournal.TripJournalAPI.dto.LikeTripDto;
import com.tripjournal.TripJournalAPI.model.Trip;
import com.tripjournal.TripJournalAPI.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static com.tripjournal.TripJournalAPI.controller.UserController.USER_COLLECTION_NAME;

@RestController
@RequestMapping("/trip")
public class TripController {

    public static final String TRIP_COLLECTION_NAME = "trip";

    @Autowired
    private Firestore dbFirestore;

    @GetMapping
    public ResponseEntity<Object> getAllTrips() {

        CollectionReference tripsCollection = dbFirestore.collection(TRIP_COLLECTION_NAME);
        return new ResponseEntity<>(tripsCollection.listDocuments(), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Object> addTrip(@RequestBody Trip trip) throws ExecutionException, InterruptedException {
        DocumentReference documentReference = dbFirestore.collection(TRIP_COLLECTION_NAME).document();

        trip.setId(documentReference.getId());
        documentReference.set(trip.toMap()).get();

        return new ResponseEntity<>(trip, HttpStatus.OK);
    }

    @PostMapping("/like")
    public ResponseEntity<Object> likeTrip(@RequestBody LikeTripDto likeTripDto) throws ExecutionException, InterruptedException {

        DocumentSnapshot documentTripSnapshot = dbFirestore.collection(TRIP_COLLECTION_NAME).document(likeTripDto.getTripId()).get().get();

        if (!documentTripSnapshot.exists()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        DocumentSnapshot documentUserSnapshot = dbFirestore.collection(USER_COLLECTION_NAME).document(likeTripDto.getUserId()).get().get();

        if (!documentUserSnapshot.exists()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Trip trip = Trip.toTrip(Objects.requireNonNull(documentTripSnapshot.getData()));
        List<String> alreadyLike = trip.getLikedBy();
        User user = User.toUser(Objects.requireNonNull(documentUserSnapshot.getData()));
        if (alreadyLike.contains(user.getEmail())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        alreadyLike.add(user.getEmail());
        trip.setLikedBy(alreadyLike);

        DocumentReference tripRef = dbFirestore.collection(TRIP_COLLECTION_NAME).document(trip.getId());
        tripRef.update(trip.generateMap()).get();

        DocumentReference documentReference = dbFirestore.collection(USER_COLLECTION_NAME).document(likeTripDto.getUserId());
        List<String> userFavorites = user.getFavorites();
        userFavorites.add(likeTripDto.getTripId());
        user.setFavorites(userFavorites);
        documentReference.update(user.generateMap()).get();

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
