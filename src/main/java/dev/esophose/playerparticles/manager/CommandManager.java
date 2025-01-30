package dev.esophose.playerparticles.manager;

import dev.esophose.playerparticles.PlayerParticles;
import dev.esophose.playerparticles.api.PlayerParticlesAPI;
import dev.esophose.playerparticles.command.AddCommandModule;
import dev.esophose.playerparticles.command.CommandModule;
import dev.esophose.playerparticles.command.CommandModuleSecondary;
import dev.esophose.playerparticles.command.DataCommandModule;
import dev.esophose.playerparticles.command.DefaultCommandModule;
import dev.esophose.playerparticles.command.EditCommandModule;
import dev.esophose.playerparticles.command.EffectsCommandModule;
import dev.esophose.playerparticles.command.FixedCommandModule;
import dev.esophose.playerparticles.command.GUICommandModule;
import dev.esophose.playerparticles.command.GroupCommandModule;
import dev.esophose.playerparticles.command.HelpCommandModule;
import dev.esophose.playerparticles.command.ListCommandModule;
import dev.esophose.playerparticles.command.OtherCommandModule;
import dev.esophose.playerparticles.command.ReloadCommandModule;
import dev.esophose.playerparticles.command.RemoveCommandModule;
import dev.esophose.playerparticles.command.ResetCommandModule;
import dev.esophose.playerparticles.command.StylesCommandModule;
import dev.esophose.playerparticles.command.ToggleCommandModule;
import dev.esophose.playerparticles.command.UseCommandModule;
import dev.esophose.playerparticles.command.VersionCommandModule;
import dev.esophose.playerparticles.command.WorldsCommandModule;
import dev.esophose.playerparticles.particles.PPlayer;
import dev.esophose.playerparticles.util.ParticleUtils;
import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.hook.PlaceholderAPIHook;
import dev.rosewood.rosegarden.manager.Manager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.CommandMinecart;
import org.tjdev.util.tjpluginutil.spigot.FoliaUtil;

public class CommandManager extends Manager implements CommandExecutor, TabCompleter {

    /**
     * A list of all commands
     */
    private List<CommandModule> commands;
    private CommandModuleSecondary ppoCommand;

    public CommandManager(RosePlugin playerParticles) {
        super(playerParticles);

        PluginCommand pp = playerParticles.getCommand("pp");
        PluginCommand ppo = playerParticles.getCommand("ppo");

        if (pp == null || ppo == null) {
            Bukkit.getPluginManager().disablePlugin(playerParticles);
            return;
        }

        pp.setTabCompleter(this);
        pp.setExecutor(this);
        ppo.setTabCompleter(this);
        ppo.setExecutor(this);
    }

    @Override
    public void reload() {
        this.commands = new ArrayList<CommandModule>() {{
            this.add(new AddCommandModule());
            this.add(new DataCommandModule());
            this.add(new DefaultCommandModule());
            this.add(new EditCommandModule());
            this.add(new EffectsCommandModule());
            this.add(new FixedCommandModule());
            this.add(new GroupCommandModule());
            this.add(new GUICommandModule());
            this.add(new HelpCommandModule());
            this.add(new ListCommandModule());
            this.add(new ReloadCommandModule());
            this.add(new RemoveCommandModule());
            this.add(new ResetCommandModule());
            this.add(new StylesCommandModule());
            this.add(new ToggleCommandModule());
            this.add(new UseCommandModule());
            this.add(new VersionCommandModule());
            this.add(new WorldsCommandModule());
        }};

        this.ppoCommand = new OtherCommandModule();
    }

    @Override
    public void disable() {

    }

    /**
     * Finds a matching CommandModule by its name
     * 
     * @param commandName The command name
     * @return The found CommandModule, otherwise null
     */
    public CommandModule findMatchingCommand(String commandName) {
        for (CommandModule commandModule : this.commands)
            if (commandModule.getName().equalsIgnoreCase(commandName)) 
                return commandModule;
        return null;
    }

    /**
     * Get a list of all available commands
     * 
     * @return A List of all CommandModules registered
     */
    public List<CommandModule> getCommands() {
        return this.commands;
    }

    /**
     * Get all available command names
     * 
     * @return All available command names
     */
    public List<String> getCommandNames() {
        List<String> commandNames = new ArrayList<>();
        for (CommandModule cmd : this.commands)
            commandNames.add(cmd.getName());
        return commandNames;
    }

