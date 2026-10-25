package com.smartretail.auth;

import com.smartretail.user.Profile;
import com.smartretail.user.ProfileRepository;
import com.smartretail.user.Role;
import com.smartretail.user.User;
import com.smartretail.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates the first admin account from configuration when no admin exists yet. */
@Component
public class BootstrapAdmin implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdmin.class);

    @ConfigurationProperties("app.bootstrap-admin")
    public record Properties(String email, String password) {
    }

    private final Properties properties;
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final PasswordEncoder passwordEncoder;

    public BootstrapAdmin(Properties properties, UserRepository users, ProfileRepository profiles,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.users = users;
        this.profiles = profiles;
        this.passwordEncoder = passwordEncoder;
    }

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
