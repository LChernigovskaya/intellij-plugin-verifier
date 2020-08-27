/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ktor.bean

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KtorVendor(
  @SerialName(VENDOR_NAME)
  val name: String? = null,
  @SerialName(VENDOR_EMAIL)
  val vendorEmail: String? = null,
  @SerialName(VENDOR_URL)
  val vendorUrl: String? = null
)

@Serializable
data class KtorFeatureDescriptor(
  @SerialName(ID)
  val pluginId: String? = null,
  @SerialName(NAME)
  val pluginName: String? = null,
  @SerialName(VERSION)
  val pluginVersion: String? = null,
  @SerialName(DESCRIPTION)
  val description: String? = null,
  @SerialName(DOCUMENTATION)
  val documentation: String? = null,
  @SerialName(INSTALL_SNIPPET)
  val installSnippet: String? = null,
  @SerialName(DEPENDENCY)
  val dependency: String? = null,
  @SerialName(COPYRIGHT)
  val copyright: String? = null,
  @SerialName(VENDOR)
  val vendor: KtorVendor? = null
)