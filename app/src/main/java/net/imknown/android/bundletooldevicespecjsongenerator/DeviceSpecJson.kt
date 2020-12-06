package net.imknown.android.bundletooldevicespecjsongenerator

import kotlinx.serialization.Serializable

@Serializable
data class DeviceSpecJson(
    val supportedAbis: List<String>,
    val supportedLocales: List<String>,
    val deviceFeatures: List<String>?,
    val glExtensions: List<String>?,
    val screenDensity: Int,
    val sdkVersion: Int
)