package com.team3.monew.mapper;

import com.team3.monew.dto.notification.NotificationDto;
import com.team3.monew.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

  @Mappings({
      @Mapping(source = "notification.user.id", target = "userId")
  })
  NotificationDto toDto(Notification notification);
}
