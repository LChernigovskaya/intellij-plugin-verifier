package com.jetbrains.pluginverifier.verifiers.filter

import com.jetbrains.plugin.structure.ide.util.KnownIdePackages
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile

/**
 * Verification filter that excludes mistakenly bundled IDE classes.
 */
object BundledIdeClassesFilter : ClassFilter {

  override fun shouldVerify(classFile: ClassFile): Boolean {
    val packageName = classFile.name.substringBeforeLast('/', "").replace('/', '.')
    return !KnownIdePackages.isKnownPackage(packageName)
  }

}