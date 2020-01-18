package com.codethen.telegram.lanxatbot.exception

import com.codethen.telegram.lanxatbot.profile.UserProfile

class ProfileNotConfiguredException(val userProfile: UserProfile)
    : LanXatException("Profile not configured for userId ${userProfile.id}")