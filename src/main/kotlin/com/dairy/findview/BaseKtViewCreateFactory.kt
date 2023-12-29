package com.dairy.findview

import com.intellij.psi.PsiFile
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtPsiFactory

abstract class BaseKtViewCreateFactory(
    @NotNull resIdBeans: MutableList<ResBean>,
    @NotNull files: PsiFile,
    @NotNull protected val ktClass: KtClass
) :
    BaseViewCreateFactory(resIdBeans, files) {

    protected val ktFactory: KtPsiFactory = KtPsiFactory(files.project)
    protected val ktBody: KtClassBody = ktClass.getBody() ?: throw RuntimeException("kotlin class body not found")
    protected val mIsActivity: Boolean = Utils.isKotlinActivity(psiFile, ktClass)
    
    protected fun getAdapterHolder(recycler: Boolean): KtClass? {
        var holderClass: KtClass? = null
        if (recycler) {
            for (inner in ktClass.declarations.filterIsInstance<KtClass>()) {
                if (Utils.isKotlinRecyclerHolder(
                        psiFile,
                        inner
                    )
                ) {
                    holderClass = inner
                    break
                }
            }
        } else {
            for (inner in ktClass.declarations.filter { it is KtClass }) {
                if (inner.name != null && inner.name!!.contains("ViewHolder")) {
                    holderClass = inner as KtClass
                    break
                }
            }
        }
        return holderClass
    }
}