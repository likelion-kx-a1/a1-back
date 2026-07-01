package com.likelion.a1.user.presentation;

import com.likelion.a1.global.response.ApiResponse;
import com.likelion.a1.user.application.UserCommandService;
import com.likelion.a1.user.domain.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.likelion.a1.user.presentation.dto.RegisterUserRequest;
import com.likelion.a1.user.presentation.dto.UserResponse;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserCommandService service;

    public UserController(UserCommandService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        User user = service.register(request.email(), request.password(), request.nickname());
        return ApiResponse.success(UserResponse.from(user));
    }
}
