package com.warehouse.warehouse_management;

import com.warehouse.warehouse_management.entity.Role;
import com.warehouse.warehouse_management.entity.User;
import com.warehouse.warehouse_management.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@SpringBootApplication
@Slf4j
public class WarehouseManagementApplication {
    @Autowired
    UserRepository userRepository;


    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public static void main(String[] args) {
        SpringApplication.run(WarehouseManagementApplication.class, args);
    }

    @Bean
    public ApplicationRunner defaultSuperUser() {
        return args -> {
            boolean hasAnyAdmin = !userRepository.findByRole(Role.SYSTEM_ADMIN).isEmpty();
            if (!hasAnyAdmin) {
                User superUser = new User();
                superUser.setName("Admin");
                superUser.setSurname("Root");
                superUser.setEmail("admin@admin.com");
                superUser.setPassword(passwordEncoder.encode("12345678"));
                superUser.setRole(Role.SYSTEM_ADMIN);
                userRepository.save(superUser);
                log.warn("Bootstrapped SYSTEM_ADMIN user: {} (please change the password)", superUser.getEmail());
            } else {
                log.info("SYSTEM_ADMIN already present — skipping bootstrap.");
            }
        };
    }

}
