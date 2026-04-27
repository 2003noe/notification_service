package com.microfinancehub.notification_service.model.dto.event;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RemboursementEvent {
    private Long      remboursementId;
    private Long      pretId;
    private Long      clientId;
    private String    clientNom;
    private String    clientEmail;
    private String    clientTelephone;
    private Double    montantPaye;
    private Double    montantRestant;
    private Integer   joursRetard;
    private Double    montantPenalite;
    private String    typeEvent;       // "RECU", "RETARD", "PENALITE"
    private LocalDate dateEcheance;
    private LocalDateTime dateEvenement;
}