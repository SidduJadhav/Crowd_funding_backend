package com.instagram.backend.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "campaign_updates", indexes = {
        @Index(name = "idx_campaign_update_campaign_created", columnList = "campaign_id, created_at")
})
public class CampaignUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @NotBlank(message = "Update title is required")
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank(message = "Update content is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "is_milestone", nullable = false)
    private Boolean isMilestone = false;

    @Column(name = "milestone_description")
    private String milestoneDescription;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}