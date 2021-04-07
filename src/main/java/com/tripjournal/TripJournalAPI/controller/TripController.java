package com.tripjournal.TripJournalAPI.controller;

import com.google.api.client.util.Base64;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.ReadChannel;
import com.google.cloud.firestore.*;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.StorageOptions;
import com.tripjournal.TripJournalAPI.dto.LikeTripDto;
import com.tripjournal.TripJournalAPI.dto.PhotoDto;
import com.tripjournal.TripJournalAPI.model.Trip;
import com.tripjournal.TripJournalAPI.model.User;
import org.apache.commons.io.IOUtils;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItem;
import org.conscrypt.io.IoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.websocket.server.PathParam;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.tripjournal.TripJournalAPI.controller.UserController.USER_COLLECTION_NAME;

@RestController
@RequestMapping("/trip")
public class TripController {

    public static final String TRIP_COLLECTION_NAME = "trip";

    private boolean initializedStorage = false;

    private StorageOptions storageOptions;

    private final String bucketName = "tripjournal-2db9e.appspot.com";
    private final String projectId = "tripjournal-2db9e";

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

    @PutMapping
    public ResponseEntity<Object> updateTrip(@RequestBody Trip trip) throws ExecutionException, InterruptedException {
        DocumentSnapshot documentSnapshot = dbFirestore.collection(TRIP_COLLECTION_NAME).document(trip.getId()).get().get();

        if (!documentSnapshot.exists()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        DocumentReference documentReference = dbFirestore.collection(TRIP_COLLECTION_NAME).document(trip.getId());
        documentReference.update(trip.generateMap()).get();
        return new ResponseEntity<>(trip, HttpStatus.OK);
    }

    @DeleteMapping("/{tripId}")
    public ResponseEntity<Object> deleteTrip(@PathVariable("tripId") String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot documentSnapshot = dbFirestore.collection(TRIP_COLLECTION_NAME).document(id).get().get();

        if (!documentSnapshot.exists()) {
            return new ResponseEntity<>(id, HttpStatus.NOT_FOUND);
        }

        DocumentReference documentReference = dbFirestore.collection(TRIP_COLLECTION_NAME).document(id);
        documentReference.delete().get();
        return new ResponseEntity<>(id, HttpStatus.OK);

    }

    @GetMapping("/others/{ownerId}")
    public ResponseEntity<Object> fetchOtherTrips(@PathVariable("ownerId") String ownerId) throws ExecutionException, InterruptedException {

        List<QueryDocumentSnapshot> queryDocumentSnapshots = dbFirestore.collection(TRIP_COLLECTION_NAME)
                .whereNotEqualTo("ownerId", ownerId).get().get().getDocuments();

        List<Trip> trips = queryDocumentSnapshots.stream()
                .map(QueryDocumentSnapshot::getData)
                .map(Trip::toTrip)
                .collect(Collectors.toList());

        return new ResponseEntity<>(trips, HttpStatus.OK);
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

    @PostMapping("/photo")
    public ResponseEntity<Object> uploadPhoto(@RequestBody PhotoDto photoDto) throws ExecutionException, InterruptedException, IOException {
        if (!initializedStorage) {
            FileInputStream serviceAccount =
                    new FileInputStream(System.getenv("GOOGLE_APPLICATION_CREDENTIALS"));
            storageOptions = StorageOptions.newBuilder()
                    .setProjectId(projectId)
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            initializedStorage = true;
        }

        DocumentReference tripDocumentReference = dbFirestore.collection(TRIP_COLLECTION_NAME).document(photoDto.getTripId());

        if (!tripDocumentReference.get().get().exists()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        byte[] decodedPhoto = Base64.decodeBase64(photoDto.getPhotoBase64Encoded());

        StringBuilder builder = new StringBuilder();
        int maxRandom = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".length();

        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 20; ++i) {
            builder.append("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".charAt(random.nextInt(maxRandom)));
        }

        String filename = photoDto.getTripId() + "_" + builder.toString();

        BlobId blobId = BlobId.of(bucketName, filename);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        Blob blob = storageOptions.getService().create(blobInfo, decodedPhoto);

        // set trip photo to what was uploaded
        tripDocumentReference.update("photo", filename).get();

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/photo")
    public ResponseEntity<Object> downloadPhoto(@RequestBody String photoId) throws ExecutionException, InterruptedException, IOException {
        if (!initializedStorage) {
            FileInputStream serviceAccount =
                    new FileInputStream(System.getenv("GOOGLE_APPLICATION_CREDENTIALS"));
            storageOptions = StorageOptions.newBuilder()
                    .setProjectId(projectId)
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            initializedStorage = true;
        }

        Blob blob = storageOptions.getService().get(BlobId.of(bucketName, photoId));
        if (!blob.exists()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        ReadChannel reader = blob.reader();
        InputStream inputStream = Channels.newInputStream(reader);

        byte[] content = IOUtils.toByteArray(inputStream);

        String encodedPhoto = Base64.encodeBase64String(content);

        return new ResponseEntity<>(encodedPhoto, HttpStatus.OK);
    }

    @PostMapping("/unlike")
    public ResponseEntity<Object> unlikeTrip(@RequestBody LikeTripDto likeTripDto) throws ExecutionException, InterruptedException {

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
        if (!alreadyLike.contains(user.getEmail())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        alreadyLike.remove(user.getEmail());
        trip.setLikedBy(alreadyLike);

        DocumentReference tripRef = dbFirestore.collection(TRIP_COLLECTION_NAME).document(trip.getId());
        tripRef.update(trip.generateMap()).get();

        DocumentReference documentReference = dbFirestore.collection(USER_COLLECTION_NAME).document(likeTripDto.getUserId());
        List<String> userFavorites = user.getFavorites();
        userFavorites.remove(likeTripDto.getTripId());
        user.setFavorites(userFavorites);
        documentReference.update(user.generateMap()).get();

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
