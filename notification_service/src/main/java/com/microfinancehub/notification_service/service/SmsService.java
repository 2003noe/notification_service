package com.microfinancehub.notification_service.service;

import static java.lang.Math.log;
import static java.lang.StrictMath.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    // Twilio sera injecté à l'Étape 6
    public void envoyer(String telephone, String message) {
        // STUB — log uniquement pour l'instant
        log.info("[SMS-STUB] À: {} | Message: {}", telephone, message);
    }
}