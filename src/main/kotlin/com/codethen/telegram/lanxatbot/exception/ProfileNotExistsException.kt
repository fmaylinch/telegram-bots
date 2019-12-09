package com.codethen.telegram.lanxatbot.exception

class ProfileNotExistsException(val userId: Int)
    : LanXatException("Profile doesn't exist for userId $userId")