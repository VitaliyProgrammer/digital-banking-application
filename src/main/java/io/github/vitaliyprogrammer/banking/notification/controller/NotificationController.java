package io.github.vitaliyprogrammer.banking.notification.controller;

import io.github.vitaliyprogrammer.banking.common.configuration.OpenApiConfiguration;
import io.github.vitaliyprogrammer.banking.common.dto.PageResponseDto;
import io.github.vitaliyprogrammer.banking.common.security.CurrentUserId;
import io.github.vitaliyprogrammer.banking.notification.dto.response.NotificationResponseDto;
import io.github.vitaliyprogrammer.banking.notification.dto.response.UnreadCountResponseDto;
import io.github.vitaliyprogrammer.banking.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "What happened on your accounts: payments, credit decisions, statements")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get my notifications", description = "Newest first")
    public PageResponseDto<NotificationResponseDto> getNotifications(
            @CurrentUserId UUID userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        return notificationService.getNotifications(userId, page, size);
    }

    @GetMapping("/unread-count")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Count my unread notifications", description = "The number on the bell")
    public UnreadCountResponseDto getUnreadCount(@CurrentUserId UUID userId) {

        return new UnreadCountResponseDto(notificationService.getUnreadCount(userId));
    }

    @PostMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Mark a notification as read")
    public NotificationResponseDto markRead(@CurrentUserId UUID userId, @PathVariable UUID notificationId) {

        return notificationService.markRead(userId, notificationId);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mark all my notifications as read")
    public void markAllRead(@CurrentUserId UUID userId) {

        notificationService.markAllRead(userId);
    }
}
