package costaber.com.github.omniflow.resource.util

import costaber.com.github.omniflow.renderer.IndentedRenderingContext

inline fun render(
    renderer: IndentedRenderingContext,
    builderAction: IndentedRenderingContext.() -> Unit
): String {
    renderer.builderAction()
    return renderer.getString()
}