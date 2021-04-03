package com.tripjournal.TripJournalAPI.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class User {
    private String email;
    private String password;
    private String username;
    private List<String> favorites;

    public static User toUser(Map<String, Object> map) {
        User user = new User();

        user.email = (String) map.get("email");
        user.password = (String) map.get("password");
        user.username = (String) map.get("username");
        user.favorites = (List<String>) map.get("favorites");

        return user;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("email", email);
        map.put("password", password);
        map.put("username", username);
        map.put("favorites", favorites);

        return map;
    }
}
