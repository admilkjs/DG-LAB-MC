package dglabmc.client.ui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class OptionPickerScreen<T> extends Screen {
    private final Screen parent;
    private final String heading;
    private final String subtitle;
    private final List<T> options;
    private final Function<T, String> labelProvider;
    private final Function<T, String> descriptionProvider;
    private final Consumer<T> selectHandler;
    private int page;

    public OptionPickerScreen(Screen parent, String heading, String subtitle, List<T> options, Function<T, String> labelProvider, Function<T, String> descriptionProvider, Consumer<T> selectHandler) {
        super(new StringTextComponent(heading));
        this.parent = parent;
        this.heading = heading;
        this.subtitle = subtitle;
        this.options = new ArrayList<T>(options);
        this.labelProvider = labelProvider;
        this.descriptionProvider = descriptionProvider;
        this.selectHandler = selectHandler;
    }

    @Override
    protected void init() {
        this.buttons.clear();
        this.children.clear();
        int panelWidth = Math.min(560, this.width - 24);
        int panelHeight = Math.min(300, this.height - 24);
        boolean compact = panelWidth < 520;
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int pageSize = compact ? 6 : 8;
        int start = this.page * pageSize;
        int end = Math.min(this.options.size(), start + pageSize);
        int buttonWidth = compact ? panelWidth - 36 : 250;

        for (int i = start; i < end; i++) {
            final T value = this.options.get(i);
            int offset = i - start;
            this.addButton(new StyledButton(left + 18, top + 52 + offset * 26, buttonWidth, 20, new StringTextComponent(trim(this.labelProvider.apply(value), compact ? 32 : 26)), StyledButton.Variant.TAB_IDLE, button -> {
                this.selectHandler.accept(value);
            }));
        }

        StyledButton prev = new StyledButton(left + 18, top + panelHeight - 34, compact ? 88 : 74, 20, new StringTextComponent("上一页"), StyledButton.Variant.GHOST, button -> {
            this.page = Math.max(0, this.page - 1);
            init();
        });
        prev.active = this.page > 0;
        this.addButton(prev);

        StyledButton next = new StyledButton(left + (compact ? 114 : 98), top + panelHeight - 34, compact ? 88 : 74, 20, new StringTextComponent("下一页"), StyledButton.Variant.GHOST, button -> {
            this.page++;
            init();
        });
        next.active = end < this.options.size();
        this.addButton(next);

        this.addButton(new StyledButton(left + panelWidth - (compact ? 106 : 128), top + panelHeight - 34, compact ? 88 : 110, 20, new StringTextComponent("返回"), StyledButton.Variant.SECONDARY, button -> this.minecraft.setScreen(this.parent)));
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        fillGradient(matrixStack, 0, 0, this.width, this.height, UiPalette.BACKGROUND_TOP, UiPalette.BACKGROUND_BOTTOM);
        int panelWidth = Math.min(560, this.width - 24);
        int panelHeight = Math.min(300, this.height - 24);
        boolean compact = panelWidth < 520;
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        UiRender.drawPanel(matrixStack, left, top, panelWidth, panelHeight, UiPalette.PANEL, UiPalette.ACCENT);
        UiRender.drawSectionTitle(matrixStack, this.font, this.heading, this.subtitle, left + 18, top + 16);
        int pageSize = compact ? 6 : 8;
        int previewIndex = Math.min(this.options.size() - 1, this.page * pageSize);
        if (!compact) {
            UiRender.drawPanel(matrixStack, left + 286, top + 52, 256, 196, UiPalette.PANEL_MUTED, UiPalette.BORDER_STRONG);
        }
        if (previewIndex >= 0 && !this.options.isEmpty() && !compact) {
            T preview = this.options.get(previewIndex);
            this.font.draw(matrixStack, trim(this.labelProvider.apply(preview), 22), (float) (left + 300), (float) (top + 68), UiPalette.TEXT_PRIMARY);
            UiRender.drawWrappedText(matrixStack, this.font, this.descriptionProvider.apply(preview), left + 300, top + 88, 228, UiPalette.TEXT_MUTED, 10);
            this.font.draw(matrixStack, "点击左侧条目立即选择", (float) (left + 300), (float) (top + 228), UiPalette.TEXT_DIM);
        } else if (compact) {
            this.font.draw(matrixStack, "点击条目立即选择", (float) (left + 18), (float) (top + panelHeight - 54), UiPalette.TEXT_DIM);
        }
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    private String trim(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxChars ? text : text.substring(0, Math.max(0, maxChars - 3)) + "...";
    }
}
