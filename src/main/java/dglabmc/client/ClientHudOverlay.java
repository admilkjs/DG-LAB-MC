package dglabmc.client;

import dglabmc.AppServices;
import dglabmc.client.ui.UiPalette;
import dglabmc.client.ui.UiRender;
import dglabmc.rule.RuleEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiOverlayEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@Mod.EventBusSubscriber(modid = dglabmc.DgLabMcMod.MODID, value = Dist.CLIENT)
public final class ClientHudOverlay {
    private ClientHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        RuleEngine.RuntimeSnapshot runtime = AppServices.get().getRuleEngine().snapshot();
        Font font = minecraft.font;
        GuiGraphics guiGraphics = event.getGuiGraphics();
        String lineA = line("A", runtime.channelA.currentStrength, runtime.channelA.effectiveMaxStrength);
        String lineB = line("B", runtime.channelB.currentStrength, runtime.channelB.effectiveMaxStrength);
        int panelWidth = Math.max(92, Math.max(font.width(lineA), font.width(lineB)) + 26);
        int panelHeight = 34;
        int left = event.getWindow().getGuiScaledWidth() - panelWidth - 8;
        int top = 8;

        UiRender.drawPanel(guiGraphics, left, top, panelWidth, panelHeight, 0xAA101721, UiPalette.ACCENT);
        drawLine(guiGraphics, font, left + 8, top + 8, lineA, runtime.channelA.outputActive);
        drawLine(guiGraphics, font, left + 8, top + 20, lineB, runtime.channelB.outputActive);
    }

    private static void drawLine(GuiGraphics guiGraphics, Font font, int x, int y, String line, boolean active) {
        guiGraphics.drawString(font, line, x, y, UiPalette.TEXT_PRIMARY);
        guiGraphics.drawString(font, "\u25CF", x + font.width(line) + 6, y, active ? UiPalette.SUCCESS : UiPalette.DANGER);
    }

    private static String line(String channel, int current, int max) {
        return channel + " " + current + " | " + Math.max(0, max);
    }
}
