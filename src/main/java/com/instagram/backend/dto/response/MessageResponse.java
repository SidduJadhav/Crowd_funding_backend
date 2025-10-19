package com.instagram.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class MessageResponse {

    private String message;

    @JsonProperty("status_code")
    @Builder.Default
    private int statusCode = 200;

    private String timestamp;

    public MessageResponse(String message) {
        this.message = message;
        this.statusCode = 200;
        this.timestamp = java.time.LocalDateTime.now().toString();
    }

}