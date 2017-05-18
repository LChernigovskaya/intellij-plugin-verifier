package com.jetbrains.pluginverifier.verifiers.clazz

import com.jetbrains.pluginverifier.problems.SuperInterfaceBecameClassProblem
import com.jetbrains.pluginverifier.utils.VerificationContext
import com.jetbrains.pluginverifier.utils.VerifierUtil
import com.jetbrains.pluginverifier.utils.resolveClassOrProblem
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode

/**
 * Check that all explicitly defined interfaces exist.

 * @author Dennis.Ushakov
 */
class InterfacesVerifier : ClassVerifier {
  override fun verify(clazz: ClassNode, ctx: VerificationContext) {
    clazz.interfaces
        .filterIsInstance(String::class.java)
        .mapNotNull { ctx.resolveClassOrProblem(it, clazz, { ctx.fromClass(clazz) }) }
        .filterNot { VerifierUtil.isInterface(it) }
        .forEach { ctx.registerProblem(SuperInterfaceBecameClassProblem(ctx.fromClass(clazz), ctx.fromClass(it))) }
  }
}
