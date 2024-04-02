package fr.maxlego08.essentials.zutils.utils.commands;

import fr.maxlego08.essentials.api.Configuration;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.EssentialsCommand;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.commands.Tab;
import fr.maxlego08.essentials.api.commands.TabCompletion;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.api.user.User;
import fr.maxlego08.essentials.zutils.utils.TimerBuilder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class VCommand extends Arguments implements EssentialsCommand {

    protected final EssentialsPlugin plugin;
    protected final List<VCommand> subVCommands = new ArrayList<>();
    private final List<String> subCommands = new ArrayList<>();
    private final List<String> requireArgs = new ArrayList<>();
    private final List<String> optionalArgs = new ArrayList<>();
    private final Map<Integer, TabCompletion> tabCompletions = new HashMap<>();
    protected VCommand parent;
    protected CommandSender sender;
    protected Player player;
    protected User user;
    protected Configuration configuration;
    private boolean consoleCanUse = true;
    private boolean ignoreParent = false;
    private boolean ignoreArgs = false;
    private String permission;
    private String syntax;
    private String description;
    private int argsMinLength;
    private int argsMaxLength;
    private boolean extendedArgs = false;
    private CommandResultType tabCompleter = CommandResultType.DEFAULT;

    public VCommand(EssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isExtendedArgs() {
        return extendedArgs;
    }

    public void setExtendedArgs(boolean extendedArgs) {
        this.extendedArgs = extendedArgs;
    }

    public EssentialsPlugin getPlugin() {
        return plugin;
    }

    public List<VCommand> getSubVCommands() {
        return subVCommands;
    }

    @Override
    public List<String> getSubCommands() {
        return subCommands;
    }

    public List<String> getRequireArgs() {
        return requireArgs;
    }

    public List<String> getOptionalArgs() {
        return optionalArgs;
    }

    public Map<Integer, TabCompletion> getTabCompletions() {
        return tabCompletions;
    }

    @Override
    public VCommand getParent() {
        return parent;
    }

    public void setParent(VCommand parent) {
        this.parent = parent;
    }

    protected User getUser(Player player) {
        return this.plugin.getStorageManager().getStorage().getUser(player.getUniqueId());
    }

    @Override
    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission.asPermission();
    }

    @Override
    public String getSyntax() {
        return syntax == null ? syntax = generateDefaultSyntax("") : syntax;
    }

    public void setSyntax(String syntax) {
        this.syntax = syntax;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDescription(Message description) {
        this.description = description.getMessage();
    }

    public int getArgsMinLength() {
        return argsMinLength;
    }

    public void setArgsMinLength(int argsMinLength) {
        this.argsMinLength = argsMinLength;
    }

    public int getArgsMaxLength() {
        return argsMaxLength;
    }

    public void setArgsMaxLength(int argsMaxLength) {
        this.argsMaxLength = argsMaxLength;
    }

    public CommandSender getSender() {
        return sender;
    }

    public void setSender(CommandSender sender) {
        this.sender = sender;
    }

    protected void setTabCompleter() {
        this.tabCompleter = CommandResultType.SUCCESS;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public boolean isIgnoreParent() {
        return ignoreParent;
    }

    public void setIgnoreParent(boolean ignoreParent) {
        this.ignoreParent = ignoreParent;
    }

    @Override
    public boolean isIgnoreArgs() {
        return ignoreArgs;
    }

    public void setIgnoreArgs(boolean ignoreArgs) {
        this.ignoreArgs = ignoreArgs;
    }

    protected void onlyPlayers() {
        this.consoleCanUse = false;
    }

    @Override
    public CommandResultType getTabCompleter() {
        return tabCompleter;
    }

    public void setTabCompleter(CommandResultType tabCompleter) {
        this.tabCompleter = tabCompleter;
    }

    public Optional<TabCompletion> getCompletionAt(int index) {
        return Optional.ofNullable(this.tabCompletions.getOrDefault(index, null));
    }

    protected void addRequireArg(String message) {
        this.requireArgs.add(message);
        this.ignoreParent = this.parent == null;
        this.ignoreArgs = true;
    }

    protected void addRequireArg(String message, TabCompletion runnable) {
        this.addRequireArg(message);
        int index = this.requireArgs.size();
        this.addCompletion(index - 1, runnable);
    }

    protected void addOptionalArg(String message) {
        this.optionalArgs.add(message);
        this.ignoreParent = this.parent == null;
        this.ignoreArgs = true;
    }

    protected void addOptionalArg(String message, TabCompletion runnable) {
        this.addOptionalArg(message);
        int index = this.requireArgs.size() + this.optionalArgs.size();
        this.addCompletion(index - 1, runnable);
    }

    private String generateDefaultSyntax(String syntax) {
        boolean update = syntax.isEmpty();

        StringBuilder syntaxBuilder = new StringBuilder();
        if (update) {
            appendRequiredArguments(syntaxBuilder);
            appendOptionalArguments(syntaxBuilder);
            syntax = syntaxBuilder.toString();
        }
        String tmpString = subCommands.get(0) + syntax;
        return parent == null ? "/" + tmpString : parent.generateDefaultSyntax(" " + tmpString);
    }

    private void appendRequiredArguments(StringBuilder syntaxBuilder) {
        requireArgs.forEach(arg -> syntaxBuilder.append(" <").append(arg).append(">"));
    }

    private void appendOptionalArguments(StringBuilder syntaxBuilder) {
        optionalArgs.forEach(arg -> syntaxBuilder.append(" [<").append(arg).append(">]"));
    }

    private int parentCount(int defaultParent) {
        return parent == null ? defaultParent : parent.parentCount(defaultParent + 1);
    }

    @Override
    public boolean isConsoleCanUse() {
        return consoleCanUse;
    }

    protected boolean isPlayer() {
        return this.player != null;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    @Override
    public void addSubCommand(String subCommand) {
        this.subCommands.add(subCommand);
    }

    public VCommand addSubCommand(VCommand command) {
        command.setParent(this);
        this.plugin.getCommandManager().registerCommand(command);
        this.subVCommands.add(command);
        return this;
    }

    public VCommand addSubCommand(String... subCommand) {
        this.subCommands.addAll(Arrays.asList(subCommand));
        return this;
    }

    protected void addCompletion(int index, TabCompletion runnable) {
        this.tabCompletions.put(index, runnable);
        this.setTabCompleter();
    }

    private String getMainCommand() {
        return this.subCommands.get(0);
    }

    @Override
    public void addSubCommand(List<String> aliases) {
        this.subCommands.addAll(aliases);
    }

    @Override
    public CommandResultType prePerform(EssentialsPlugin plugin, CommandSender commandSender, String[] args) {
        updateArgumentCounts();

        if (this.syntax == null) {
            this.syntax = generateDefaultSyntax("");
        }

        this.args = args;

        if (isSubCommandMatch()) {
            return CommandResultType.CONTINUE;
        }

        if (isSyntaxError(args.length)) {
            return CommandResultType.SYNTAX_ERROR;
        }

        this.sender = commandSender;
        setPlayerIfApplicable();

        return safelyPerformCommand(plugin);
    }

    private void updateArgumentCounts() {
        this.parentCount = this.parentCount(0);
        this.argsMaxLength = this.requireArgs.size() + this.optionalArgs.size() + this.parentCount;
        this.argsMinLength = this.requireArgs.size() + this.parentCount;
    }

    private boolean isSubCommandMatch() {
        String defaultString = super.argAsString(0);
        if (defaultString != null) {
            for (VCommand subCommand : subVCommands) {
                if (subCommand.getSubCommands().contains(defaultString.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSyntaxError(int argsLength) {
        return (this.argsMinLength != 0 && argsLength < this.argsMinLength) || (this.argsMaxLength != 0 && argsLength > this.argsMaxLength && !this.extendedArgs);
    }

    private void setPlayerIfApplicable() {
        if (this.sender instanceof Player player) {
            this.player = player;
            this.user = this.plugin.getStorageManager().getStorage().getUser(player.getUniqueId());
        } else {
            this.player = null;
            this.user = null;
        }
    }

    private CommandResultType safelyPerformCommand(EssentialsPlugin plugin) {
        try {

            int cooldownSeconds = 0;
            String key = this.getMainCommand();
            configuration = this.plugin.getConfiguration();

            // Check for cooldown
            if (user != null && (!this.user.hasPermission(Permission.ESSENTIALS_BYPASS_COOLDOWN) || !configuration.isEnableCooldownBypass())) {
                Optional<Integer> optional = configuration.getCooldown(this.sender, key);
                if (optional.isPresent()) {
                    cooldownSeconds = optional.get();
                    if (this.user.isCooldown(key)) {
                        long milliSeconds = this.user.getCooldown(key) - System.currentTimeMillis();
                        message(this.sender, Message.COOLDOWN, "%cooldown%", TimerBuilder.getStringTime(milliSeconds));
                        return CommandResultType.COOLDOWN;
                    }
                }
            }

            CommandResultType commandResultType = perform(plugin);

            if (commandResultType != CommandResultType.SYNTAX_ERROR && cooldownSeconds != 0 && this.user != null && (!this.user.hasPermission(Permission.ESSENTIALS_BYPASS_COOLDOWN) || !configuration.isEnableCooldownBypass())) {
                this.user.addCooldown(key, cooldownSeconds);
            }

            return commandResultType;
        } catch (Exception exception) {
            if (plugin.getConfiguration().isEnableDebug()) {
                exception.printStackTrace();
            }
            return CommandResultType.SYNTAX_ERROR;
        }
    }

    protected abstract CommandResultType perform(EssentialsPlugin plugin);

    public boolean sameSubCommands() {
        if (this.parent == null) {
            return false;
        }
        for (String command : this.subCommands) {
            if (this.parent.getSubCommands().contains(command)) return true;
        }
        return false;
    }

    @Override
    public List<String> toTab(EssentialsPlugin plugin, CommandSender sender, String[] args) {

        this.parentCount = this.parentCount(0);

        int currentInex = (args.length - this.parentCount) - 1;
        Optional<TabCompletion> optional = this.getCompletionAt(currentInex);

        if (optional.isPresent()) {

            TabCompletion collectionRunnable = optional.get();
            String startWith = args[args.length - 1];
            return this.generateList(collectionRunnable.accept(sender, args), startWith);

        }

        return null;
    }

    protected List<String> generateList(List<String> defaultList, String startWith) {
        return generateList(defaultList, startWith, Tab.CONTAINS);
    }

    protected List<String> generateList(List<String> defaultList, String startWith, Tab tab) {
        List<String> newList = new ArrayList<>();
        for (String str : defaultList) {
            if (startWith.length() == 0 || (tab.equals(Tab.START) ? str.toLowerCase().startsWith(startWith.toLowerCase()) : str.toLowerCase().contains(startWith.toLowerCase()))) {
                newList.add(str);
            }
        }
        return newList.size() == 0 ? null : newList;
    }

}
