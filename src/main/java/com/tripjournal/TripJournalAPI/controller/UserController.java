package com.tripjournal.TripJournalAPI.controller;


import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.tripjournal.TripJournalAPI.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/user")
public class UserController {

    public static final String USER_COLLECTION_NAME = "user";

    @Autowired
    private Firestore dbFirestore;

    @GetMapping("/{id}")
    public User getUser(@PathVariable("id") String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot documentSnapshot = dbFirestore.collection(USER_COLLECTION_NAME).document(id).get().get();

        User user = User.toUser(Objects.requireNonNull(documentSnapshot.getData()));
        user.setPassword(null);

        return user;
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) throws ExecutionException, InterruptedException {
        DocumentReference documentReference = dbFirestore.collection(USER_COLLECTION_NAME).document(user.getEmail());
        if (documentReference.get().get().exists()) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        documentReference.set(user.toMap()).get();

        return new ResponseEntity<>(user, HttpStatus.OK);
    }
}
