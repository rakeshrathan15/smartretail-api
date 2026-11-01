package com.smartretail.auth.support;

import com.smartretail.user.entity.Profile;
import com.smartretail.user.entity.User;
import com.smartretail.user.enums.Role;
import com.smartretail.user.repository.ProfileRepository;
import com.smartretail.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates the first admin account from configuration when no admin exists yet. */
@RequiredArgsConstructor
@Slf4j
@Component
public class BootstrapAdmin implements ApplicationRunner {

    @ConfigurationProperties("app.bootstrap-admin")
    public record Properties(String email, String password) {
    }

    private final Properties properties;
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = Identifiers.normalizeEmail(properties.email());
        if (email == null || properties.password() == null || properties.password().isBlank()
                || users.existsWithRole(Role.ADMIN)) {
            return;
        }
        User admin = users.findByEmail(email).orElse(null);
        if (admin == null) {
            admin = users.save(new User(email, null, passwordEncoder.encode(properties.password()), Role.ADMIN));
            profiles.save(new Profile(admin.getId(), "Administrator", email, null));
        } else {
            admin.addRole(Role.ADMIN);
        }
        log.info("Bootstrap admin account ready: {}", email);
    }
}
