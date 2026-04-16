package com.flowpvp.client.screen;

import com.flowpvp.client.feature.LeaderboardManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public class LeaderboardScreen extends Screen {

    private static final String[] MODES = {
        "GLOBAL", "SWORD", "AXE", "UHC", "VANILLA",
        "MACE", "DIAMOND_POT", "NETHERITE_OP", "SMP", "DIAMOND_SMP"
    };

    private static final String[] MODE_LABELS = {
        "Global", "Sword", "Axe", "UHC", "Vanilla",
        "Mace", "D.Pot", "NOP", "SMP", "D.SMP"
    };

    private String currentMode = "GLOBAL";
    private int scrollOffset = 0;

    // Bottom "Load More" button reference so we can update its label
    private ButtonWidget loadMoreButton;

    public LeaderboardScreen() {
        super(Text.literal("FlowPvP Leaderboards"));
    }

    @Override
    protected void init() {
        // Two rows of 5 tabs each, 70px wide, 4px gap
        int tabW = 70;
        int tabH = 18;
        int gap = 4;
        int row1Y = 28;
        int row2Y = row1Y + tabH + 3;
        int startX = (width - (5 * tabW + 4 * gap)) / 2;

        for (int i = 0; i < MODES.length; i++) {
            final String mode = MODES[i];
            final String label = MODE_LABELS[i];
            int row = i / 5;
            int col = i % 5;
            int bx = startX + col * (tabW + gap);
            int by = row == 0 ? row1Y : row2Y;

            addDrawableChild(ButtonWidget.builder(Text.literal(label), btn -> {
                currentMode = mode;
                scrollOffset = 0;
                LeaderboardManager.load(mode);
            }).dimensions(bx, by, tabW, tabH).build());
        }

        // "Load More" button at the bottom centre
        loadMoreButton = ButtonWidget.builder(Text.literal("Load More"), btn -> {
            LeaderboardManager.loadMore();
        }).dimensions(width / 2 - 50, height - 24, 100, 18).build();
        addDrawableChild(loadMoreButton);

        LeaderboardManager.load(currentMode);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset -= (int)(verticalAmount * 12);
        if (scrollOffset < 0) scrollOffset = 0;

        // Auto-load more when user scrolls near the bottom of the list
        List<LeaderboardManager.Entry> list = LeaderboardManager.getCached();
        if (!list.isEmpty()) {
            int entryStartY = 82;
            int lineH = 13;
            int totalContentHeight = list.size() * lineH;
            int visibleHeight = height - entryStartY - 30;
            int maxScroll = Math.max(0, totalContentHeight - visibleHeight);
            if (scrollOffset >= maxScroll - 30 && LeaderboardManager.hasMorePages() && !LeaderboardManager.isLoading()) {
                LeaderboardManager.loadMore();
            }
        }

        return true;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        // Title
        ctx.drawCenteredTextWithShadow(textRenderer,
                "FlowPvP Leaderboard — " + currentMode,
                width / 2, 10, 0xFF00BFFF);

        List<LeaderboardManager.Entry> list = LeaderboardManager.getCached();

        int entryStartY = 82;
        int lineH = 13;
        int bottomBarY = height - 30;

        if (list.isEmpty()) {
            String msg = LeaderboardManager.isLoading() ? "Loading..." : "No data.";
            ctx.drawCenteredTextWithShadow(textRenderer,
                    msg, width / 2, entryStartY + 20, 0xFFAAAAAA);
            if (loadMoreButton != null) loadMoreButton.visible = false;
            return;
        }

        // Column headers (drawn outside the scissor region so they don't scroll away)
        ctx.drawTextWithShadow(textRenderer, "#",      50, entryStartY - 12, 0xFF888888);
        ctx.drawTextWithShadow(textRenderer, "Player", 80, entryStartY - 12, 0xFF888888);
        ctx.drawTextWithShadow(textRenderer, "ELO",   220, entryStartY - 12, 0xFF888888);

        // Clamp scroll
        int totalContentHeight = list.size() * lineH;
        int visibleHeight = bottomBarY - entryStartY;
        int maxScroll = Math.max(0, totalContentHeight - visibleHeight);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        ctx.enableScissor(0, entryStartY, width, bottomBarY);

        for (int i = 0; i < list.size(); i++) {
            LeaderboardManager.Entry e = list.get(i);
            int y = entryStartY + i * lineH - scrollOffset;

            // Skip rows fully outside the visible window
            if (y + lineH < entryStartY || y > bottomBarY) continue;

            // Alternating row background
            if (i % 2 == 0) {
                ctx.fill(30, y - 1, width - 30, y + lineH - 2, 0x22FFFFFF);
            }

            ctx.drawTextWithShadow(textRenderer,
                    String.valueOf(e.position > 0 ? e.position : i + 1),
                    50, y, 0xFFFFFF55);
            ctx.drawTextWithShadow(textRenderer, e.name,         80, y, 0xFFFFFFFF);
            ctx.drawTextWithShadow(textRenderer, e.elo + " ELO", 220, y, 0xFFAAAAAA);
        }

        ctx.disableScissor();

        // Update Load More button
        if (loadMoreButton != null) {
            loadMoreButton.visible = true;
            if (LeaderboardManager.isLoading()) {
                loadMoreButton.setMessage(Text.literal("Loading..."));
                loadMoreButton.active = false;
            } else if (!LeaderboardManager.hasMorePages()) {
                loadMoreButton.setMessage(Text.literal("No more pages"));
                loadMoreButton.active = false;
            } else {
                loadMoreButton.setMessage(Text.literal("Load More"));
                loadMoreButton.active = true;
            }
        }

        // Page info bottom-left
        int page = (list.size() + 9) / 10;
        ctx.drawTextWithShadow(textRenderer,
                list.size() + " players | page " + page,
                5, height - 20, 0xFF666666);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
