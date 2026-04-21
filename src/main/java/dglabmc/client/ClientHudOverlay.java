package dglabmc.client;

import dglabmc.AppServices;
import dglabmc.DgLabMcMod;
import dglabmc.client.ui.UiPalette;
import dglabmc.client.ui.UiRender;
import dglabmc.client.ui.UiUtil;
import dglabmc.rule.RuleEngine;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DgLabMcMod.MODID, value = Dist.CLIENT)
public final class ClientHudOverlay {
    private static long lastFlashTimeA;
    private static long lastFlashTimeB;
    private static int prevStrengthA;
    private static int prevStrengthB;

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

        long now = System.currentTimeMillis();
        if (runtime.channelA.currentStrength != prevStrengthA) {
            lastFlashTimeA = now;
            prevStrengthA = runtime.channelA.currentStrength;
        }
        if (runtime.channelB.currentStrength != prevStrengthB) {
            lastFlashTimeB = now;
            prevStrengthB = runtime.channelB.currentStrength;
        }

        String lineA = line("A", runtime.channelA.currentStrength, runtime.channelA.effectiveMaxStrength);
        String lineB = line("B", runtime.channelB.currentStrength, runtime.channelB.effectiveMaxStrength);
        int panelWidth = Math.max(92, Math.max(font.width(lineA), font.width(lineB)) + 26);
        int panelHeight = 34;
        int left = event.getWindow().getGuiScaledWidth() - panelWidth - 8;
        int top = 8;

        UiRender.drawPanel(matrixStack, left, top, panelWidth, panelHeight, UiPalette.HUD_BG, UiPalette.ACCENT);
        drawLine(matrixStack, font, left + 8, top + 8, lineA, runtime.channelA.outputActive, flashColor(now, lastFlashTimeA));
        drawLine(matrixStack, font, left + 8, top + 20, lineB, runtime.channelB.outputActive, flashColor(now, lastFlashTimeB));
    }

    private static int flashColor(long now, long lastFlash) {
        long elapsed = now - lastFlash;
        if (elapsed > 500L || lastFlash == 0L) {
            return UiPalette.TEXT_PRIMARY;
        }
        float progress = Math.min(1.0F, (float) elapsed / 500.0F);
        return UiUtil.lerpColor(UiPalette.WARNING, UiPalette.TEXT_PRIMARY, progress);
    }

    private static void drawLine(MatrixStack matrixStack, FontRenderer font, int x, int y, String line, boolean active, int textColor) {
        font.draw(matrixStack, line, (float) x, (float) y, textColor);
        font.draw(matrixStack, "●", (float) (x + font.width(line) + 6), (float) y, active ? UiPalette.SUCCESS : UiPalette.DANGER);
    }

    private static String line(String channel, int current, int max) {
        return channel + " " + current + " | " + Math.max(0, max);
    }
}
