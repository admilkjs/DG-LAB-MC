package dglabmc.client.ui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class OptionPickerScreen<T> extends BaseScreen {
    private final String heading;
    private final String subtitle;
    private final List<T> options;
    private final Function<T, String> labelProvider;
    private final Function<T, String> descriptionProvider;
    private final Consumer<T> selectHandler;
    private int page;

    public OptionPickerScreen(Screen parent, String heading, String subtitle, List<T> options, Function<T, String> labelProvider, Function<T, String> descriptionProvider, Consumer<T> selectHandler) {
        super(new StringTextComponent(heading), parent);
        this.heading = heading;
        this.subtitle = subtitle;
        this.options = new ArrayList<T>(options);
        this.labelProvider = labelProvider;
        this.descriptionProvider = descriptionProvider;
        this.selectHandler = selectHandler;
    }

    @Override protected int maxPanelWidth() { return 560; }
    @Override protected int compactThreshold() { return 520; }

    @Override
    protected void buildWidgets() {
        int pageSize = compact ? 6 : 8;
        int start = this.page * pageSize;
        int end = Math.min(this.options.size(), start + pageSize);
        int buttonWidth = compact ? innerWidth() : 250;

        for (int i = start; i < end; i++) {
            final T value = this.options.get(i);
            int offset = i - start;
            this.addButton(new StyledButton(innerLeft(), innerTop() + offset * 26, buttonWidth, 20, new StringTextComponent(UiUtil.trimChars(this.labelProvider.apply(value), compact ? 32 : 26)), StyledButton.Variant.TAB_IDLE, button -> {
                this.selectHandler.accept(value);
            }));
        }

        StyledButton prev = new StyledButton(innerLeft(), panelTop + panelHeight - 34, compact ? 88 : 74, 20, new StringTextComponent("上一页"), StyledButton.Variant.GHOST, button -> {
            this.page = Math.max(0, this.page - 1);
            init();
        });
        prev.active = this.page > 0;
        this.addButton(prev);

        StyledButton next = new StyledButton(innerLeft() + (compact ? 96 : 80), panelTop + panelHeight - 34, compact ? 88 : 74, 20, new StringTextComponent("下一页"), StyledButton.Variant.GHOST, button -> {
            this.page++;
            init();
        });
        next.active = end < this.options.size();
        this.addButton(next);

        this.addButton(new StyledButton(panelLeft + panelWidth - (compact ? 106 : 128), panelTop + panelHeight - 34, compact ? 88 : 110, 20, new StringTextComponent("返回"), StyledButton.Variant.SECONDARY, button -> this.minecraft.setScreen(this.parent)));
    }

    @Override
    protected void renderContent(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        UiRender.drawSectionTitle(matrixStack, this.font, this.heading, this.subtitle, innerLeft(), panelTop + 16);
        int pageSize = compact ? 6 : 8;
        int previewIndex = Math.min(this.options.size() - 1, this.page * pageSize);
        if (!compact) {
            UiRender.drawPanel(matrixStack, panelLeft + 286, innerTop(), 256, 196, UiPalette.PANEL_MUTED, UiPalette.BORDER_STRONG);
        }
        if (previewIndex >= 0 && !this.options.isEmpty() && !compact) {
            T preview = this.options.get(previewIndex);
            this.font.draw(matrixStack, UiUtil.trimChars(this.labelProvider.apply(preview), 22), (float) (panelLeft + 300), (float) (innerTop() + 16), UiPalette.TEXT_PRIMARY);
            UiRender.drawWrappedText(matrixStack, this.font, this.descriptionProvider.apply(preview), panelLeft + 300, innerTop() + 36, 228, UiPalette.TEXT_MUTED, 10);
            this.font.draw(matrixStack, "点击左侧条目立即选择", (float) (panelLeft + 300), (float) (innerTop() + 176), UiPalette.TEXT_DIM);
        } else if (compact) {
            this.font.draw(matrixStack, "点击条目立即选择", (float) innerLeft(), (float) (panelTop + panelHeight - 54), UiPalette.TEXT_DIM);
        }
    }
}
