package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
@Getter
@Setter
public class JwtSecurityProperties {

    private String issuer;
    private String audience;
    private String hmacSecret;
}

