package com.eternity.races.client.gui.widgets;

import com.eternity.races.common.ability.AbstractAbility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

/**
 * Кликабельный виджет одной способности в дереве прогрессии.
 * Показывает название, стоимость, статус (✓ разблокировано / + покупаемо / X заблокировано).
 */
public class AbilityNodeWidget extends AbstractWidget {

    private final AbstractAbility ability;
    private final int abilityIndex;
    private final boolean unlocked;
    private final boolean purchasable;
    private final IntConsumer onClick;

    // Цвета состояний
    private static final int COLOR_UNLOCKED   = 0xFF22AA22;
    private static final int COLOR_PURCHASABLE = 0xFFAAAA22;
    private static final int COLOR_LOCKED     = 0xFF555555;
    private static final int COLOR_BORDER_UNLOCKED   = 0xFF44FF44;
    private static final int COLOR_BORDER_PURCHASABLE = 0xFFFFFF44;
    private static final int COLOR_BORDER_LOCKED     = 0xFF888888;

    public AbilityNodeWidget(int x, int y, int width, int height,
                              AbstractAbility ability, int abilityIndex,
                              boolean unlocked, boolean purchasable,
                              IntConsumer onClick) {
        super(x, y, width, height, Component.literal(ability.nameKey));
        this.ability = ability;
        this.abilityIndex = abilityIndex;
        this.unlocked = unlocked;
        this.purchasable = purchasable;
        this.onClick = onClick;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
        int bgColor  = unlocked ? COLOR_UNLOCKED : (purchasable ? COLOR_PURCHASABLE : COLOR_LOCKED);
        int bdrColor = unlocked ? COLOR_BORDER_UNLOCKED : (purchasable ? COLOR_BORDER_PURCHASABLE : COLOR_BORDER_LOCKED);

        // Фон и рамка
        g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bgColor);
        g.renderOutline(getX(), getY(), getWidth(), getHeight(), bdrColor);

        // Статус-иконка
        String status = unlocked ? "§a✓ " : (purchasable ? "§e+ " : "§c✗ ");

        // Название (сокращённое)
        String name = ability.nameKey;
        // Берём часть после последней точки (короткое имя)
        int dot = name.lastIndexOf('.');
        String shortName = (dot >= 0 && dot < name.length() - 1) ? name.substring(dot + 1) : name;
        if (shortName.length() > 9) shortName = shortName.substring(0, 9) + "…";

        var font = Minecraft.getInstance().font;
        g.drawString(font, status + shortName, getX() + 3, getY() + 4, 0xFFFFFFFF, false);

        // Стоимость
        if (!unlocked) {
            String costStr = "§6" + ability.unlockCost + "оч";
            g.drawString(font, costStr, getX() + 3, getY() + 14, 0xFFFFFFFF, false);
        }

        // Подсветка при наведении
        if (isHovered()) {
            g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x33FFFFFF);
            // Тултип с полным описанием
            java.util.List<net.minecraft.util.FormattedCharSequence> tooltip = java.util.List.of(
                    Component.literal("§e" + shortName),
                    Component.literal("§7Стоимость: §6" + ability.unlockCost + " очков"),
                    Component.literal(unlocked ? "§aРазблокировано" :
                            (purchasable ? "§eДоступно для покупки" : "§cЗаблокировано"))
            ).stream().map(net.minecraft.network.chat.Component::getVisualOrderText)
                    .collect(java.util.stream.Collectors.toList());
            g.renderTooltip(font, tooltip, mx, my);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (purchasable && !unlocked) {
            onClick.accept(abilityIndex);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    public int getCenterX() { return getX() + getWidth() / 2; }
    public int getBottom()  { return getY() + getHeight(); }
}
