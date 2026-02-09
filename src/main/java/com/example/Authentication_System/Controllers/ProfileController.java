package com.example.Authentication_System.Controllers;

import com.example.Authentication_System.Domain.model.*;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.EducationRepository;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.ExperienceRepository;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.OutboxEventRepository;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.SkillRepository;
import com.example.Authentication_System.Security.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/profile")
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String PROFILE_CHANGES_TOPIC = "profile-changes";

    private final SkillRepository skillRepository;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    public ProfileController(SkillRepository skillRepository,
                            ExperienceRepository experienceRepository,
                            EducationRepository educationRepository,
                            OutboxEventRepository outboxEventRepository,
                            JwtUtils jwtUtils,
                            ObjectMapper objectMapper) {
        this.skillRepository = skillRepository;
        this.experienceRepository = experienceRepository;
        this.educationRepository = educationRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.jwtUtils = jwtUtils;
        this.objectMapper = objectMapper;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    private UUID getCurrentUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String userIdStr = jwtUtils.getUserIdFromToken(token);
            return UUID.fromString(userIdStr);
        }
        throw new IllegalArgumentException("Invalid or missing Authorization header");
    }

    private String getCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    private void setupMdc(String correlationId, UUID userId, String action, String ipAddress, String userAgent) {
        MDC.put("correlationId", correlationId);
        MDC.put("userId", userId != null ? userId.toString() : "anonymous");
        MDC.put("action", action);
        MDC.put("ipAddress", ipAddress != null ? ipAddress : "unknown");
        MDC.put("userAgent", userAgent != null ? userAgent : "unknown");
    }

    private void clearMdc() {
        MDC.clear();
    }

    // ==================== Helper method to create outbox events ====================

    private void createOutboxEvent(Object event) {
        try {
            String eventType = event.getClass().getSimpleName();
            String payload = objectMapper.writeValueAsString(event);
            
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventType(eventType)
                    .payload(payload)
                    .topic(PROFILE_CHANGES_TOPIC)
                    .status("PENDING")
                    .createdAt(Instant.now())
                    .retryCount(0)
                    .build();
            
            outboxEventRepository.save(outboxEvent);
            logger.info("[KAFKA_OUTBOX] Event {} saved for publishing", eventType);
        } catch (Exception e) {
            logger.error("[KAFKA_OUTBOX] Failed to save event: {}", e.getMessage());
            // Don't throw - we don't want to fail the main operation
        }
    }

    // ==================== SKILLS CRUD ====================

    @GetMapping("/skills")
    public ResponseEntity<List<Skill>> getSkills(HttpServletRequest request) {
        String correlationId = getCorrelationId(request);
        UUID userId = null;
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        String action = "GET_SKILLS";
        
        try {
            setupMdc(correlationId, null, action, ipAddress, userAgent);
            logger.info("[START] Retrieving skills for user");
            
            userId = getCurrentUserId(request);
            setupMdc(correlationId, userId, action, ipAddress, userAgent);
            
            List<Skill> skills = skillRepository.findByUserId(userId);
            logger.info("[SUCCESS] Retrieved {} skills for userId={}", skills.size(), userId);
            return ResponseEntity.ok(skills);
        } catch (Exception e) {
            logger.error("[ERROR] Failed to retrieve skills: {}", e.getMessage());
            throw e;
        } finally {
            clearMdc();
        }
    }

    @PostMapping("/skills")
    public ResponseEntity<Skill> createSkill(@Valid @RequestBody SkillRequest skillRequest, HttpServletRequest request) {
        String correlationId = getCorrelationId(request);
        UUID userId = null;
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        String action = "CREATE_SKILL";
        
        try {
            setupMdc(correlationId, null, action, ipAddress, userAgent);
            logger.info("[START] Creating new skill");
            
            userId = getCurrentUserId(request);
            setupMdc(correlationId, userId, action, ipAddress, userAgent);
            
            Skill skill = Skill.builder()
                    .userId(userId)
                    .name(skillRequest.getName())
                    .category(skillRequest.getCategory())
                    .proficiencyLevel(skillRequest.getProficiencyLevel())
                    .yearsOfExperience(skillRequest.getYearsOfExperience())
                    .isVerified(false)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            Skill savedSkill = skillRepository.save(skill);
            
            // Create Kafka event for skill added
            SkillAddedEvent skillAddedEvent = SkillAddedEvent.fromSkill(savedSkill, userId, correlationId);
            createOutboxEvent(skillAddedEvent);
            
            logger.info("[SUCCESS] Skill created with id={}, name={}, category={}", 
                    savedSkill.getId(), savedSkill.getName(), savedSkill.getCategory());
            return ResponseEntity.ok(savedSkill);
        } catch (Exception e) {
            logger.error("[ERROR] Failed to create skill: {}", e.getMessage());
            throw e;
        } finally {
            clearMdc();
        }
    }

    @PutMapping("/skills/{skillId}")
    public ResponseEntity<Skill> updateSkill(@PathVariable UUID skillId,
                                            @Valid @RequestBody SkillRequest skillRequest,
                                            HttpServletRequest request) {
        String correlationId = getCorrelationId(request);
        UUID userId = null;
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        String action = "UPDATE_SKILL";
        
        try {
            setupMdc(correlationId, null, action, ipAddress, userAgent);
            logger.info("[START] Updating skill with id={}", skillId);
            
            userId = getCurrentUserId(request);
            setupMdc(correlationId, userId, action, ipAddress, userAgent);
            
            Skill skill = skillRepository.findById(skillId)
                    .orElseThrow(() -> new IllegalArgumentException("Skill not found"));

            if (!skill.getUserId().equals(userId)) {
                logger.warn("[ACCESS_DENIED] User {} attempted to update skill {} owned by {}", userId, skillId, skill.getUserId());
                throw new IllegalArgumentException("Not authorized to update this skill");
            }

            skill.setName(skillRequest.getName());
            skill.setCategory(skillRequest.getCategory());
            skill.setProficiencyLevel(skillRequest.getProficiencyLevel());
            skill.setYearsOfExperience(skillRequest.getYearsOfExperience());
            skill.setUpdatedAt(Instant.now());

            Skill updatedSkill = skillRepository.save(skill);
            
            // Create Kafka event for skill updated
            SkillUpdatedEvent skillUpdatedEvent = SkillUpdatedEvent.fromSkill(updatedSkill, userId, correlationId);
            createOutboxEvent(skillUpdatedEvent);
            
            logger.info("[SUCCESS] Skill updated: id={}, name={}", updatedSkill.getId(), updatedSkill.getName());
            return ResponseEntity.ok(updatedSkill);
        } catch (IllegalArgumentException e) {
            logger.error("[ERROR] Failed to update skill {}: {}", skillId, e.getMessage());
            throw e;
        } finally {
            clearMdc();
        }
    }

    @DeleteMapping("/skills/{skillId}")
    public ResponseEntity<Void> deleteSkill(@PathVariable UUID skillId, HttpServletRequest request) {
        String correlationId = getCorrelationId(request);
        UUID userId = null;
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        String action = "DELETE_SKILL";
        
        try {
            setupMdc(correlationId, null, action, ipAddress, userAgent);
            logger.info("[START] Deleting skill with id={}", skillId);
            
            userId = getCurrentUserId(request);
            setupMdc(correlationId, userId, action, ipAddress, userAgent);
            
            Skill skill = skillRepository.findById(skillId)
                    .orElseThrow(() -> new IllegalArgumentException("Skill not found"));

            if (!skill.getUserId().equals(userId)) {
                logger.warn("[ACCESS_DENIED] User {} attempted to delete skill {} owned by {}", userId, skillId, skill.getUserId());
                throw new IllegalArgumentException("Not authorized to delete this skill");
            }

            String skillName = skill.getName();
            skillRepository.deleteById(skillId);
            
            // Create Kafka event for skill deleted
            SkillDeletedEvent skillDeletedEvent = SkillDeletedEvent.create(skillId, skillName, userId, correlationId);
            createOutboxEvent(skillDeletedEvent);
            
            logger.info("[SUCCESS] Skill deleted: id={}, name={}", skillId, skillName);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.error("[ERROR] Failed to delete skill {}: {}", skillId, e.getMessage());
            throw e;
        } finally {
            clearMdc();
        }
    }

    // ==================== EXPERIENCE CRUD ====================

    @GetMapping("/experience")
    public ResponseEntity<List<Experience>> getExperiences(HttpServletRequest request) {
        String correlationId = getCorrelationId(request);
        UUID userId = null;
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        String action = "GET_EXPERIENCES";
        
        try {
            setupMdc(correlationId, null, action, ipAddress, userAgent);
            logger.info("[START] Retrieving experiences for user");
            
            userId = getCurrentUserId(request);
            setupMdc(correlationId, userId, action, ipAddress, userAgent);
            
            List<Experience> experiences = experienceRepository.findByUserId(userId);
            logger.info("[SUCCESS] Retrieved {} experiences for userId={}", experiences.size(), userId);
            return ResponseEntity.ok(experiences);
        } catch (Exception e) {
            logger.error("[ERROR] Failed to retrieve experiences: {}", e.getMessage());
            throw e;
        } finally {
            clearMdc();
        }
    }

    @PostMapping("/experience")
    public ResponseEntity<Experience> createExperience(@Valid @RequestBody ExperienceRequest experienceRequest, HttpServletRequest request) {
        String correlationId = getCorrelationId(request);
        UUID userId = null;
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        String action = "CREATE_EXPERIENCE";
        
        try {
            setupMdc(correlationId, null, action, ipAddress, userAgent);
            logger.info("[START] Creating new experience");
            
            userId = getCurrentUserId(request);
            setupMdc(correlationId, userId, action, ipAddress, userAgent);
            
            Experience experience = Experience.builder()
                    .userId(userId)
                    .companyName(experienceRequest.getCompanyName())
                    .jobTitle(experienceRequest.getJobTitle())
                    .location(experienceRequest.getLocation())
                    .isRemote(experienceRequest.isRemote())
                    .startDate(experienceRequest.getStartDate())
                    .endDate(experienceRequest.getEndDate())
                    .isCurrentPosition(experienceRequest.isCurrentPosition())
                    .description(experienceRequest.getDescription())
                    .employmentType(experienceRequest.getEmploymentType())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            Experience savedExperience = experienceRepository.save(experience);
            
            // Create Kafka event for experience added
            ExperienceAddedEvent experienceAddedEvent = ExperienceAddedEvent.fromExperience(savedExperience, userId, correlationId);
            createOutboxEvent(experienceAddedEvent);
            
            logger.info("[SUCCESS] Experience created with id={}, company={}, title={}", 
                    savedExperience.getId(), savedExperience.getCompanyName(), savedExperience.getJobTitle());
            return ResponseEntity.ok(savedExperience);
        } catch (Exception e) {
            logger.error("[ERROR] Failed to create experience: {}", e.getMessage());
            throw e;
        } finally {
            clearMdc();
        }
    }

    @PutMapping("/experience/{experienceId}")
    public ResponseEntity<Experience> updateExperience(@PathVariable UUID experienceId,
                                                        @Valid @RequestBody ExperienceRequest experienceRequest,
                                                        HttpServletRequest request) {
        String correlationId = getCorrelationId(request);
        UUID userId = null;
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        String action = "UPDATE_EXPERIENCE";
        
        try {
            setupMdc(correlationId, null, action, ipAddress, userAgent);
            logger.info("[START] Updating experience with id={}", experienceId);
            
            userId = getCurrentUserId(request);
            setupMdc(correlationId, userId, action, ipAddress, userAgent);
            
            Experience experience = experienceRepository.findById(experienceId)
                    .orElseThrow(() -> new IllegalArgumentException("Experience not found"));

            if (!experience.getUserId().equals(userId)) {
                logger.warn("[ACCESS_DENIED] User {} attempted to update experience {} owned by {}", userId, experienceId, experience.getUserId());
                throw new IllegalArgumentException("Not authorized to update this experience");
            }

            experience.setCompanyName(experienceRequest.getCompanyName());
            experience.setJobTitle(experienceRequest.getJobTitle());
            experience.setLocation(experienceRequest.getLocation());
            experience.setRemote(experienceRequest.isRemote());
            experience.setStartDate(experienceRequest.getStartDate());
            experience.setEndDate(experienceRequest.getEndDate());
            experience.setCurrentPosition(experienceRequest.isCurrentPosition());
            experience.setDescription(experienceRequest.getDescription());
            experience.setEmploymentType(experienceRequest.getEmploymentType());
            experience.setUpdatedAt(Instant.now());

            Experience updatedExperience = experienceRepository.save(experience);
            
            // Create Kafka event for experience updated
            ExperienceUpdatedEvent experienceUpdatedEvent = ExperienceUpdatedEvent.fromExperience(updatedExperience, userId, correlationId);
            createOutboxEvent(experienceUpdatedEvent);
            
            logger.info("[SUCCESS] Experience updated: id={}, company={}, title={}", 
                    updatedExperience.getId(), updatedExperience.getCompanyName(), updatedExperience.getJobTitle());
            return ResponseEntity.ok(updatedExperience);
        } catch (IllegalArgumentException e) {
            logger.error("[ERROR] Failed to update experience {}: {}", experienceId, e.getMessage());
            throw e;
        } finally {
            clearMdc();
        }
    }

    @DeleteMapping("/experience/{experienceId}")
    public ResponseEntity<Void> deleteExperience(@PathVariable UUID experienceId, HttpServletRequest request) {
        String correlationId = getCorrelationId(request);
        UUID userId = null;
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        String action = "DELETE_EXPERIENCE";
        
        try {
            setupMdc(correlationId, null, action, ipAddress, userAgent);
            logger.info("[START] Deleting experience with id={}", experienceId);
            
            userId = getCurrentUserId(request);
            setupMdc(correlationId, userId, action, ipAddress, userAgent);
            
            Experience experience = experienceRepository.findById(experienceId)
                    .orElseThrow(() -> new IllegalArgumentException("Experience not found"));

            if (!experience.getUserId().equals(userId)) {
                logger.warn("[ACCESS_DENIED] User {} attempted to delete experience {} owned by {}", userId, experienceId, experience.getUserId());
                throw new IllegalArgumentException("Not authorized to delete this experience");
            }

            String jobTitle = experience.getJobTitle();
            String companyName = experience.getCompanyName();
            experienceRepository.deleteById(experienceId);
            
            // Create Kafka event for experience deleted
            ExperienceDeletedEvent experienceDeletedEvent = ExperienceDeletedEvent.create(experienceId, jobTitle, companyName, userId, correlationId);
            createOutboxEvent(experienceDeletedEvent);
            
            logger.info("[SUCCESS] Experience deleted: id={}, company={}", experienceId, companyName);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.error("[ERROR] Failed to delete experience {}: {}", experienceId, e.getMessage());
            throw e;
        } finally {
            clearMdc();
        }
    }

    // ==================== EDUCATION CRUD ====================

    @GetMapping("/education")
    public ResponseEntity<List<Education>> getEducations(HttpServletRequest request) {
        String correlationId = getCorrelationId(request);
        UUID userId = null;
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        String action = "GET_EDUCATIONS";
        
        try {
            setupMdc(correlationId, null, action, ipAddress, userAgent);
            logger.info("[START] Retrieving educations for user");
            
            userId = getCurrentUserId(request);
            setupMdc(correlationId, userId, action, ipAddress, userAgent);
            
            List<Education> educations = educationRepository.findByUserId(userId);
            logger.info("[SUCCESS] Retrieved {} educations for userId={}", educations.size(), userId);
            return ResponseEntity.ok(educations);
        } catch (Exception e) {
            logger.error("[ERROR] Failed to retrieve educations: {}", e.getMessage());
            throw e;
        } finally {
            clearMdc();
        }
    }

    @PostMapping("/education")
    public ResponseEntity<Education> createEducation(@Valid @RequestBody EducationRequest educationRequest, HttpServletRequest request) {
        String correlationId = getCorrelationId(request);
        UUID userId = null;
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        String action = "CREATE_EDUCATION";
        
        try {
            setupMdc(correlationId, null, action, ipAddress, userAgent);
            logger.info("[START] Creating new education");
            
            userId = getCurrentUserId(request);
            setupMdc(correlationId, userId, action, ipAddress, userAgent);
            
            Education education = Education.builder()
                    .userId(userId)
                    .institutionName(educationRequest.getInstitutionName())
                    .degree(educationRequest.getDegree())
                    .fieldOfStudy(educationRequest.getFieldOfStudy())
                    .location(educationRequest.getLocation())
                    .startDate(educationRequest.getStartDate())
                    .endDate(educationRequest.getEndDate())
                    .isCurrent(educationRequest.isCurrent())
                    .description(educationRequest.getDescription())
                    .gpa(educationRequest.getGpa())
                    .honors(educationRequest.getHonors())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            Education savedEducation = educationRepository.save(education);
            
            // Create Kafka event for education added
            EducationAddedEvent educationAddedEvent = EducationAddedEvent.fromEducation(savedEducation, userId, correlationId);
            createOutboxEvent(educationAddedEvent);
            
            logger.info("[SUCCESS] Education created with id={}, institution={}, degree={}", 
                    savedEducation.getId(), savedEducation.getInstitutionName(), savedEducation.getDegree());
            return ResponseEntity.ok(savedEducation);
        } catch (Exception e) {
            logger.error("[ERROR] Failed to create education: {}", e.getMessage());
            throw e;
        } finally {
            clearMdc();
        }
    }

    @PutMapping("/education/{educationId}")
    public ResponseEntity<Education> updateEducation(@PathVariable UUID educationId,
                                                     @Valid @RequestBody EducationRequest educationRequest,
                                                     HttpServletRequest request) {
        String correlationId = getCorrelationId(request);
        UUID userId = null;
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        String action = "UPDATE_EDUCATION";
        
        try {
            setupMdc(correlationId, null, action, ipAddress, userAgent);
            logger.info("[START] Updating education with id={}", educationId);
            
            userId = getCurrentUserId(request);
            setupMdc(correlationId, userId, action, ipAddress, userAgent);
            
            Education education = educationRepository.findById(educationId)
                    .orElseThrow(() -> new IllegalArgumentException("Education not found"));

            if (!education.getUserId().equals(userId)) {
                logger.warn("[ACCESS_DENIED] User {} attempted to update education {} owned by {}", userId, educationId, education.getUserId());
                throw new IllegalArgumentException("Not authorized to update this education");
            }

            education.setInstitutionName(educationRequest.getInstitutionName());
            education.setDegree(educationRequest.getDegree());
            education.setFieldOfStudy(educationRequest.getFieldOfStudy());
            education.setLocation(educationRequest.getLocation());
            education.setStartDate(educationRequest.getStartDate());
            education.setEndDate(educationRequest.getEndDate());
            education.setCurrent(educationRequest.isCurrent());
            education.setDescription(educationRequest.getDescription());
            education.setGpa(educationRequest.getGpa());
            education.setHonors(educationRequest.getHonors());
            education.setUpdatedAt(Instant.now());

            Education updatedEducation = educationRepository.save(education);
            
            // Create Kafka event for education updated
            EducationUpdatedEvent educationUpdatedEvent = EducationUpdatedEvent.fromEducation(updatedEducation, userId, correlationId);
            createOutboxEvent(educationUpdatedEvent);
            
            logger.info("[SUCCESS] Education updated: id={}, institution={}, degree={}", 
                    updatedEducation.getId(), updatedEducation.getInstitutionName(), updatedEducation.getDegree());
            return ResponseEntity.ok(updatedEducation);
        } catch (IllegalArgumentException e) {
            logger.error("[ERROR] Failed to update education {}: {}", educationId, e.getMessage());
            throw e;
        } finally {
            clearMdc();
        }
    }

    @DeleteMapping("/education/{educationId}")
    public ResponseEntity<Void> deleteEducation(@PathVariable UUID educationId, HttpServletRequest request) {
        String correlationId = getCorrelationId(request);
        UUID userId = null;
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        String action = "DELETE_EDUCATION";
        
        try {
            setupMdc(correlationId, null, action, ipAddress, userAgent);
            logger.info("[START] Deleting education with id={}", educationId);
            
            userId = getCurrentUserId(request);
            setupMdc(correlationId, userId, action, ipAddress, userAgent);
            
            Education education = educationRepository.findById(educationId)
                    .orElseThrow(() -> new IllegalArgumentException("Education not found"));

            if (!education.getUserId().equals(userId)) {
                logger.warn("[ACCESS_DENIED] User {} attempted to delete education {} owned by {}", userId, educationId, education.getUserId());
                throw new IllegalArgumentException("Not authorized to delete this education");
            }

            String degree = education.getDegree();
            String fieldOfStudy = education.getFieldOfStudy();
            String institutionName = education.getInstitutionName();
            educationRepository.deleteById(educationId);
            
            // Create Kafka event for education deleted
            EducationDeletedEvent educationDeletedEvent = EducationDeletedEvent.create(educationId, degree, fieldOfStudy, institutionName, userId, correlationId);
            createOutboxEvent(educationDeletedEvent);
            
            logger.info("[SUCCESS] Education deleted: id={}, institution={}", educationId, institutionName);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.error("[ERROR] Failed to delete education {}: {}", educationId, e.getMessage());
            throw e;
        } finally {
            clearMdc();
        }
    }
}
