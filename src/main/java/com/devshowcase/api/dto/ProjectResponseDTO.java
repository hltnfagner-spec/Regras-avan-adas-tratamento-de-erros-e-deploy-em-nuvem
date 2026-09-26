package com.devshowcase.api.dto;

import java.util.Set;

public record ProjectResponseDTO(
    Long id, 
    String title, 
    String description, 
    String repositoryUrl, 
    Long profileId, 
    Set<String> technologies
) {}