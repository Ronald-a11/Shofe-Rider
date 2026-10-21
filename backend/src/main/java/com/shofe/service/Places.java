package com.shofe.service;

import com.shofe.dto.MiscDtos;

import java.util.List;
import java.util.Locale;

public final class Places {
    private Places() {}

    public static final List<MiscDtos.Place> HARARE = List.of(
            new MiscDtos.Place("Harare CBD — First Street", -17.8292, 31.0522),
            new MiscDtos.Place("Joina City", -17.8314, 31.0528),
            new MiscDtos.Place("Eastgate Mall", -17.8320, 31.1470),
            new MiscDtos.Place("Avondale Shopping Centre", -17.7910, 31.0370),
            new MiscDtos.Place("Sam Levy Village, Borrowdale", -17.7760, 31.0870),
            new MiscDtos.Place("Robert Gabriel Mugabe International Airport", -17.9318, 31.0928),
            new MiscDtos.Place("Mbare Musika", -17.8590, 31.0330),
            new MiscDtos.Place("University of Zimbabwe", -17.7840, 31.0530),
            new MiscDtos.Place("Westgate Shopping Centre", -17.7840, 30.9850),
            new MiscDtos.Place("Newlands Shopping Centre", -17.8040, 31.0660),
            new MiscDtos.Place("Borrowdale Brooke", -17.7460, 31.0910),
            new MiscDtos.Place("Msasa Industrial", -17.8300, 31.1100)
    );

    public static List<MiscDtos.Place> matching(String query) {
        if (query == null || query.isBlank()) {
            return HARARE;
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        return HARARE.stream().filter(place -> place.label().toLowerCase(Locale.ROOT).contains(needle)).toList();
    }
}
