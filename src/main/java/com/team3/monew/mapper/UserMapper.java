package com.team3.monew.mapper;

import com.team3.monew.dto.user.UserRegisterRequest;
import com.team3.monew.dto.user.UserDto;
import com.team3.monew.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

  default User toEntity(UserRegisterRequest request) {
    return User.create(request.email(), request.nickname(), request.password());
  }

  UserDto toDto(User user);
}
