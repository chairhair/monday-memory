package com.monday.monday_backend.auth.dto;

import org.springframework.data.domain.PageRequest;

import java.awt.print.Pageable;
import java.util.List;

public record UserSearchRequestDTO(Integer pageNumber, Integer itemsPerPage, List<Long> userIds) {
    public Pageable toPageable() {
        return (Pageable) PageRequest.of(pageNumber != null ? pageNumber : 0, itemsPerPage != null ? itemsPerPage : 20);
    }
}
