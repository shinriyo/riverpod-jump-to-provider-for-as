package com.shinriyo.riverpodjumptoproviderforas

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.psi.*
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.openapi.progress.ProcessCanceledException

import java.io.File

class RiverpodGotoDeclarationHandler : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(
        element: PsiElement?,
        offset: Int,
        editor: Editor
    ): Array<PsiElement>? {
        try {
            if (element == null) return null
            
            val file = element.containingFile
            println("💡 File type: ${file::class}")
            println("💡 File type (java): ${file.javaClass}")
            println("💡 File name: ${file.name}")
            println("💡 Is DartFile: ${file is DartFile}")
            if (file !is DartFile) {
                println("❌ Not a DartFile, returning null")
                return null
            }
            println("✅ Is a DartFile, continuing...")

            val ref = PsiTreeUtil.getParentOfType(element, DartReferenceExpression::class.java) ?: return null
            if (!ref.text.endsWith("Provider")) return null

            val project = element.project
            val gDartFiles = FilenameIndex.getAllFilesByExt(project, "dart", GlobalSearchScope.projectScope(project))
                .filter { it.name.endsWith(".g.dart") }

            for (gDartFile in gDartFiles) {
                try {
                    val psiFile = PsiManager.getInstance(project).findFile(gDartFile) ?: continue
                    if (!psiFile.text.contains(ref.text)) continue

                    val partOfMatch = Regex("""part of ['"](.+?)['"]""").find(psiFile.text) ?: continue
                    val relativePath = partOfMatch.groupValues[1]
                    
                    val resolvedFile = resolveRelativePath(gDartFile, relativePath) ?: continue
                    val originPsi = PsiManager.getInstance(project).findFile(resolvedFile) as? DartFile ?: continue

                    val candidates = PsiTreeUtil.findChildrenOfType(originPsi, DartComponent::class.java)
                    for (component in candidates) {
                        if (component.name == ref.text) {
                            return arrayOf(component)
                        }
                    }

                    return arrayOf(originPsi)
                } catch (e: ProcessCanceledException) {
                    // キャンセルされた場合は次のファイルを試す
                    continue
                }
            }
        } catch (e: ProcessCanceledException) {
            // キャンセルされた場合はnullを返す
            return null
        } catch (e: Exception) {
            // その他のエラーはログに出力
            println("💥 Error in getGotoDeclarationTargets: ${e.message}")
            return null
        }

        return null
    }

    private fun resolveRelativePath(from: VirtualFile, relativePath: String): VirtualFile? {
        val fromDir = from.parent ?: return null
        val targetFile = File(fromDir.path, relativePath).normalize()
        return VfsUtil.findFileByIoFile(targetFile, true)
    }
}
