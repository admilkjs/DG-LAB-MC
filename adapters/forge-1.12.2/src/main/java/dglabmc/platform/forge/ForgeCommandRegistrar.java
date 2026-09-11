package dglabmc.platform.forge;

import dglabmc.AppServices;
import dglabmc.client.ClientCommandRouter;
import dglabmc.core.rule.RuleDefinition;
import dglabmc.core.wave.WaveformDefinition;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.IClientCommand;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ForgeCommandRegistrar extends CommandBase implements IClientCommand {
    public static void register() {
        ClientCommandHandler.instance.registerCommand(new ForgeCommandRegistrar());
    }

    @Override
    public String getName() {
        return "dglab";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/dglab";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("dgl");
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        StringBuilder command = new StringBuilder("/dglab");
        for (String arg : args) {
            command.append(' ').append(quote(arg));
        }
        ClientCommandRouter.tryHandle(command.toString());
    }

    @Override
    public boolean allowUsageWithoutPrefix(ICommandSender sender, String message) {
        return false;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length <= 1) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("password", "ui", "open", "status", "pair", "export", "global", "rule", "waveform"));
        }
        String root = lower(args[0]);
        if ("pair".equals(root) && args.length == 2) {
            return getListOfStringsMatchingLastWord(args, Collections.singletonList("refresh"));
        }
        if ("global".equals(root)) {
            return tabGlobal(args);
        }
        if ("rule".equals(root)) {
            return tabRule(args);
        }
        if ("waveform".equals(root) || "wave".equals(root)) {
            return tabWaveform(args);
        }
        return Collections.emptyList();
    }

    @Override
    public int compareTo(net.minecraft.command.ICommand other) {
        return this.getName().compareTo(other.getName());
    }

    private List<String> tabGlobal(String[] args) {
        if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("a", "b", "strength"));
        }
        if (args.length == 3 && ("a".equalsIgnoreCase(args[1]) || "b".equalsIgnoreCase(args[1]))) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("max", "event", "damage", "delay", "decayInterval", "decayValue", "death", "deathDelay", "min"));
        }
        return Collections.emptyList();
    }

    private List<String> tabRule(String[] args) {
        if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("list", "enable", "disable", "test", "rename", "move", "row"));
        }
        if (args.length == 3 && !"list".equalsIgnoreCase(args[1])) {
            return getListOfStringsMatchingLastWord(args, toList(collectRuleTokens()));
        }
        if (args.length == 4 && "move".equalsIgnoreCase(args[1])) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("up", "down", "top", "bottom"));
        }
        if (args.length == 4 && "row".equalsIgnoreCase(args[1])) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("mergeUp", "splitNext"));
        }
        return Collections.emptyList();
    }

    private List<String> tabWaveform(String[] args) {
        if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("list", "test"));
        }
        if (args.length == 3 && "test".equalsIgnoreCase(args[1])) {
            return getListOfStringsMatchingLastWord(args, toList(collectWaveformTokens()));
        }
        if (args.length == 4 && "test".equalsIgnoreCase(args[1])) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("A", "B"));
        }
        return Collections.emptyList();
    }

    private static Iterable<String> collectRuleTokens() {
        Set<String> tokens = new LinkedHashSet<String>();
        for (RuleDefinition rule : AppServices.get().getConfig().rules) {
            addToken(tokens, rule.id);
            addToken(tokens, rule.name);
        }
        return tokens;
    }

    private static Iterable<String> collectWaveformTokens() {
        Set<String> tokens = new LinkedHashSet<String>();
        for (WaveformDefinition waveform : AppServices.get().getConfig().waveforms) {
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

    private static List<String> toList(Iterable<String> values) {
        List<String> result = new ArrayList<String>();
        for (String value : values) {
            result.add(value);
        }
        return result;
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
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
