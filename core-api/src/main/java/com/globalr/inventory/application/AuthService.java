package com.globalr.inventory.application;

import com.globalr.inventory.application.dto.ApiDtos.*;
import com.globalr.inventory.domain.model.Country;
import com.globalr.inventory.domain.model.UserEntity;
import com.globalr.inventory.domain.repository.CountryRepository;
import com.globalr.inventory.domain.repository.UserRepository;
import com.globalr.inventory.infrastructure.audit.AuditService;
import com.globalr.inventory.infrastructure.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CountryRepository countryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(
            UserRepository userRepository,
            CountryRepository countryRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.countryRepository = countryRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    public LoginResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByUsernameIgnoreCase(request.username().trim())
                .orElseThrow(() -> {
                    auditService.recordAuthEvent(null, null, request.username(), "UNKNOWN", "LOGIN_FAILED",
                            Map.of("reason", "Usuario no encontrado"));
                    return new BadCredentialsException("Credenciales inválidas");
                });

        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            auditService.recordAuthEvent(user.getCountryId(), user.getId(), user.getUsername(),
                    user.getRole().name(), "LOGIN_FAILED", Map.of("reason", "Contraseña incorrecta o usuario inactivo"));
            throw new BadCredentialsException("Credenciales inválidas");
        }

        Country userCountry = countryRepository.findById(user.getCountryId())
                .orElseThrow(() -> new IllegalStateException("País de usuario no encontrado"));

        String token = jwtService.generateToken(user, userCountry.getCode());

        List<CountryDto> availableCountries = user.isMultiCountryAccess()
                ? countryRepository.findAll().stream()
                    .map(c -> new CountryDto(c.getId(), c.getCode(), c.getName(), c.getCurrencyCode()))
                    .toList()
                : List.of(new CountryDto(userCountry.getId(), userCountry.getCode(), userCountry.getName(), userCountry.getCurrencyCode()));

        auditService.recordAuthEvent(user.getCountryId(), user.getId(), user.getUsername(),
                user.getRole().name(), "LOGIN", Map.of("countryCode", userCountry.getCode()));

        UserSummaryDto summary = new UserSummaryDto(
                user.getId(), user.getUsername(), user.getEmail(), user.getFullName(),
                user.getNationalId(), user.getRole(), user.getCountryId(),
                userCountry.getCode(), user.isMultiCountryAccess()
        );
        return new LoginResponse(token, summary, availableCountries);
    }
}