    /**
     * Called when a player executes a PlayerParticles command
     * Checks what PlayerParticles command it is and calls the corresponding module
     * 
     * @param sender Who executed the command
     * @param cmd The command
     * @param label The command label
     * @param args The arguments following the command
     * @return true
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        LocaleManager localeManager = PlayerParticles.getInstance().getManager(LocaleManager.class);

        if (cmd.getName().equalsIgnoreCase("pp")) {
            String commandName = args.length > 0 ? args[0] : "";
            CommandModule commandModule = this.findMatchingCommand(commandName);
            if (commandModule == null) {
                sender.sendMessage(localeManager.getLocaleMessage("command-error-unknown"));
                return true;
            }

            FoliaUtil.scheduler.runTaskAsynchronously(() -> {
                String[] cmdArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

                if (!commandModule.canConsoleExecute()) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.RED + "Error: This command can only be executed by a player.");
                        return;
                    }
                } else if (sender instanceof ConsoleCommandSender || sender instanceof BlockCommandSender || sender instanceof CommandMinecart) {
                    commandModule.onCommandExecute(PlayerParticlesAPI.getInstance().getConsolePPlayer(), cmdArgs);
                    return;
                }

                Player p = (Player) sender;

                this.rosePlugin.getManager(DataManager.class).getPPlayer(p.getUniqueId(), (pplayer) -> {
                    PermissionManager permissionManager = PlayerParticles.getInstance().getManager(PermissionManager.class);
                    if (commandModule.requiresEffectsAndStyles() && (permissionManager.getEffectsUserHasPermissionFor(pplayer).isEmpty() || permissionManager.getStylesUserHasPermissionFor(pplayer).isEmpty())) {
                        localeManager.sendMessage(pplayer, "command-error-missing-effects-or-styles");
                    } else {
                        commandModule.onCommandExecute(pplayer, cmdArgs);
                    }
                });
            });

            return true;
        } else if (cmd.getName().equalsIgnoreCase("ppo")) {
            // Replace placeholders if somebody tried to use them
            Player player = sender instanceof Player ? (Player) sender : null;
            for (int i = 0; i < args.length; i++)
                if (args[i].startsWith("%"))
                    args[i] = PlaceholderAPIHook.applyPlaceholders(player, args[i]);

            // Replace selectors if from command blocks
            if ((sender instanceof BlockCommandSender || sender instanceof CommandMinecart) && args.length > 0 && args[0].startsWith("@")) {
                String selector = args[0];
                try {
                    List<Entity> selectedEntities = Bukkit.selectEntities(sender, selector);
                    if (selectedEntities.isEmpty()) {
                        sender.sendMessage("Error: No entities found for selector '" + selector + "'");
                        return true;
                    }

                    if (selectedEntities.size() > 1) {
                        sender.sendMessage("Error: More than one entity found for selector '" + selector + "'");
                        return true;
                    }

                    Entity entity = selectedEntities.get(0);
                    if (!(entity instanceof Player)) {
                        sender.sendMessage("Error: Entity '" + entity.getName() + "' is not a player");
                        return true;
                    }

                    args[0] = entity.getName();
                } catch (IllegalArgumentException e) {
                    sender.sendMessage("Error: Invalid player selector '" + selector + "'");
                    return true;
                }
            }

            // Run the /ppo command
            this.ppoCommand.onCommandExecute(sender, args);
        }
        
        return true;
    }

    /**
     * Activated when a user pushes tab in chat prefixed with /pp
     * 
     * @param sender The sender that hit tab, should always be a player
     * @param cmd The command the player is executing
     * @param alias The possible alias for the command
     * @param args All arguments following the command
     * @return A list of commands available to the sender
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (cmd.getName().equalsIgnoreCase("pp")) {
            PPlayer pplayer;
            if (sender instanceof Player && !ParticleUtils.getTargetBlock((Player) sender).getType().name().contains("COMMAND_BLOCK")) {
                pplayer = PlayerParticlesAPI.getInstance().getPPlayer(sender);
            } else {
                pplayer = PlayerParticlesAPI.getInstance().getConsolePPlayer();
            }

            if (pplayer == null)
                return new ArrayList<>();
            
            if (args.length <= 1) {
                CommandModule commandModule = this.findMatchingCommand(""); // Get the default command module
                return commandModule.onTabComplete(pplayer, args);
            } else {
                CommandModule commandModule = this.findMatchingCommand(args[0]);
                if (commandModule != null) {
                    if (pplayer.getPlayer() == null && !commandModule.canConsoleExecute())
                        return new ArrayList<>();

                    String[] cmdArgs = Arrays.copyOfRange(args, 1, args.length);
                    return commandModule.onTabComplete(pplayer, cmdArgs);
                }
            }
        } else if (cmd.getName().equalsIgnoreCase("ppo")) {
            return this.ppoCommand.onTabComplete(sender, args);
        }
        
        return new ArrayList<>();
    }
}
