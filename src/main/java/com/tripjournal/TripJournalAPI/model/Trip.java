package com.tripjournal.TripJournalAPI.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors
public class Trip {
    private String id;
    private String ownerId;
    private String name;
    private String destinationName;
    private String destinationCoords;
    private double cost;
    private int rating;
    private String description;
    private List<String> likedBy;
}
