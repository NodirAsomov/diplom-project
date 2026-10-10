package ru.practicum.ewm.comment.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.client.EventInfo;
import ru.practicum.ewm.user.dto.UserDto;

import java.time.LocalDateTime;

@Component
public class CommentMapper {

    public Comment toComment(NewCommentDto dto, EventInfo event, UserDto author) {
        return Comment.builder()
                .text(dto.getText())
                .eventId(event.getId())
                .authorId(author.getId()).authorName(author.getName())
                .created(LocalDateTime.now())
                .build();
    }

    public CommentDto toCommentDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .authorId(comment.getAuthorId())
                .authorName(comment.getAuthorName())
                .eventId(comment.getEventId())
                .created(comment.getCreated())
                .updated(comment.getUpdated())
                .build();
    }
}
