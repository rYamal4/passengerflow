package io.github.ryamal4.passengerflow.service.user;

import io.github.ryamal4.passengerflow.dto.UserDto;

import java.util.List;

public interface UserService {
    List<UserDto> getUsers();

    UserDto create(UserDto userDto);

    UserDto getUser(Long userId);

    UserDto getUser(String username);

    UserDto updateUser(Long userId, UserDto userDto);

    String deleteUser(Long userId);

}