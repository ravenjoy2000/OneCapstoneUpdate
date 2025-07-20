// Package kung saan naka-store ang Constants object
package com.example.mediconnect.utils

// Ginagamit ang object para gumawa ng singleton class — iisa lang ang instance nito sa buong app
object Constants {

    // 🔸 Constant para sa pangalan ng "users" collection sa Firestore
    const val USERS: String = "users"

    // 🔸 Constant para sa field name ng profile image ng user
    const val IMAGE: String = "image"

    // 🔸 Constant para sa field name ng pangalan ng user
    const val NAME: String = "name"

    // 🔸 Constant para sa field name ng mobile number ng user
    const val MOBILE: String = "mobile"
}
