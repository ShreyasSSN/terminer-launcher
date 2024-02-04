package com.example.terminer

data class DrawerItem(
    val id : String ,
    val name : String,
    val number : String? = null,
    val type : ItemType)

enum class ItemType{
    APP,
    CONTACT,
    UNINSTALL
}