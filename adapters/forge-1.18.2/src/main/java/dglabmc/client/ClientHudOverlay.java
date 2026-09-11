package dglabmc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dglabmc.AppServices;
import dglabmc.client.ui.UiPalette;
import dglabmc.client.ui.UiRender;
import dglabmc.core.config.AppConfig;
import dglabmc.core.rule.RuleEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = dglabmc.DgLabMcMod.MODID, value = Dist.CLIENT)
public final class ClientHudOverlay {
    private static final int PANEL_HEIGHT = 34;
    private static final int FIXED_MARGIN_X = 8;
    private static final int FIXED_MARGIN_Y = 8;

    private ClientHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        AppConfig.HudOverlayPreferences hud = AppServices.get().getConfig().ui.hudOverlay;
        if (!hud.enabled) {
            return;
        }

        RuleEngine.RuntimeSnapshot runtime = AppServices.get().getRuleEngine().snapshot();
        Font font = minecraft.font;
        PoseStack matrixStack = event.getMatrixStack();
        String lineA = line("A", runtime.channelA.currentStrength, runtime.channelA.effectiveMaxStrength);
        String lineB = line("B", runtime.channelB.currentStrength, runtime.channelB.effectiveMaxStrength);
        int panelWidth = Math.max(108, Math.max(font.width(lineA), font.width(lineB)) + 30);
        int left = event.getWindow().getGuiScaledWidth() - Math.max(1, (int) Math.round(panelWidth * hud.scale)) - FIXED_MARGIN_X;
        int top = FIXED_MARGIN_Y;

        PoseStack poseStack = matrixStack;
        poseStack.pushPose();
        poseStack.translate(left, top, 0.0F);
        poseStack.scale((float) hud.scale, (float) hud.scale, 1.0F);
        UiRender.drawPanel(poseStack, 0, 0, panelWidth, PANEL_HEIGHT, alphaColor(0x101721, hud.panelOpacity), UiPalette.ACCENT);
        drawLine(poseStack, font, 8, 8, lineA, runtime.channelA.outputActive, 0xFF4545, 0x7A1E1E);
        drawLine(poseStack, font, 8, 20, lineB, runtime.channelB.outputActive, 0x2FE7FF, 0x156A78);
        poseStack.popPose();
    }

    private static void drawLine(PoseStack poseStack, Font font, int x, int y, String line, boolean active, int activeColor, int inactiveColor) {
        font.draw(poseStack, line, (float) x, (float) y, UiPalette.TEXT_PRIMARY);
        font.draw(poseStack, "\u25CF", (float) (x + font.width(line) + 6), (float) y, active ? activeColor : inactiveColor);
    }

    private static String line(String channel, int current, int max) {
        return channel + " " + current + " | " + Math.max(0, max);
    }

    private static int alphaColor(int rgb, double opacity) {
        int alpha = (int) Math.round(Math.max(0.0D, Math.min(1.0D, opacity)) * 255.0D);
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }
}


