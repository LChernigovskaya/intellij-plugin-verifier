package com.jetbrains.pluginverifier.resolution

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.createPluginResolver
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.parameters.packages.PackageFilter
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntryResult
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.verifiers.resolution.ClassResolver
import com.jetbrains.pluginverifier.verifiers.resolution.PluginApiClassResolver
import java.io.Closeable
import java.nio.file.Path

class PluginApiClassResolverProvider(
    private val jdkDescriptorCache: JdkDescriptorsCache,
    private val jdkPath: Path,
    private val basePluginResolver: Resolver,
    private val basePluginPackageFilter: PackageFilter
) : ClassResolverProvider {

  override fun provide(
      checkedPluginDetails: PluginDetails,
      verificationResult: VerificationResult
  ): ClassResolver {
    val pluginResolver = checkedPluginDetails.pluginClassesLocations.createPluginResolver()
    return with(jdkDescriptorCache.getJdkResolver(jdkPath)) {
      when (this) {
        is ResourceCacheEntryResult.Found -> {
          val jdkClassesResolver = resourceCacheEntry.resource.jdkResolver
          val closeableResources = listOf<Closeable>(resourceCacheEntry)
          PluginApiClassResolver(pluginResolver, basePluginResolver, jdkClassesResolver, closeableResources, basePluginPackageFilter)
        }
        is ResourceCacheEntryResult.Failed -> throw IllegalStateException("Unable to resolve JDK descriptor", error)
        is ResourceCacheEntryResult.NotFound -> throw IllegalStateException("Unable to find JDK $jdkPath: $message")
      }
    }
  }
}