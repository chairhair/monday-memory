package com.monday.monday_backend.auth.dto;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public record UserSearchRequestDTO(Integer pageNumber, Integer itemsPerPage, List<Long> userIds) {
    public Pageable toPageable() {
        return PageRequest.of(pageNumber != null ? pageNumber : 0, itemsPerPage != null ? itemsPerPage : 20);
    }
}
