package com.microfinancehub.notification_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ===== NOMS DES EXCHANGES =====
    public static final String EXCHANGE_NOTIFICATIONS = "microfinance.notifications";
    public static final String EXCHANGE_DEAD_LETTER   = "microfinance.notifications.dlx";

    // ===== NOMS DES QUEUES =====
    public static final String QUEUE_PRET             = "queue.notification.pret";
    public static final String QUEUE_REMBOURSEMENT    = "queue.notification.remboursement";
    public static final String QUEUE_COMPTE           = "queue.notification.compte";
    public static final String QUEUE_DEAD_LETTER      = "queue.notification.dead-letter";

    // ===== ROUTING KEYS =====
    public static final String RK_PRET_APPROUVE       = "pret.approuve";
    public static final String RK_PRET_REJETE         = "pret.rejete";
    public static final String RK_PRET_DECAISSE       = "pret.decaisse";
    public static final String RK_REMB_RECU           = "remboursement.recu";
    public static final String RK_REMB_RETARD         = "remboursement.retard";
    public static final String RK_PENALITE            = "remboursement.penalite";
    public static final String RK_COMPTE_CREE         = "compte.cree";
    public static final String RK_DEPOT               = "compte.depot";
    public static final String RK_RETRAIT             = "compte.retrait";

    // ===== EXCHANGE PRINCIPAL (topic) =====
    @Bean
    public TopicExchange notificationExchange() {
        return ExchangeBuilder
                .topicExchange(EXCHANGE_NOTIFICATIONS)
                .durable(true)
                .build();
    }

    // ===== EXCHANGE DEAD LETTER =====
    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder
                .directExchange(EXCHANGE_DEAD_LETTER)
                .durable(true)
                .build();
    }

    // ===== QUEUES =====
    @Bean
    public Queue queuePret() {
        return QueueBuilder
                .durable(QUEUE_PRET)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DEAD_LETTER)
                .withArgument("x-dead-letter-routing-key", "dead.pret")
                .build();
    }

    @Bean
    public Queue queueRemboursement() {
        return QueueBuilder
                .durable(QUEUE_REMBOURSEMENT)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DEAD_LETTER)
                .withArgument("x-dead-letter-routing-key", "dead.remboursement")
                .build();
    }

    @Bean
    public Queue queueCompte() {
        return QueueBuilder
                .durable(QUEUE_COMPTE)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DEAD_LETTER)
                .withArgument("x-dead-letter-routing-key", "dead.compte")
                .build();
    }

    @Bean
    public Queue queueDeadLetter() {
        return QueueBuilder
                .durable(QUEUE_DEAD_LETTER)
                .build();
    }

    // ===== BINDINGS =====
    // Tous les events prêt → queue pret
    @Bean
    public Binding bindingPretApprouve() {
        return BindingBuilder.bind(queuePret())
                .to(notificationExchange())
                .with("pret.*");
    }

    // Tous les events remboursement → queue remboursement
    @Bean
    public Binding bindingRemboursement() {
        return BindingBuilder.bind(queueRemboursement())
                .to(notificationExchange())
                .with("remboursement.*");
    }

    // Tous les events compte → queue compte
    @Bean
    public Binding bindingCompte() {
        return BindingBuilder.bind(queueCompte())
                .to(notificationExchange())
                .with("compte.*");
    }

    // Dead letter binding
    @Bean
    public Binding bindingDeadLetter() {
        return BindingBuilder.bind(queueDeadLetter())
                .to(deadLetterExchange())
                .with("dead.*");
    }

    // ===== CONVERTISSEUR JSON =====
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}