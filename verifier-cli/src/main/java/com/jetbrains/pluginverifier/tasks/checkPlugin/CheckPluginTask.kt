package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.core.Verification
import com.jetbrains.pluginverifier.core.VerifierTask
import com.jetbrains.pluginverifier.dependencies.resolution.ChainDependencyFinder
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.dependencies.resolution.IdeDependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.misc.VersionComparatorUtil
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.local.LocalPluginInfo
import com.jetbrains.pluginverifier.tasks.Task

/**
 * The 'check-plugin' [task] [Task] that verifies
 * each plugin from the [CheckPluginParams.pluginsToCheck]
 * against each IDE from the [CheckPluginParams.ideDescriptors].
 *
 * If one [verified] [CheckPluginParams.pluginsToCheck] plugin depends on
 * another verified plugin then the [dependency resolution] [DependencyFinder]
 * prefers the verified plugin to a plugin from the [PluginRepository].
 */
class CheckPluginTask(private val parameters: CheckPluginParams,
                      private val pluginRepository: PluginRepository,
                      private val pluginDetailsCache: PluginDetailsCache) : Task {

  /**
   * The 'check-plugin' task must try to find the plugin among all the verified plugins:
   * suppose plugins A and B are verified simultaneously and the A depends on the B.
   * Then the B must be resolved from the local plugins when checking the A.
   */
  private inner class VerifiedPluginsDependencyFinder : DependencyFinder {

    override fun findPluginDependency(dependency: PluginDependency) =
        parameters.pluginsToCheck.plugins
            .filterIsInstance<LocalPluginInfo>()
            .filter { it.pluginId == dependency.id }
            .maxWith(compareBy(VersionComparatorUtil.COMPARATOR) { it.version })
            ?.let { DependencyFinder.Result.DetailsProvided(pluginDetailsCache.getPluginDetails(it)) }
            ?: DependencyFinder.Result.NotFound("Not found among local plugins")

  }

  /**
   * Creates the [DependencyFinder] that:
   * 1) Resolves the [dependency] [PluginDependency] in the [verified] [CheckPluginParams.pluginsToCheck] plugins
   * 2) If not found, resolves the [dependency] [PluginDependency] using the [IdeDependencyFinder].
   */
  private fun createDependencyFinder(ideDescriptor: IdeDescriptor): DependencyFinder {
    val ideDependencyFinder = IdeDependencyFinder(ideDescriptor.ide, pluginRepository, pluginDetailsCache)
    val localPluginDependencyFinder = VerifiedPluginsDependencyFinder()
    return ChainDependencyFinder(listOf(localPluginDependencyFinder, ideDependencyFinder))
  }

  override fun execute(verificationReportage: VerificationReportage): CheckPluginResult {
    val tasks = parameters.ideDescriptors.flatMap { ideDescriptor ->
      val dependencyFinder = createDependencyFinder(ideDescriptor)
      parameters.pluginsToCheck.plugins.map { pluginInfo ->
        VerifierTask(pluginInfo, ideDescriptor, dependencyFinder)
      }
    }
    val verifierParams = VerifierParameters(
        parameters.externalClassesPrefixes,
        parameters.problemsFilters,
        parameters.externalClasspath,
        true
    )
    val results = Verification.run(verifierParams, pluginDetailsCache, tasks, verificationReportage, parameters.jdkDescriptor)
    return CheckPluginResult(parameters.pluginsToCheck.invalidPluginFiles, results)
  }

}
