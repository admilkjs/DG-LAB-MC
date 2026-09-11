package dglabmc.client;

import dglabmc.AppServices;
import dglabmc.DgLabMcMod;
import dglabmc.client.ui.FontAdapter;
import dglabmc.client.ui.UiPalette;
import dglabmc.client.ui.UiRender;
import dglabmc.core.rule.RuleEngine;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = DgLabMcMod.MODID, value = Side.CLIENT)
public final class ClientHudOverlay {
    public ClientHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null || minecraft.gameSettings.hideGUI) {
            return;
        }

        RuleEngine.RuntimeSnapshot runtime = AppServices.get().getRuleEngine().snapshot();
        FontAdapter font = new FontAdapter(minecraft.fontRenderer);
        String lineA = line("A", runtime.channelA.currentStrength, runtime.channelA.effectiveMaxStrength);
        String lineB = line("B", runtime.channelB.currentStrength, runtime.channelB.effectiveMaxStrength);
        int panelWidth = Math.max(92, Math.max(font.width(lineA), font.width(lineB)) + 26);
        int panelHeight = 34;
        int left = event.getResolution().getScaledWidth() - panelWidth - 8;
        int top = 8;

        UiRender.drawPanel(null, left, top, panelWidth, panelHeight, 0xAA101721, UiPalette.ACCENT);
        drawLine(font, left + 8, top + 8, lineA, runtime.channelA.outputActive);
        drawLine(font, left + 8, top + 20, lineB, runtime.channelB.outputActive);
    }

    private static void drawLine(FontAdapter font, int x, int y, String line, boolean active) {
        font.draw(null, line, (float) x, (float) y, UiPalette.TEXT_PRIMARY);
        font.draw(null, "\u25CF", (float) (x + font.width(line) + 6), (float) y, active ? UiPalette.SUCCESS : UiPalette.DANGER);
    }

    private static String line(String channel, int current, int max) {
        return channel + " " + current + " | " + Math.max(0, max);
    }
}
