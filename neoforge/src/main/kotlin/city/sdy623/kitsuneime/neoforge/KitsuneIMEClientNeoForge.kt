package city.sdy623.kitsuneime.neoforge

import city.sdy623.kitsuneime.KitsuneIMEClient
import city.sdy623.kitsuneime.client.handler.KeyHandler
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_CONTEXT

@Mod("ingameime")
object KitsuneIMEClientNeoForge {

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
    
    @Suppress("UNUSED_PARAMETER")
    private fun onClientSetup(event: FMLClientSetupEvent) {
        KitsuneIMEClient.onInitClient()
    }
    
    private fun onRegisterKeyMappings(event: RegisterKeyMappingsEvent) {
        event.register(KeyHandler.toggleKey)
    }
}
