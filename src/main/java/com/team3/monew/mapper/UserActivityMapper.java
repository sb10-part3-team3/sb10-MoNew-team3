package com.team3.monew.mapper;

import com.team3.monew.document.UserActivityDocument;
import com.team3.monew.document.UserActivityRequest;
import com.team3.monew.dto.useractivity.UserActivityDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserActivityMapper {

  default UserActivityDocument toDocument(UserActivityRequest request) {
    return UserActivityDocument.create(
        request.id(),
        request.email(),
        request.nickname(),
        request.createdAt());
  }

  UserActivityDto toDto(UserActivityDocument activity);
}
