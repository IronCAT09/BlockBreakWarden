package com.ironcat.blockbreakwarden.gui;

import com.ironcat.blockbreakwarden.BreakRules;
import com.ironcat.blockbreakwarden.BlockBreakWardenConfig;
import com.ironcat.blockbreakwarden.Mode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран настроек BlockBreakWarden: смена режима, переключатели предупреждений/звука
 * и редактирование списка блоков/тэгов (добавить, удалить, очистить).
 */
public class BlockBreakWardenConfigScreen extends Screen {

    private static final int ENTRIES_PER_PAGE = 6;
    private static final int ROW_HEIGHT = 20;

    private final Screen parent;
    private final BlockBreakWardenConfig config;

    private int page = 0;
    private String pendingInput = "";

    /** Какой список сейчас редактируется в меню: whitelist или blacklist. */
    private Mode editing = Mode.WHITELIST;

    private TextFieldWidget inputField;

    // позиции, нужные для отрисовки подписей поверх виджетов
    private final List<String> shownEntries = new ArrayList<>();
    private int listTopY;
    private int navY;
    private int inputY;
    private long invalidUntil = 0L;

    public BlockBreakWardenConfigScreen(Screen parent) {
        super(Text.translatable("title.blockbreakwarden.config"));
        this.parent = parent;
        this.config = BlockBreakWardenConfig.get();
    }

