package city.sdy623.kitsuneime.compat

import dev.architectury.event.EventResult
import dev.architectury.event.events.client.ClientScreenInputEvent
import dev.architectury.platform.Platform
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.screens.Screen
import net.minecraft.server.packs.resources.Resource
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.resources.ResourceLocation
import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.platform.Window
import org.joml.Vector4f
import kotlin.math.roundToInt

/** Minecraft API differences selected at build time by minecraft_version. */
object MinecraftCompat {
    fun renderDelta(delta: Float): Float = delta

    fun nativeResource(manager: ResourceManager): Resource =
        manager.getResource(ResourceLocation("kitsuneime", "natives/jni.dll")).orElseThrow()

    fun windowHandle(window: Window): Long = window.window

    fun createKeyMapping(name: String, type: InputConstants.Type, keyCode: Int, category: String): KeyMapping =
        KeyMapping(name, type, keyCode, category)

    fun registerConfigurationScreen(modId: String, factory: (Screen?) -> Screen) {
        Platform.getMod(modId).registerConfigurationScreen(factory)
    }

    fun registerKeyInputEvents(
        onPressed: (Int, Int, Int) -> EventResult,
        onReleased: (Int, Int, Int) -> EventResult
    ) {
        ClientScreenInputEvent.KEY_PRESSED_PRE.register(ClientScreenInputEvent.KeyPressed { _, _, key, scan, modifiers ->
            onPressed(key, scan, modifiers)
        })
        ClientScreenInputEvent.KEY_RELEASED_PRE.register(ClientScreenInputEvent.KeyReleased { _, _, key, scan, modifiers ->
            onReleased(key, scan, modifiers)
        })
    }

    fun pushGuiMatrix(graphics: GuiGraphics) {
        graphics.pose().pushPose()
        graphics.pose().translate(0.0, 0.0, 500.0)
    }

    fun popGuiMatrix(graphics: GuiGraphics) = graphics.pose().popPose()

    fun transformGuiPosition(graphics: GuiGraphics, x: Int, y: Int): Pair<Int, Int> {
        val position = Vector4f(x.toFloat(), y.toFloat(), 0f, 1f).mul(graphics.pose().last().pose())
        return position.x.roundToInt() to position.y.roundToInt()
    }

    fun drawStringAndGetEnd(
        graphics: GuiGraphics, font: Font, text: String, x: Int, y: Int, color: Int, shadow: Boolean
    ): Int = graphics.drawString(font, text, x, y, color, shadow)
}
