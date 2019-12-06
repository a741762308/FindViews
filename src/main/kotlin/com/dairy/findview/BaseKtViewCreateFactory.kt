package com.dairy.findview

import com.intellij.psi.PsiFile
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtPsiFactory

abstract class BaseKtViewCreateFactory(@NotNull resIdBeans: MutableList<ResBean>, @NotNull files: PsiFile, @NotNull private val ktClass: KtClass) :
    BaseViewCreateFactory(resIdBeans, files) {

    protected val ktFactory: KtPsiFactory = KtPsiFactory(files.project)
    protected val ktBody: KtClassBody = ktClass.getBody() ?: throw RuntimeException("kotlin class body not found")
    protected val mIsActivity: Boolean = Utils.isKotlinActivity(psiFile, ktClass)
}