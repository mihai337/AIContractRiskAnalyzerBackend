package licenta.mihai.aicontractriskanalyzerbackend.api;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

final class TestJwtTokenFactory {

    private static final String SECRET = "change-me-dev-secret-change-me-dev-secret";
    private static final String ISSUER = "https://securetoken.google.com/local-dev";
    private static final String AUDIENCE = "local-dev";

    private TestJwtTokenFactory() {
    }

    static String validToken() throws JOSEException {
        Instant now = Instant.now();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .subject("test-user")
            .issuer(ISSUER)
            .audience(List.of(AUDIENCE))
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(3600)))
            .build();

        SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
        signedJwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
        return signedJwt.serialize();
    }
}

