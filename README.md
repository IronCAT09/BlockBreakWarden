# BlockBreakWarden

[English](#english) · [Русский](#русский)

A client-side **Minecraft (Fabric)** mod that restricts which blocks the player is
allowed to break and shows a warning when you try to break a forbidden block.

---

## English

### Features

- **Three modes** (cycled in the GUI and with a hotkey):
  - `Off` — the mod does nothing;
  - `Whitelist` — you may break **only** the blocks/tags in the whitelist;
  - `Blacklist` — you may **not** break the blocks/tags in the blacklist.
- **Whitelist and blacklist are independent lists**, edited separately in the GUI
  (switch which one you are editing with a dedicated button).
- **Each list** holds two kinds of entries:
  - a block ID: `minecraft:oak_log`;
  - a block tag (with `#`): `#minecraft:logs`.
- **Hotkeys** (rebindable in *Options → Controls → Key Binds → BlockBreakWarden*):
  - `K` — cycle through the modes;
  - `N` — add the block you are looking at (by block ID) to the **current mode's**
    list (whitelist or blacklist); pressing it again on an already-added block
    **removes** it. In `Off` mode it does nothing and tells you to pick a mode first.
- **GUI** (via **Mod Menu**, or opened directly):
  - change the mode;
  - toggle warnings and sound;
  - add an entry by ID or tag;
  - remove a single entry (the `X` button);
  - clear the whole list;
  - pagination for long lists.
- The warning is shown on the line above the hotbar plus an optional sound; it does not
  spam while you hold down the left mouse button.

### Settings storage

All settings (mode, the warning/sound toggles, and the block/tag list) are stored in an
external JSON file `config/blockbreakwarden.json` in the game folder. The file is created
automatically, re-read on startup, and can be edited by hand.

> The mod is **client-side**: the restriction works through `AttackBlockCallback` on the
> client and applies on any server, but it only limits **you** (it does not protect blocks
> from other players).

## Русский

Клиентский мод для **Minecraft (Fabric)**, который ограничивает, какие блоки
игрок может ломать, и показывает предупреждение при попытке сломать запрещённый блок.

### Возможности

- **Три режима** (переключаются в GUI и горячей клавишей по кругу):
  - `Выключен` — мод не вмешивается;
  - `Whitelist` — ломать можно **только** блоки/тэги из белого списка;
  - `Blacklist` — ломать **нельзя** блоки/тэги из чёрного списка.
- **Whitelist и blacklist — независимые списки**, редактируются раздельно в GUI
  (отдельная кнопка переключает, какой из них вы сейчас редактируете).
- **Каждый список** хранит записи двух видов:
  - ID блока: `minecraft:oak_log`;
  - тэг блока (с `#`): `#minecraft:logs`.
- **Горячие клавиши** (настраиваются в *Управление → Клавиши → BlockBreakWarden*):
  - `K` — переключить режим по кругу;
  - `N` — добавить блок, на который смотрит игрок (по ID блока), в список
    **текущего режима** (whitelist или blacklist); повторное нажатие на уже
    добавленный блок **убирает** его. В режиме `Выключен` действие недоступно —
    мод попросит сначала выбрать режим.
- **GUI** (через **Mod Menu** или открывается напрямую):
  - смена режима;
  - вкл/выкл предупреждений и звука;
  - добавление записи по ID или тэгу;
  - удаление отдельной записи (кнопка `X`);
  - полная очистка списка;
  - пагинация длинного списка.
- Предупреждение выводится в строке над хотбаром + опциональный звук; не спамит при удержании ЛКМ.

### Хранение настроек

Все настройки (режим, переключатели предупреждений/звука и список блоков/тэгов)
сохраняются во внешнем JSON-файле `config/blockbreakwarden.json` в папке игры.
Файл создаётся автоматически, перечитывается при запуске и его можно править вручную.

> Мод **клиентский**: ограничение работает через `AttackBlockCallback` на стороне клиента
> и действует на любом сервере, но ограничивает только вас (не защищает блоки от других игроков).
