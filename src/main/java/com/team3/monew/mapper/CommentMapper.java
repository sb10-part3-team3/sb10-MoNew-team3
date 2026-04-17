package com.team3.monew.mapper;

import com.team3.monew.dto.data.CommentDto;
import com.team3.monew.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "articleId", source = "comment.article.id")
    @Mapping(target = "userId", source = "comment.user.id")
    @Mapping(target = "userNickname", source = "comment.user.nickname")
    @Mapping(target = "likeCount", expression = "java((long) comment.getLikeCount())")
    @Mapping(target = "likedByMe", source = "likedByMe")
    CommentDto toDto(Comment comment, Boolean likedByMe);
}
