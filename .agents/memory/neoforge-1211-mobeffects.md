---
name: NeoForge 1.21.1 MobEffects API
description: Эффекты, которые переименованы или отсутствуют в Minecraft 1.21.1 / NeoForge 21.1.x
---

## Правило

Перед использованием `MobEffects.*` проверять, существует ли имя в 1.21.1.

**Why:** Несколько эффектов были переименованы или удалены, что приводит к ошибкам компиляции.

## Известные замены

| Неверное имя         | Верное имя в 1.21.1                      |
|----------------------|------------------------------------------|
| `STRENGTH`           | `DAMAGE_BOOST`                           |
| `SILENCE`            | Не существует — заменить на `WEAKNESS` + `MOVEMENT_SLOWDOWN` |
| `THORNS`             | Не существует — заменить флагом `PersistentData` |

## Рабочие имена в 1.21.1

`DAMAGE_RESISTANCE`, `GLOWING`, `INVISIBILITY`, `BLINDNESS`, `WEAKNESS`,
`MOVEMENT_SLOWDOWN`, `REGENERATION`, `DIG_SPEED`, `NIGHT_VISION`, `DAMAGE_BOOST`,
`WITHER`, `LEVITATION`, `CONFUSION` (=Тошнота), `SLOW_FALLING`, `ABSORPTION`,
`FIRE_RESISTANCE`, `MOVEMENT_SPEED`, `DARKNESS`, `LUCK`, `HUNGER`, `POISON`, `DIG_SLOWDOWN`

**How to apply:** При написании любых способностей, проверить имя эффекта по таблице выше.
