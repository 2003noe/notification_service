package com.microfinancehub.notification_service.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    // ================================================================
    // ENVOI SIMPLE (texte brut)
    // ================================================================
    @Async
    public void envoyer(String destinataire, String sujet, String message) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mime, true, "UTF-8");

            helper.setFrom("notifications@microfinancehub.cm");
            helper.setTo(destinataire);
            helper.setSubject(sujet);
            helper.setText(message, false);

            mailSender.send(mime);
            log.info("[EMAIL] Envoyé à: {} | Sujet: {}", destinataire, sujet);

        } catch (MessagingException e) {
            log.error("[EMAIL] Échec envoi à: {} | Erreur: {}",
                    destinataire, e.getMessage());
            throw new RuntimeException("Échec envoi email : " + e.getMessage());
        }
    }

    // ================================================================
    // ENVOI HTML avec template Thymeleaf
    // ================================================================
    @Async
    public void envoyerHtml(String destinataire,
                            String sujet,
                            String templateName,
                            Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);

            String htmlContent = templateEngine.process(
                    "email/" + templateName, context);

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mime, true, "UTF-8");

            helper.setFrom("notifications@microfinancehub.cm");
            helper.setTo(destinataire);
            helper.setSubject(sujet);
            helper.setText(htmlContent, true);

            mailSender.send(mime);
            log.info("[EMAIL-HTML] Envoyé à: {} | Template: {}",
                    destinataire, templateName);

        } catch (MessagingException e) {
            log.error("[EMAIL-HTML] Échec envoi à: {} | Erreur: {}",
                    destinataire, e.getMessage());
            throw new RuntimeException("Échec envoi email HTML : " + e.getMessage());
        }
    }

    // ================================================================
    // ENVOI PRÊT APPROUVÉ
    // ================================================================
    public void envoyerPretApprouve(String destinataire,
                                    Map<String, Object> variables) {
        envoyerHtml(destinataire,
                "Votre pret a ete approuve - MicroFinanceHub",
                "pret-approuve",
                variables);
    }

    // ================================================================
    // ENVOI RAPPEL ÉCHÉANCE
    // ================================================================
    public void envoyerRappelEcheance(String destinataire,
                                      Map<String, Object> variables) {
        envoyerHtml(destinataire,
                "Rappel echeance - MicroFinanceHub",
                "rappel-echeance",
                variables);
    }

    // ================================================================
    // ENVOI CONFIRMATION REMBOURSEMENT
    // ================================================================
    public void envoyerConfirmationRemboursement(String destinataire,
                                                  Map<String, Object> variables) {
        envoyerHtml(destinataire,
                "Remboursement confirme - MicroFinanceHub",
                "confirmation-remboursement",
                variables);
    }
}