package com.tripjournal.TripJournalAPI.controller;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.tripjournal.TripJournalAPI.dto.LoginRequest;
import com.tripjournal.TripJournalAPI.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/user")
public class UserController {

    public static final String USER_COLLECTION_NAME = "user";

    @Autowired
    private Firestore dbFirestore;

    @GetMapping("/{id}")
    public ResponseEntity<Object> getUser(@PathVariable("id") String id) throws ExecutionException, InterruptedException {

        DocumentSnapshot documentSnapshot = dbFirestore.collection(USER_COLLECTION_NAME).document(id).get().get();

        if (!documentSnapshot.exists()) {
            return new ResponseEntity<>(id, HttpStatus.NOT_FOUND);
        }
        User user = User.toUser(Objects.requireNonNull(documentSnapshot.getData()));
        user.setPassword(null);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<User> register(@RequestBody User user) throws ExecutionException, InterruptedException {
        DocumentReference documentReference = dbFirestore.collection(USER_COLLECTION_NAME).document(user.getEmail());

        if (documentReference.get().get().exists()) {
            return new ResponseEntity<>(user, HttpStatus.CONFLICT);
        }

        if (!dbFirestore.collection(USER_COLLECTION_NAME).whereEqualTo("username", user.getUsername()).get().get().isEmpty()) {
            return new ResponseEntity<>(user, HttpStatus.CONFLICT);
        }
        documentReference.set(user.toMap()).get();
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PutMapping
    public ResponseEntity<User> updateUser(@RequestBody User user) throws ExecutionException, InterruptedException {

        DocumentSnapshot documentSnapshot = dbFirestore.collection(USER_COLLECTION_NAME).document(user.getEmail()).get().get();

        if (!documentSnapshot.exists()) {
            return new ResponseEntity<>(user, HttpStatus.NOT_FOUND);
        }

        if (!dbFirestore.collection(USER_COLLECTION_NAME).whereEqualTo("username", user.getUsername()).get().get().isEmpty()) {
            return new ResponseEntity<>(user, HttpStatus.CONFLICT);
        }
        DocumentReference documentReference = dbFirestore.collection(USER_COLLECTION_NAME).document(user.getEmail());
        documentReference.update(user.generateMap()).get();
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteUser(@PathVariable("id") String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot documentSnapshot = dbFirestore.collection(USER_COLLECTION_NAME).document(id).get().get();
        if (!documentSnapshot.exists()) {
            return new ResponseEntity<>(id, HttpStatus.NOT_FOUND);
        }
        DocumentReference documentReference = dbFirestore.collection(USER_COLLECTION_NAME).document(id);

        documentReference.delete().get();
        return new ResponseEntity<>(id, HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody LoginRequest loginRequest) throws ExecutionException, InterruptedException {
        DocumentSnapshot documentSnapshot = dbFirestore.collection(USER_COLLECTION_NAME).document(loginRequest.getEmail()).get().get();

        if (!documentSnapshot.exists()) {
            return new ResponseEntity<>(loginRequest, HttpStatus.NOT_FOUND);
        }

        User user = User.toUser(Objects.requireNonNull(documentSnapshot.getData()));
        if (!user.getPassword().equals(loginRequest.getPassword())) {
            return new ResponseEntity<>(loginRequest, HttpStatus.BAD_REQUEST);
        }

        user.setPassword(null);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

}
