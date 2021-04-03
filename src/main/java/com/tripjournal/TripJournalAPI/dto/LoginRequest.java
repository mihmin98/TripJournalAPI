package com.tripjournal.TripJournalAPI.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class LoginRequest {

    private String email;
    private String password;
}
