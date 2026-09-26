package com.devshowcase.api.controller;

import com.devshowcase.api.dto.FeedbackRequestDTO;
import com.devshowcase.api.dto.ProjectRequestDTO;
import com.devshowcase.api.dto.ProjectResponseDTO;
import com.devshowcase.api.model.Feedback;
import com.devshowcase.api.model.Profile;
import com.devshowcase.api.model.Project;
import com.devshowcase.api.model.Technology;
import com.devshowcase.api.repository.FeedbackRepository;
import com.devshowcase.api.repository.ProfileRepository;
import com.devshowcase.api.repository.ProjectRepository;
import com.devshowcase.api.repository.TechnologyRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final ProfileRepository profileRepository;
    private final TechnologyRepository technologyRepository;
    private final FeedbackRepository feedbackRepository;

    public ProjectController(ProjectRepository projectRepository, 
                             ProfileRepository profileRepository, 
                             TechnologyRepository technologyRepository,
                             FeedbackRepository feedbackRepository) {
        this.projectRepository = projectRepository;
        this.profileRepository = profileRepository;
        this.technologyRepository = technologyRepository;
        this.feedbackRepository = feedbackRepository;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ProjectRequestDTO dto) {
        Profile profile = profileRepository.findById(dto.profileId()).orElse(null);
        if (profile == null) {
            return ResponseEntity.badRequest().body("Profile não encontrado com o ID fornecido.");
        }

        Project project = new Project();
        project.setTitle(dto.title());
        project.setDescription(dto.description());
        project.setRepositoryUrl(dto.repositoryUrl());
        project.setProfile(profile);

        if (dto.technologyIds() != null && !dto.technologyIds().isEmpty()) {
            List<Technology> techs = technologyRepository.findAllById(dto.technologyIds());
            project.setTechnologies(new HashSet<>(techs));
        }

        Project saved = projectRepository.save(project);

        Set<String> techNames = saved.getTechnologies().stream()
                .map(Technology::getName)
                .collect(Collectors.toSet());

        ProjectResponseDTO response = new ProjectResponseDTO(
            saved.getId(), saved.getTitle(), saved.getDescription(), 
            saved.getRepositoryUrl(), saved.getProfile().getId(), techNames
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ProjectResponseDTO>> listProjects(
            @RequestParam(required = false) String technology,
            Pageable pageable) {
        
        Page<Project> projectPage;

        if (technology != null && !technology.isBlank()) {
            projectPage = projectRepository.findByTechnologiesNameContainingIgnoreCase(technology, pageable);
        } else {
            projectPage = projectRepository.findAll(pageable);
        }

        Page<ProjectResponseDTO> responsePage = projectPage.map(p -> new ProjectResponseDTO(
                p.getId(),
                p.getTitle(),
                p.getDescription(),
                p.getRepositoryUrl(),
                p.getProfile().getId(),
                p.getTechnologies().stream().map(Technology::getName).collect(Collectors.toSet())
        ));

        return ResponseEntity.ok(responsePage);
    }

    @PutMapping("/{id}/upvote")
    public ResponseEntity<Project> upvoteProject(@PathVariable Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado com o ID: " + id));
        
        project.setUpvotes(project.getUpvotes() + 1);
        Project savedProject = projectRepository.save(project);
        
        return ResponseEntity.ok(savedProject);
    }

    @PostMapping("/{id}/feedbacks")
    public ResponseEntity<Project> addFeedback(
            @PathVariable Long id, 
            @Valid @RequestBody FeedbackRequestDTO dto) {
        
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado com o ID: " + id));

        Feedback feedback = new Feedback();
        feedback.setRating(dto.getRating());
        feedback.setComment(dto.getComment());
        feedback.setProject(project);

        feedbackRepository.save(feedback);

        project.getFeedbacks().add(feedback);

        double average = project.getFeedbacks().stream()
                .mapToInt(Feedback::getRating)
                .average()
                .orElse(0.0);
        
        project.setAverageRating(average);
        Project savedProject = projectRepository.save(project);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedProject);
    }
}