package city.sdy623.kitsuneime.client.neoforge

import city.sdy623.kitsuneime.neoforge.mixin.MixinKeyboardHandler
import net.minecraft.client.KeyboardHandler
import net.minecraft.client.input.CharacterEvent

object KeyboardHelperImpl {
    @JvmStatic
    fun sendCharTyped(handler: KeyboardHandler, window: Long, codePoint: Int, modifiers: Int) {
        (handler as MixinKeyboardHandler).invokeCharTyped(window, CharacterEvent(codePoint, modifiers))
    }
}
