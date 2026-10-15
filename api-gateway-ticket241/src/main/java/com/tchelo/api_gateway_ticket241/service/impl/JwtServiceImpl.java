package com.tchelo.api_gateway_ticket241.service.impl;
import com.tchelo.api_gateway_ticket241.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Service
@Slf4j
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.secret}")
    private String secretKey;


    // Validation du token : vérifie la signature et l'expiration
    @Override
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Date expiration = claims.getExpiration();

            boolean isNotExpired = expiration.after(new Date());

            log.debug("validation du token - Expiration: {}, Valid: {}", expiration, isNotExpired);
            return isNotExpired;

        } catch (Exception e) {
            log.error("Echec de la validation du token: {}", e.getMessage());
            return false;
        }
    }


    //Extrait le numéro de téléphone
    @Override
    public String extractPhoneNumber(String token) {
        return extractClaim(token, Claims::getSubject);
    }


     // Extraction  des rôles du token
    @Override
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        return (List<String>) claims.get("roles");
    }


    // Extraction de l'email du token
    @Override
    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }


    // Extrait une claim spécifique
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }


    // Extrait toutes les claims et vérifie la signature
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // conversion de la clé secrete en objet key
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
