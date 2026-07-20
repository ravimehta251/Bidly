package com.bidflare.auth;

import com.bidflare.auth.dto.AuthResponse;
import com.bidflare.auth.dto.LoginRequest;
import com.bidflare.auth.dto.RegisterRequest;
import com.bidflare.user.User;
import com.bidflare.user.UserRepository;
import com.bidflare.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.displayName(),
                UserRole.BUYER
        );
        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail(), user.getId(), user.getRole().name());
        return new AuthResponse(token, jwtService.getExpirationMs());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getId(), user.getRole().name());
        return new AuthResponse(token, jwtService.getExpirationMs());
    }
}
