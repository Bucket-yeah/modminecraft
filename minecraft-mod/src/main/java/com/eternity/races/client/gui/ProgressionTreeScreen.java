package com.eternity.races.client.gui;

import com.eternity.races.client.gui.widgets.AbilityNodeWidget;
import com.eternity.races.common.ability.AbilityManager;
import com.eternity.races.common.ability.AbstractAbility;
import com.eternity.races.common.cap.RaceData;
import com.eternity.races.common.network.ResetPointsPacket;
import com.eternity.races.common.network.UnlockAbilityPacket;
import com.eternity.races.common.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран дерева способностей.
 * Открывается клавишей G или кнопкой в инвентаре.
 *
 * Структура дерева (3 уровня):
 *   [Abil 0] [Abil 1] [Abil 2]        ← Tier 1 (бесплатно)
 *        [Ветка A]  [Ветка B]         ← выбор ветки
 *   [Ab 3] [Ab 4]  [Ab 5] [Ab 6]      ← Tier 2 (20 очков/способность)
 *              [Ability 7]             ← Tier 3 (30 очков)
 */
public class ProgressionTreeScreen extends Screen {

    private final List<AbilityNodeWidget> nodes = new ArrayList<>();
    private RaceData raceData;
    private int raceId;

    // Имена рас для заголовка
    private static final String[] RACE_NAMES = {
        "", "Эхо-Голем", "Мнемо-Инженер", "Химера-Симбионт",
        "Ткач Измерений", "Кинетик-Пиромант", "Лунный Жнец",
        "Грав-Ткач", "Энтроп. Разрушитель", "Теневой Дипломат",
        "Алхимик-Трансмутатор", "Садовник-Симбиот", "Странник-Картограф",
        "Редстоун-Инженер", "Меркантильный Маг", "Глубинный Гном"
    };

    public ProgressionTreeScreen() {
        super(Component.literal("§6Дерево способностей"));
    }

    @Override
    protected void init() {
        var player = Minecraft.getInstance().player;
        if (player == null) { this.onClose(); return; }

        raceData = player.getData(ModAttachments.RACE_DATA.get());
        raceId = raceData.getRaceId();

        if (!raceData.hasChosen()) { this.onClose(); return; }

        buildNodeWidgets();

        // Кнопка сброса очков
        this.addRenderableWidget(Button.builder(
                Component.literal("§cСбросить очки"),
                btn -> onResetPoints()
        ).bounds(this.width - 130, this.height - 30, 120, 20).build());

        // Кнопка закрытия
        this.addRenderableWidget(Button.builder(
                Component.literal("§7Закрыть"),
                btn -> this.onClose()
        ).bounds(10, this.height - 30, 80, 20).build());
    }

    private void buildNodeWidgets() {
        nodes.clear();
        List<AbstractAbility> abilities = AbilityManager.getAbilitiesForRace(raceId);
        if (abilities.isEmpty()) return;

        int cx = this.width / 2;
        int nodeW = 90, nodeH = 28;

        // Tier 1 (0, 1, 2) — три в ряд
        int t1Y = 60;
        int[] t1X = { cx - 120, cx, cx + 120 };
        for (int i = 0; i < 3 && i < abilities.size(); i++) {
            AbstractAbility a = abilities.get(i);
            boolean unlocked = raceData.isAbilityUnlocked(i);
            boolean purchasable = !unlocked;
            AbilityNodeWidget w = new AbilityNodeWidget(
                    t1X[i] - nodeW / 2, t1Y, nodeW, nodeH,
                    a, i, unlocked, purchasable, this::onNodeClick);
            nodes.add(w);
            this.addRenderableWidget(w);
        }

        // Tier 2 — ветки A (3,4) и B (5,6)
        int t2Y = 120;
        int[] t2X = { cx - 150, cx - 60, cx + 60, cx + 150 };
        int[] t2Idx = { 3, 4, 5, 6 };
        for (int j = 0; j < 4 && j + 3 < abilities.size(); j++) {
            int i = t2Idx[j];
            AbstractAbility a = abilities.get(i);
            boolean unlocked = raceData.isAbilityUnlocked(i);
            int myBranch = (j < 2) ? 0 : 1;
            boolean wrongBranch = raceData.getSelectedBranch() != -1 && raceData.getSelectedBranch() != myBranch;
            boolean purchasable = !unlocked && !wrongBranch && raceData.getRacePoints() >= 20;
            AbilityNodeWidget w = new AbilityNodeWidget(
                    t2X[j] - nodeW / 2, t2Y, nodeW, nodeH,
                    a, i, unlocked, purchasable, this::onNodeClick);
            nodes.add(w);
            this.addRenderableWidget(w);
        }

        // Tier 3 (индекс 7) — один по центру
        if (abilities.size() >= 8) {
            int t3Y = 185;
            AbstractAbility a = abilities.get(7);
            boolean unlocked = raceData.isAbilityUnlocked(7);
            int totalSpent = 0;
            for (int sp : raceData.getSpentPoints()) totalSpent += sp;
            boolean purchasable = !unlocked && totalSpent >= 100 && raceData.getRacePoints() >= 30;
            AbilityNodeWidget w = new AbilityNodeWidget(
                    cx - nodeW / 2, t3Y, nodeW, nodeH,
                    a, 7, unlocked, purchasable, this::onNodeClick);
            nodes.add(w);
            this.addRenderableWidget(w);
        }
    }

