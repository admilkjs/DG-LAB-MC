package dglabmc.client.ui;

import dglabmc.AppServices;
import dglabmc.config.AppConfig;
import dglabmc.rule.RuleDefinition;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.TextComponent;

import java.util.ArrayList;
import java.util.List;

public class RuleOrderScreen extends Screen {
    private static final int ROW_HEIGHT = 44;
    private static final int ROW_GAP = 10;
    private static final int CARD_HEIGHT = 28;

    private final Screen parent;
    private int selectedIndex = -1;
    private int page;
    private String statusMessage = "";

    private int draggingIndex = -1;
    private int draggingCardWidth;
    private int draggingCardHeight;
    private double draggingMouseX;
    private double draggingMouseY;
    private double draggingStartX;
    private double draggingStartY;
    private double draggingOffsetX;
    private double draggingOffsetY;
    private boolean dragMoved;
    private DropTarget dropTarget;

    private StyledButton movePreviousButton;
    private StyledButton moveNextButton;
    private StyledButton mergeButton;
    private StyledButton splitButton;
    private StyledButton previousPageButton;
    private StyledButton nextPageButton;

    public RuleOrderScreen(Screen parent) {
        super(new TextComponent("规则顺序"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        clearDragState();

        List<RowData> rows = buildRows(false);
        List<RuleDefinition> currentRules = rules();
        if (currentRules.isEmpty()) {
            this.selectedIndex = -1;
            this.page = 0;
        } else {
            this.selectedIndex = Math.max(0, Math.min(this.selectedIndex, currentRules.size() - 1));
            this.page = Math.max(0, Math.min(this.page, maxPageForRowCount(rows.size())));
            RulePosition selectedPosition = findPosition(rows, selectedRuleId());
            if (selectedPosition != null) {
                int selectedPage = selectedPosition.rowIndex / rowsPerPage();
                if (selectedPage != this.page) {
                    this.page = selectedPage;
                }
            }
        }

        int innerLeft = panelLeft() + 18;
        int innerWidth = panelWidth() - 36;
        int blockTop = buttonBlockTop();
        if (compact()) {
            int halfWidth = (innerWidth - 8) / 2;
            this.movePreviousButton = this.addRenderableWidget(new StyledButton(innerLeft, blockTop, halfWidth, 20, new TextComponent("前移"), StyledButton.Variant.GHOST, button -> moveSelected(-1)));
            this.moveNextButton = this.addRenderableWidget(new StyledButton(innerLeft + halfWidth + 8, blockTop, halfWidth, 20, new TextComponent("后移"), StyledButton.Variant.GHOST, button -> moveSelected(1)));
            this.mergeButton = this.addRenderableWidget(new StyledButton(innerLeft, blockTop + 24, halfWidth, 20, new TextComponent("并到上一行"), StyledButton.Variant.SECONDARY, button -> mergeIntoPreviousRow()));
            this.splitButton = this.addRenderableWidget(new StyledButton(innerLeft + halfWidth + 8, blockTop + 24, halfWidth, 20, new TextComponent("单独一行"), StyledButton.Variant.SECONDARY, button -> splitToNextRow()));
            this.previousPageButton = this.addRenderableWidget(new StyledButton(innerLeft, blockTop + 48, halfWidth, 20, new TextComponent("上一页"), StyledButton.Variant.GHOST, button -> {
                this.page = Math.max(0, this.page - 1);
                init();
            }));
            this.nextPageButton = this.addRenderableWidget(new StyledButton(innerLeft + halfWidth + 8, blockTop + 48, halfWidth, 20, new TextComponent("下一页"), StyledButton.Variant.GHOST, button -> {
                this.page = Math.min(maxPageForRowCount(buildRows(false).size()), this.page + 1);
                init();
            }));
        } else {
            int buttonWidth = (innerWidth - 40) / 6;
            this.movePreviousButton = this.addRenderableWidget(new StyledButton(innerLeft, blockTop, buttonWidth, 20, new TextComponent("前移"), StyledButton.Variant.GHOST, button -> moveSelected(-1)));
            this.moveNextButton = this.addRenderableWidget(new StyledButton(innerLeft + (buttonWidth + 8), blockTop, buttonWidth, 20, new TextComponent("后移"), StyledButton.Variant.GHOST, button -> moveSelected(1)));
            this.mergeButton = this.addRenderableWidget(new StyledButton(innerLeft + (buttonWidth + 8) * 2, blockTop, buttonWidth, 20, new TextComponent("并到上一行"), StyledButton.Variant.SECONDARY, button -> mergeIntoPreviousRow()));
            this.splitButton = this.addRenderableWidget(new StyledButton(innerLeft + (buttonWidth + 8) * 3, blockTop, buttonWidth, 20, new TextComponent("单独一行"), StyledButton.Variant.SECONDARY, button -> splitToNextRow()));
            this.previousPageButton = this.addRenderableWidget(new StyledButton(innerLeft + (buttonWidth + 8) * 4, blockTop, buttonWidth, 20, new TextComponent("上一页"), StyledButton.Variant.GHOST, button -> {
                this.page = Math.max(0, this.page - 1);
                init();
            }));
            this.nextPageButton = this.addRenderableWidget(new StyledButton(innerLeft + (buttonWidth + 8) * 5, blockTop, buttonWidth, 20, new TextComponent("下一页"), StyledButton.Variant.GHOST, button -> {
                this.page = Math.min(maxPageForRowCount(buildRows(false).size()), this.page + 1);
                init();
            }));
        }
        this.addRenderableWidget(new StyledButton(panelLeft() + panelWidth() - 116, panelTop() + 16, 98, 20, new TextComponent("返回"), StyledButton.Variant.PRIMARY, button -> onClose()));

        updateButtonState();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            CardLayout card = findCardAt(mouseX, mouseY, false);
            if (card != null) {
                this.selectedIndex = card.absoluteIndex;
                this.draggingIndex = card.absoluteIndex;
                this.draggingCardWidth = card.width;
                this.draggingCardHeight = card.height;
                this.draggingMouseX = mouseX;
                this.draggingMouseY = mouseY;
                this.draggingStartX = mouseX;
                this.draggingStartY = mouseY;
                this.draggingOffsetX = mouseX - card.x;
                this.draggingOffsetY = mouseY - card.y;
                this.dragMoved = false;
                this.dropTarget = null;
                updateButtonState();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.draggingIndex >= 0) {
            this.draggingMouseX = mouseX;
            this.draggingMouseY = mouseY;
            if (!this.dragMoved) {
                this.dragMoved = Math.abs(mouseX - this.draggingStartX) > 3.0D || Math.abs(mouseY - this.draggingStartY) > 3.0D;
            }
            if (this.dragMoved) {
                this.dropTarget = computeDropTarget(mouseX, mouseY);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.draggingIndex >= 0) {
            if (this.dragMoved) {
                this.dropTarget = computeDropTarget(mouseX, mouseY);
                applyDragMove();
            }
            clearDragState();
            updateButtonState();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        fillGradient(matrixStack, 0, 0, this.width, this.height, UiPalette.BACKGROUND_TOP, UiPalette.BACKGROUND_BOTTOM);

        int left = panelLeft();
        int top = panelTop();
        int innerLeft = left + 18;
        List<RowData> rows = buildRows(false);
        int totalPages = Math.max(1, maxPageForRowCount(rows.size()) + 1);
        int currentPage = Math.min(this.page, totalPages - 1) + 1;
        String pageLabel = "第 " + currentPage + " / " + totalPages + " 页";

        UiRender.drawPanel(matrixStack, left, top, panelWidth(), panelHeight(), UiPalette.PANEL, UiPalette.ACCENT);
        UiRender.drawSectionTitle(matrixStack, this.font, "规则顺序", "拖到目标行，空隙单独成行", innerLeft, top + 16);
        this.font.draw(matrixStack, pageLabel, (float) (left + panelWidth() - 18 - this.font.width(pageLabel)), (float) (top + 28), UiPalette.TEXT_MUTED);

        renderRows(matrixStack, mouseX, mouseY);

        if (rows.isEmpty()) {
            this.font.draw(matrixStack, "当前没有规则", (float) innerLeft, (float) (listTop() + 12), UiPalette.TEXT_MUTED);
        } else {
            RulePosition selectedPosition = findPosition(rows, selectedRuleId());
            if (selectedPosition != null) {
                RuleDefinition selected = rules().get(this.selectedIndex);
                this.font.draw(matrixStack, "已选：第 " + (selectedPosition.rowIndex + 1) + " 行 / " + displayRuleName(selected), (float) innerLeft, (float) infoTop(), UiPalette.TEXT_MUTED);
            }
        }

        if (!this.statusMessage.isEmpty()) {
            this.font.draw(matrixStack, this.statusMessage, (float) innerLeft, (float) (infoTop() + 14), UiPalette.WARNING);
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    private void renderRows(PoseStack matrixStack, int mouseX, int mouseY) {
        List<RowLayout> layouts = buildVisibleRowLayouts(this.dragMoved);
        CardLayout hoveredCard = this.dragMoved ? null : findCardAt(mouseX, mouseY, false);
        if (this.dragMoved && layouts.isEmpty()) {
            UiRender.drawPanel(matrixStack, listLeft(), listTop(), listWidth(), ROW_HEIGHT, 0x66172233, UiPalette.ACCENT);
            this.font.draw(matrixStack, "松开后建立第一行", (float) (listLeft() + 16), (float) (listTop() + 17), UiPalette.TEXT_PRIMARY);
        }

        for (RowLayout row : layouts) {
            boolean rowTarget = this.dragMoved && this.dropTarget != null && this.dropTarget.mode == DropMode.IN_ROW && this.dropTarget.rowIndex == row.rowIndex;
            drawRowBackground(matrixStack, row, rowTarget);
            for (CardLayout card : row.cards) {
                boolean selected = card.absoluteIndex == this.selectedIndex;
                boolean hovered = hoveredCard != null && hoveredCard.absoluteIndex == card.absoluteIndex;
                drawCard(matrixStack, card.x, card.y, card.width, card.height, card.rule, selected, hovered, false);
            }
        }

        if (this.dragMoved && this.dropTarget != null) {
            drawDropTarget(matrixStack, layouts, this.dropTarget);
        }
        if (this.dragMoved && this.draggingIndex >= 0 && this.draggingIndex < rules().size()) {
            int drawX = Mth.floor(this.draggingMouseX - this.draggingOffsetX);
            int drawY = Mth.floor(this.draggingMouseY - this.draggingOffsetY);
            drawCard(matrixStack, drawX, drawY, Math.max(72, this.draggingCardWidth), Math.max(CARD_HEIGHT, this.draggingCardHeight), rules().get(this.draggingIndex), true, true, true);
        }
    }

    private void drawRowBackground(PoseStack matrixStack, RowLayout row, boolean targetRow) {
        int border = targetRow ? UiPalette.ACCENT : UiPalette.BORDER;
        int accent = targetRow ? UiPalette.ACCENT : UiPalette.INFO;
        UiRender.drawPanel(matrixStack, row.x, row.y, row.width, row.height, 0x66172233, border);
        fill(matrixStack, row.x + 8, row.y + 7, row.x + 12, row.y + row.height - 7, accent);
        this.font.draw(matrixStack, "第" + (row.rowIndex + 1) + "行", (float) (row.x + 18), (float) (row.y + 16), UiPalette.TEXT_MUTED);
    }

    private void drawCard(PoseStack matrixStack, int x, int y, int width, int height, RuleDefinition rule, boolean selected, boolean hovered, boolean floating) {
        int background = selected ? 0xCC243041 : hovered ? 0xB3233043 : rule.enabled ? 0x99172233 : 0x77202A38;
        int border = selected ? UiPalette.ACCENT : hovered ? UiPalette.BORDER_STRONG : UiPalette.BORDER;
        if (floating) {
            background = 0xD9243041;
        }
        UiRender.drawPanel(matrixStack, x, y, width, height, background, border);
        fill(matrixStack, x + 6, y + 6, x + 10, y + height - 6, rule.enabled ? UiPalette.SUCCESS : UiPalette.TEXT_DIM);
        String label = trimToWidth(displayRuleName(rule), width - 22);
        this.font.draw(matrixStack, label, (float) (x + 16), (float) (y + 10), rule.enabled ? UiPalette.TEXT_PRIMARY : UiPalette.TEXT_MUTED);
    }

    private void drawDropTarget(PoseStack matrixStack, List<RowLayout> layouts, DropTarget target) {
        if (target.mode == DropMode.EMPTY) {
            fill(matrixStack, listLeft(), listTop() + ROW_HEIGHT / 2, listLeft() + listWidth(), listTop() + ROW_HEIGHT / 2 + 2, UiPalette.ACCENT);
            return;
        }
        if (target.mode == DropMode.IN_ROW) {
            RowLayout row = findRowLayout(layouts, target.rowIndex);
            if (row == null) {
                return;
            }
            int x = dropMarkerX(row, target.slotIndex);
            fill(matrixStack, x, row.y + 6, x + 3, row.y + row.height - 6, UiPalette.ACCENT);
            return;
        }

        int y;
        if (target.mode == DropMode.NEW_ROW_BEFORE) {
            RowLayout row = findRowLayout(layouts, target.rowIndex);
            y = row == null ? listTop() - 2 : row.y - 3;
        } else {
            RowLayout row = findRowLayout(layouts, target.rowIndex);
            y = row == null ? listTop() + ROW_HEIGHT + 2 : row.y + row.height + 1;
        }
        fill(matrixStack, listLeft(), y, listLeft() + listWidth(), y + 2, UiPalette.ACCENT);
    }

    private int dropMarkerX(RowLayout row, int slotIndex) {
        if (row.cards.isEmpty()) {
            return row.cardsLeft;
        }
        if (slotIndex <= 0) {
            return row.cards.get(0).x - 3;
        }
        if (slotIndex >= row.cards.size()) {
            CardLayout last = row.cards.get(row.cards.size() - 1);
            return last.x + last.width + 2;
        }
        CardLayout previous = row.cards.get(slotIndex - 1);
        CardLayout next = row.cards.get(slotIndex);
        return (previous.x + previous.width + next.x) / 2 - 1;
    }

    private void moveSelected(int delta) {
        String ruleId = selectedRuleId();
        if (ruleId.isEmpty()) {
            return;
        }
        List<RowData> rows = buildRows(false);
        RulePosition position = findPosition(rows, ruleId);
        RuleDefinition moving = findRuleById(ruleId);
        if (position == null || moving == null) {
            return;
        }

        AppConfig config = AppServices.get().getConfig();
        List<List<RuleDefinition>> reorderedRows = buildMutableRows(config.rules, ruleId);
        int sourceRowSize = rows.get(position.rowIndex).cards.size();
        if (delta < 0) {
            if (position.slotIndex > 0) {
                reorderedRows.get(position.rowIndex).add(position.slotIndex - 1, moving);
            } else if (position.rowIndex > 0) {
                reorderedRows.get(position.rowIndex - 1).add(moving);
            } else {
                return;
            }
            saveRows(config, reorderedRows, ruleId);
            this.statusMessage = "已前移";
            init();
            return;
        }

        if (position.slotIndex < sourceRowSize - 1) {
            reorderedRows.get(position.rowIndex).add(Math.min(position.slotIndex + 1, reorderedRows.get(position.rowIndex).size()), moving);
        } else {
            int targetRow = sourceRowSize == 1 ? position.rowIndex : position.rowIndex + 1;
            if (targetRow >= reorderedRows.size()) {
                return;
            }
            reorderedRows.get(targetRow).add(0, moving);
        }
        saveRows(config, reorderedRows, ruleId);
        this.statusMessage = "已后移";
        init();
    }

    private void mergeIntoPreviousRow() {
        String ruleId = selectedRuleId();
        if (ruleId.isEmpty()) {
            return;
        }
        List<RowData> rows = buildRows(false);
        RulePosition position = findPosition(rows, ruleId);
        RuleDefinition moving = findRuleById(ruleId);
        if (position == null || moving == null || position.rowIndex <= 0) {
            return;
        }

        AppConfig config = AppServices.get().getConfig();
        List<List<RuleDefinition>> reorderedRows = buildMutableRows(config.rules, ruleId);
        reorderedRows.get(position.rowIndex - 1).add(moving);
        saveRows(config, reorderedRows, ruleId);
        this.statusMessage = "已并到上一行";
        init();
    }

    private void splitToNextRow() {
        String ruleId = selectedRuleId();
        if (ruleId.isEmpty()) {
            return;
        }
        List<RowData> rows = buildRows(false);
        RulePosition position = findPosition(rows, ruleId);
        RuleDefinition moving = findRuleById(ruleId);
        if (position == null || moving == null) {
            return;
        }
        if (rows.get(position.rowIndex).cards.size() <= 1) {
            this.statusMessage = "当前已是单独一行";
            return;
        }

        AppConfig config = AppServices.get().getConfig();
        List<List<RuleDefinition>> reorderedRows = buildMutableRows(config.rules, ruleId);
        List<RuleDefinition> newRow = new ArrayList<RuleDefinition>();
        newRow.add(moving);
        reorderedRows.add(Math.min(position.rowIndex + 1, reorderedRows.size()), newRow);
        saveRows(config, reorderedRows, ruleId);
        this.statusMessage = "已拆成单独一行";
        init();
    }

    private void applyDragMove() {
        String ruleId = draggingRuleId();
        if (ruleId.isEmpty()) {
            return;
        }
        RuleDefinition moving = findRuleById(ruleId);
        if (moving == null) {
            return;
        }

        AppConfig config = AppServices.get().getConfig();
        List<List<RuleDefinition>> reorderedRows = buildMutableRows(config.rules, ruleId);
        DropTarget target = this.dropTarget == null ? DropTarget.empty() : this.dropTarget;
        int targetRowIndex = 0;
        if (target.mode == DropMode.EMPTY || reorderedRows.isEmpty()) {
            List<RuleDefinition> row = new ArrayList<RuleDefinition>();
            row.add(moving);
            reorderedRows.add(row);
            targetRowIndex = 0;
        } else if (target.mode == DropMode.IN_ROW) {
            int rowIndex = Math.max(0, Math.min(target.rowIndex, reorderedRows.size() - 1));
            List<RuleDefinition> row = reorderedRows.get(rowIndex);
            row.add(Math.max(0, Math.min(target.slotIndex, row.size())), moving);
            targetRowIndex = rowIndex;
        } else if (target.mode == DropMode.NEW_ROW_BEFORE) {
            int rowIndex = Math.max(0, Math.min(target.rowIndex, reorderedRows.size()));
            List<RuleDefinition> row = new ArrayList<RuleDefinition>();
            row.add(moving);
            reorderedRows.add(rowIndex, row);
            targetRowIndex = rowIndex;
        } else {
            int rowIndex = Math.max(0, Math.min(target.rowIndex + 1, reorderedRows.size()));
            List<RuleDefinition> row = new ArrayList<RuleDefinition>();
            row.add(moving);
            reorderedRows.add(rowIndex, row);
            targetRowIndex = rowIndex;
        }

        saveRows(config, reorderedRows, ruleId);
        this.statusMessage = "已移动到第 " + (targetRowIndex + 1) + " 行";
        init();
    }

    private void saveRows(AppConfig config, List<List<RuleDefinition>> rows, String focusRuleId) {
        config.rules.clear();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<RuleDefinition> row = rows.get(rowIndex);
            for (RuleDefinition rule : row) {
                rule.orderGroup = rowIndex;
                config.rules.add(rule);
            }
        }
        AppServices.get().saveConfig(config);
        this.selectedIndex = findRuleIndex(config.rules, focusRuleId);
    }

    private void clearDragState() {
        this.draggingIndex = -1;
        this.draggingCardWidth = 0;
        this.draggingCardHeight = 0;
        this.draggingMouseX = 0.0D;
        this.draggingMouseY = 0.0D;
        this.draggingStartX = 0.0D;
        this.draggingStartY = 0.0D;
        this.draggingOffsetX = 0.0D;
        this.draggingOffsetY = 0.0D;
        this.dragMoved = false;
        this.dropTarget = null;
    }

    private CardLayout findCardAt(double mouseX, double mouseY, boolean excludeDragging) {
        for (RowLayout row : buildVisibleRowLayouts(excludeDragging)) {
            for (CardLayout card : row.cards) {
                if (mouseX >= card.x && mouseX <= card.x + card.width && mouseY >= card.y && mouseY <= card.y + card.height) {
                    return card;
                }
            }
        }
        return null;
    }

    private DropTarget computeDropTarget(double mouseX, double mouseY) {
        List<RowLayout> layouts = buildVisibleRowLayouts(true);
        if (layouts.isEmpty()) {
            return DropTarget.empty();
        }
        if (mouseY <= layouts.get(0).y + 4) {
            return DropTarget.newRowBefore(layouts.get(0).rowIndex);
        }
        for (int i = 0; i < layouts.size(); i++) {
            RowLayout row = layouts.get(i);
            int rowBottom = row.y + row.height;
            if (mouseY >= row.y && mouseY <= rowBottom) {
                return DropTarget.inRow(row.rowIndex, computeSlot(row, mouseX));
            }
            int nextTop = i + 1 < layouts.size() ? layouts.get(i + 1).y : listBottom();
            if (mouseY > rowBottom && mouseY <= nextTop) {
                return DropTarget.newRowAfter(row.rowIndex);
            }
        }
        return DropTarget.newRowAfter(layouts.get(layouts.size() - 1).rowIndex);
    }

    private int computeSlot(RowLayout row, double mouseX) {
        if (row.cards.isEmpty()) {
            return 0;
        }
        if (mouseX <= row.cards.get(0).x) {
            return 0;
        }
        for (CardLayout card : row.cards) {
            if (mouseX < card.x + (card.width / 2.0D)) {
                return card.slotIndex;
            }
        }
        return row.cards.size();
    }

    private void updateButtonState() {
        List<RowData> rows = buildRows(false);
        RulePosition position = findPosition(rows, selectedRuleId());
        boolean hasSelection = position != null;
        int rowCount = rows.size();

        if (this.movePreviousButton != null) {
            this.movePreviousButton.active = hasSelection && (position.slotIndex > 0 || position.rowIndex > 0);
        }
        if (this.moveNextButton != null) {
            this.moveNextButton.active = hasSelection && (position.slotIndex < rows.get(position.rowIndex).cards.size() - 1 || position.rowIndex < rowCount - 1);
        }
        if (this.mergeButton != null) {
            this.mergeButton.active = hasSelection && position.rowIndex > 0;
        }
        if (this.splitButton != null) {
            this.splitButton.active = hasSelection && rows.get(position.rowIndex).cards.size() > 1;
        }
        if (this.previousPageButton != null) {
            this.previousPageButton.active = this.page > 0;
        }
        if (this.nextPageButton != null) {
            this.nextPageButton.active = this.page < maxPageForRowCount(rowCount);
        }
    }

    private List<RowLayout> buildVisibleRowLayouts(boolean excludeDragging) {
        List<RowData> rows = buildRows(excludeDragging);
        List<RowLayout> layouts = new ArrayList<RowLayout>();
        int visiblePage = Math.max(0, Math.min(this.page, maxPageForRowCount(rows.size())));
        int start = visiblePage * rowsPerPage();
        int end = Math.min(rows.size(), start + rowsPerPage());
        int rowY = listTop();
        for (int index = start; index < end; index++) {
            RowData row = rows.get(index);
            RowLayout layout = new RowLayout(row.rowIndex, listLeft(), rowY, listWidth(), ROW_HEIGHT);
            layout.cardsLeft = layout.x + 64;
            layout.cardsWidth = Math.max(96, layout.width - 78);
            int gap = cardGap(row.cards.size());
            int cardWidth = computeCardWidth(row.cards.size(), layout.cardsWidth, gap);
            int cardY = rowY + (ROW_HEIGHT - CARD_HEIGHT) / 2;
            int cardX = layout.cardsLeft;
            for (int slot = 0; slot < row.cards.size(); slot++) {
                RuleCard ruleCard = row.cards.get(slot);
                layout.cards.add(new CardLayout(ruleCard.absoluteIndex, ruleCard.rule, row.rowIndex, slot, cardX, cardY, cardWidth, CARD_HEIGHT));
                cardX += cardWidth + gap;
            }
            layouts.add(layout);
            rowY += ROW_HEIGHT + ROW_GAP;
        }
        return layouts;
    }

    private List<RowData> buildRows(boolean excludeDragging) {
        List<RowData> rows = new ArrayList<RowData>();
        RowData currentRow = null;
        int previousGroup = Integer.MIN_VALUE;
        for (int index = 0; index < rules().size(); index++) {
            if (excludeDragging && this.dragMoved && index == this.draggingIndex) {
                continue;
            }
            RuleDefinition rule = rules().get(index);
            int currentGroup = Math.max(0, rule.orderGroup);
            if (currentRow == null || currentGroup != previousGroup) {
                currentRow = new RowData(rows.size());
                rows.add(currentRow);
                previousGroup = currentGroup;
            }
            currentRow.cards.add(new RuleCard(index, rule));
        }
        return rows;
    }

    private List<List<RuleDefinition>> buildMutableRows(List<RuleDefinition> source, String excludedRuleId) {
        List<List<RuleDefinition>> rows = new ArrayList<List<RuleDefinition>>();
        List<RuleDefinition> currentRow = null;
        int previousGroup = Integer.MIN_VALUE;
        for (RuleDefinition rule : source) {
            if (excludedRuleId != null && excludedRuleId.equals(rule.id)) {
                continue;
            }
            int currentGroup = Math.max(0, rule.orderGroup);
            if (currentRow == null || currentGroup != previousGroup) {
                currentRow = new ArrayList<RuleDefinition>();
                rows.add(currentRow);
                previousGroup = currentGroup;
            }
            currentRow.add(rule);
        }
        return rows;
    }

    private RulePosition findPosition(List<RowData> rows, String ruleId) {
        if (ruleId == null || ruleId.isEmpty()) {
            return null;
        }
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            RowData row = rows.get(rowIndex);
            for (int slotIndex = 0; slotIndex < row.cards.size(); slotIndex++) {
                RuleCard card = row.cards.get(slotIndex);
                if (ruleId.equals(card.rule.id)) {
                    return new RulePosition(rowIndex, slotIndex);
                }
            }
        }
        return null;
    }

    private RuleDefinition findRuleById(String ruleId) {
        if (ruleId == null || ruleId.isEmpty()) {
            return null;
        }
        for (RuleDefinition rule : rules()) {
            if (ruleId.equals(rule.id)) {
                return rule;
            }
        }
        return null;
    }

    private RowLayout findRowLayout(List<RowLayout> rows, int rowIndex) {
        for (RowLayout row : rows) {
            if (row.rowIndex == rowIndex) {
                return row;
            }
        }
        return null;
    }

    private String selectedRuleId() {
        if (this.selectedIndex < 0 || this.selectedIndex >= rules().size()) {
            return "";
        }
        RuleDefinition selected = rules().get(this.selectedIndex);
        return selected.id == null ? "" : selected.id;
    }

    private String draggingRuleId() {
        if (this.draggingIndex < 0 || this.draggingIndex >= rules().size()) {
            return "";
        }
        RuleDefinition selected = rules().get(this.draggingIndex);
        return selected.id == null ? "" : selected.id;
    }

    private int findRuleIndex(List<RuleDefinition> rules, String ruleId) {
        if (ruleId == null || ruleId.isEmpty()) {
            return -1;
        }
        for (int index = 0; index < rules.size(); index++) {
            if (ruleId.equals(rules.get(index).id)) {
                return index;
            }
        }
        return -1;
    }

    private List<RuleDefinition> rules() {
        return AppServices.get().getConfig().rules;
    }

    private int panelWidth() {
        return Math.min(760, this.width - 24);
    }

    private int panelHeight() {
        return Math.min(compact() ? 432 : 408, this.height - 24);
    }

    private int panelLeft() {
        return (this.width - panelWidth()) / 2;
    }

    private int panelTop() {
        return (this.height - panelHeight()) / 2;
    }

    private boolean compact() {
        return panelWidth() < 680;
    }

    private int rowsPerPage() {
        return Math.max(1, listHeight() / (ROW_HEIGHT + ROW_GAP));
    }

    private int maxPageForRowCount(int rowCount) {
        return rowCount <= 0 ? 0 : Math.max(0, (rowCount - 1) / rowsPerPage());
    }

    private int listLeft() {
        return panelLeft() + 18;
    }

    private int listWidth() {
        return panelWidth() - 36;
    }

    private int listTop() {
        return panelTop() + 58;
    }

    private int buttonBlockTop() {
        return panelTop() + panelHeight() - (compact() ? 84 : 36);
    }

    private int buttonBlockHeight() {
        return compact() ? 68 : 20;
    }

    private int infoTop() {
        return buttonBlockTop() - 34;
    }

    private int listBottom() {
        return infoTop() - 10;
    }

    private int listHeight() {
        return Math.max(ROW_HEIGHT, listBottom() - listTop());
    }

    private int cardGap(int count) {
        return count >= 6 ? 4 : 8;
    }

    private int computeCardWidth(int count, int availableWidth, int gap) {
        if (count <= 0) {
            return availableWidth;
        }
        int rawWidth = (availableWidth - gap * Math.max(0, count - 1)) / count;
        int maxWidth = count == 1 ? Math.min(availableWidth, 220) : Math.min(availableWidth, 168);
        int width = Math.min(maxWidth, Math.max(36, rawWidth));
        while (width * count + gap * Math.max(0, count - 1) > availableWidth && width > 24) {
            width--;
        }
        return Math.max(24, width);
    }

    private String displayRuleName(RuleDefinition rule) {
        if (rule == null) {
            return "";
        }
        if (rule.name != null && !rule.name.trim().isEmpty()) {
            return rule.name;
        }
        return rule.id == null ? "" : rule.id;
    }

    private String trimToWidth(String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        String clipped = this.font.plainSubstrByWidth(text, Math.max(0, maxWidth - this.font.width(suffix)));
        return clipped == null || clipped.isEmpty() ? "" : clipped + suffix;
    }

    private static final class RuleCard {
        final int absoluteIndex;
        final RuleDefinition rule;

        private RuleCard(int absoluteIndex, RuleDefinition rule) {
            this.absoluteIndex = absoluteIndex;
            this.rule = rule;
        }
    }

    private static final class RowData {
        final int rowIndex;
        final List<RuleCard> cards = new ArrayList<RuleCard>();

        private RowData(int rowIndex) {
            this.rowIndex = rowIndex;
        }
    }

    private static final class RowLayout {
        final int rowIndex;
        final int x;
        final int y;
        final int width;
        final int height;
        int cardsLeft;
        int cardsWidth;
        final List<CardLayout> cards = new ArrayList<CardLayout>();

        private RowLayout(int rowIndex, int x, int y, int width, int height) {
            this.rowIndex = rowIndex;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private static final class CardLayout {
        final int absoluteIndex;
        final RuleDefinition rule;
        final int rowIndex;
        final int slotIndex;
        final int x;
        final int y;
        final int width;
        final int height;

        private CardLayout(int absoluteIndex, RuleDefinition rule, int rowIndex, int slotIndex, int x, int y, int width, int height) {
            this.absoluteIndex = absoluteIndex;
            this.rule = rule;
            this.rowIndex = rowIndex;
            this.slotIndex = slotIndex;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private static final class RulePosition {
        final int rowIndex;
        final int slotIndex;

        private RulePosition(int rowIndex, int slotIndex) {
            this.rowIndex = rowIndex;
            this.slotIndex = slotIndex;
        }
    }

    private enum DropMode {
        EMPTY,
        IN_ROW,
        NEW_ROW_BEFORE,
        NEW_ROW_AFTER
    }

    private static final class DropTarget {
        final DropMode mode;
        final int rowIndex;
        final int slotIndex;

        private DropTarget(DropMode mode, int rowIndex, int slotIndex) {
            this.mode = mode;
            this.rowIndex = rowIndex;
            this.slotIndex = slotIndex;
        }

        private static DropTarget empty() {
            return new DropTarget(DropMode.EMPTY, 0, 0);
        }

        private static DropTarget inRow(int rowIndex, int slotIndex) {
            return new DropTarget(DropMode.IN_ROW, rowIndex, slotIndex);
        }

        private static DropTarget newRowBefore(int rowIndex) {
            return new DropTarget(DropMode.NEW_ROW_BEFORE, rowIndex, 0);
        }

        private static DropTarget newRowAfter(int rowIndex) {
            return new DropTarget(DropMode.NEW_ROW_AFTER, rowIndex, 0);
        }
    }
}
