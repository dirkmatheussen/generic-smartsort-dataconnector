package com.art4l.dataconnector.module.common.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.*;

public class SecurityUtil {
    private SecurityUtil(){}

    public static String generateJWT(String subject, Map<String, Object> claims, String secret){
        if(claims == null){
            claims = new HashMap<>();
        }
        return Jwts.builder()
                .setSubject(subject)
                .setClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 24 hours from now
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }
}
