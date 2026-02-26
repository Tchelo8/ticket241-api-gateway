package com.tchelo.api_gateway_ticket241.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {


    @PostMapping("/auth")
    @GetMapping("/auth")
    public ResponseEntity<Map<String, Object>> authFallback() {
        log.warn(" Auth Service fallback activé - Service indisponible");

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "Le service d'authentification est temporairement indisponible. Veuillez réessayer dans quelques instants.");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "auth-service");
        response.put("suggestion", "Si le problème persiste, contactez le support.");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }


    @GetMapping("/events")
    @PostMapping("/events")
    @PutMapping("/events")
    @DeleteMapping("/events")
    public ResponseEntity<Map<String, Object>> eventsFallback() {
        log.warn("Event Service fallback activé - Service indisponible");

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "Le service d'événements est temporairement indisponible. Veuillez réessayer dans quelques instants.");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "event-service");
        response.put("data", new HashMap<>());  // Retourne un objet vide pour éviter les erreurs côté client

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }


    @GetMapping("/tickets")
    @PostMapping("/tickets")
    @PutMapping("/tickets")
    @DeleteMapping("/tickets")
    public ResponseEntity<Map<String, Object>> ticketsFallback() {
        log.warn("Ticket Service fallback activé - Service indisponible");

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "Le service de billetterie est temporairement indisponible. Veuillez réessayer dans quelques instants.");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "ticket-service");
        response.put("data", new HashMap<>());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }


    @RequestMapping(value = {"", "/**"}, method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<Map<String, Object>> defaultFallback() {
        log.warn("Fallback générique activé - Service indisponible");

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "Un service est temporairement indisponible. Veuillez réessayer dans quelques instants.");
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}