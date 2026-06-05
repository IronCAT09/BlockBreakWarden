package com.ironcat.blockbreakwarden;

import com.ironcat.blockbreakwarden.gui.BlockBreakWardenConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BlockBreakWarden implements ClientModInitializer {

    public static final String MOD_ID = "blockbreakwarden";
    public static final Logger LOGGER = LoggerFactory.getLogger("BlockBreakWarden");

    private static KeyBinding cycleModeKey;
    private static KeyBinding addTargetKey;

    /** Чтобы не спамить предупреждениями при удержании ЛКМ. */
    private long lastWarnTime = 0L;

    @Override
    public void onInitializeClient() {
        BlockBreakWardenConfig.load();
        registerKeyBindings();
        registerTickHandler();
        registerBreakGuard();
        LOGGER.info("BlockBreakWarden загружен. Текущий режим: {}", BlockBreakWardenConfig.get().mode);
    }

    private void registerKeyBindings() {
        // 1.21.9+: категория клавиш — это KeyBinding.Category, а не строковый ключ перевода.
        // Категорию регистрируем один раз: повторный create() с тем же id бросает исключение.
        //? if >=1.21.9 {
        /*KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));*/
        //?}
        cycleModeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.blockbreakwarden.cycle_mode",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                //? if >=1.21.9 {
                /*category*/
                //?} else {
                "category.blockbreakwarden"
                //?}
        ));
        addTargetKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.blockbreakwarden.add_target",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                //? if >=1.21.9 {
                /*category*/
                //?} else {
                "category.blockbreakwarden"
                //?}
        ));
    }

    private void registerTickHandler() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (cycleModeKey.wasPressed()) {
                cycleMode(client);
            }
            while (addTargetKey.wasPressed()) {
                addTargetBlock(client);
            }
        });
    }

    private void registerBreakGuard() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            // мод чисто клиентский: вмешиваемся только на стороне клиента
            if (!world.isClient()) {
                return ActionResult.PASS;
            }
            BlockState state = world.getBlockState(pos);
            if (BreakRules.canBreak(state)) {
                return ActionResult.PASS;
            }
            warn(state);
            return ActionResult.FAIL; // отменяем ломание
        });
    }

    // --- горячие клавиши ---

    private void cycleMode(MinecraftClient client) {
        BlockBreakWardenConfig config = BlockBreakWardenConfig.get();
        config.mode = config.mode.next();
        BlockBreakWardenConfig.save();
        if (client.player != null) {
            client.player.sendMessage(
                    Text.translatable("message.blockbreakwarden.mode_changed",
                            Text.translatable(config.mode.translationKey())).formatted(Formatting.YELLOW),
                    true);
        }
    }

    private void addTargetBlock(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }
        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            client.player.sendMessage(
                    Text.translatable("message.blockbreakwarden.no_target").formatted(Formatting.GRAY), true);
            return;
        }

        BlockPos pos = blockHit.getBlockPos();
        BlockState state = client.world.getBlockState(pos);
        Identifier id = Registries.BLOCK.getId(state.getBlock());
        String entry = id.toString();

        BlockBreakWardenConfig config = BlockBreakWardenConfig.get();
        // в режиме OFF нет активного списка — нечего редактировать
        if (config.mode == Mode.OFF) {
            client.player.sendMessage(
                    Text.translatable("message.blockbreakwarden.off_no_list").formatted(Formatting.GRAY), true);
            return;
        }
        // хоткей редактирует список текущего режима (whitelist или blacklist)
        List<String> list = config.entriesFor(config.mode);
        // повторное нажатие на уже добавленный блок убирает его из списка
        if (list.remove(entry)) {
            BlockBreakWardenConfig.save();
            client.player.sendMessage(
                    Text.translatable("message.blockbreakwarden.removed", entry).formatted(Formatting.YELLOW), true);
            return;
        }
        list.add(entry);
        BlockBreakWardenConfig.save();
        client.player.sendMessage(
                Text.translatable("message.blockbreakwarden.added", entry).formatted(Formatting.GREEN), true);
    }

    // --- предупреждение ---

    private void warn(BlockState state) {
        BlockBreakWardenConfig config = BlockBreakWardenConfig.get();
        if (!config.showWarnings) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastWarnTime < 800L) {
            return;
        }
        lastWarnTime = now;

        client.player.sendMessage(
                Text.translatable("message.blockbreakwarden.blocked",
                        state.getBlock().getName()).formatted(Formatting.RED),
                true);

        if (config.warningSound) {
            // 1.21.9+: PositionedSoundInstance.master(...) удалён; ui(...) принимает RegistryEntry.
            client.getSoundManager().play(
                    //? if >=1.21.9 {
                    /*PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_BASS, 0.6F));*/
                    //?} else {
                    PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.6F));
                    //?}
        }
    }

    /** Открыть экран настроек (используется Mod Menu и может быть вызвано напрямую). */
    public static BlockBreakWardenConfigScreen createConfigScreen(Screen parent) {
        return new BlockBreakWardenConfigScreen(parent);
    }
}
