package com.codethen.telegram.lanxatbot.exception

class ProfileNotExistsException(val userId: Int)
    : RuntimeException("Profile doesn't exist for userId $userId")