package com.jetbrains.pluginverifier.usages.nonExtendable

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.NO_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.WITH_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassOption.FULL_NAME
import com.jetbrains.pluginverifier.results.presentation.formatClassLocation
import com.jetbrains.pluginverifier.usages.formatUsageLocation
import java.util.*

class NonExtendableTypeInherited(
    override val apiElement: ClassLocation,
    override val usageLocation: ClassLocation
) : NonExtendableApiUsage() {

  override val shortDescription
    get() = "Non-extendable " + apiElement.elementType.presentableName + " '${apiElement.formatClassLocation(FULL_NAME, NO_GENERICS)}' is inherited"

  override val fullDescription: String
    get() = buildString {
      append("Non-extendable " + apiElement.elementType.presentableName + " '${apiElement.formatClassLocation(FULL_NAME, WITH_GENERICS)}' is inherited by ")
      append("'" + usageLocation.formatUsageLocation() + "'. ")
      append("This " + apiElement.elementType.presentableName)
      append(" is marked with '@org.jetbrains.annotations.ApiStatus.NonExtendable', which indicates that the ")
      append(apiElement.elementType.presentableName)
      append(" is not supposed to be extended. See documentation of the '@ApiStatus.NonExtendable' for more info.")
    }

  override fun equals(other: Any?) = other is NonExtendableTypeInherited
      && apiElement == other.apiElement
      && usageLocation == other.usageLocation

  override fun hashCode() = Objects.hash(apiElement, usageLocation)
}