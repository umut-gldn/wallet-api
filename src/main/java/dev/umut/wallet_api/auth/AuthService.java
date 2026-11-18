package dev.umut.wallet_api.auth;

import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class AuthService {

    private static final Duration TOKEN_TTL=Duration.ofHours(1);
    private static final String EMAIL_CONSTRAINT = "uk_user_email";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder  jwtEncoder;

    @Transactional
    UserResponse register(RegisterRequest request) {
        String email=request.email();
        if(userRepository.existsByEmail(email)){
            throw new EmailAlreadyRegisteredException(email);
        }
        try {
            User user=userRepository.saveAndFlush(User.register(email,passwordEncoder.encode(request.password())));
            return UserResponse.from(user);
        }catch (DataIntegrityViolationException ex){
            if(violates(ex,EMAIL_CONSTRAINT)){
                throw new EmailAlreadyRegisteredException(email);
            }
            throw ex;
        }
    }

    TokenResponse login(LoginRequest request) {
        User user=userRepository.findByEmail(request.email())
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

    private static boolean violates(DataIntegrityViolationException ex, String constraintName){
        return ex.getCause() instanceof ConstraintViolationException cve && constraintName.equalsIgnoreCase(cve.getConstraintName());
    }
}
