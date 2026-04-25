package dglabmc.platform.forge;

import dglabmc.AppServices;
import dglabmc.client.ClientCommandRouter;
import dglabmc.platform.PlatformServices;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.LinkedHashSet;
import java.util.Set;

public final class ForgeCommandRegistrar {
    private static final SuggestionProvider<CommandSourceStack> RULE_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggest(collectRuleTokens(), builder);
    private static final SuggestionProvider<CommandSourceStack> WAVEFORM_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggest(collectWaveformTokens(), builder);

    private ForgeCommandRegistrar() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        if (FMLEnvironment.dist.isDedicatedServer()) {
            return;
        }
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("dglab")
            .executes(ctx -> run(ctx.getSource(), "/dglab"))
            .then(Commands.literal("password")
                .then(Commands.argument("value", StringArgumentType.string())
                    .executes(ctx -> run(ctx.getSource(), "/dglab password " + quote(StringArgumentType.getString(ctx, "value"))))))
            .then(Commands.literal("ui").executes(ctx -> run(ctx.getSource(), "/dglab ui")))
            .then(Commands.literal("open").executes(ctx -> run(ctx.getSource(), "/dglab open")))
            .then(Commands.literal("status").executes(ctx -> run(ctx.getSource(), "/dglab status")))
            .then(Commands.literal("pair")
                .executes(ctx -> run(ctx.getSource(), "/dglab pair"))
                .then(Commands.literal("refresh").executes(ctx -> run(ctx.getSource(), "/dglab pair refresh"))))
            .then(Commands.literal("export").executes(ctx -> run(ctx.getSource(), "/dglab export")))
            .then(Commands.literal("global")
                .executes(ctx -> run(ctx.getSource(), "/dglab global"))
                .then(globalChannelCommands("a"))
                .then(globalChannelCommands("b"))
                .then(Commands.literal("strength")
                    .then(Commands.argument("base", IntegerArgumentType.integer(0, 200))
                        .executes(ctx -> run(ctx.getSource(), "/dglab global strength " + IntegerArgumentType.getInteger(ctx, "base")))
                        .then(Commands.argument("max", IntegerArgumentType.integer(0, 200))
                            .executes(ctx -> run(ctx.getSource(), "/dglab global strength " + IntegerArgumentType.getInteger(ctx, "base") + " " + IntegerArgumentType.getInteger(ctx, "max")))))))
            .then(ruleCommands())
            .then(waveformCommands());
        event.getDispatcher().register(root);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> globalChannelCommands(String channel) {
        return Commands.literal(channel)
            .executes(ctx -> run(ctx.getSource(), "/dglab global " + channel))
            .then(Commands.literal("max")
                .then(Commands.argument("value", IntegerArgumentType.integer(0, 200))
                    .executes(ctx -> run(ctx.getSource(), "/dglab global " + channel + " max " + IntegerArgumentType.getInteger(ctx, "value")))))
            .then(Commands.literal("event")
                .then(Commands.argument("value", IntegerArgumentType.integer(0, 200))
                    .executes(ctx -> run(ctx.getSource(), "/dglab global " + channel + " event " + IntegerArgumentType.getInteger(ctx, "value")))))
            .then(Commands.literal("damage")
                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0D, 20.0D))
                    .executes(ctx -> run(ctx.getSource(), "/dglab global " + channel + " damage " + DoubleArgumentType.getDouble(ctx, "value")))))
            .then(Commands.literal("delay")
                .then(Commands.argument("value", IntegerArgumentType.integer(0, 600000))
                    .executes(ctx -> run(ctx.getSource(), "/dglab global " + channel + " delay " + IntegerArgumentType.getInteger(ctx, "value")))))
            .then(Commands.literal("decayInterval")
                .then(Commands.argument("value", IntegerArgumentType.integer(50, 600000))
                    .executes(ctx -> run(ctx.getSource(), "/dglab global " + channel + " decayInterval " + IntegerArgumentType.getInteger(ctx, "value")))))
            .then(Commands.literal("decayValue")
                .then(Commands.argument("value", IntegerArgumentType.integer(0, 200))
                    .executes(ctx -> run(ctx.getSource(), "/dglab global " + channel + " decayValue " + IntegerArgumentType.getInteger(ctx, "value")))))
            .then(Commands.literal("death")
                .then(Commands.argument("value", IntegerArgumentType.integer(0, 200))
                    .executes(ctx -> run(ctx.getSource(), "/dglab global " + channel + " death " + IntegerArgumentType.getInteger(ctx, "value")))))
            .then(Commands.literal("deathDelay")
                .then(Commands.argument("value", IntegerArgumentType.integer(0, 600000))
                    .executes(ctx -> run(ctx.getSource(), "/dglab global " + channel + " deathDelay " + IntegerArgumentType.getInteger(ctx, "value")))))
            .then(Commands.literal("min")
                .then(Commands.argument("value", IntegerArgumentType.integer(0, 200))
                    .executes(ctx -> run(ctx.getSource(), "/dglab global " + channel + " min " + IntegerArgumentType.getInteger(ctx, "value")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> ruleCommands() {
        return Commands.literal("rule")
            .then(Commands.literal("list").executes(ctx -> run(ctx.getSource(), "/dglab rule list")))
            .then(Commands.literal("enable")
                .then(Commands.argument("rule", StringArgumentType.string())
                    .suggests(RULE_SUGGESTIONS)
                    .executes(ctx -> run(ctx.getSource(), "/dglab rule enable " + quote(StringArgumentType.getString(ctx, "rule"))))))
            .then(Commands.literal("disable")
                .then(Commands.argument("rule", StringArgumentType.string())
                    .suggests(RULE_SUGGESTIONS)
                    .executes(ctx -> run(ctx.getSource(), "/dglab rule disable " + quote(StringArgumentType.getString(ctx, "rule"))))))
            .then(Commands.literal("test")
                .then(Commands.argument("rule", StringArgumentType.string())
                    .suggests(RULE_SUGGESTIONS)
                    .executes(ctx -> run(ctx.getSource(), "/dglab rule test " + quote(StringArgumentType.getString(ctx, "rule"))))))
            .then(Commands.literal("rename")
                .then(Commands.argument("rule", StringArgumentType.string())
                    .suggests(RULE_SUGGESTIONS)
                    .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> run(ctx.getSource(), "/dglab rule rename " + quote(StringArgumentType.getString(ctx, "rule")) + " " + quote(StringArgumentType.getString(ctx, "name")))))))
            .then(Commands.literal("move")
                .then(Commands.argument("rule", StringArgumentType.string())
                    .suggests(RULE_SUGGESTIONS)
                    .then(Commands.literal("up").executes(ctx -> run(ctx.getSource(), "/dglab rule move " + quote(StringArgumentType.getString(ctx, "rule")) + " up")))
                    .then(Commands.literal("down").executes(ctx -> run(ctx.getSource(), "/dglab rule move " + quote(StringArgumentType.getString(ctx, "rule")) + " down")))
                    .then(Commands.literal("top").executes(ctx -> run(ctx.getSource(), "/dglab rule move " + quote(StringArgumentType.getString(ctx, "rule")) + " top")))
                    .then(Commands.literal("bottom").executes(ctx -> run(ctx.getSource(), "/dglab rule move " + quote(StringArgumentType.getString(ctx, "rule")) + " bottom")))))
            .then(Commands.literal("row")
                .then(Commands.argument("rule", StringArgumentType.string())
                    .suggests(RULE_SUGGESTIONS)
                    .then(Commands.literal("mergeUp").executes(ctx -> run(ctx.getSource(), "/dglab rule row " + quote(StringArgumentType.getString(ctx, "rule")) + " mergeUp")))
                    .then(Commands.literal("splitNext").executes(ctx -> run(ctx.getSource(), "/dglab rule row " + quote(StringArgumentType.getString(ctx, "rule")) + " splitNext")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> waveformCommands() {
        return Commands.literal("waveform")
            .then(Commands.literal("list").executes(ctx -> run(ctx.getSource(), "/dglab waveform list")))
            .then(Commands.literal("test")
                .then(Commands.argument("waveform", StringArgumentType.string())
                    .suggests(WAVEFORM_SUGGESTIONS)
                    .then(Commands.literal("A").executes(ctx -> run(ctx.getSource(), "/dglab waveform test " + quote(StringArgumentType.getString(ctx, "waveform")) + " A")))
                    .then(Commands.literal("B").executes(ctx -> run(ctx.getSource(), "/dglab waveform test " + quote(StringArgumentType.getString(ctx, "waveform")) + " B")))));
    }

    private static int run(CommandSourceStack source, String command) {
        ServerPlayer sourcePlayer;
        try {
            sourcePlayer = source.getPlayerOrException();
        } catch (Exception ignored) {
            return 0;
        }
        if (!isCurrentLocalPlayer(sourcePlayer)) {
            source.sendFailure(Component.literal("该命令仅本机玩家可用。"));
            return 0;
        }
        ClientCommandRouter.tryHandle(command);
        return 1;
    }

    private static boolean isCurrentLocalPlayer(ServerPlayer sourcePlayer) {
        return PlatformServices.client().isCurrentLocalPlayer(sourcePlayer.getUUID());
    }

    private static Iterable<String> collectRuleTokens() {
        Set<String> tokens = new LinkedHashSet<String>();
        for (dglabmc.rule.RuleDefinition rule : AppServices.get().getConfig().rules) {
            addToken(tokens, rule.id);
            addToken(tokens, rule.name);
        }
        return tokens;
    }

    private static Iterable<String> collectWaveformTokens() {
        Set<String> tokens = new LinkedHashSet<String>();
        for (dglabmc.wave.WaveformDefinition waveform : AppServices.get().getConfig().waveforms) {
            addToken(tokens, waveform.id);
            addToken(tokens, waveform.name);
        }
        return tokens;
    }

    private static void addToken(Set<String> tokens, String value) {
        if (value != null) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                tokens.add(trimmed);
            }
        }
    }

    private static String quote(String value) {
        if (value == null) {
            return "\"\"";
        }
        String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"");
        if (escaped.isEmpty() || escaped.indexOf(' ') >= 0 || escaped.indexOf('"') >= 0 || escaped.indexOf('\\') >= 0) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
