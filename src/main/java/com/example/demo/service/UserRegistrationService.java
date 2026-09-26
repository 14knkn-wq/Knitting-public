package com.example.demo.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.User;
import com.example.demo.exception.RegistrationValidationException;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.PasswordBlocklist;

@Service
public class UserRegistrationService {
	
	private static final String USERNAME_UNIQUE_CONSTRAINT =
	        "uq_users_username";

	private static final String EMAIL_UNIQUE_CONSTRAINT =
	        "uq_users_email";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final PasswordBlocklist passwordBlocklist;

    public UserRegistrationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            Clock clock,
            PasswordBlocklist passwordBlocklist) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.passwordBlocklist = passwordBlocklist;
    }
    
    @Transactional
    public void registerUser(
            String email,
            String username,
            String password) {

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new RegistrationValidationException(
                    RegistrationValidationException.Reason.USERNAME_DUPLICATE);
        }
        
        if (userRepository.existsByEmail(email)) {
            throw new RegistrationValidationException(
                    RegistrationValidationException.Reason.EMAIL_DUPLICATE);
        }
        
        if (passwordBlocklist.isBlocked(password)) {
            throw new RegistrationValidationException(
                    RegistrationValidationException.Reason.PASSWORD_BLOCKED);
        }
        
        String passwordHash = passwordEncoder.encode(password);
        
        User user = new User(
                email,
                username,
                passwordHash,
                LocalDateTime.now(clock));
        
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            Throwable cause = e.getCause();

            while (cause != null) {
                if (cause instanceof ConstraintViolationException constraintViolationException
                        && USERNAME_UNIQUE_CONSTRAINT.equals(
                                constraintViolationException.getConstraintName())) {

                    throw new RegistrationValidationException(
                            RegistrationValidationException.Reason.USERNAME_DUPLICATE);
                }
                
                if (cause instanceof ConstraintViolationException constraintViolationException
                        && EMAIL_UNIQUE_CONSTRAINT.equals(
                                constraintViolationException.getConstraintName())) {

                    throw new RegistrationValidationException(
                            RegistrationValidationException.Reason.EMAIL_DUPLICATE);
                }

                cause = cause.getCause();
            }

            throw e;
        }
    }

}