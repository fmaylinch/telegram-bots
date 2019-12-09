package com.codethen.telegram.lanxatbot.exception

class LangConfigNotExistsException(val name: String)
    : LanXatException("Lang config $name doesn't exist")