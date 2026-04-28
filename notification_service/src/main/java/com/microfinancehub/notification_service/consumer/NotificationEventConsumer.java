package com.microfinancehub.notification_service.consumer;

import com.microfinancehub.notification_service.config.RabbitMQConfig;
import com.microfinancehub.notification_service.model.dto.event.PretEvent;
import com.microfinancehub.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PRET)
    public void recevoirPret(PretEvent event) {

        log.info("📩 Event prêt reçu : {}", event.getStatut());

        if (!"APPROUVE".equalsIgnoreCase(event.getStatut())) {
            log.info("⛔ Event ignoré (pas approuvé)");
            return;
        }

        try {
            Map<String, Object> variables = new HashMap<>();

            variables.put("clientNom", event.getClientNom());
            variables.put("montant", event.getMontant());
            variables.put("dureeEnMois", event.getDureeEnMois());
            variables.put("tauxInteret", event.getTauxInteret());
            variables.put("dateApprobation", LocalDateTime.now());

            emailService.envoyerPretApprouve(
                    event.getClientEmail(),
                    variables
            );

            log.info("✅ Email prêt approuvé envoyé à {}", event.getClientEmail());

        } catch (Exception e) {
            log.error("❌ Erreur traitement prêt : {}", e.getMessage());
        }
    }
}