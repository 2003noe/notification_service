package com.microfinancehub.notification_service.model.dto.event;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PretEvent {
    private Long      pretId;
    private Long      clientId;
    private String    clientNom;
    private String    clientEmail;
    private String    clientTelephone;
    private Double    montant;
    private Integer   dureeEnMois;
    private Double    tauxInteret;
    private String    statut;          // "APPROUVE", "REJETE", "DECAISSE"
    private String    motifRejet;
    private LocalDateTime dateEvenement;
}