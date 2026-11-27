package com.trackr.core.model

/** 定位到其他城市时的处理方式；旧版升级和未知存储值均默认询问。 */
enum class CityChangeMode {
    Ask, Automatic, Manual;

    companion object {
        fun fromName(name: String?): CityChangeMode = entries.firstOrNull { it.name == name } ?: Ask
    }
}
