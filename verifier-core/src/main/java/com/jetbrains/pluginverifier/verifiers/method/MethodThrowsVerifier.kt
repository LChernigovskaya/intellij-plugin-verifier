package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.utils.VerificationContext
import com.jetbrains.pluginverifier.utils.VerifierUtil
import com.jetbrains.pluginverifier.utils.checkClassExistsOrExternal
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.jetbrains.intellij.plugins.internal.asm.tree.MethodNode

/**
 * @author Sergey Patrikeev
 */
class MethodThrowsVerifier : MethodVerifier {
  @Suppress("UNCHECKED_CAST")
  override fun verify(clazz: ClassNode, method: MethodNode, ctx: VerificationContext) {
    val exceptions = method.exceptions as List<String>
    for (exception in exceptions) {
      val descr = VerifierUtil.extractClassNameFromDescr(exception) ?: continue
      ctx.checkClassExistsOrExternal(descr, { ctx.fromMethod(clazz, method) })
    }
  }
}
