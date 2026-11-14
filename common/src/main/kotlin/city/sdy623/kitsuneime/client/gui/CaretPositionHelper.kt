package city.sdy623.kitsuneime.client.gui

import city.sdy623.kitsuneime.client.event.ClientScreenEventHooks
import city.sdy623.kitsuneime.compat.MinecraftCompat
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics

object CaretPositionHelper {
    @JvmStatic
    fun emitCaretFromScreen(edit: Any, screenX: Int, screenY: Int) {
        ClientScreenEventHooks.EDIT_CARET.invoker().onEditCaret(edit, Pair(screenX, screenY))
    }

    @JvmStatic
    fun emitCaretFromGui(edit: Any, guiGraphics: GuiGraphics, localX: Int, localY: Int) {
        val (x, y) = MinecraftCompat.transformGuiPosition(guiGraphics, localX, localY)
        emitCaretFromScreen(edit, x, y)
    }

    @JvmStatic
    fun emitSignCaret(
        edit: Any,
        guiGraphics: GuiGraphics,
        font: Font,
        text: String,
        cursor: Int,
        line: Int,
        lineHeight: Int,
        totalLines: Int
    ) {
        if (cursor < 0 || line < 0 || line >= totalLines) return
        val safeCursor = cursor.coerceAtMost(text.length)
        val lineOffset = ((totalLines - 1) * lineHeight) / 2
        val baseY = line * lineHeight - lineOffset
        val baseX = -font.width(text) / 2
        val caretX = baseX + font.width(text.substring(0, safeCursor))
        emitCaretFromGui(edit, guiGraphics, caretX, baseY)
    }
}
