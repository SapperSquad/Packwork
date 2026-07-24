package com.sappersquad.packwork.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * An {@link EditBox} that draws its text CRISP - with no drop shadow - so the dark
 * search text on the pale canvas strip reads sharp at every GUI scale. Vanilla EditBox
 * always draws text via the shadowed {@code drawString} overload, and a dark glyph plus
 * its near-black shadow on a light background muddies into the "fuzzy" look SapperSquad saw.
 * Only the on-screen text draw is replaced; all editing behaviour is inherited.
 */
public class CrispEditBox extends EditBox {

    private final Font font;
    private final int textColor;
    private Component hintText;

    public CrispEditBox(Font font, int x, int y, int w, int h, Component msg, int textColor) {
        super(font, x, y, w, h, msg);
        this.font = font;
        this.textColor = textColor;
        setBordered(false);
        setTextColor(textColor);
    }

    public void setHintText(Component c) {
        this.hintText = c;
        setHint(c);
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!this.isVisible()) return;
        int x = getX();
        int y = getY();
        String value = getValue();

        if (value.isEmpty() && hintText != null && !isFocused()) {
            // dim hint, still crisp (no shadow)
            g.drawString(font, hintText, x, y, 0xFF8A7658, false);
            return;
        }
        // trim from the front so the caret end stays visible instead of spilling off the strip
        String shown = value;
        while (!shown.isEmpty() && font.width(shown) > getWidth() - 1) {
            shown = shown.substring(1);
        }
        g.drawString(font, shown, x, y, textColor, false); // dropShadow = false -> crisp
        if (isFocused() && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cx = x + font.width(shown);
            g.fill(cx, y - 1, cx + 1, y + font.lineHeight, 0xFF000000 | (textColor & 0xFFFFFF));
        }
    }
}