    private void onNodeClick(int abilityIndex) {
        int tier = AbilityManager.getTierForIndex(raceId, abilityIndex);
        int branch = AbilityManager.getBranchForIndex(raceId, abilityIndex);
        PacketDistributor.sendToServer(new UnlockAbilityPacket(abilityIndex, branch));
        // Перестраиваем виджеты после отправки пакета
        this.clearWidgets();
        this.init();
    }

    private void onResetPoints() {
        PacketDistributor.sendToServer(new ResetPointsPacket());
        this.clearWidgets();
        this.init();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g, mx, my, pt);

        // Заголовок
        String raceName = (raceId >= 1 && raceId <= 15) ? RACE_NAMES[raceId] : "???";
        g.drawCenteredString(this.font,
                Component.literal("§6Дерево способностей — " + raceName), this.width / 2, 12, 0xFFFFFFFF);

        // Очки
        if (raceData != null) {
            g.drawString(this.font, "§6Очки рас: §e" + raceData.getRacePoints(), 10, 12, 0xFFFFFFFF);
        }

        // Метки уровней
        g.drawCenteredString(this.font, Component.literal("§7— Tier 1 (бесплатно) —"), this.width / 2, 48, 0xFFAAAAFF);
        g.drawCenteredString(this.font, Component.literal("§7— Tier 2 (Ветка A / Ветка B) —"), this.width / 2, 108, 0xFFAAAAFF);
        g.drawCenteredString(this.font, Component.literal("§7— Tier 3 (100+ потрачено) —"), this.width / 2, 173, 0xFFAAAAFF);

        // Линии-связи между узлами
        drawConnectionLines(g);

        // Виджеты
        super.render(g, mx, my, pt);
    }

    private void drawConnectionLines(GuiGraphics g) {
        // Tier1 → Tier2 Branch A
        if (nodes.size() >= 7) {
            drawLine(g, nodes.get(0).getCenterX(), nodes.get(0).getBottom(),
                    nodes.get(3).getCenterX(), nodes.get(3).getY(), 0xFF555577);
            drawLine(g, nodes.get(1).getCenterX(), nodes.get(1).getBottom(),
                    nodes.get(4).getCenterX(), nodes.get(4).getY(), 0xFF555577);
            // Tier1 → Tier2 Branch B
            drawLine(g, nodes.get(2).getCenterX(), nodes.get(2).getBottom(),
                    nodes.get(5).getCenterX(), nodes.get(5).getY(), 0xFF557755);
            drawLine(g, nodes.get(2).getCenterX(), nodes.get(2).getBottom(),
                    nodes.get(6).getCenterX(), nodes.get(6).getY(), 0xFF557755);
        }
        // Tier2 → Tier3
        if (nodes.size() >= 8) {
            for (int i = 3; i <= 6; i++) {
                drawLine(g, nodes.get(i).getCenterX(), nodes.get(i).getBottom(),
                        nodes.get(7).getCenterX(), nodes.get(7).getY(), 0xFF777700);
            }
        }
    }

    private void drawLine(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(Math.min(x1, x2), Math.min(y1, y2),
                Math.max(x1, x2) + 1, Math.max(y1, y2) + 1, color);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
