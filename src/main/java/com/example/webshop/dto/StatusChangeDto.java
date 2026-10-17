package com.example.webshop.dto;

import com.example.webshop.entity.StatusChange;

import java.time.LocalDateTime;

public record StatusChangeDto(String oldStatus, String newStatus, LocalDateTime changedAt) {

    public static StatusChangeDto from(StatusChange s) {
        return new StatusChangeDto(s.getOldStatus(), s.getNewStatus(), s.getChangedAt());
    }
}
