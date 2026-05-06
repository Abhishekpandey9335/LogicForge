package com.apcode.service;

import com.apcode.dto.UserProfile;

public interface UserService {
    UserProfile getProfile(String username);
}
