package dglabmc.client;

import dglabmc.AppServices;
import dglabmc.core.multiplayer.PlayerChannelState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;

/** Adds a compact DG-LAB status suffix to player nameplates on NeoForge 1.21.4. */
@EventBusSubscriber(modid = dglabmc.DgLabMcMod.MODID, value = Dist.CLIENT)
public final class ClientNameplateOverlay {
    private ClientNameplateOverlay() {
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent.CanRender event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        boolean debugMode = ClientCommandRouter.isDebugMode();
        if (!AppServices.get().getConfig().ui.showPlayerStatus && !debugMode) {
            return;
        }
        PlayerChannelState state = ClientPlayerStateCache.getDisplayState(player);
        if (state == null) {
            return;
        }
        Component original = event.getContent();
        if (original == null) {
            original = event.getOriginalContent();
        }
        if (original == null) {
            original = Component.literal(player.getName().getString());
        }
        String suffix = " [A" + state.strengthA + (state.outputA ? "●" : "○")
            + " B" + state.strengthB + (state.outputB ? "●" : "○") + "]";
        event.setContent(Component.literal(original.getString() + suffix));
    }
}

