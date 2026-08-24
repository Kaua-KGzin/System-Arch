package dev.kauakgzin.archhub.web.dto;

import java.util.List;

public record NetworkResponse(String primaryUrl, List<String> urls) {
}
