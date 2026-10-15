package com.tchelo.api_gateway_ticket241.service;
import java.util.List;

public interface JwtService {

    //boolean qui va valider le token et renvoyer soit true ou false pour le cas échéant
    boolean isTokenValid(String token);

   //extraire le numéro de téléphone du token
    String extractPhoneNumber(String token);

   //extraire le rôle du token
    List<String> extractRoles(String token);

    //extraire l'email du token (null pour les tokens émis avant l'ajout de la claim)
    String extractEmail(String token);
}
