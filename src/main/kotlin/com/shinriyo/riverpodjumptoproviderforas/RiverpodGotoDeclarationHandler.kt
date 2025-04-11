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

import java.io.File

class RiverpodGotoDeclarationHandler : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(
        element: PsiElement?,
        offset: Int,
        editor: Editor
    ): Array<PsiElement>? {
        if (element == null || element.containingFile !is DartFile) return null

        val ref = PsiTreeUtil.getParentOfType(element, DartReferenceExpression::class.java) ?: return null
        if (!ref.text.endsWith("Provider")) return null

        val project = element.project

        // ファイル中の import から逆引きせずに、.g.dart ファイル全部を見る（遅いが確実）
        val gDartFiles = FilenameIndex.getAllFilesByExt(project, "dart", GlobalSearchScope.projectScope(project))
            .filter { it.name.endsWith(".g.dart") }

        for (gDartFile in gDartFiles) {
            val psiFile = PsiManager.getInstance(project).findFile(gDartFile) ?: continue
            if (!psiFile.text.contains(ref.text)) continue

            val partOfMatch = Regex("""part of ['"](.+?)['"]""").find(psiFile.text) ?: continue
            val relativePath = partOfMatch.groupValues[1]
            val resolvedFile = resolveRelativePath(gDartFile, relativePath) ?: continue
            val originPsi = PsiManager.getInstance(project).findFile(resolvedFile) as? DartFile ?: continue

            // precise: DartComponent で定義取得
            val candidates = PsiTreeUtil.findChildrenOfType(originPsi, DartComponent::class.java)
            for (component in candidates) {
                if (component.name == ref.text) {
                    return arrayOf(component)
                }
            }

            return arrayOf(originPsi) // fallback: ファイル先頭
        }

        return null
    }

    private fun resolveRelativePath(from: VirtualFile, relativePath: String): VirtualFile? {
        val fromDir = from.parent ?: return null
        val targetFile = File(fromDir.path, relativePath).normalize()
        return VfsUtil.findFileByIoFile(targetFile, true)
    }
}
