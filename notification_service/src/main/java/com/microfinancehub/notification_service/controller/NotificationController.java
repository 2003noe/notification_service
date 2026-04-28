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
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

        NotificationResponse response =
                notificationService.envoyerNotification(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ================================================================
    // GET /api/v1/notifications/{id}
    // ================================================================
    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getById(@PathVariable Long id) {
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
    public ResponseEntity<Void> marquerCommeLue(@PathVariable Long id) {
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
    // TEST — PRÊT APPROUVÉ
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

        return ResponseEntity.ok(result);
    }

    // ================================================================
    // TEST — PRÊT REJETÉ
    // ================================================================
    @PostMapping("/test/pret-rejete")
    public ResponseEntity<Map<String, String>> testPretRejete(
            @RequestParam String destinataire,
            @RequestParam(defaultValue = "Jean Nkomo") String clientNom,
            @RequestParam(defaultValue = "500000") Double montant,
            @RequestParam(defaultValue = "Dossier incomplet") String motifRejet) {

        PretEvent event = PretEvent.builder()
                .pretId(2L)
                .clientId(1L)
                .clientNom(clientNom)
                .clientEmail(destinataire)
                .clientTelephone("+237699000001")
                .montant(montant)
                .statut("REJETE")
                .motifRejet(motifRejet)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NOTIFICATIONS,
                RabbitMQConfig.RK_PRET_REJETE,
                event);

        Map<String, Object> variables = new HashMap<>();
        variables.put("clientNom", clientNom);
        variables.put("montant", montant);
        variables.put("motifRejet", motifRejet);
        variables.put("dateRejet", LocalDateTime.now());

        emailService.envoyerHtml(
                destinataire,
                "Résultat de votre demande de prêt - MicroFinanceHub",
                "pret-rejete",
                variables
        );

        Map<String, String> result = new HashMap<>();
        result.put("statut", "Email prêt rejeté envoyé à " + destinataire);

        return ResponseEntity.ok(result);
    }

    // ================================================================
    // TEST — PRÊT DÉCAISSÉ
    // ================================================================
    @PostMapping("/test/pret-decaisse")
    public ResponseEntity<Map<String, String>> testPretDecaisse(
            @RequestParam String destinataire,
            @RequestParam(defaultValue = "Jean Nkomo") String clientNom,
            @RequestParam(defaultValue = "500000") Double montant) {

        PretEvent event = PretEvent.builder()
                .pretId(3L)
                .clientId(1L)
                .clientNom(clientNom)
                .clientEmail(destinataire)
                .clientTelephone("+237699000001")
                .montant(montant)
                .statut("DECAISSE")
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NOTIFICATIONS,
                RabbitMQConfig.RK_PRET_DECAISSE,
                event);

        Map<String, Object> variables = new HashMap<>();
        variables.put("clientNom", clientNom);
        variables.put("montant", montant);
        variables.put("dateDecaissement", LocalDateTime.now());

        emailService.envoyerHtml(
                destinataire,
                "Votre prêt a été décaissé - MicroFinanceHub",
                "pret-decaisse",
                variables
        );

        Map<String, String> result = new HashMap<>();
        result.put("statut", "Email décaissement envoyé à " + destinataire);

        return ResponseEntity.ok(result);
    }

    // ================================================================
    // TEST — RAPPEL ÉCHÉANCE
    // ================================================================
    @PostMapping("/test/rappel-echeance")
    public ResponseEntity<Map<String, String>> testRappelEcheance(
            @RequestParam String destinataire,
            @RequestParam(defaultValue = "Jean Nkomo") String clientNom,
            @RequestParam(defaultValue = "62500") Double montant,
            @RequestParam(defaultValue = "5") Integer joursAvantEcheance) {

        Map<String, Object> variables = new HashMap<>();
        variables.put("clientNom", clientNom);
        variables.put("montant", montant);
        variables.put("joursAvantEcheance", joursAvantEcheance);
        variables.put("dateEcheance",
                LocalDate.now().plusDays(joursAvantEcheance));

        emailService.envoyerRappelEcheance(destinataire, variables);

        Map<String, String> result = new HashMap<>();
        result.put("statut", "Email rappel échéance envoyé à " + destinataire);

        return ResponseEntity.ok(result);
    }

    // ================================================================
    // TEST — PÉNALITÉ
    // ================================================================
    @PostMapping("/test/penalite")
    public ResponseEntity<Map<String, String>> testPenalite(
            @RequestParam String destinataire,
            @RequestParam(defaultValue = "Jean Nkomo") String clientNom,
            @RequestParam(defaultValue = "15") Integer joursRetard,
            @RequestParam(defaultValue = "62500") Double montantDu) {

        double penalite = montantDu * 0.05;

        RemboursementEvent event = RemboursementEvent.builder()
                .remboursementId(3L)
                .pretId(1L)
                .clientId(1L)
                .clientNom(clientNom)
                .clientEmail(destinataire)
                .clientTelephone("+237699000001")
                .montantPaye(montantDu)
                .montantRestant(montantDu + penalite)
                .joursRetard(joursRetard)
                .montantPenalite(penalite)
                .typeEvent("PENALITE")
                .dateEcheance(LocalDate.now().minusDays(joursRetard))
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NOTIFICATIONS,
                RabbitMQConfig.RK_PENALITE,
                event);

        Map<String, Object> variables = new HashMap<>();
        variables.put("clientNom", clientNom);
        variables.put("joursRetard", joursRetard);
        variables.put("montantDu", montantDu);
        variables.put("penalite", penalite);
        variables.put("montantTotal", montantDu + penalite);
        variables.put("dateCalcul", LocalDateTime.now());

        emailService.envoyerHtml(
                destinataire,
                "Pénalité appliquée sur votre prêt - MicroFinanceHub",
                "penalite",
                variables
        );

        Map<String, String> result = new HashMap<>();
        result.put("statut", "Email pénalité envoyé à " + destinataire);

        return ResponseEntity.ok(result);
    }

    // ================================================================
    // TEST — DÉPÔT
    // ================================================================
    @PostMapping("/test/depot")
    public ResponseEntity<Map<String, String>> testDepot(
            @RequestParam String destinataire,
            @RequestParam(defaultValue = "Jean Nkomo") String clientNom,
            @RequestParam(defaultValue = "150000") Double montant,
            @RequestParam(defaultValue = "350000") Double soldeApres) {

        CompteEvent event = CompteEvent.builder()
                .compteId(1L)
                .clientId(1L)
                .clientNom(clientNom)
                .clientEmail(destinataire)
                .clientTelephone("+237699000001")
                .numeroCompte("MFH-2024-00042")
                .montant(montant)
                .soldeApres(soldeApres)
                .typeEvent("DEPOT")
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NOTIFICATIONS,
                RabbitMQConfig.RK_DEPOT,
                event);

        Map<String, Object> variables = new HashMap<>();
        variables.put("clientNom", clientNom);
        variables.put("montant", montant);
        variables.put("soldeApres", soldeApres);
        variables.put("numeroCompte", "MFH-2024-00042");
        variables.put("dateOperation", LocalDateTime.now());

        emailService.envoyerHtml(
                destinataire,
                "Dépôt confirmé - MicroFinanceHub",
                "depot",
                variables
        );

        Map<String, String> result = new HashMap<>();
        result.put("statut", "Email dépôt envoyé à " + destinataire);

        return ResponseEntity.ok(result);
    }

    // ================================================================
    // TEST — RETRAIT
    // ================================================================
    @PostMapping("/test/retrait")
    public ResponseEntity<Map<String, String>> testRetrait(
            @RequestParam String destinataire,
            @RequestParam(defaultValue = "Jean Nkomo") String clientNom,
            @RequestParam(defaultValue = "50000") Double montant,
            @RequestParam(defaultValue = "300000") Double soldeApres) {

        CompteEvent event = CompteEvent.builder()
                .compteId(1L)
                .clientId(1L)
                .clientNom(clientNom)
                .clientEmail(destinataire)
                .clientTelephone("+237699000001")
                .numeroCompte("MFH-2024-00042")
                .montant(montant)
                .soldeApres(soldeApres)
                .typeEvent("RETRAIT")
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NOTIFICATIONS,
                RabbitMQConfig.RK_RETRAIT,
                event);

        Map<String, Object> variables = new HashMap<>();
        variables.put("clientNom", clientNom);
        variables.put("montant", montant);
        variables.put("soldeApres", soldeApres);
        variables.put("numeroCompte", "MFH-2024-00042");
        variables.put("dateOperation", LocalDateTime.now());

        emailService.envoyerHtml(
                destinataire,
                "Retrait confirmé - MicroFinanceHub",
                "retrait",
                variables
        );

        Map<String, String> result = new HashMap<>();
        result.put("statut", "Email retrait envoyé à " + destinataire);

        return ResponseEntity.ok(result);
    }

    // ================================================================
    // TEST — REMBOURSEMENT CONFIRMÉ
    // ================================================================
    @PostMapping("/test/remboursement-confirme")
    public ResponseEntity<Map<String, String>> testRemboursementConfirme(
            @RequestParam String destinataire,
            @RequestParam(defaultValue = "Noe Biyiha") String clientNom,
            @RequestParam(defaultValue = "62500") Double montantPaye,
            @RequestParam(defaultValue = "437500") Double montantRestant,
            @RequestParam(defaultValue = "MFH-PRET-001") String numeroPret) {

        double montantTotal = montantPaye + montantRestant;
        double progression = Math.round((montantPaye / montantTotal) * 100);

        Map<String, Object> variables = new HashMap<>();
        variables.put("clientNom", clientNom);
        variables.put("montantPaye", montantPaye);
        variables.put("montantRestant", montantRestant);
        variables.put("numeroPret", numeroPret);
        variables.put("datePaiement", LocalDateTime.now());
        variables.put("progression", (int) progression);

        emailService.envoyerHtml(destinataire,
                "Remboursement confirmé - MicroFinanceHub",
                "remboursement-confirme",
                variables);

        Map<String, String> result = new HashMap<>();
        result.put("statut", "Email remboursement envoyé à " + destinataire);
        result.put("progression", (int) progression + "%");

        return ResponseEntity.ok(result);
    }

    // ================================================================
    // TEST — COMPTE CRÉÉ
    // ================================================================
    @PostMapping("/test/compte-cree")
    public ResponseEntity<Map<String, String>> testCompteCree(
            @RequestParam String destinataire,
            @RequestParam(defaultValue = "Noe Biyiha") String clientNom,
            @RequestParam(defaultValue = "MFH-2024-00099") String numeroCompte,
            @RequestParam(defaultValue = "EPARGNE") String typeCompte) {

        CompteEvent event = CompteEvent.builder()
                .compteId(1L)
                .clientId(1L)
                .clientNom(clientNom)
                .clientEmail(destinataire)
                .clientTelephone("+237699000001")
                .numeroCompte(numeroCompte)
                .typeEvent("CREE")
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NOTIFICATIONS,
                RabbitMQConfig.RK_COMPTE_CREE,
                event);

        Map<String, Object> variables = new HashMap<>();
        variables.put("clientNom", clientNom);
        variables.put("numeroCompte", numeroCompte);
        variables.put("typeCompte", typeCompte);
        variables.put("dateCreation", LocalDateTime.now());

        emailService.envoyerHtml(destinataire,
                "Votre compte MicroFinanceHub est créé !",
                "compte-cree",
                variables);

        Map<String, String> result = new HashMap<>();
        result.put("statut", "Email compte créé envoyé à " + destinataire);
        result.put("numeroCompte", numeroCompte);

        return ResponseEntity.ok(result);
    }

    // ================================================================
    // TEST — BIENVENUE
    // ================================================================
    @PostMapping("/test/bienvenue")
    public ResponseEntity<Map<String, String>> testBienvenue(
            @RequestParam String destinataire,
            @RequestParam(defaultValue = "Noe Biyiha") String clientNom) {

        Map<String, Object> variables = new HashMap<>();
        variables.put("clientNom", clientNom);
        variables.put("dateInscription", LocalDateTime.now());

        emailService.envoyerHtml(destinataire,
                "Bienvenue chez MicroFinanceHub !",
                "bienvenue",
                variables);

        Map<String, String> result = new HashMap<>();
        result.put("statut", "Email bienvenue envoyé à " + destinataire);

        return ResponseEntity.ok(result);
    }

    // ================================================================
    // TEST — DEMANDE PRÊT REÇUE
    // ================================================================
    @PostMapping("/test/demande-pret")
    public ResponseEntity<Map<String, String>> testDemandePret(
            @RequestParam String destinataire,
            @RequestParam(defaultValue = "Noe Biyiha") String clientNom,
            @RequestParam(defaultValue = "500000") Double montant,
            @RequestParam(defaultValue = "12") Integer dureeEnMois) {

        String referenceDossier =
                "MFH-REQ-" + String.valueOf(System.currentTimeMillis()).substring(8);

        Map<String, Object> variables = new HashMap<>();
        variables.put("clientNom", clientNom);
        variables.put("montant", montant);
        variables.put("dureeEnMois", dureeEnMois);
        variables.put("referenceDossier", referenceDossier);
        variables.put("dateSubmission", LocalDateTime.now());

        emailService.envoyerHtml(destinataire,
                "Votre demande de prêt a été reçue - MicroFinanceHub",
                "demande-pret",
                variables);

        Map<String, String> result = new HashMap<>();
        result.put("statut", "Email demande prêt envoyé à " + destinataire);
        result.put("referenceDossier", referenceDossier);

        return ResponseEntity.ok(result);
    }

    // ================================================================
    // TEST — RAPPORT MENSUEL
    // ================================================================
    @PostMapping("/test/rapport-mensuel")
    public ResponseEntity<Map<String, String>> testRapportMensuel(
            @RequestParam String destinataire,
            @RequestParam(defaultValue = "Noe Biyiha") String clientNom,
            @RequestParam(defaultValue = "350000") Double solde,
            @RequestParam(defaultValue = "150000") Double totalDepots,
            @RequestParam(defaultValue = "50000") Double totalRetraits,
            @RequestParam(defaultValue = "437500") Double resteAPayer) {

        List<Map<String, Object>> pretsActifs = new ArrayList<>();

        Map<String, Object> pret1 = new HashMap<>();
        pret1.put("reference", "MFH-PRET-001");
        pret1.put("montantInitial", 500000.0);
        pret1.put("resteAPayer", resteAPayer);
        pret1.put("prochaineEcheance",
                LocalDate.now().plusDays(15).toString());

        pretsActifs.add(pret1);

        Map<String, Object> variables = new HashMap<>();
        variables.put("clientNom", clientNom);
        variables.put("mois", LocalDate.now()
                .getMonth()
                .getDisplayName(TextStyle.FULL, Locale.FRENCH));
        variables.put("annee", LocalDate.now().getYear());
        variables.put("solde", solde);
        variables.put("totalDepots", totalDepots);
        variables.put("totalRetraits", totalRetraits);
        variables.put("resteAPayer", resteAPayer);
        variables.put("pretsActifs", pretsActifs);

        emailService.envoyerHtml(destinataire,
                "Votre rapport mensuel - MicroFinanceHub",
                "rapport-mensuel",
                variables);

        Map<String, String> result = new HashMap<>();
        result.put("statut", "Email rapport mensuel envoyé à " + destinataire);

        return ResponseEntity.ok(result);
    }
}