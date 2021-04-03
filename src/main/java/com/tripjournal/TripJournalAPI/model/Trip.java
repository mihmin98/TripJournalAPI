package com.tripjournal.TripJournalAPI.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Accessors
public class Trip {
    private String id;
    private String ownerId;
    private String name;
    private String photo;
    private String destinationName;
    private String destinationCoords;
    private double cost;
    private int rating;
    private String description;
    private List<String> likedBy;

    public Map<String, Object> generateMap() {
        Map<String, Object> map = new HashMap<>();
        if (ownerId != null) map.put("ownerId", ownerId);
        if (name != null) map.put("name", name);
        if (photo != null) map.put("photo", photo);
        if (destinationName != null) map.put("destinationName", destinationName);
        if (destinationCoords != null) map.put("destinationCoords", destinationCoords);
        if (cost != 0) map.put("cost", cost);
        if (rating != 0) map.put("rating", rating);
        if (description != null) map.put("description", description);
        if (likedBy != null) map.put("likedBy", likedBy);
        return map;
    }

    public static Trip toTrip(Map<String, Object> map) {
        Trip trip = new Trip();

        trip.setId((String) map.get("id"));
        trip.setOwnerId((String) map.get("ownerId"));
        trip.setName((String) map.get("name"));
        trip.setPhoto((String) map.get("photo"));
        trip.setDestinationName((String) map.get("destinationName"));
        trip.setDestinationCoords((String) map.get("destinationCoords"));
        trip.setCost(Double.parseDouble((String) map.get("cost")));
        trip.setRating(Integer.parseInt((String) map.get("rating")));
        trip.setDescription((String) map.get("description"));
        trip.setLikedBy((List<String>) map.get("likedBy"));

        return trip;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("ownerId", ownerId);
        map.put("name", name);
        map.put("photo", photo);
        map.put("destinationName", destinationName);
        map.put("destinationCoords", destinationCoords);
        map.put("cost", cost);
        map.put("rating", rating);
        map.put("description", description);
        map.put("likedBy", likedBy);
        return map;
    }
}
