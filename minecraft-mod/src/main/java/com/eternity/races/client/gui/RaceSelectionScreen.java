package com.eternity.races.client.gui;

import com.eternity.races.common.network.SelectRacePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * GUI выбора расы (первый вход).
 * Показывает сетку 5×3 из 15 рас. Клик → панель описания → кнопка ВЫБРАТЬ.
 */
public class RaceSelectionScreen extends Screen {

    private static final int CELL_W = 80;
    private static final int CELL_H = 40;
    private static final int COLS = 5;
    private static final int ROWS = 3;
    private static final int PADDING = 8;

    private int selectedRace = -1;
    private Button confirmButton;

    // Названия, описания, пассивы, штрафы
    private static final String[] NAMES = {
        "Эхо-Голем", "Мнемо-Инженер", "Химера-Симбионт",
        "Ткач Измерений", "Кинетик-Пиромант", "Лунный Жнец",
        "Грав-Ткач", "Энтроп. Разрушитель", "Теневой Дипломат",
        "Алхимик-Трансмутатор", "Садовник-Симбиот", "Странник-Картограф",
        "Редстоун-Инженер", "Меркантильный Маг", "Глубинный Гном"
    };

    private static final String[] PASSIVES = {
        "Голод 1.5× при беге; вибрация",
        "Временная перемотка; 3 точки сохранения",
        "Реген 1♥/5сек вне боя; одержимость",
        "Пространственные порталы; якоря",
        "Иммунитет к огню; огненный след",
        "Ночь: 20♥ +30% скорость; волки",
        "Гравит. манипуляции; левитация",
        "Мгновенный распад блоков",
        "Невидимость без брони и атаки",
        "Трансмутация блоков; алхимия",
        "Реген на солнце; рост урожая",
        "Координаты; автокарта; бег без голода",
        "Видит сигнал; Ускорение I",
        "Скидка 30% у жителей",
        "Ночное зрение; руды сквозь стены"
    };

    private static final String[] PENALTIES = {
        "30% слабее на шерсти; нотные блоки = урон",
        "Инвентарь перемешивается каждые 2 мин",
        "Без брони/оружия; голод<3 = урон/мин",
        "10% ошибка телепорта; Незер 50% шанс",
        "Стоять >3сек = урон; вода = урон",
        "День: 12♥ -20% скорость; нужно убивать",
        "Двойной урон от падения; утопание 2×",
        "Нельзя ставить блоки; 18 слотов",
        "Атака = все мобы агрессивны 1 мин",
        "Нельзя крафтить; 10% взрыв",
        "Вода = урон; только дерево/золото",
        "Спавн = мировой; карта 500×500",
        "5% шанс возгорания; молния >30сек",
        "Нельзя добыть алмазы/изумруды",
        "Выше Y=60: урон + замедление"
    };

    private static final int[] RACE_COLORS = {
        0xFF5588AA, 0xFF88AA55, 0xFFAA5555, 0xFF8855AA, 0xFFCC6600,
        0xFF334466, 0xFF224422, 0xFF882222, 0xFF224444, 0xFFBB8800,
        0xFF338833, 0xFF224499, 0xFFCC3300, 0xFFCC9900, 0xFF556677
    };

    public RaceSelectionScreen() {
        super(Component.literal("§6Выбор расы — EternityRaces"));
    }

    @Override
    protected void init() {
        int gridW = COLS * CELL_W + (COLS - 1) * PADDING;
        int startX = (this.width - gridW) / 2;
        int startY = 40;

        for (int i = 0; i < 15; i++) {
            final int raceIdx = i;
            final int row = i / COLS;
            final int col = i % COLS;
            int x = startX + col * (CELL_W + PADDING);
            int y = startY + row * (CELL_H + PADDING);

            this.addRenderableWidget(Button.builder(
                    Component.literal("§e" + NAMES[i]),
                    btn -> selectRace(raceIdx + 1)
            ).bounds(x, y, CELL_W, CELL_H).build());
        }

        // Кнопка подтверждения
        confirmButton = Button.builder(
                Component.literal("§a[ВЫБРАТЬ ЭТУ РАСУ]"),
                btn -> confirmSelection()
        ).bounds(this.width / 2 - 80, this.height - 50, 160, 20).build();
        confirmButton.active = false;
        this.addRenderableWidget(confirmButton);

        // Кнопка отмены
        this.addRenderableWidget(Button.builder(
                Component.literal("§cОтмена"),
                btn -> this.onClose()
        ).bounds(this.width / 2 - 80, this.height - 26, 160, 20).build());
    }

    private void selectRace(int raceId) {
        this.selectedRace = raceId;
        confirmButton.active = true;
    }

    private void confirmSelection() {
        if (selectedRace < 1 || selectedRace > 15) return;
        // Отправляем пакет выбора расы на сервер
        PacketDistributor.sendToServer(new SelectRacePacket(selectedRace));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Затемнённый фон
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        // Заголовок
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFAA00);
        graphics.drawCenteredString(this.font,
                Component.literal("§7Выбери свою расу — выбор необратим!"),
                this.width / 2, 24, 0xFFAAAAAA);

        // Панель описания выбранной расы
        if (selectedRace > 0) {
            int idx = selectedRace - 1;
            int panelX = 10;
            int panelY = this.height - 120;
            int panelW = this.width - 20;

            graphics.fill(panelX, panelY, panelX + panelW, panelY + 90, 0xCC000000);
            graphics.drawString(this.font, "§6" + NAMES[idx], panelX + 6, panelY + 5, 0xFFFFFFFF);
            graphics.drawString(this.font, "§aПассив: §7" + PASSIVES[idx], panelX + 6, panelY + 20, 0xFFFFFFFF);
            graphics.drawString(this.font, "§cШтраф: §7" + PENALTIES[idx], panelX + 6, panelY + 35, 0xFFFFFFFF);
            graphics.drawString(this.font, "§eНажми §a[ВЫБРАТЬ ЭТУ РАСУ]§e для подтверждения.",
                    panelX + 6, panelY + 55, 0xFFFFFFFF);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