    @Override
    protected void init() {
        shownEntries.clear();
        int cx = this.width / 2;
        int y = 30;

        // --- режим ---
        addDrawableChild(ButtonWidget.builder(modeText(), button -> {
            config.mode = config.mode.next();
            button.setMessage(modeText());
        }).dimensions(cx - 120, y, 240, 20).build());
        y += 24;

        // --- переключатели ---
        addDrawableChild(ButtonWidget.builder(warnText(), button -> {
            config.showWarnings = !config.showWarnings;
            button.setMessage(warnText());
        }).dimensions(cx - 120, y, 118, 20).build());
        addDrawableChild(ButtonWidget.builder(soundText(), button -> {
            config.warningSound = !config.warningSound;
            button.setMessage(soundText());
        }).dimensions(cx + 2, y, 118, 20).build());
        y += 24;

        // --- выбор редактируемого списка (whitelist / blacklist) ---
        addDrawableChild(ButtonWidget.builder(editingText(), button -> {
            editing = (editing == Mode.WHITELIST) ? Mode.BLACKLIST : Mode.WHITELIST;
            page = 0;
            clearAndInit();
        }).dimensions(cx - 120, y, 240, 20).build());
        y += 28;

        // --- список записей (текущая страница выбранного списка) ---
        final List<String> entries = config.entriesFor(editing);
        listTopY = y;
        page = Math.max(0, Math.min(page, maxPage()));
        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(entries.size(), start + ENTRIES_PER_PAGE);
        for (int i = start; i < end; i++) {
            final String entry = entries.get(i);
            int rowY = listTopY + (i - start) * ROW_HEIGHT;
            addDrawableChild(ButtonWidget.builder(Text.literal("X").formatted(Formatting.RED), button -> {
                entries.remove(entry);
                clearAndInit();
            }).dimensions(cx + 100, rowY, 20, 18).build());
            shownEntries.add(entry);
        }

        // --- навигация по страницам ---
        navY = listTopY + ENTRIES_PER_PAGE * ROW_HEIGHT + 2;
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.blockbreakwarden.prev"), button -> {
            if (page > 0) {
                page--;
                clearAndInit();
            }
        }).dimensions(cx - 120, navY, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.blockbreakwarden.next"), button -> {
            if (page < maxPage()) {
                page++;
                clearAndInit();
            }
        }).dimensions(cx + 50, navY, 70, 20).build());

        // --- добавление новой записи ---
        inputY = navY + 26;
        inputField = new TextFieldWidget(this.textRenderer, cx - 120, inputY, 175, 20, Text.empty());
        inputField.setMaxLength(256);
        inputField.setText(pendingInput);
        inputField.setPlaceholder(Text.translatable("label.blockbreakwarden.input_hint").formatted(Formatting.DARK_GRAY));
        inputField.setChangedListener(s -> pendingInput = s);
        addDrawableChild(inputField);

        addDrawableChild(ButtonWidget.builder(Text.translatable("button.blockbreakwarden.add"), button -> addEntry())
                .dimensions(cx + 60, inputY, 60, 20).build());

        // --- нижний ряд: очистить / готово ---
        int bottomY = this.height - 28;
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.blockbreakwarden.clear").formatted(Formatting.RED), button -> {
            config.entriesFor(editing).clear();
            page = 0;
            clearAndInit();
        }).dimensions(cx - 120, bottomY, 118, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.blockbreakwarden.done"), button -> this.close())
                .dimensions(cx + 2, bottomY, 118, 20).build());
    }

    private void addEntry() {
        String normalized = BreakRules.normalize(pendingInput);
        if (normalized == null) {
            invalidUntil = System.currentTimeMillis() + 1500L;
            return;
        }
        List<String> entries = config.entriesFor(editing);
        if (!entries.contains(normalized)) {
            entries.add(normalized);
        }
        pendingInput = "";
        page = maxPage();
        clearAndInit();
    }

    private int maxPage() {
        int size = config.entriesFor(editing).size();
        return size == 0 ? 0 : (size - 1) / ENTRIES_PER_PAGE;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Фон экрана рисуется по-разному в зависимости от версии Minecraft:
        // - 1.21.6+: Screen.render() сам рисует фон с блюром; повторный renderBackground()
        //   вызывает "Can only blur once per frame", поэтому его НЕ зовём;
        // - 1.20.2 .. 1.21.5: фон нужно рисовать вручную (сигнатура с mouseX/mouseY/delta);
        // - 1.20 / 1.20.1: фон рисуется вручную старой однопараметровой сигнатурой.
        //? if >=1.21.6 {
        super.render(context, mouseX, mouseY, delta);
        //?} elif >=1.20.2 {
        /*this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);*/
        //?} else {
        /*this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);*/
        //?}

        int cx = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, 12, 0xFFFFFFFF);

        // подписи записей или сообщение о пустом списке
        if (config.entriesFor(editing).isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("label.blockbreakwarden.empty"), cx, listTopY + 6, 0xFFAAAAAA);
        } else {
            for (int i = 0; i < shownEntries.size(); i++) {
                String entry = shownEntries.get(i);
                int rowY = listTopY + i * ROW_HEIGHT + 5;
                int color = entry.startsWith("#") ? 0xFFFFD24D : 0xFFFFFFFF; // тэги — золотым
                String text = this.textRenderer.trimToWidth(entry, 210);
                context.drawTextWithShadow(this.textRenderer, Text.literal(text), cx - 118, rowY, color);
            }
        }

        // индикатор страницы
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("label.blockbreakwarden.page", page + 1, maxPage() + 1),
                cx, navY + 6, 0xFFCCCCCC);

        // подсказка о неверном вводе
        if (System.currentTimeMillis() < invalidUntil) {
            context.drawTextWithShadow(this.textRenderer,
                    Text.translatable("label.blockbreakwarden.invalid").formatted(Formatting.RED),
                    cx - 120, inputY - 11, 0xFFFF5555);
        }
    }

    @Override
    public void close() {
        BlockBreakWardenConfig.save();
        MinecraftClient.getInstance().setScreen(parent);
    }

    // --- тексты кнопок ---

    private Text modeText() {
        return Text.translatable("label.blockbreakwarden.mode")
                .append(": ")
                .append(Text.translatable(config.mode.translationKey()));
    }

    private Text editingText() {
        return Text.translatable("label.blockbreakwarden.editing_list")
                .append(": ")
                .append(Text.translatable(editing.translationKey()));
    }

    private Text warnText() {
        return Text.translatable(config.showWarnings
                ? "button.blockbreakwarden.warnings_on"
                : "button.blockbreakwarden.warnings_off");
    }

    private Text soundText() {
        return Text.translatable(config.warningSound
                ? "button.blockbreakwarden.sound_on"
                : "button.blockbreakwarden.sound_off");
    }
}
