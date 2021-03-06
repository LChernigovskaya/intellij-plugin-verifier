package com.jetbrains.pluginverifier.options

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.resolution.LastVersionSelector
import com.jetbrains.pluginverifier.dependencies.resolution.PluginVersionSelector
import com.jetbrains.pluginverifier.misc.exists
import com.jetbrains.pluginverifier.misc.readLines
import com.jetbrains.pluginverifier.misc.retry
import com.jetbrains.pluginverifier.reporting.verification.Reportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import com.jetbrains.pluginverifier.tasks.InvalidPluginFile
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Utility class used to fill [pluginsSet] with a list of plugins to check.
 */
class PluginsParsing(
    private val pluginRepository: PluginRepository,
    private val reportage: Reportage,
    private val pluginsSet: PluginsSet
) {

  /**
   * Parses command line options and add specified plugins compatible with [ideVersion].
   */
  fun addPluginsFromCmdOpts(opts: CmdOpts, ideVersion: IdeVersion) {
    for (pluginId in opts.pluginToCheckAllBuilds) {
      addAllCompatibleVersionsOfPlugin(pluginId, ideVersion)
    }

    for (pluginId in opts.pluginToCheckLastBuild) {
      addLastCompatibleVersionOfPlugin(pluginId, ideVersion)
    }

    val pluginsToCheckFile = opts.pluginsToCheckFile?.let { Paths.get(it) }
    if (pluginsToCheckFile != null) {
      addPluginsListedInFile(pluginsToCheckFile, listOf(ideVersion))
    }
  }

  /**
   * Adds update #[updateId] to [pluginsSet].
   */
  fun addUpdateById(updateId: Int) {
    val updateInfo = pluginRepository.retry("get plugin info for #$updateId") {
      (pluginRepository as? MarketplaceRepository)?.getPluginInfoById(updateId)
    } ?: throw IllegalArgumentException("Update #$updateId is not found in the Plugin Repository")
    pluginsSet.schedulePlugin(updateInfo)
  }

  /**
   * Adds last version of [pluginId] to [pluginsSet].
   */
  fun addLastPluginVersion(pluginId: String) {
    val selector = LastVersionSelector()
    val selectResult = selector.selectPluginVersion(pluginId, pluginRepository)
    if (selectResult is PluginVersionSelector.Result.Selected) {
      pluginsSet.schedulePlugin(selectResult.pluginInfo)
    }
  }

  /**
   * Parses lines of [pluginsListFile] and adds specified plugins to the [pluginsSet].
   *
   * ```
   * id:<plugin-id>     // all compatible version of <plugin-id>
   * $id or id$         // only the last version of the plugin compatible with IDEs will be checked
   * #<update-id>       // update #<update-id>
   * path:<plugin-path> // plugin from <plugin-path>
   * <other>            // treated as a path: or id:
   * ```
   */
  fun addPluginsListedInFile(pluginsListFile: Path, ideVersions: List<IdeVersion>) {
    val lines = pluginsListFile.readLines().map { it.trim() }.filterNot { it.isEmpty() }
    for (ideVersion in ideVersions) {
      for (line in lines) {
        if (line.startsWith("//")) {
          continue
        }

        if (line.startsWith('$') || line.endsWith('$')) {
          val pluginId = line.trim('$').trim()
          addLastCompatibleVersionOfPlugin(pluginId, ideVersion)
          continue
        }

        if (line.startsWith("#")) {
          val updateId = line.substringAfter("#").toIntOrNull() ?: continue
          addUpdateById(updateId)
          continue
        }

        if (line.startsWith("id:")) {
          val pluginId = line.substringAfter("id:")
          addAllCompatibleVersionsOfPlugin(pluginId, ideVersion)
          continue
        }

        val pluginFile = if (line.startsWith("path:")) {
          val linePath = line.substringAfter("path:")
          tryFindPluginByPath(pluginsListFile, linePath)
              ?: throw IllegalArgumentException("Invalid path: $linePath")
        } else {
          tryFindPluginByPath(pluginsListFile, line)
        }

        if (pluginFile != null) {
          addPluginFile(pluginFile, true)
        } else {
          addAllCompatibleVersionsOfPlugin(line, ideVersion)
        }
      }
    }
  }

  private fun tryFindPluginByPath(baseFilePath: Path, linePath: String): Path? {
    val path = try {
      Paths.get(linePath)
    } catch (e: Exception) {
      return null
    }

    if (path.exists()) {
      return path
    }

    val siblingPath = baseFilePath.resolveSibling(linePath)
    if (siblingPath.exists()) {
      return siblingPath
    }
    return null
  }

  /**
   * Adds all versions of the plugin with ID `pluginId` compatible with `ideVersion`.
   */
  private fun addAllCompatibleVersionsOfPlugin(pluginId: String, ideVersion: IdeVersion) {
    val compatibleVersions = pluginRepository.retry("fetch all compatible versions of plugin $pluginId with $ideVersion") {
      getAllCompatibleVersionsOfPlugin(ideVersion, pluginId)
    }
    pluginsSet.schedulePlugins(compatibleVersions)
  }

  /**
   * Adds the last version of plugin with ID `pluginId` compatible with `ideVersion`.
   */
  private fun addLastCompatibleVersionOfPlugin(pluginId: String, ideVersion: IdeVersion) {
    val lastVersion = pluginRepository.retry("get last version of $pluginId compatible with $ideVersion") {
      getLastCompatibleVersionOfPlugin(ideVersion, pluginId)
    } ?: return
    pluginsSet.schedulePlugin(lastVersion)
  }

  /**
   * Adds plugin from [pluginFile].
   */
  fun addPluginFile(pluginFile: Path, validateDescriptor: Boolean) {
    if (!pluginFile.exists()) {
      throw RuntimeException("Plugin file '$pluginFile' with absolute path '${pluginFile.toAbsolutePath()}' doesn't exist")
    }

    reportage.logVerificationStage("Reading plugin to check from $pluginFile")
    val pluginCreationResult = IdePluginManager.createManager().createPlugin(pluginFile.toFile(), validateDescriptor)
    with(pluginCreationResult) {
      when (this) {
        is PluginCreationSuccess -> pluginsSet.scheduleLocalPlugin(plugin)
        is PluginCreationFail -> {
          reportage.logVerificationStage("Plugin is invalid in $pluginFile: ${errorsAndWarnings.joinToString()}")
          pluginsSet.invalidPluginFiles.add(InvalidPluginFile(pluginFile, errorsAndWarnings))
        }
      }
    }
  }

}