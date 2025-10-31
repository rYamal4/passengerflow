package io.github.ryamal4.passengerflow.service.user;

import io.github.ryamal4.passengerflow.dto.UserDto;
import io.github.ryamal4.passengerflow.dto.UserLoggedDto;
import io.github.ryamal4.passengerflow.model.Permission;
import io.github.ryamal4.passengerflow.model.User;

import java.util.stream.Collectors;

public class UserMapper {
    public static UserDto userToUserDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getRole().getAuthority(),
                user.getRole().getPermissions().stream()
                .map(Permission::getAuthority)
                .collect(Collectors.toSet())
        );
    }
    public static User userDtoToUser(UserDto dto) {
        User user = new User();
        user.setUsername(dto.username());
        return user;
    }
    public static UserLoggedDto userToUserLoggedDto(User user) {
        return new UserLoggedDto(
                user.getUsername(),
                user.getRole().getAuthority(),
                user.getRole().getPermissions().stream().map(Permission::getAuthority).collect(Collectors.toSet())
        );
    }
}