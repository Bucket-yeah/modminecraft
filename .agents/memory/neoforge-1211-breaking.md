---
name: NeoForge 1.21.1 Breaking Changes
description: Все breaking API changes, с которыми столкнулись при сборке racecraft мода — полезно для любого мода на 1.21.1
---

## KeyMapping constructor
`KeyMapping(String, IKeyConflictContext, int, String)` → второй аргумент-клавиша теперь `InputConstants.Key`, не `int`.
Fix: `InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_X)`.

## GuiGraphics.renderTooltip
`renderTooltip(Font, List<MutableComponent>, int, int)` удалён.
Fix: конвертировать через `.stream().map(Component::getVisualOrderText).collect(...)` в `List<FormattedCharSequence>`.

## SmallFireball constructor
`SmallFireball(Level, LivingEntity, double, double, double)` удалён.
Fix: `new SmallFireball(level, owner, look.scale(power))` — Vec3 вместо трёх double.

## Entity identity comparison in lambdas
`e != player` где `e: Mob` и `player: Player` — Java отклоняет как "incomparable types".
Fix: `!player.is(e)` (метод Entity.is() сравнивает по UUID).

## Curios 9.x ICurioItem API
Все три ключевых метода получили доп. параметр `ItemStack currentStack`:
- `onEquip(SlotContext, ItemStack prevStack, ItemStack currentStack)`
- `onUnequip(SlotContext, ItemStack newStack, ItemStack currentStack)`
- `canEquip(SlotContext, ItemStack currentStack)`

## SmartBrainLib 1.16.11 FollowOwner
`FollowOwner<E extends TamableAnimal>` — работает ТОЛЬКО с TamableAnimal.
Fix: использовать `FollowEntity<E, LivingEntity>().following(e -> e.getOwner())`.
Требует чтобы класс наследовал `PathfinderMob`, не просто `Mob`.

## CombatTracker.inCombat() удалён в 1.21.1
Fix: `p.getLastHurtByMob() != null && (p.tickCount - p.getLastHurtByMobTimestamp()) <= 100`

## ItemEntity.addEffect() не существует
`ItemEntity` — не `LivingEntity`, у него нет `addEffect`.
Fix: `itemEntity.setGlowingTag(true)` для подсветки.

**Why:** Все эти изменения произошли между 1.20.x и 1.21.1 в рамках рефакторинга Mojang/NeoForge.
