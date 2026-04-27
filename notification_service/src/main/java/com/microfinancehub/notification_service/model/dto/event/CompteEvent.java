package com.microfinancehub.notification_service.model.dto.event;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompteEvent {
    private Long      compteId;
    private Long      clientId;
    private String    clientNom;
    private String    clientEmail;
    private String    clientTelephone;
    private String    numeroCompte;
    private Double    montant;
    private Double    soldeApres;
    private String    typeEvent;       // "CREE", "DEPOT", "RETRAIT"
    private LocalDateTime dateEvenement;
}