package com.planner.user;

import com.planner.file.FileStorageService;
import com.planner.user.dto.UserResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    public MeController(FileStorageService fileStorageService, UserRepository userRepository) {
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal User user) {
        return UserResponse.from(user);
    }

    @PostMapping("/avatar")
    public UserResponse uploadAvatar(@AuthenticationPrincipal User user,
                                     @RequestParam("file") MultipartFile file) {
        String filename = fileStorageService.storeAvatar(file);
        user.setAvatarPath(filename);
        userRepository.save(user);
        return UserResponse.from(user);
    }
}
