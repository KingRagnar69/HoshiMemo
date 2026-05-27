package com.ragdev69.hoshimemo.data.model

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val age: String = "",
    val bio: String = "",
    val avatarInitial: String = ""
) {
    constructor() : this("", "", "", "", "", "")
}