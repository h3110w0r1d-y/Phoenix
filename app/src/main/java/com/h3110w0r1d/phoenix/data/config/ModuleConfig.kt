package com.h3110w0r1d.phoenix.data.config

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class KeepAliveConfig(
    val enabled: Boolean = false,
    val maxAdj: Int? = null,
    val persistent: Boolean = false,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class ModuleConfig(
    val moduleEnabled: Boolean = false,
    val globalMaxAdj: Int = 0,
    val appKeepAliveConfigs: HashMap<String, KeepAliveConfig> = hashMapOf(),
)
