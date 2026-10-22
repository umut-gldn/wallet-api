package dev.umut.wallet_api.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class AuthService {

    private static final Duration TOKEN_TTL=Duration.ofHours(1);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder  jwtEncoder;

    @Transactional
    UserResponse register(RegisterRequest request) {
        String email=normalize(request.email());
        if(userRepository.existsByEmail(email)){
            throw new EmailAlreadyRegisteredException(email);
        }
        User user=userRepository.save(User.register(email,passwordEncoder.encode(request.password())));
        return UserResponse.from(user);
    }

    TokenResponse login(LoginRequest request) {
        User user=userRepository.findByEmail(normalize(request.email()))
                .filter(u->passwordEncoder.matches(request.password(),u.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return issueToken(user);
    }
    private TokenResponse issueToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims=JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_TTL))
                .build();
        JwsHeader header=JwsHeader.with(MacAlgorithm.HS256).build();
        String token=jwtEncoder.encode(JwtEncoderParameters.from(header,claims)).getTokenValue();
        return new TokenResponse(token,"Bearer",TOKEN_TTL.toSeconds());
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
