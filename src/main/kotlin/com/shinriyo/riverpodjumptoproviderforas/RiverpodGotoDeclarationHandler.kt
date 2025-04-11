package com.shinriyo.riverpodjumptoproviderforas

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.psi.DartCallExpression
import com.jetbrains.lang.dart.psi.DartFile
import com.jetbrains.lang.dart.psi.DartReferenceExpression

class RiverpodGotoDeclarationHandler : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(
        element: PsiElement?,
        offset: Int,
        editor: Editor
    ): Array<PsiElement>? {
        if (element == null || element.containingFile !is DartFile) return null

        val ref = PsiTreeUtil.getParentOfType(element, DartReferenceExpression::class.java) ?: return null
        val word = ref.text
        if (!word.endsWith("Provider")) return null

        val providerName = word.removeSuffix("Provider")
        val project = element.project

        val gDartFiles = FilenameIndex.getAllFilesByExt(project, "dart", GlobalSearchScope.projectScope(project))
            .filter { it.name.endsWith(".g.dart") }

        for (gDartFile in gDartFiles) {
            val psiFile = PsiManager.getInstance(project).findFile(gDartFile) ?: continue
            val text = psiFile.text
            if (!text.contains(word as CharSequence)) continue

            val partOfMatch = Regex("part of ['\"](.+?)['\"]").find(text) ?: continue
            val relativePath = partOfMatch.groupValues[1]
            val gDartDir = gDartFile.parent ?: continue
            
            // Find the original file using the relative path
            val originFile = findFileByRelativePath(gDartDir, relativePath) ?: continue
            val originPsi = PsiManager.getInstance(project).findFile(originFile) as? DartFile ?: continue

            val providerForMatch = Regex("@ProviderFor\\((\\w+)\\)").find(text)
            val targetName = providerForMatch?.groupValues?.get(1) ?: toPascalCase(providerName)

            val candidates = PsiTreeUtil.findChildrenOfType(originPsi, DartCallExpression::class.java)
            for (call in candidates) {
                val identifier = call.firstChild?.text ?: continue
                if (identifier == targetName) {
                    return arrayOf(call)
                }
            }
        }

        return null
    }

    override fun getActionText(context: DataContext): String? = null

    private fun toPascalCase(input: String): String {
        return input.split("_").joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
    }
    
    private fun findFileByRelativePath(baseDir: VirtualFile, relativePath: String): VirtualFile? {
        // Handle both forward slashes and backslashes
        val normalizedPath = relativePath.replace("\\", "/")
        
        // If the path starts with a slash, remove it
        val path = if (normalizedPath.startsWith("/")) normalizedPath.substring(1) else normalizedPath
        
        // Try to find the file directly
        var currentDir = baseDir
        val pathParts = path.split("/")
        
        for (part in pathParts) {
            val child = currentDir.findChild(part) ?: return null
            currentDir = child
        }
        
        return currentDir
    }
}
