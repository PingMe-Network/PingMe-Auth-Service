package org.ping_me.service.user;

import org.ping_me.dto.request.user.ChangePasswordRequest;
import org.ping_me.dto.request.user.ChangeProfileRequest;
import org.ping_me.dto.request.user.CreateNewPasswordRequest;
import org.ping_me.dto.response.auth.ActiveAccountResponse;
import org.ping_me.dto.response.auth.CreateNewPasswordResponse;
import org.ping_me.dto.response.auth.CurrentUserProfileResponse;
import org.ping_me.dto.response.auth.CurrentUserSessionResponse;
import org.ping_me.model.constant.UserStatus;
import org.springframework.web.multipart.MultipartFile;

/**
 * Admin 1/9/2026
 *
 **/
public interface CurrentUserProfileService {
    CurrentUserProfileResponse getCurrentUserInfo();

    CurrentUserSessionResponse getCurrentUserSession();

    CurrentUserSessionResponse updateCurrentUserPassword(
            ChangePasswordRequest changePasswordRequest
    );

    CurrentUserSessionResponse updateCurrentUserProfile(
            ChangeProfileRequest changeProfileRequest
    );

    CurrentUserSessionResponse updateCurrentUserAvatar(
            MultipartFile avatarFile
    );

    void updateStatus(Long userId, UserStatus status);

    CreateNewPasswordResponse createNewPassword(CreateNewPasswordRequest request);

    ActiveAccountResponse activateAccount();
}
