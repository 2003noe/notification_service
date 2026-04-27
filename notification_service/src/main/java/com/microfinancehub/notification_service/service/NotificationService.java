package com.microfinancehub.notification_service.service;

import com.microfinancehub.notification_service.config.NotificationProperties;
import com.microfinancehub.notification_service.model.dto.NotificationRequest;
import com.microfinancehub.notification_service.model.dto.NotificationResponse;
import com.microfinancehub.notification_service.model.entity.Notification;
import com.microfinancehub.notification_service.model.enums.StatutNotification;
import com.microfinancehub.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final NotificationProperties properties;

    // ================================================================
    // ENVOI PRINCIPAL
    // ================================================================
    @Transactional
    public NotificationResponse envoyerNotification(NotificationRequest request) {

        Notification notification = Notification.builder()
                .clientId(request.getClientId())
                .destinataireEmail(request.getEmail())
                .destinataireTelephone(request.getTelephone())
                .sujet(request.getSujet())
                .message(request.getMessage())
                .type(request.getType())
                .canal(request.getCanal())
                .priorite(request.getPriorite())
                .referenceId(request.getReferenceId())
                .referenceType(request.getReferenceType())
                .statut(
                        request.getDateProgrammee() != null
                                ? StatutNotification.PROGRAMMEE
                                : StatutNotification.EN_ATTENTE
                )
                .dateProgrammee(request.getDateProgrammee())
                .build();

        notification = notificationRepository.save(notification);

        log.info(
                "Notification créée [id={}] type={} canal={}",
                notification.getId(),
                notification.getType(),
                notification.getCanal()
        );

        if (notification.getStatut() == StatutNotification.PROGRAMMEE) {
            return toResponse(notification);
        }

        notification = router(notification);

        return toResponse(notification);
    }

    // ================================================================
    // ROUTER
    // ================================================================
    @Transactional
    public Notification router(Notification notification) {

        try {
            notification.setStatut(StatutNotification.EN_COURS);
            notificationRepository.save(notification);

            switch (notification.getCanal()) {

                case EMAIL -> envoyerEmail(notification);

                case SMS -> envoyerSms(notification);

                case IN_APP -> marquerEnvoyeeInApp(notification);

                case EMAIL_SMS -> {
                    envoyerEmail(notification);
                    envoyerSms(notification);
                }
            }

            notification.setStatut(StatutNotification.ENVOYEE);
            notification.setDateEnvoi(LocalDateTime.now());

            log.info(
                    "Notification [id={}] envoyée avec succès",
                    notification.getId()
            );

        } catch (Exception e) {
            gererEchec(notification, e);
        }

        return notificationRepository.save(notification);
    }

    // ================================================================
    // CANAUX
    // ================================================================
    private void envoyerEmail(Notification notification) {

        if (notification.getDestinataireEmail() == null
                || notification.getDestinataireEmail().isBlank()) {

            log.warn(
                    "Email manquant pour notification id={} — envoi ignoré",
                    notification.getId()
            );
            return;
        }

        // Appel @Async : non bloquant
        emailService.envoyer(
                notification.getDestinataireEmail(),
                notification.getSujet(),
                notification.getMessage()
        );
    }

    private void envoyerSms(Notification notification) {

        if (notification.getDestinataireTelephone() == null
                || notification.getDestinataireTelephone().isBlank()) {

            throw new IllegalArgumentException(
                    "Téléphone destinataire manquant pour notification id="
                            + notification.getId()
            );
        }

        smsService.envoyer(
                notification.getDestinataireTelephone(),
                notification.getMessage()
        );
    }

    private void marquerEnvoyeeInApp(Notification notification) {
        log.info(
                "Notification IN_APP [id={}] disponible dans l'application",
                notification.getId()
        );
    }

    // ================================================================
    // GESTION ECHEC
    // ================================================================
    private void gererEchec(Notification notification, Exception e) {

        int tentatives = notification.getTentatives() == null
                ? 1
                : notification.getTentatives() + 1;

        notification.setTentatives(tentatives);
        notification.setErreurMessage(e.getMessage());

        if (tentatives >= properties.getRetryMax()) {

            notification.setStatut(
                    StatutNotification.ECHEC_DEFINITIF
            );

            log.error(
                    "Notification [id={}] échec définitif après {} tentative(s)",
                    notification.getId(),
                    tentatives
            );

        } else {

            notification.setStatut(StatutNotification.ECHEC);

            log.warn(
                    "Notification [id={}] échec tentative {}/{}",
                    notification.getId(),
                    tentatives,
                    properties.getRetryMax()
            );
        }
    }

    // ================================================================
    // RETRY
    // ================================================================
    @Transactional
    public void reessayerNotificationsEnEchec() {

        List<Notification> notifications = notificationRepository
                .findByStatutAndTentativesLessThan(
                        StatutNotification.ECHEC,
                        properties.getRetryMax()
                );

        log.info(
                "Retry : {} notification(s) à retraiter",
                notifications.size()
        );

        notifications.forEach(this::router);
    }

    // ================================================================
    // PROGRAMMÉES
    // ================================================================
    @Transactional
    public void envoyerNotificationsProgrammees() {

        List<Notification> notifications = notificationRepository
                .findByStatutAndDateProgrammeeBefore(
                        StatutNotification.PROGRAMMEE,
                        LocalDateTime.now()
                );

        log.info(
                "Scheduler : {} notification(s) programmée(s)",
                notifications.size()
        );

        notifications.forEach(this::router);
    }

    // ================================================================
    // LECTURE
    // ================================================================
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getHistoriqueClient(
            Long clientId,
            Pageable pageable
    ) {
        return notificationRepository
                .findByClientIdOrderByCreatedAtDesc(clientId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public NotificationResponse getById(Long id) {

        Notification notification = notificationRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Notification non trouvée id=" + id
                        )
                );

        return toResponse(notification);
    }

    @Transactional(readOnly = true)
    public long countNonLues(Long clientId) {
        return notificationRepository.countByClientIdAndStatut(
                clientId,
                StatutNotification.ENVOYEE
        );
    }

    // ================================================================
    // MARQUER COMME LUE
    // ================================================================
    @Transactional
    public void marquerCommeLue(Long id) {

        Notification notification = notificationRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Notification non trouvée id=" + id
                        )
                );

        notification.setStatut(StatutNotification.LUE);

        notificationRepository.save(notification);
    }

    // ================================================================
    // MAPPING DTO
    // ================================================================
    private NotificationResponse toResponse(Notification n) {

        return NotificationResponse.builder()
                .id(n.getId())
                .clientId(n.getClientId())
                .sujet(n.getSujet())
                .message(n.getMessage())
                .type(n.getType())
                .canal(n.getCanal())
                .statut(n.getStatut())
                .priorite(n.getPriorite())
                .tentatives(n.getTentatives())
                .dateEnvoi(n.getDateEnvoi())
                .dateProgrammee(n.getDateProgrammee())
                .createdAt(n.getCreatedAt())
                .referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType())
                .erreurMessage(n.getErreurMessage())
                .build();
    }
}