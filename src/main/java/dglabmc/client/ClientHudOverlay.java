package dglabmc.client;

import dglabmc.AppServices;
import dglabmc.client.ui.UiPalette;
import dglabmc.client.ui.UiRender;
import dglabmc.rule.RuleEngine;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = dglabmc.DgLabMcMod.MODID, value = Dist.CLIENT)
public final class ClientHudOverlay {
    private ClientHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        RuleEngine.RuntimeSnapshot runtime = AppServices.get().getRuleEngine().snapshot();
        FontRenderer font = minecraft.font;
        MatrixStack matrixStack = event.getMatrixStack();
        String lineA = line("A", runtime.channelA.currentStrength, runtime.channelA.effectiveMaxStrength);
        String lineB = line("B", runtime.channelB.currentStrength, runtime.channelB.effectiveMaxStrength);
        int panelWidth = Math.max(92, Math.max(font.width(lineA), font.width(lineB)) + 26);
        int panelHeight = 34;
        int left = event.getWindow().getGuiScaledWidth() - panelWidth - 8;
        int top = 8;

        UiRender.drawPanel(matrixStack, left, top, panelWidth, panelHeight, 0xAA101721, UiPalette.ACCENT);
        drawLine(matrixStack, font, left + 8, top + 8, lineA, runtime.channelA.outputActive);
        drawLine(matrixStack, font, left + 8, top + 20, lineB, runtime.channelB.outputActive);
    }

    private static void drawLine(MatrixStack matrixStack, FontRenderer font, int x, int y, String line, boolean active) {
        font.draw(matrixStack, line, (float) x, (float) y, UiPalette.TEXT_PRIMARY);
        font.draw(matrixStack, "\u25CF", (float) (x + font.width(line) + 6), (float) y, active ? UiPalette.SUCCESS : UiPalette.DANGER);
    }

    private static String line(String channel, int current, int max) {
        return channel + " " + current + " | " + Math.max(0, max);
    }
}
