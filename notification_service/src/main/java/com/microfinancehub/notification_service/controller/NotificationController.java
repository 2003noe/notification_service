package com.microfinancehub.notification_service.controller;

import com.microfinancehub.notification_service.config.RabbitMQConfig;
import com.microfinancehub.notification_service.model.dto.NotificationRequest;
import com.microfinancehub.notification_service.model.dto.NotificationResponse;
import com.microfinancehub.notification_service.model.dto.event.CompteEvent;
import com.microfinancehub.notification_service.model.dto.event.PretEvent;
import com.microfinancehub.notification_service.model.dto.event.RemboursementEvent;
import com.microfinancehub.notification_service.service.EmailService;
import com.microfinancehub.notification_service.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final RabbitTemplate rabbitTemplate;
    private final EmailService emailService;

    // ================================================================
    // POST /api/v1/notifications
    // ================================================================
    @PostMapping
    public ResponseEntity<NotificationResponse> envoyerNotification(
            @Valid @RequestBody NotificationRequest request) {

        log.info("REST → envoi notification type={} canal={}",
                request.getType(), request.getCanal());

        NotificationResponse response =
                notificationService.envoyerNotification(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ================================================================
    // GET /api/v1/notifications/{id}
    // ================================================================
    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(notificationService.getById(id));
    }

    // ================================================================
    // GET /api/v1/notifications/client/{clientId}
    // ================================================================
    @GetMapping("/client/{clientId}")
    public ResponseEntity<Page<NotificationResponse>> getHistoriqueClient(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return ResponseEntity.ok(
                notificationService.getHistoriqueClient(clientId, pageable));
    }

    // ================================================================
    // GET /api/v1/notifications/client/{clientId}/non-lues
    // ================================================================
    @GetMapping("/client/{clientId}/non-lues")
    public ResponseEntity<Map<String, Long>> countNonLues(
            @PathVariable Long clientId) {

        long count = notificationService.countNonLues(clientId);

        Map<String, Long> result = new HashMap<>();
        result.put("nonLues", count);

        return ResponseEntity.ok(result);
    }

    // ================================================================
    // PATCH /api/v1/notifications/{id}/lue
    // ================================================================
    @PatchMapping("/{id}/lue")
    public ResponseEntity<Void> marquerCommeLue(
            @PathVariable Long id) {

        notificationService.marquerCommeLue(id);
        return ResponseEntity.noContent().build();
    }

    // ================================================================
    // POST /api/v1/notifications/retry
    // ================================================================
    @PostMapping("/retry")
    public ResponseEntity<Map<String, String>> retryEchecs() {

        notificationService.reessayerNotificationsEnEchec();

        Map<String, String> result = new HashMap<>();
        result.put("statut", "retry lancé");

        return ResponseEntity.ok(result);
    }

    // ================================================================
    // TEST — PRÊT APPROUVÉ (email HTML dynamique)
    // ================================================================
    @PostMapping("/test/pret-approuve")
    public ResponseEntity<Map<String, String>> testPretApprouve(
            @RequestParam String destinataire,
            @RequestParam(defaultValue = "Jean Nkomo") String clientNom,
            @RequestParam(defaultValue = "500000") Double montant,
            @RequestParam(defaultValue = "12") Integer dureeEnMois,
            @RequestParam(defaultValue = "8.5") Double tauxInteret) {

        PretEvent event = PretEvent.builder()
                .pretId(1L)
                .clientId(1L)
                .clientNom(clientNom)
                .clientEmail(destinataire)
                .clientTelephone("+237699000001")
                .montant(montant)
                .dureeEnMois(dureeEnMois)
                .tauxInteret(tauxInteret)
                .statut("APPROUVE")
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NOTIFICATIONS,
                RabbitMQConfig.RK_PRET_APPROUVE,
                event);

        Map<String, Object> variables = new HashMap<>();
        variables.put("clientNom", clientNom);
        variables.put("montant", montant);
        variables.put("dureeEnMois", dureeEnMois);
        variables.put("tauxInteret", tauxInteret);
        variables.put("dateApprobation", LocalDateTime.now());

        emailService.envoyerPretApprouve(destinataire, variables);

        Map<String, String> result = new HashMap<>();
        result.put("statut", "Email prêt approuvé envoyé à " + destinataire);
        result.put("clientNom", clientNom);
        result.put("montant", montant + " XAF");

        return ResponseEntity.ok(result);
    }

    // ================================================================
    // TEST — CONFIRMATION REMBOURSEMENT
    // ================================================================
    @PostMapping("/test/remboursement-recu")
    public ResponseEntity<Map<String, String>> testRemboursement(
            @RequestParam String destinataire,
            @RequestParam(defaultValue = "Alice Ndjock") String clientNom,
            @RequestParam(defaultValue = "45000") Double montantPaye,
            @RequestParam(defaultValue = "405000") Double montantRestant) {

        RemboursementEvent event = RemboursementEvent.builder()
                .remboursementId(1L)
                .pretId(1L)
                .clientId(2L)
                .clientNom(clientNom)
                .clientEmail(destinataire)
                .clientTelephone("+237677000002")
                .montantPaye(montantPaye)
                .montantRestant(montantRestant)
                .typeEvent("RECU")
                .dateEcheance(LocalDate.now())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NOTIFICATIONS,
                RabbitMQConfig.RK_REMB_RECU,
                event);

        Map<String, Object> variables = new HashMap<>();
        variables.put("clientNom", clientNom);
        variables.put("montantPaye", montantPaye);
        variables.put("montantRestant", montantRestant);
        variables.put("datePaiement", LocalDateTime.now());

        emailService.envoyerConfirmationRemboursement(
                destinataire,
                variables
        );

        Map<String, String> result = new HashMap<>();
        result.put("statut",
                "Email confirmation remboursement envoyé à " + destinataire);
        result.put("clientNom", clientNom);
        result.put("montantPaye", montantPaye + " XAF");

        return ResponseEntity.ok(result);
    }

    // ================================================================
    // TEST — ALERTE RETARD
    // ================================================================
    @PostMapping("/test/retard")
    public ResponseEntity<Map<String, String>> testRetard(
            @RequestParam String destinataire,
            @RequestParam(defaultValue = "Paul Essomba") String clientNom,
            @RequestParam(defaultValue = "7") Integer joursRetard,
            @RequestParam(defaultValue = "52000") Double montantDu) {

        RemboursementEvent event = RemboursementEvent.builder()
                .remboursementId(2L)
                .pretId(2L)
                .clientId(3L)
                .clientNom(clientNom)
                .clientEmail(destinataire)
                .clientTelephone("+237655000003")
                .montantPaye(montantDu)
                .montantRestant(montantDu)
                .joursRetard(joursRetard)
                .montantPenalite(montantDu * 0.02)
                .typeEvent("RETARD")
                .dateEcheance(LocalDate.now().minusDays(joursRetard))
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NOTIFICATIONS,
                RabbitMQConfig.RK_REMB_RETARD,
                event);

        Map<String, Object> variables = new HashMap<>();
        variables.put("clientNom", clientNom);
        variables.put("joursRetard", joursRetard);
        variables.put("montantDu", montantDu);
        variables.put("dateEcheance",
                LocalDate.now().minusDays(joursRetard));
        variables.put("penalite", montantDu * 0.02);

        emailService.envoyerHtml(
                destinataire,
                "Alerte retard de paiement - MicroFinanceHub",
                "alerte-retard",
                variables
        );

        Map<String, String> result = new HashMap<>();
        result.put("statut", "Email alerte retard envoyé à " + destinataire);
        result.put("joursRetard", joursRetard + " jours");

        return ResponseEntity.ok(result);
    }

    // ================================================================
    // TEST — COMPTE CRÉÉ
    // ================================================================
    @PostMapping("/test/compte-cree")
    public ResponseEntity<Map<String, String>> testCompteCree(
            @RequestParam String destinataire,
            @RequestParam(defaultValue = "Marie Kouam") String clientNom,
            @RequestParam(defaultValue = "MFH-2024-00042") String numeroCompte) {

        CompteEvent event = CompteEvent.builder()
                .compteId(1L)
                .clientId(4L)
                .clientNom(clientNom)
                .clientEmail(destinataire)
                .clientTelephone("+237699000004")
                .numeroCompte(numeroCompte)
                .montant(0.0)
                .soldeApres(0.0)
                .typeEvent("CREE")
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NOTIFICATIONS,
                RabbitMQConfig.RK_COMPTE_CREE,
                event);

        Map<String, Object> variables = new HashMap<>();
        variables.put("clientNom", clientNom);
        variables.put("numeroCompte", numeroCompte);
        variables.put("dateCreation", LocalDateTime.now());

        emailService.envoyerHtml(
                destinataire,
                "Bienvenue chez MicroFinanceHub !",
                "compte-cree",
                variables
        );

        Map<String, String> result = new HashMap<>();
        result.put("statut", "Email bienvenue envoyé à " + destinataire);
        result.put("numeroCompte", numeroCompte);

        return ResponseEntity.ok(result);
    }

    // ================================================================
    // TEST — EMAIL HTML direct
    // ================================================================
    @PostMapping("/test/email-html")
    public ResponseEntity<Map<String, String>> testEmailHtml(
            @RequestParam String destinataire,
            @RequestParam(defaultValue = "Jean Nkomo") String clientNom,
            @RequestParam(defaultValue = "500000") Double montant) {

        Map<String, Object> variables = new HashMap<>();
        variables.put("clientNom", clientNom);
        variables.put("montant", montant);
        variables.put("dureeEnMois", 12);
        variables.put("tauxInteret", 8.5);
        variables.put("dateApprobation", LocalDateTime.now());

        emailService.envoyerPretApprouve(destinataire, variables);

        Map<String, String> result = new HashMap<>();
        result.put("statut", "Email envoyé à " + destinataire);
        result.put("clientNom", clientNom);
        result.put("montant", montant + " XAF");

        return ResponseEntity.ok(result);
    }
}