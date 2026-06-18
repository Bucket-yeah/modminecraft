---
name: NeoForge 1.21.1 Registration patterns
description: Правильные паттерны регистрации компонентов в NeoForge 1.21.1 (entities, attributes, reload listeners)
---

## Правило

В NeoForge 1.21.1 разные компоненты регистрируются на разных шинах событий.

**Why:** Путаница между `modEventBus` и `NeoForge.EVENT_BUS` приводит к тому, что регистрация не происходит или вызывается в неверное время.

## Паттерны

### Атрибуты EntityType
```java
// modEventBus — вызывается при инициализации мода
modEventBus.addListener(this::registerEntityAttributes);

private void registerEntityAttributes(EntityAttributeCreationEvent event) {
    event.put(ModEntities.MY_MOB.get(), MyMob.createAttributes().build());
}
```

### Resource Reload Listener (ProgressionLoader и т.п.)
```java
// NeoForge.EVENT_BUS — вызывается при каждом /reload и старте мира
NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);

private void onAddReloadListeners(AddReloadListenerEvent event) {
    event.addListener(ProgressionLoader.INSTANCE);
}
```

### Регистрация KeyBindings (client-only)
```java
// modEventBus, только на клиенте
modEventBus.addListener(ClientSetup::onRegisterKeyMappings);
// Метод: @SubscribeEvent public static void onRegisterKeyMappings(RegisterKeyMappingsEvent e)
```

### neoforge.mods.toml порядок
`[[mods]]` секция ОБЯЗАТЕЛЬНО перед `[[dependencies.*]]` секциями.

**How to apply:** При добавлении новых mob-типов или resource-загрузчиков следовать этим паттернам.
