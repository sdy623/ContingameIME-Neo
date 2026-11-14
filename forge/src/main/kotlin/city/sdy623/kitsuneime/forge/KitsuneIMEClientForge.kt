package city.sdy623.kitsuneime.forge

import city.sdy623.kitsuneime.KitsuneIMEClient
import city.sdy623.kitsuneime.client.handler.KeyHandler
import net.minecraftforge.client.event.RegisterKeyMappingsEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import thedarkcolour.kotlinforforge.forge.MOD_CONTEXT

@Mod("ingameime")
object KitsuneIMEClientForge {
    init {
        KitsuneIMEClient.registerConfigScreen()
        if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
            KitsuneIMEClient.LOGGER.info("it is Windows OS! Loading mod...")
            val modBus = MOD_CONTEXT.getKEventBus()
            modBus.addListener(::onClientSetup)
            modBus.addListener(::onRegisterKeyMappings)
        } else {
            KitsuneIMEClient.LOGGER.warn("This mod cant work on ${System.getProperty("os.name")}!")
        }
    }

    private fun onClientSetup(event: FMLClientSetupEvent) {
        KitsuneIMEClient.onInitClient()
    }

    private fun onRegisterKeyMappings(event: RegisterKeyMappingsEvent) {
        event.register(KeyHandler.toggleKey)
    }
}
