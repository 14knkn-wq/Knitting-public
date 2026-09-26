package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.entity.User;
import com.example.demo.exception.RegistrationValidationException;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.PasswordBlocklist;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {
	
	@Mock
	private UserRepository userRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private PasswordBlocklist passwordBlocklist;
	
	private static final Clock FIXED_CLOCK =
	        Clock.fixed(
	                Instant.parse("2026-09-25T06:00:00Z"),
	                ZoneId.of("Asia/Tokyo"));
	
	private UserRegistrationService userRegistrationService;
	
	@BeforeEach
	void setUp() {
	    userRegistrationService = new UserRegistrationService(
	            userRepository,
	            passwordEncoder,
	            FIXED_CLOCK,
	            passwordBlocklist);
	}
	
	@Test
	void usernameが重複している場合はUSERNAME_DUPLICATEになる() {
	    when(userRepository.existsByUsernameIgnoreCase("Mame123"))
	            .thenReturn(true);

	    RegistrationValidationException exception =
	            assertThrows(
	                    RegistrationValidationException.class,
	                    () -> userRegistrationService.registerUser(
	                            "mame@example.com",
	                            "Mame123",
	                            "StrongPass123!"));

	    assertEquals(
	            RegistrationValidationException.Reason.USERNAME_DUPLICATE,
	            exception.getReason());
	}
	
	@Test
	void emailが重複している場合はEMAIL_DUPLICATEになる() {
	    when(userRepository.existsByUsernameIgnoreCase("Mame123"))
	            .thenReturn(false);
	    when(userRepository.existsByEmail("mame@example.com"))
	            .thenReturn(true);

	    RegistrationValidationException exception =
	            assertThrows(
	                    RegistrationValidationException.class,
	                    () -> userRegistrationService.registerUser(
	                            "mame@example.com",
	                            "Mame123",
	                            "StrongPass123!"));

	    assertEquals(
	            RegistrationValidationException.Reason.EMAIL_DUPLICATE,
	            exception.getReason());
	}
	
	@Test
	void passwordがBlocklistに含まれる場合はPASSWORD_BLOCKEDになる() {
	    when(userRepository.existsByUsernameIgnoreCase("Mame123"))
	            .thenReturn(false);
	    when(userRepository.existsByEmail("mame@example.com"))
	            .thenReturn(false);
	    when(passwordBlocklist.isBlocked("password"))
	            .thenReturn(true);

	    RegistrationValidationException exception =
	            assertThrows(
	                    RegistrationValidationException.class,
	                    () -> userRegistrationService.registerUser(
	                            "mame@example.com",
	                            "Mame123",
	                            "password"));

	    assertEquals(
	            RegistrationValidationException.Reason.PASSWORD_BLOCKED,
	            exception.getReason());
	}
	
	@Test
	void 正常な入力ではpasswordをhash化してUserを保存する() {
	    when(passwordEncoder.encode("StrongPass123!"))
	            .thenReturn("{bcrypt}hashed-password");

	    userRegistrationService.registerUser(
	            "mame@example.com",
	            "Mame123",
	            "StrongPass123!");

	    verify(passwordEncoder).encode("StrongPass123!");
	    verify(userRepository).saveAndFlush(any(User.class));
	}
	
	@Test
	void username重複時は後続処理を行わない() {
	    when(userRepository.existsByUsernameIgnoreCase("Mame123"))
	            .thenReturn(true);

	    assertThrows(
	            RegistrationValidationException.class,
	            () -> userRegistrationService.registerUser(
	                    "mame@example.com",
	                    "Mame123",
	                    "StrongPass123!"));

	    verify(userRepository, never())
	            .existsByEmail("mame@example.com");
	    verify(passwordBlocklist, never())
	            .isBlocked("StrongPass123!");
	    verify(passwordEncoder, never())
	            .encode("StrongPass123!");
	    verify(userRepository, never())
	            .saveAndFlush(any(User.class));
	}
	
	@Test
	void email重複時は後続処理を行わない() {
	    when(userRepository.existsByUsernameIgnoreCase("Mame123"))
	            .thenReturn(false);
	    when(userRepository.existsByEmail("mame@example.com"))
	            .thenReturn(true);

	    assertThrows(
	            RegistrationValidationException.class,
	            () -> userRegistrationService.registerUser(
	                    "mame@example.com",
	                    "Mame123",
	                    "StrongPass123!"));

	    verify(passwordBlocklist, never())
	            .isBlocked("StrongPass123!");
	    verify(passwordEncoder, never())
	            .encode("StrongPass123!");
	    verify(userRepository, never())
	            .saveAndFlush(any(User.class));
	}
	
	@Test
	void passwordがBlocklistに該当した場合は後続処理を行わない() {
	    when(userRepository.existsByUsernameIgnoreCase("Mame123"))
	            .thenReturn(false);
	    when(userRepository.existsByEmail("mame@example.com"))
	            .thenReturn(false);
	    when(passwordBlocklist.isBlocked("password"))
	            .thenReturn(true);

	    assertThrows(
	            RegistrationValidationException.class,
	            () -> userRegistrationService.registerUser(
	                    "mame@example.com",
	                    "Mame123",
	                    "password"));

	    verify(passwordEncoder, never())
	            .encode("password");
	    verify(userRepository, never())
	            .saveAndFlush(any(User.class));
	}
	
	@Test
	void DBのusernameUnique制約違反はUSERNAME_DUPLICATEになる() {
	    ConstraintViolationException constraintException =
	            mock(ConstraintViolationException.class);

	    when(constraintException.getConstraintName())
	            .thenReturn("uq_users_username");

	    DataIntegrityViolationException dataException =
	            new DataIntegrityViolationException(
	                    "username unique constraint violation",
	                    constraintException);

	    when(passwordEncoder.encode("StrongPass123!"))
	            .thenReturn("{bcrypt}hashed-password");

	    when(userRepository.saveAndFlush(any(User.class)))
	            .thenThrow(dataException);

	    RegistrationValidationException exception =
	            assertThrows(
	                    RegistrationValidationException.class,
	                    () -> userRegistrationService.registerUser(
	                            "mame@example.com",
	                            "Mame123",
	                            "StrongPass123!"));

	    assertEquals(
	            RegistrationValidationException.Reason.USERNAME_DUPLICATE,
	            exception.getReason());
	}
	
	@Test
	void DBのemailUnique制約違反はEMAIL_DUPLICATEになる() {
	    ConstraintViolationException constraintException =
	            mock(ConstraintViolationException.class);

	    when(constraintException.getConstraintName())
	            .thenReturn("uq_users_email");

	    DataIntegrityViolationException dataException =
	            new DataIntegrityViolationException(
	                    "email unique constraint violation",
	                    constraintException);

	    when(passwordEncoder.encode("StrongPass123!"))
	            .thenReturn("{bcrypt}hashed-password");

	    when(userRepository.saveAndFlush(any(User.class)))
	            .thenThrow(dataException);

	    RegistrationValidationException exception =
	            assertThrows(
	                    RegistrationValidationException.class,
	                    () -> userRegistrationService.registerUser(
	                            "mame@example.com",
	                            "Mame123",
	                            "StrongPass123!"));

	    assertEquals(
	            RegistrationValidationException.Reason.EMAIL_DUPLICATE,
	            exception.getReason());
	}
	
	@Test
	void 原因不明のDataIntegrityViolationExceptionはそのままthrowされる() {
	    DataIntegrityViolationException dataException =
	            new DataIntegrityViolationException(
	                    "unknown database error");

	    when(passwordEncoder.encode("StrongPass123!"))
	            .thenReturn("{bcrypt}hashed-password");

	    when(userRepository.saveAndFlush(any(User.class)))
	            .thenThrow(dataException);

	    DataIntegrityViolationException exception =
	            assertThrows(
	                    DataIntegrityViolationException.class,
	                    () -> userRegistrationService.registerUser(
	                            "mame@example.com",
	                            "Mame123",
	                            "StrongPass123!"));

	    assertEquals(dataException, exception);
	}
	
	@Test
	void 未知の制約違反はDataIntegrityViolationExceptionのままthrowされる() {
	    ConstraintViolationException constraintException =
	            mock(ConstraintViolationException.class);

	    when(constraintException.getConstraintName())
	            .thenReturn("unknown_constraint");

	    DataIntegrityViolationException dataException =
	            new DataIntegrityViolationException(
	                    "unknown constraint violation",
	                    constraintException);

	    when(passwordEncoder.encode("StrongPass123!"))
	            .thenReturn("{bcrypt}hashed-password");

	    when(userRepository.saveAndFlush(any(User.class)))
	            .thenThrow(dataException);

	    DataIntegrityViolationException exception =
	            assertThrows(
	                    DataIntegrityViolationException.class,
	                    () -> userRegistrationService.registerUser(
	                            "mame@example.com",
	                            "Mame123",
	                            "StrongPass123!"));

	    assertSame(dataException, exception);
	}
	
	@Test
	void 正常登録時は正しい内容のUserを保存する() {
	    when(passwordEncoder.encode("StrongPass123!"))
	            .thenReturn("{bcrypt}hashed-password");

	    userRegistrationService.registerUser(
	            "mame@example.com",
	            "Mame123",
	            "StrongPass123!");

	    ArgumentCaptor<User> userCaptor =
	            ArgumentCaptor.forClass(User.class);

	    verify(userRepository).saveAndFlush(userCaptor.capture());

	    User savedUser = userCaptor.getValue();

	    assertEquals("mame@example.com", savedUser.getEmail());
	    assertEquals("Mame123", savedUser.getUsername());
	    assertEquals(
	            "{bcrypt}hashed-password",
	            savedUser.getPasswordHash());
	    assertEquals(
	            LocalDateTime.of(2026, 9, 25, 15, 0),
	            savedUser.getCreatedAt());
	}

}