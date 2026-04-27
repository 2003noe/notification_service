// TypeNotification.java
package com.microfinancehub.notification_service.model.enums;
public enum TypeNotification {
    // Prêts
    DEMANDE_PRET, APPROBATION_PRET, REJET_PRET, DECAISSEMENT,
    // Remboursements
    RAPPEL_ECHEANCE, CONFIRMATION_REMB, ALERTE_RETARD, PENALITE_APPLIQUEE,
    // Comptes
    CREATION_COMPTE, DEPOT_EFFECTUE, RETRAIT_EFFECTUE,
    // Système
    PROMOTION, NEWSLETTER, ALERTE_SYSTEME
}