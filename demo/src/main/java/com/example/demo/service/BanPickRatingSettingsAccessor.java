package com.example.demo.service;

@FunctionalInterface
public interface BanPickRatingSettingsAccessor {

    BanPickRatingSettingsSnapshot getCurrentSettings();
}
