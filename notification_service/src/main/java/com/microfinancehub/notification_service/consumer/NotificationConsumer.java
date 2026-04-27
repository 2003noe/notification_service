package com.microfinancehub.notification_service.consumer;

import com.microfinancehub.notification_service.config.RabbitMQConfig;
import com.microfinancehub.notification_service.model.dto.NotificationRequest;
import com.microfinancehub.notification_service.model.dto.event.CompteEvent;
import com.microfinancehub.notification_service.model.dto.event.PretEvent;
import com.microfinancehub.notification_service.model.dto.event.RemboursementEvent;
import com.microfinancehub.notification_service.model.enums.CanalNotification;
import com.microfinancehub.notification_service.model.enums.TypeNotification;
import com.microfinancehub.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;

    // ================================================================
    // QUEUE PRÊT
    // ================================================================
    @RabbitListener(queues = RabbitMQConfig.QUEUE_PRET)
    public void handlePretEvent(PretEvent event) {

        log.info("Event reçu [PRET] clientId={} statut={}",
                event.getClientId(), event.getStatut());

        try {
            switch (event.getStatut()) {

                case "APPROUVE" -> {
                    NotificationRequest req = NotificationRequest.builder()
                            .clientId(event.getClientId())
                            .email(event.getClientEmail())
                            .telephone(event.getClientTelephone())
                            .sujet("✅ Votre prêt a été approuvé")
                            .message(buildMessagePretApprouve(event))
                            .type(TypeNotification.APPROBATION_PRET)
                            .canal(CanalNotification.EMAIL_SMS)
                            .priorite(2)
                            .referenceId(event.getPretId())
                            .referenceType("PRET")
                            .build();

                    notificationService.envoyerNotification(req);
                }

                case "REJETE" -> {
                    NotificationRequest req = NotificationRequest.builder()
                            .clientId(event.getClientId())
                            .email(event.getClientEmail())
                            .telephone(event.getClientTelephone())
                            .sujet("❌ Votre demande de prêt a été rejetée")
                            .message(buildMessagePretRejete(event))
                            .type(TypeNotification.REJET_PRET)
                            .canal(CanalNotification.EMAIL_SMS)
                            .priorite(3)
                            .referenceId(event.getPretId())
                            .referenceType("PRET")
                            .build();

                    notificationService.envoyerNotification(req);
                }

                case "DECAISSE" -> {
                    NotificationRequest req = NotificationRequest.builder()
                            .clientId(event.getClientId())
                            .email(event.getClientEmail())
                            .telephone(event.getClientTelephone())
                            .sujet("💰 Votre prêt a été décaissé")
                            .message(buildMessagePretDecaisse(event))
                            .type(TypeNotification.DECAISSEMENT)
                            .canal(CanalNotification.EMAIL_SMS)
                            .priorite(1)
                            .referenceId(event.getPretId())
                            .referenceType("PRET")
                            .build();

                    notificationService.envoyerNotification(req);
                }

                default -> log.warn("Statut prêt inconnu: {}", event.getStatut());
            }

        } catch (Exception e) {
            log.error("Erreur traitement event PRET clientId={} : {}",
                    event.getClientId(), e.getMessage(), e);
        }
    }

    // ================================================================
    // QUEUE REMBOURSEMENT
    // ================================================================
    @RabbitListener(queues = RabbitMQConfig.QUEUE_REMBOURSEMENT)
    public void handleRemboursementEvent(RemboursementEvent event) {

        log.info("Event reçu [REMBOURSEMENT] clientId={} type={}",
                event.getClientId(), event.getTypeEvent());

        try {
            switch (event.getTypeEvent()) {

                case "RECU" -> {
                    NotificationRequest req = NotificationRequest.builder()
                            .clientId(event.getClientId())
                            .email(event.getClientEmail())
                            .telephone(event.getClientTelephone())
                            .sujet("✅ Remboursement confirmé")
                            .message(buildMessageRemboursementRecu(event))
                            .type(TypeNotification.CONFIRMATION_REMB)
                            .canal(CanalNotification.SMS)
                            .priorite(3)
                            .referenceId(event.getRemboursementId())
                            .referenceType("REMBOURSEMENT")
                            .build();

                    notificationService.envoyerNotification(req);
                }

                case "RETARD" -> {
                    NotificationRequest req = NotificationRequest.builder()
                            .clientId(event.getClientId())
                            .email(event.getClientEmail())
                            .telephone(event.getClientTelephone())
                            .sujet("⚠️ Alerte retard de paiement")
                            .message(buildMessageRetard(event))
                            .type(TypeNotification.ALERTE_RETARD)
                            .canal(CanalNotification.EMAIL_SMS)
                            .priorite(1)
                            .referenceId(event.getPretId())
                            .referenceType("PRET")
                            .build();

                    notificationService.envoyerNotification(req);
                }

                case "PENALITE" -> {
                    NotificationRequest req = NotificationRequest.builder()
                            .clientId(event.getClientId())
                            .email(event.getClientEmail())
                            .telephone(event.getClientTelephone())
                            .sujet("🔴 Pénalité appliquée sur votre prêt")
                            .message(buildMessagePenalite(event))
                            .type(TypeNotification.PENALITE_APPLIQUEE)
                            .canal(CanalNotification.EMAIL_SMS)
                            .priorite(1)
                            .referenceId(event.getPretId())
                            .referenceType("PRET")
                            .build();

                    notificationService.envoyerNotification(req);
                }

                default -> log.warn("TypeEvent remboursement inconnu: {}",
                        event.getTypeEvent());
            }

        } catch (Exception e) {
            log.error("Erreur traitement event REMBOURSEMENT clientId={} : {}",
                    event.getClientId(), e.getMessage(), e);
        }
    }

    // ================================================================
    // QUEUE COMPTE
    // ================================================================
    @RabbitListener(queues = RabbitMQConfig.QUEUE_COMPTE)
    public void handleCompteEvent(CompteEvent event) {

        log.info("Event reçu [COMPTE] clientId={} type={}",
                event.getClientId(), event.getTypeEvent());

        try {
            switch (event.getTypeEvent()) {

                case "CREE" -> {
                    NotificationRequest req = NotificationRequest.builder()
                            .clientId(event.getClientId())
                            .email(event.getClientEmail())
                            .telephone(event.getClientTelephone())
                            .sujet("🎉 Bienvenue chez MicroFinanceHub !")
                            .message(buildMessageCompteCree(event))
                            .type(TypeNotification.CREATION_COMPTE)
                            .canal(CanalNotification.EMAIL_SMS)
                            .priorite(2)
                            .referenceId(event.getCompteId())
                            .referenceType("COMPTE")
                            .build();

                    notificationService.envoyerNotification(req);
                }

                case "DEPOT" -> {
                    NotificationRequest req = NotificationRequest.builder()
                            .clientId(event.getClientId())
                            .email(event.getClientEmail())
                            .telephone(event.getClientTelephone())
                            .sujet("💵 Dépôt effectué sur votre compte")
                            .message(buildMessageDepot(event))
                            .type(TypeNotification.DEPOT_EFFECTUE)
                            .canal(CanalNotification.SMS)
                            .priorite(4)
                            .referenceId(event.getCompteId())
                            .referenceType("COMPTE")
                            .build();

                    notificationService.envoyerNotification(req);
                }

                case "RETRAIT" -> {
                    NotificationRequest req = NotificationRequest.builder()
                            .clientId(event.getClientId())
                            .email(event.getClientEmail())
                            .telephone(event.getClientTelephone())
                            .sujet("💸 Retrait effectué sur votre compte")
                            .message(buildMessageRetrait(event))
                            .type(TypeNotification.RETRAIT_EFFECTUE)
                            .canal(CanalNotification.SMS)
                            .priorite(4)
                            .referenceId(event.getCompteId())
                            .referenceType("COMPTE")
                            .build();

                    notificationService.envoyerNotification(req);
                }

                default -> log.warn("TypeEvent compte inconnu: {}",
                        event.getTypeEvent());
            }

        } catch (Exception e) {
            log.error("Erreur traitement event COMPTE clientId={} : {}",
                    event.getClientId(), e.getMessage(), e);
        }
    }

    // ================================================================
    // BUILDERS DE MESSAGES
    // ================================================================
    private String buildMessagePretApprouve(PretEvent e) {
        return String.format(
            "Bonjour %s,\n\n" +
            "Votre demande de prêt a été approuvée.\n\n" +
            "Détails :\n" +
            "- Montant approuvé : %.0f XAF\n" +
            "- Durée : %d mois\n" +
            "- Taux d'intérêt : %.1f%%\n\n" +
            "Votre argent sera disponible sous 24h.\n\n" +
            "MicroFinanceHub",
            e.getClientNom(),
            e.getMontant(),
            e.getDureeEnMois(),
            e.getTauxInteret()
        );
    }

    private String buildMessagePretRejete(PretEvent e) {
        return String.format(
            "Bonjour %s,\n\n" +
            "Nous avons le regret de vous informer que votre demande " +
            "de prêt de %.0f XAF n'a pas été approuvée.\n\n" +
            "Motif : %s\n\n" +
            "Vous pouvez soumettre une nouvelle demande dans 30 jours.\n\n" +
            "MicroFinanceHub",
            e.getClientNom(),
            e.getMontant(),
            e.getMotifRejet() != null ? e.getMotifRejet() : "Dossier incomplet"
        );
    }

    private String buildMessagePretDecaisse(PretEvent e) {
        return String.format(
            "Bonjour %s,\n\n" +
            "Votre prêt de %.0f XAF a été décaissé sur votre compte.\n\n" +
            "Connectez-vous à l'application pour consulter votre échéancier.\n\n" +
            "MicroFinanceHub",
            e.getClientNom(),
            e.getMontant()
        );
    }

    private String buildMessageRemboursementRecu(RemboursementEvent e) {
        return String.format(
            "Bonjour %s,\n\n" +
            "Votre paiement de %.0f XAF a bien été reçu.\n" +
            "Reste à payer : %.0f XAF\n\n" +
            "Merci pour votre ponctualité.\n\n" +
            "MicroFinanceHub",
            e.getClientNom(),
            e.getMontantPaye(),
            e.getMontantRestant()
        );
    }

    private String buildMessageRetard(RemboursementEvent e) {
        return String.format(
            "Bonjour %s,\n\n" +
            "⚠️ Votre paiement est en retard de %d jour(s).\n\n" +
            "Montant dû : %.0f XAF\n" +
            "Date d'échéance dépassée : %s\n\n" +
            "Veuillez régulariser votre situation au plus vite " +
            "pour éviter des pénalités supplémentaires.\n\n" +
            "MicroFinanceHub",
            e.getClientNom(),
            e.getJoursRetard(),
            e.getMontantPaye(),
            e.getDateEcheance()
        );
    }

    private String buildMessagePenalite(RemboursementEvent e) {
        return String.format(
            "Bonjour %s,\n\n" +
            "Une pénalité de %.0f XAF a été appliquée sur votre prêt " +
            "suite à un retard de %d jour(s).\n\n" +
            "Contactez-nous rapidement pour régulariser.\n\n" +
            "MicroFinanceHub",
            e.getClientNom(),
            e.getMontantPenalite(),
            e.getJoursRetard()
        );
    }

    private String buildMessageCompteCree(CompteEvent e) {
        return String.format(
            "Bonjour %s,\n\n" +
            "🎉 Bienvenue chez MicroFinanceHub !\n\n" +
            "Votre compte a été créé avec succès.\n" +
            "Numéro de compte : %s\n\n" +
            "Vous pouvez dès maintenant accéder à nos services.\n\n" +
            "MicroFinanceHub",
            e.getClientNom(),
            e.getNumeroCompte()
        );
    }

    private String buildMessageDepot(CompteEvent e) {
        return String.format(
            "Bonjour %s,\n\n" +
            "Un dépôt de %.0f XAF a été effectué sur votre compte.\n" +
            "Solde actuel : %.0f XAF\n\n" +
            "MicroFinanceHub",
            e.getClientNom(),
            e.getMontant(),
            e.getSoldeApres()
        );
    }

    private String buildMessageRetrait(CompteEvent e) {
        return String.format(
            "Bonjour %s,\n\n" +
            "Un retrait de %.0f XAF a été effectué sur votre compte.\n" +
            "Solde actuel : %.0f XAF\n\n" +
            "MicroFinanceHub",
            e.getClientNom(),
            e.getMontant(),
            e.getSoldeApres()
        );
    }
}