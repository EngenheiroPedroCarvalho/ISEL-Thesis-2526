package costaber.com.github.omniflow.renderer

abstract class IndentedNodeRenderer : NodeRenderer<String> {

    override fun beginRender(renderingContext: RenderingContext): String {
        val indentedRenderingContext = renderingContext as IndentedRenderingContext
        val render = internalBeginRender(indentedRenderingContext)
        indentedRenderingContext.incIndentationLevel()
        return render
    }

    override fun endRender(renderingContext: RenderingContext): String {
        val indentedRenderingContext = renderingContext as IndentedRenderingContext
        indentedRenderingContext.decIndentationLevel()
        return internalEndRender(indentedRenderingContext)
    }

    protected abstract fun internalBeginRender(renderingContext: IndentedRenderingContext): String

    protected abstract fun internalEndRender(renderingContext: IndentedRenderingContext): String
}