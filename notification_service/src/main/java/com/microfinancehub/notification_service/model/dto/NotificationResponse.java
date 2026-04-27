package com.microfinancehub.notification_service.model.dto;

import com.microfinancehub.notification_service.model.enums.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;
    private Long clientId;
    private String sujet;
    private String message;
    private TypeNotification type;
    private CanalNotification canal;
    private StatutNotification statut;
    private Integer priorite;
    private Integer tentatives;
    private LocalDateTime dateEnvoi;
    private LocalDateTime dateProgrammee;
    private LocalDateTime createdAt;
    private Long referenceId;
    private String referenceType;
    private String erreurMessage;
}