package dglabmc.client;

import dglabmc.AppServices;
import dglabmc.multiplayer.PlayerChannelState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;

@Mod.EventBusSubscriber(modid = dglabmc.DgLabMcMod.MODID, value = Dist.CLIENT)
public final class ClientNameplateOverlay {
    private static final int BAR_Y = -18;
    private static final int TEXT_COLOR = 0xFFF7FBFF;
    private static final int COLOR_A = 0xFFD62424;
    private static final int COLOR_B = 0xFF0098B8;
    private static final int COLOR_ACTIVE = 0xFF23C552;
    private static final int COLOR_IDLE = 0xFFC88400;
    private static final float SNEAK_DIM = 0.6F;

    private ClientNameplateOverlay() {
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
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

        renderStatusLine(event, player, buildLayout(state));
    }

    private static void renderStatusLine(RenderNameTagEvent event, Player player, StatusLayout layout) {
        EntityRenderer<?> renderer = event.getEntityRenderer();
        Font font = renderer.getFont();
        boolean sneaking = player.isDiscrete();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        float yOffset = player.getNameTagOffsetY();

        poseStack.pushPose();
        poseStack.translate(0.0F, yOffset, 0.0F);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);

        Matrix4f matrix = poseStack.last().pose();
        float textX = -(layout.textWidth / 2.0F);
        float textY = BAR_Y;
        drawSegments(font, matrix, buffer, textX, textY, layout, sneaking, event.getPackedLight());
        poseStack.popPose();
    }

    private static StatusLayout buildLayout(PlayerChannelState state) {
        StatusSegment[] segments = new StatusSegment[] {
            new StatusSegment("A", COLOR_A),
            new StatusSegment(String.valueOf(state.strengthA), COLOR_A),
            new StatusSegment(" ", TEXT_COLOR),
            new StatusSegment("\u25CF", state.outputA ? COLOR_ACTIVE : COLOR_IDLE),
            new StatusSegment("   ", TEXT_COLOR),
            new StatusSegment("B", COLOR_B),
            new StatusSegment(String.valueOf(state.strengthB), COLOR_B),
            new StatusSegment(" ", TEXT_COLOR),
            new StatusSegment("\u25CF", state.outputB ? COLOR_ACTIVE : COLOR_IDLE)
        };

        int width = 0;
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        for (StatusSegment segment : segments) {
            width += font.width(segment.text);
        }
        return new StatusLayout(segments, width);
    }

    private static void drawSegments(Font font, Matrix4f matrix, MultiBufferSource buffer, float startX, float y, StatusLayout layout, boolean sneaking, int packedLight) {
        float currentX = startX;
        for (StatusSegment segment : layout.segments) {
            int color = tintColor(segment.color, sneaking);
            font.drawInBatch(segment.text, currentX, y, color, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight);
            currentX += font.width(segment.text);
        }
    }

    private static int tintColor(int color, boolean sneaking) {
        if (!sneaking) {
            return color;
        }
        int alpha = (color >>> 24) & 255;
        if (alpha == 0) {
            alpha = 255;
        }
        int red = (int) (((color >>> 16) & 255) * SNEAK_DIM);
        int green = (int) (((color >>> 8) & 255) * SNEAK_DIM);
        int blue = (int) ((color & 255) * SNEAK_DIM);
        int tintedAlpha = Math.max(200, (int) (alpha * 0.9F));
        return (tintedAlpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static final class StatusLayout {
        private final StatusSegment[] segments;
        private final int textWidth;

        private StatusLayout(StatusSegment[] segments, int textWidth) {
            this.segments = segments;
            this.textWidth = textWidth;
        }
    }

    private static final class StatusSegment {
        private final String text;
        private final int color;

        private StatusSegment(String text, int color) {
            this.text = text;
            this.color = color;
        }
    }
}
