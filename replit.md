# EternityRaces (racecraft)

NeoForge 1.21.1 Minecraft мод с 15 игровыми расами, каждая имеет 8 активных способностей, пассивные черты, деревья прокачки и штрафы. Весь внутриигровой текст на русском языке.

## Run & Operate

- `pnpm --filter @workspace/api-server run dev` — запуск API сервера (порт 5000)
- `pnpm run typecheck` — проверка типов по всем пакетам
- `pnpm run build` — typecheck + сборка всех пакетов
- `pnpm --filter @workspace/api-spec run codegen` — регенерация API хуков и Zod схем из OpenAPI
- `pnpm --filter @workspace/db run push` — применить изменения схемы БД (только dev)
- Required env: `DATABASE_URL` — строка подключения Postgres

## Minecraft Mod Build

Мод находится в `minecraft-mod/`. Для сборки нужен Gradle (входит в gradlew):

```bash
cd minecraft-mod
./gradlew build
# Артефакт: build/libs/racecraft-1.0.0.jar
```

Требования для сборки: Java 21 JDK, подключение к интернету (для скачивания зависимостей NeoForge).

## Stack

- pnpm workspaces, Node.js 24, TypeScript 5.9
- API: Express 5
- DB: PostgreSQL + Drizzle ORM
- Validation: Zod (`zod/v4`), `drizzle-zod`
- API codegen: Orval (from OpenAPI spec)
- Build: esbuild (CJS bundle)

### Minecraft Mod Stack

- NeoForge 21.1.172 (MC 1.21.1)
- Curios 7.1.3+1.21.1 (слоты экипировки)
- GeckoLib 4.6 (анимации мобов)
- SmartBrainLib 1.13 (AI призванных мобов)
- YACL 3.5.0+1.21.1-neoforge (конфигурация)

## Where things live

- `minecraft-mod/src/main/java/com/eternity/races/` — весь исходный код мода
  - `RacesMod.java` — точка входа, регистрация всех событий и компонентов
  - `common/ability/abilities/` — 15 файлов с 8 способностями каждой расы
  - `common/handler/RaceEventHandler.java` — все пассивные черты, штрафы, прогрессия
  - `common/progression/` — система деревьев прокачки (JSON-конфиги + загрузчик)
  - `common/mob/` — 3 класса призванных мобов (волк, паук, слизень)
  - `client/gui/` — экраны выбора расы и дерева способностей
  - `common/network/` — 5 сетевых пакетов (SelectRace, Ability, Unlock, Reset, Sync)
- `minecraft-mod/src/main/resources/data/racecraft/trees/` — 15 JSON деревьев прокачки
- `minecraft-mod/src/main/resources/assets/racecraft/lang/ru_ru.json` — локализация (120+ ключей)

## Architecture decisions

- **Progression через JSON**: деревья прокачки грузятся как ResourceReloadListener — позволяет менять баланс без перекомпиляции.
- **AttachmentType для RaceData**: NeoForge 1.21.1 `AttachmentType` вместо устаревших Capabilities. Данные сохраняются в NBT игрока.
- **AbilityManager как реестр**: все способности регистрируются по паре (raceId, abilityIndex), позволяя находить их по O(1).
- **SummonedMob + SmartBrainLib**: призванные существа следуют за хозяином и атакуют его врагов через SmartBrain AI.
- **PacketDistributor**: сеть использует NeoForge 1.21.1 API (`PacketDistributor.sendToPlayer`, `sendToServer`).

## Product

Игрок выбирает расу через книгу выбора расы (`/give @s racecraft:race_selection_book`). Каждая раса даёт:
- 8 активных способностей (привязаны к клавишам R/G/H/J/Y/U/I/O)
- Уникальные пассивные черты (реген HP, иммунитеты, бонусы к крафту)
- Дерево прокачки (30 очков, ветки A/B, Tier 3 финальная способность)
- Штрафы (дебаффы, зависящие от расы)

### 15 рас:
1. Эхо-Голем, 2. Алхимик-Трансмутор, 3. Химера-Симбионт, 4. Измерительный Ткач,
5. Гравитационный Ткач, 6. Лунный Жнец, 7. Торговый Маг, 8. Мнемо-Инженер,
9. Кинетический Пиромант, 10. Рэдстоун-Инженер, 11. Тень-Дипломат,
12. Странник-Картограф, 13. Садовник-Симбионт, 14. Энтропийный Разрушитель, 15. Глубинный Гном

## User preferences

_Populate as you build — explicit user instructions worth remembering across sessions._

## Gotchas

- `MobEffects.SILENCE` не существует в 1.21.1 — заменён на WEAKNESS+MOVEMENT_SLOWDOWN
- `MobEffects.THORNS` не существует в 1.21.1 — заменён флагом персистентных данных `nature_armor_expire`
- `MobEffects.STRENGTH` → `MobEffects.DAMAGE_BOOST` в 1.21.1
- `neoforge.mods.toml`: секция `[[mods]]` должна быть ДО `[[dependencies.*]]`
- Атрибуты EntityType регистрируются через `EntityAttributeCreationEvent` на `modEventBus`
- ProgressionLoader регистрируется через `AddReloadListenerEvent` на `NeoForge.EVENT_BUS`

## Pointers

- See the `pnpm-workspace` skill for workspace structure, TypeScript setup, and package details
- Полная техническая спецификация: `attached_assets/Pasted--COMPREHENSIVE-TECHNICAL-SPECIFICATION-Minecraft-Mod-15_1781807068770.txt`
