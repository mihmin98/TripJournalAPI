package com.tripjournal.TripJournalAPI.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class LikeTripDto {

    private String tripId;
    private String userId;
}
