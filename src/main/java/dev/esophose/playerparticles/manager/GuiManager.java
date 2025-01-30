package dev.esophose.playerparticles.manager;

import dev.esophose.playerparticles.PlayerParticles;
import dev.esophose.playerparticles.gui.GuiInventory;
import dev.esophose.playerparticles.gui.GuiInventoryDefault;
import dev.esophose.playerparticles.gui.GuiInventoryLoadPresetGroups;
import dev.esophose.playerparticles.gui.GuiInventoryManageGroups;
import dev.esophose.playerparticles.gui.GuiInventoryManageParticles;
import dev.esophose.playerparticles.manager.ConfigurationManager.Setting;
import dev.esophose.playerparticles.particles.PPlayer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.scheduler.BukkitTask;
import org.tjdev.util.tjpluginutil.spigot.FoliaUtil;
import org.tjdev.util.tjpluginutil.spigot.scheduler.universalscheduler.scheduling.tasks.MyScheduledTask;

public class GuiManager extends Manager implements Listener, Runnable {

    private List<GuiInventory> guiInventories;
    private MyScheduledTask guiTask;

    public GuiManager(RosePlugin playerParticles) {
        super(playerParticles);

        this.guiInventories = Collections.synchronizedList(new ArrayList<>());
        this.guiTask = null;

        Bukkit.getPluginManager().registerEvents(this, playerParticles);
    }

    @Override
    public void reload() {
        if (this.guiTask != null)
            this.guiTask.cancel();
        this.guiTask = FoliaUtil.scheduler.runTaskTimer(this, 0, 10);
    }

    @Override
    public void disable() {
        this.forceCloseAllOpenGUIs();
    }

    /**
     * Ticks GuiInventories
     */
    public void run() {
        this.guiInventories.forEach(GuiInventory::onTick);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) event.getWhoClicked();
        GuiInventory inventory = this.getGuiInventory(player);
        if (inventory == null)
            return;
        
        event.setCancelled(true);
        FoliaUtil.scheduler.runTaskAsynchronously(() -> inventory.onClick(event));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player))
            return;

        Player player = (Player) event.getPlayer();
        GuiInventory inventory = this.getGuiInventory(player);
        if (inventory == null)
            return;

        this.guiInventories.remove(inventory);
    }

    /**
     * Gets if the GUI is disabled by the server owner or not
     * 
     * @return True if the GUI is disabled
     */
    public boolean isGuiDisabled() {
        return !Setting.GUI_ENABLED.getBoolean();
    }

    /**
     * Forcefully closes all open PlayerParticles GUIs
     * Used for when the plugin unloads so players can't take items from the GUI
     */
    public void forceCloseAllOpenGUIs() {
        List<Player> toClose = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (GuiInventory inventory : this.guiInventories) {
                if (inventory.getPPlayer().getUniqueId().equals(player.getUniqueId()) && inventory.getInventory().equals(player.getOpenInventory().getTopInventory())) {
                    toClose.add(player);
                    break;
                }
            }
        }
        this.guiInventories.clear();

        for (Player player : toClose) FoliaUtil.scheduler.runTask(player, player::closeInventory);
    }
    
    /**
     * Opens the default GUI screen for a player
     * 
     * @param pplayer The PPlayer to open the GUI screen for
     */
    public void openDefault(PPlayer pplayer) {
        if (Setting.GUI_PRESETS_ONLY.getBoolean()) {
            this.openPresetGroups(pplayer);
            return;
        }

        FoliaUtil.scheduler.runTask(pplayer.getPlayer(), () ->
                this.openGui(pplayer, new GuiInventoryDefault(pplayer)));
    }

    /**
     * Opens the preset groups GUI screen for a player
     *
     * @param pplayer The PPlayer to open the GUI screen for
     */
    public void openPresetGroups(PPlayer pplayer) {
        FoliaUtil.scheduler.runTask(pplayer.getPlayer(), () ->
                this.openGui(pplayer, new GuiInventoryLoadPresetGroups(pplayer, true, 1)));
    }

    /**
     * Opens the groups GUI screen for a player
     *
     * @param pplayer The PPlayer to open the GUI screen for
     */
    public void openGroups(PPlayer pplayer) {
        FoliaUtil.scheduler.runTask(pplayer.getPlayer(), () ->
                this.openGui(pplayer, new GuiInventoryManageGroups(pplayer, 1)));
    }

    /**
     * Opens the edit particles GUI screen for a player
     *
     * @param pplayer The PPlayer to open the GUI screen for
     */
    public void openParticles(PPlayer pplayer) {
        FoliaUtil.scheduler.runTask(pplayer.getPlayer(), () ->
                this.openGui(pplayer, new GuiInventoryManageParticles(pplayer)));
    }

    /**
     * Opens a GUI screen for a player
     *
     * @param pplayer The PPlayer to open for
     * @param guiInventory The GuiInventory to open
     */
    private void openGui(PPlayer pplayer, GuiInventory guiInventory) {
        pplayer.getPlayer().openInventory(guiInventory.getInventory());
        this.guiInventories.add(guiInventory);
    }
    
    /**
     * Changes the player's inventory to another one
     * 
     * @param nextInventory The GuiInventory to transition to
     */
    public void transition(GuiInventory nextInventory) {
        FoliaUtil.scheduler.runTask(nextInventory.getPPlayer().getPlayer(), () -> {
            nextInventory.getPPlayer().getPlayer().openInventory(nextInventory.getInventory());
            this.guiInventories.add(nextInventory);
        });
    }
    
    /**
     * Gets a GuiInventory by Player
     * 
     * @param player The Player
     * @return The GuiInventory belonging to the Player, if any
     */
    private GuiInventory getGuiInventory(Player player) {
        return this.guiInventories.stream()
                .filter(x -> x.getPPlayer().getUniqueId().equals(player.getUniqueId()))
                .findFirst()
                .orElse(null);
    }

}
