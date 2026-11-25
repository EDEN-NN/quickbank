package com.quickbank.bootstrap.identity;

import com.quickbank.bootstrap.identity.IdentityDirectory.TokenSet;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@SecurityRequirements
public class AuthApiController {

    private final IdentityService identity;

    public AuthApiController(IdentityService identity) {
        this.identity = identity;
    }

    @PostMapping("/registrations")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request) {
        String username = identity.register(
                request.username(), request.email(), request.firstName(), request.lastName(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(new RegistrationResponse(username));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenResponse.from(identity.login(request.username(), request.password()));
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return TokenResponse.from(identity.refresh(request.refreshToken()));
    }

    public record RegistrationRequest(
            @NotBlank @Size(min = 3, max = 64) @Pattern(regexp = "[A-Za-z0-9._-]+") String username,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Size(min = 8, max = 72) String password
    ) {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(max = 72) String password
    ) {
    }

    public record RefreshRequest(@NotBlank @Size(max = 8192) String refreshToken) {
    }

    public record RegistrationResponse(String username) {
    }

    public record TokenResponse(String accessToken, String refreshToken, long expiresIn) {
        static TokenResponse from(TokenSet tokens) {
            return new TokenResponse(tokens.accessToken(), tokens.refreshToken(), tokens.expiresIn());
        }
    }
}
