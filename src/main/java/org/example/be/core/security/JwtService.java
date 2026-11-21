package org.example.be.core.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final byte[] secretKey;

    @Getter
    private final long accessTokenExpiresIn;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.access-token-ttl-seconds:900}") long accessTokenExpiresIn) {
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenExpiresIn = accessTokenExpiresIn;
    }

    public String generateAccessToken(String userId, String deviceId) {
        Date now = new Date();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userId)
                .claim("did", deviceId)
                .issueTime(now)
                .expirationTime(new Date(now.getTime() + accessTokenExpiresIn * 1000))
                .build();
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            signedJWT.sign(new MACSigner(secretKey));
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign access token", e);
        }
        return signedJWT.serialize();
    }

}
