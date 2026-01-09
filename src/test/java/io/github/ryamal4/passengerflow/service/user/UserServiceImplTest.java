package io.github.ryamal4.passengerflow.service.user;

import io.github.ryamal4.passengerflow.dto.UserDto;
import io.github.ryamal4.passengerflow.exception.AppException;
import io.github.ryamal4.passengerflow.exception.ResourceNotFoundException;
import io.github.ryamal4.passengerflow.model.Permission;
import io.github.ryamal4.passengerflow.model.Role;
import io.github.ryamal4.passengerflow.model.User;
import io.github.ryamal4.passengerflow.repository.RoleRepository;
import io.github.ryamal4.passengerflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "password";
    private static final String ENCODED_PASSWORD = "encoded_password";
    private static final String ROLE_USER = "USER";

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private Role testRole;

    @BeforeEach
    void setUp() {
        Permission testPermission = Permission.builder()
                .id(1L)
                .resource("users")
                .operation("read")
                .build();

        testRole = Role.builder()
                .id(1L)
                .name(ROLE_USER)
                .permissions(Set.of(testPermission))
                .build();

        testUser = User.builder()
                .id(1L)
                .username(TEST_USERNAME)
                .password(ENCODED_PASSWORD)
                .role(testRole)
                .tokens(new HashSet<>())
                .build();
    }

    @Test
    void testGetUsersReturnsAllUsers() {
        var user2 = User.builder()
                .id(2L)
                .username("user2")
                .password("pass2")
                .role(testRole)
                .build();
        when(userRepository.findAll()).thenReturn(List.of(testUser, user2));

        var result = userService.getUsers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).username()).isEqualTo(TEST_USERNAME);
        assertThat(result.get(1).username()).isEqualTo("user2");
        verify(userRepository).findAll();
    }

    @Test
    void testCreateUserSuccess() {
        var userDto = new UserDto(null, TEST_USERNAME, TEST_PASSWORD, ROLE_USER, Set.of());
        when(roleRepository.findByName(ROLE_USER)).thenReturn(Optional.of(testRole));
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        var result = userService.create(userDto);

        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo(TEST_USERNAME);
        assertThat(result.role()).isEqualTo(ROLE_USER);
        verify(passwordEncoder).encode(TEST_PASSWORD);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testCreateUserEncodesPassword() {
        var userDto = new UserDto(null, TEST_USERNAME, TEST_PASSWORD, ROLE_USER, Set.of());
        var captor = ArgumentCaptor.forClass(User.class);
        when(roleRepository.findByName(ROLE_USER)).thenReturn(Optional.of(testRole));
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.create(userDto);

        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo(ENCODED_PASSWORD);
    }

    @Test
    void testCreateUserRoleNotFoundThrowsException() {
        var userDto = new UserDto(null, TEST_USERNAME, TEST_PASSWORD, "NONEXISTENT", Set.of());
        when(roleRepository.findByName("NONEXISTENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.create(userDto))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void testGetUserByIdSuccess() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        var result = userService.getUser(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.username()).isEqualTo(TEST_USERNAME);
        verify(userRepository).findById(1L);
    }

    @Test
    void testGetUserByIdNotFoundThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(999L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void testGetUserByUsernameSuccess() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        var result = userService.getUser(TEST_USERNAME);

        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo(TEST_USERNAME);
        verify(userRepository).findByUsername(TEST_USERNAME);
    }

    @Test
    void testGetUserByUsernameNotFoundThrowsException() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser("nonexistent"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void testUpdateUserSuccess() {
        var updateDto = new UserDto(1L, "updatedUser", "newPassword", ROLE_USER, Set.of());
        var updatedUser = User.builder()
                .id(1L)
                .username("updatedUser")
                .password(ENCODED_PASSWORD)
                .role(testRole)
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName(ROLE_USER)).thenReturn(Optional.of(testRole));
        when(passwordEncoder.encode("newPassword")).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        var result = userService.updateUser(1L, updateDto);

        assertThat(result.username()).isEqualTo("updatedUser");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testUpdateUserNotFoundThrowsException() {
        var updateDto = new UserDto(999L, "user", "pass", ROLE_USER, Set.of());
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(999L, updateDto))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
        verify(userRepository, never()).save(any());
    }

    @Test
    void testUpdateUserRoleNotFoundThrowsException() {
        var updateDto = new UserDto(1L, "user", "pass", "NONEXISTENT", Set.of());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("NONEXISTENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(1L, updateDto))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
        verify(userRepository, never()).save(any());
    }

    @Test
    void testDeleteUserSuccess() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        var result = userService.deleteUser(1L);

        assertThat(result).contains("1", "deleted successfully");
        verify(userRepository).delete(testUser);
    }

    @Test
    void testDeleteUserNotFoundThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void testGetUserReturnsDtoWithPermissions() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        var result = userService.getUser(1L);

        assertThat(result.permissions()).isNotEmpty();
        assertThat(result.permissions()).contains("USERS:READ");
    }
}
