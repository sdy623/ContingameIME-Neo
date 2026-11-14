package city.sdy623.kitsuneime.client.forge

import city.sdy623.kitsuneime.forge.mixin.MixinKeyboardHandler
import net.minecraft.client.KeyboardHandler
import net.minecraft.client.input.CharacterEvent

object KeyboardHelperImpl {
    @JvmStatic
    fun sendCharTyped(handler: KeyboardHandler, window: Long, codePoint: Int, modifiers: Int) {
        (handler as MixinKeyboardHandler).invokeCharTyped(window, CharacterEvent(codePoint, modifiers))
    }
}
