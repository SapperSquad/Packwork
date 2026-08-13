package com.sappersquad.packwork.client;

import com.sappersquad.packwork.guide.HandbookContent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * The Outfitter's Handbook: a stamped-leather book with a chapter rail down the left and
 * paginated, wrapped prose on the right, in Packwork's own leather-and-brass styling.
 * Content lives in {@link HandbookContent}; this screen only lays it out.
 *
 * <p>A plain {@link Screen} (not a container screen), so it draws its whole panel in
 * {@link #render} - the {@code renderBg()}-only-fires-from-{@code renderBackground()}
 * container-screen gotcha does not apply here.
 */
public class OutfitterHandbookScreen extends Screen {

    // leather + brass palette (ARGB), matched to the pack GUI's tones
    private static final int LEATHER_HI = 0xFF7A5636;
    private static final int LEATHER_LO = 0xFF5E4128;
    private static final int LEATHER_EDGE = 0xFF3E2A18;
    private static final int LEATHER_DARK = 0xFF4A3320;
    private static final int BRASS = 0xFFC9A24B;
    private static final int BRASS_HI = 0xFFE7CC82;
    private static final int BRASS_LO = 0xFF8A6A28;
    private static final int PARCHMENT = 0xFFEAD9A6;
    private static final int TEXT_DIM = 0xFFB59B6E;
    private static final int HEADING = 0xFFF3E6C6;
    private static final int SLOT_BG = 0xFF3C2A19;

    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 216;
    private static final int SIDEBAR_WIDTH = 106;
    private static final int CONTENT_PADDING = 8;
    private static final int LINE_HEIGHT = 10;
    private static final int LINES_PER_PAGE = 15;
    private static final int CHAPTER_BUTTON_HEIGHT = 18;
    private static final int HEADER_HEIGHT = 16;

    private int panelLeft;
    private int panelTop;
    private int chapterIndex = 0;
    private int pageIndex = 0;

    private final List<Object> wrappedLines = new ArrayList<>();
    private static final Object SPACER = new Object();

    public OutfitterHandbookScreen() {
        super(Component.translatable("packwork.handbook.title"));
    }

    @Override
    protected void init() {
        panelLeft = (width - PANEL_WIDTH) / 2;
        panelTop = (height - PANEL_HEIGHT) / 2;
        rebuildLines();
    }

    private int contentX() { return panelLeft + SIDEBAR_WIDTH + CONTENT_PADDING; }
    private int contentWidth() { return PANEL_WIDTH - SIDEBAR_WIDTH - CONTENT_PADDING * 2; }
    private int contentY() { return panelTop + 4 + HEADER_HEIGHT + 24; }

    private void rebuildLines() {
        wrappedLines.clear();
        HandbookContent.Chapter chapter = HandbookContent.CHAPTERS.get(chapterIndex);
        for (HandbookContent.Entry entry : chapter.entries()) {
            if (entry instanceof HandbookContent.TextEntry textEntry) {
                var paragraphLines = font.split(Component.literal(textEntry.text()), contentWidth());
                int remaining = LINES_PER_PAGE - (wrappedLines.size() % LINES_PER_PAGE);
                if (paragraphLines.size() > remaining && paragraphLines.size() <= LINES_PER_PAGE) {
                    padToPageBoundary();
                }
                wrappedLines.addAll(paragraphLines);
                addParagraphGap();
            } else if (entry instanceof HandbookContent.ItemsEntry itemsEntry) {
                // A caption that would clip at the panel edge (the six-pack ladder rows)
                // becomes its own dim line ABOVE the icons instead of a truncated tail.
                boolean splitCaption = !itemsEntry.caption().isEmpty()
                        && font.width(itemsEntry.caption())
                                > contentWidth() - (itemsEntry.items().size() * 20 + 4);
                if (LINES_PER_PAGE - (wrappedLines.size() % LINES_PER_PAGE) < (splitCaption ? 3 : 2)) {
                    padToPageBoundary();
                }
                if (splitCaption) {
                    wrappedLines.add(Component.literal(itemsEntry.caption())
                            .withStyle(s -> s.withColor(TEXT_DIM & 0xFFFFFF)).getVisualOrderText());
                    wrappedLines.add(new HandbookContent.ItemsEntry("", itemsEntry.items()));
                } else {
                    wrappedLines.add(itemsEntry);
                }
                wrappedLines.add(SPACER);
                addParagraphGap();
            }
        }
        pageIndex = Math.min(pageIndex, maxPage());
    }

    private void padToPageBoundary() {
        while (wrappedLines.size() % LINES_PER_PAGE != 0) {
            wrappedLines.add(FormattedCharSequence.EMPTY);
        }
    }

    private void addParagraphGap() {
        if (wrappedLines.size() % LINES_PER_PAGE != 0) {
            wrappedLines.add(FormattedCharSequence.EMPTY);
        }
    }

    private int maxPage() {
        return Math.max(0, (wrappedLines.size() - 1) / LINES_PER_PAGE);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        drawPanel(g, panelLeft, panelTop, PANEL_WIDTH, PANEL_HEIGHT);

        // full-width brass title plate; the book title lives here and never collides
        // with the per-chapter heading below
        drawHeaderBar(g, panelLeft + 4, panelTop + 4, PANEL_WIDTH - 8);
        g.drawString(font, title, panelLeft + 12, panelTop + 8, LEATHER_EDGE, false);

        // sidebar divider: brass seam
        int dividerTop = panelTop + 4 + HEADER_HEIGHT + 2;
        g.fill(panelLeft + SIDEBAR_WIDTH, dividerTop,
            panelLeft + SIDEBAR_WIDTH + 1, panelTop + PANEL_HEIGHT - 6, BRASS_LO);
        g.fill(panelLeft + SIDEBAR_WIDTH + 1, dividerTop,
            panelLeft + SIDEBAR_WIDTH + 2, panelTop + PANEL_HEIGHT - 6, BRASS_HI);

        // chapter tabs: stamped leather, brass edge when selected
        for (int i = 0; i < HandbookContent.CHAPTERS.size(); i++) {
            int by = chapterButtonY(i);
            boolean selected = i == chapterIndex;
            boolean hovered = isOverChapterButton(mouseX, mouseY, i);
            int bg = selected ? LEATHER_HI : (hovered ? LEATHER_DARK : LEATHER_LO);
            g.fill(panelLeft + 8, by, panelLeft + SIDEBAR_WIDTH - 4, by + CHAPTER_BUTTON_HEIGHT - 2, bg);
            if (selected) {
                g.fill(panelLeft + 8, by, panelLeft + 10, by + CHAPTER_BUTTON_HEIGHT - 2, BRASS_HI);
            }
            String trimmed = font.plainSubstrByWidth(
                HandbookContent.CHAPTERS.get(i).title(), SIDEBAR_WIDTH - 17);
            g.drawString(font, trimmed, panelLeft + 13, by + 5,
                selected ? HEADING : TEXT_DIM, false);
        }

        // chapter heading + page body
        HandbookContent.Chapter chapter = HandbookContent.CHAPTERS.get(chapterIndex);
        int headingY = panelTop + 4 + HEADER_HEIGHT + 6;
        g.drawString(font, chapter.title(), contentX(), headingY, HEADING, false);
        g.fill(contentX(), headingY + 9,
            contentX() + font.width(chapter.title()), headingY + 10, BRASS);

        int start = pageIndex * LINES_PER_PAGE;
        for (int i = 0; i < LINES_PER_PAGE && start + i < wrappedLines.size(); i++) {
            Object line = wrappedLines.get(start + i);
            int lineY = contentY() + i * LINE_HEIGHT;
            if (line instanceof FormattedCharSequence seq) {
                g.drawString(font, seq, contentX(), lineY, PARCHMENT, false);
            } else if (line instanceof HandbookContent.ItemsEntry itemsEntry) {
                renderItemRow(g, itemsEntry, contentX(), lineY);
            }
        }

        // pager
        if (maxPage() > 0) {
            String pager = (pageIndex + 1) + " / " + (maxPage() + 1);
            g.drawCenteredString(font, pager,
                contentX() + contentWidth() / 2, panelTop + PANEL_HEIGHT - 14, TEXT_DIM);
            g.drawString(font, "<", contentX(), panelTop + PANEL_HEIGHT - 14,
                pageIndex > 0 ? BRASS_HI : BRASS_LO, false);
            g.drawString(font, ">", contentX() + contentWidth() - 6, panelTop + PANEL_HEIGHT - 14,
                pageIndex < maxPage() ? BRASS_HI : BRASS_LO, false);
        }
    }

    /** A riveted brass-framed leather panel. */
    private void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, LEATHER_LO);
        // subtle top-lit leather band
        g.fill(x + 3, y + 3, x + w - 3, y + h / 3, LEATHER_HI);
        // brass frame, three nested outlines
        g.renderOutline(x, y, w, h, BRASS_LO);
        g.renderOutline(x + 1, y + 1, w - 2, h - 2, BRASS);
        g.renderOutline(x + 2, y + 2, w - 4, h - 4, BRASS_HI);
        // corner rivets
        rivet(g, x + 5, y + 5);
        rivet(g, x + w - 6, y + 5);
        rivet(g, x + 5, y + h - 6);
        rivet(g, x + w - 6, y + h - 6);
    }

    private void drawHeaderBar(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + HEADER_HEIGHT - 2, BRASS_LO);
        g.fill(x, y, x + w, y + 1, BRASS_HI);
        g.fill(x, y + HEADER_HEIGHT - 3, x + w, y + HEADER_HEIGHT - 2, LEATHER_EDGE);
    }

    private void rivet(GuiGraphics g, int cx, int cy) {
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, BRASS_LO);
        g.fill(cx, cy, cx + 1, cy + 1, BRASS_HI);
    }

    /** An icon row: slot-styled backdrops, rendered items, a dim caption to the right. */
    private void renderItemRow(GuiGraphics g, HandbookContent.ItemsEntry entry, int x, int y) {
        int iconX = x;
        for (var stack : entry.items()) {
            g.fill(iconX - 1, y - 1, iconX + 17, y + 17, SLOT_BG);
            g.fill(iconX - 1, y - 1, iconX + 17, y, LEATHER_EDGE);
            g.fill(iconX - 1, y + 16, iconX + 17, y + 17, BRASS_LO);
            g.renderItem(stack, iconX, y);
            iconX += 20;
        }
        if (!entry.caption().isEmpty()) {
            int captionX = iconX + 4;
            String caption = font.plainSubstrByWidth(entry.caption(),
                contentX() + contentWidth() - captionX);
            g.drawString(font, caption, captionX, y + 4, TEXT_DIM, false);
        }
    }

    private int chapterButtonY(int index) {
        return panelTop + 4 + HEADER_HEIGHT + 6 + index * CHAPTER_BUTTON_HEIGHT;
    }

    private boolean isOverChapterButton(double mouseX, double mouseY, int index) {
        int by = chapterButtonY(index);
        return mouseX >= panelLeft + 4 && mouseX < panelLeft + SIDEBAR_WIDTH - 4
            && mouseY >= by && mouseY < by + CHAPTER_BUTTON_HEIGHT - 2;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        for (int i = 0; i < HandbookContent.CHAPTERS.size(); i++) {
            if (isOverChapterButton(mouseX, mouseY, i)) {
                if (i != chapterIndex) {
                    chapterIndex = i;
                    pageIndex = 0;
                    rebuildLines();
                }
                return true;
            }
        }
        int pagerY = panelTop + PANEL_HEIGHT - 14;
        if (mouseY >= pagerY - 2 && mouseY < pagerY + 10) {
            if (mouseX >= contentX() - 2 && mouseX < contentX() + 10 && pageIndex > 0) {
                pageIndex--;
                return true;
            }
            if (mouseX >= contentX() + contentWidth() - 10 && mouseX < contentX() + contentWidth() + 2
                    && pageIndex < maxPage()) {
                pageIndex++;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY < 0 && pageIndex < maxPage()) {
            pageIndex++;
            return true;
        }
        if (scrollY > 0 && pageIndex > 0) {
            pageIndex--;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Jump to a chapter by index. Used by the dev screenshot harness; harmless otherwise. */
    public void devSelectChapter(int i) {
        if (i >= 0 && i < HandbookContent.CHAPTERS.size()) {
            chapterIndex = i;
            pageIndex = 0;
            rebuildLines();
        }
    }
}
