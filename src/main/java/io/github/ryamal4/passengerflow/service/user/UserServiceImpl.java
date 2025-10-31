package io.github.ryamal4.passengerflow.service.user;

import io.github.ryamal4.passengerflow.dto.UserDto;
import io.github.ryamal4.passengerflow.exception.AppException;
import io.github.ryamal4.passengerflow.exception.ResourceNotFoundException;
import io.github.ryamal4.passengerflow.model.Role;
import io.github.ryamal4.passengerflow.model.User;
import io.github.ryamal4.passengerflow.repository.RoleRepository;
import io.github.ryamal4.passengerflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UserDto> getUsers() {
        return userRepository.findAll().stream()
                .map(UserMapper::userToUserDto).toList();
    }

    @Override
    public UserDto create(UserDto userDto) {
        User user = UserMapper.userDtoToUser(userDto);

        // get role from db
        Role role = roleRepository.findByName(userDto.role()).orElseThrow(
                () -> new ResourceNotFoundException("Role not found")
        );

        user.setRole(role);
        user.setPassword(passwordEncoder.encode(userDto.password()));

        return UserMapper.userToUserDto(userRepository.save(user));
    }

    @Override
    public UserDto getUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new AppException(HttpStatus.NOT_FOUND, "User not found")
        );
        return UserMapper.userToUserDto(user);
    }

    @Override
    public UserDto getUser(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new AppException(HttpStatus.NOT_FOUND, "User not found")
        );
        return UserMapper.userToUserDto(user);
    }

    @Override
    public UserDto updateUser(Long userId, UserDto userDto) {
        // get user from db
        User user = userRepository.findById(userId).orElseThrow(
                () -> new AppException(HttpStatus.NOT_FOUND, "User not found")
        );

        // get role from db
        Role role = roleRepository.findByName(userDto.role()).orElseThrow(
                () -> new AppException(HttpStatus.NOT_FOUND, "Role not found")
        );

        user.setUsername(userDto.username());
        user.setPassword(passwordEncoder.encode(userDto.password()));
        user.setRole(role);

        return UserMapper.userToUserDto(userRepository.save(user));
    }

    @Override
    public String deleteUser(Long userId) {
        // get user from db
        User user = userRepository.findById(userId).orElseThrow(
                () -> new AppException(HttpStatus.NOT_FOUND, "User not found")
        );

        userRepository.delete(user);

        return String.format("User with %d deleted successfully", userId);
    }

}