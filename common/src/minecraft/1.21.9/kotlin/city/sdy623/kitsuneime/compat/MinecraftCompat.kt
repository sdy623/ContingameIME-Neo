package city.sdy623.kitsuneime.compat

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.platform.Window
import dev.architectury.event.EventResult
import dev.architectury.event.events.client.ClientScreenInputEvent
import dev.architectury.platform.Platform
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.Resource
import net.minecraft.server.packs.resources.ResourceManager
import org.joml.Vector2f
import kotlin.math.roundToInt

/** API adapter for the 1.21.9 input, key category, and GUI matrix APIs. */
object MinecraftCompat {
    fun renderDelta(delta: Float): Float = delta

    fun nativeResource(manager: ResourceManager): Resource =
        manager.getResource(ResourceLocation.fromNamespaceAndPath("kitsuneime", "natives/jni.dll")).orElseThrow()

    fun windowHandle(window: Window): Long = window.handle()

    fun createKeyMapping(name: String, type: InputConstants.Type, keyCode: Int, category: String): KeyMapping {
        val categoryId = ResourceLocation.fromNamespaceAndPath("kitsuneime", "keybinding")
        val keyCategory = KeyMapping.Category.register(categoryId)
        return KeyMapping(name, type, keyCode, keyCategory)
    }

    fun registerConfigurationScreen(modId: String, factory: (Screen?) -> Screen) {
        val mod = Platform.getMod(modId)
        val registration = mod.javaClass.methods.firstOrNull {
            it.name == "registerConfigurationScreen" && it.parameterCount == 1
        } ?: return
        registration.invoke(mod, factory)
    }

    fun registerKeyInputEvents(
        onPressed: (Int, Int, Int) -> EventResult,
        onReleased: (Int, Int, Int) -> EventResult
    ) {
        ClientScreenInputEvent.KEY_PRESSED_PRE.register(ClientScreenInputEvent.KeyPressed { _, _, event ->
            onPressed(event.key(), event.scancode(), event.modifiers())
        })
        ClientScreenInputEvent.KEY_RELEASED_PRE.register(ClientScreenInputEvent.KeyReleased { _, _, event ->
            onReleased(event.key(), event.scancode(), event.modifiers())
        })
    }

    fun pushGuiMatrix(graphics: GuiGraphics) {
        graphics.pose().pushMatrix()
    }

    fun popGuiMatrix(graphics: GuiGraphics) = graphics.pose().popMatrix()

    fun transformGuiPosition(graphics: GuiGraphics, x: Int, y: Int): Pair<Int, Int> {
        val position = graphics.pose().transformPosition(x.toFloat(), y.toFloat(), Vector2f())
        return position.x.roundToInt() to position.y.roundToInt()
    }

    fun drawStringAndGetEnd(
        graphics: GuiGraphics, font: Font, text: String, x: Int, y: Int, color: Int, shadow: Boolean
    ): Int {
        graphics.drawString(font, text, x, y, color, shadow)
        return x + font.width(text)
    }
}
