package com.sainttx.factionsfly;

import com.massivecraft.factions.*;
import com.massivecraft.factions.struct.Relation;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by Matthew on 02/01/2015.
 */
public class FactionsFly extends JavaPlugin implements Listener {

    private Economy econ;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);

        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            econ = economyProvider.getProvider();
        }

        if (econ == null) {
            getLogger().severe("An economy provider was not found, players will not be charged to toggle flight.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getPlayer().hasPermission("factionsfly.fly.bypass")) {
            return;
        } else if (!event.getPlayer().getAllowFlight()) {
            return;
        } else if (event.getFrom().getBlockX() >> 4 == event.getTo().getBlockX() >> 4 && event.getFrom().getBlockZ() >> 4 == event.getTo().getBlockZ() >> 4 && event.getFrom().getWorld() == event.getTo().getWorld()) {
            return;
        } else {
            Player player = event.getPlayer();
            FPlayer fplayer = FPlayers.getInstance().getByPlayer(player);
            Faction dest = Board.getInstance().getFactionAt(new FLocation(event.getTo()));
            Relation rel = fplayer.getRelationTo(dest);

            if (rel == Relation.MEMBER && player.hasPermission("factionsfly.fly")) {
                return;
            } else if (rel == Relation.ALLY && player.hasPermission("factionsfly.fly.ally")) {
                return;
            } else {
                player.setAllowFlight(false);
                player.sendMessage(this.color(getConfig().getString("messages.toggle-off")));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player player = (Player) event.getEntity();
            Player damager = (Player) event.getDamager();

            if (player.getAllowFlight() && !player.hasPermission("factionsfly.fly.bypass")) {
                player.setAllowFlight(false);
                player.sendMessage(this.color(getConfig().getString("messages.toggle-off")));
            }

            if (damager.getAllowFlight() && !damager.hasPermission("factionsfly.fly.bypass")) {
                damager.setAllowFlight(false);
                player.sendMessage(this.color(getConfig().getString("messages.toggle-off")));
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 && sender instanceof Player) {
            Player player = (Player) sender;
            FPlayer fplayer = FPlayers.getInstance().getByPlayer(player);
            Faction fac = fplayer.getFaction();

            if (player.hasPermission("factionsfly.fly.bypass")) {
                togglePlayerFlight(player);
            } else if (fac == null || fac.isNone()) {
                sender.sendMessage(this.color(getConfig().getString("messages.need-faction")));
            } else if (!player.hasPermission("factionsfly.fly")) {
                sender.sendMessage(this.color(getConfig().getString("messages.permission")));
            } else {
                Relation rel = fplayer.getRelationToLocation();

                if (rel == Relation.MEMBER || (rel == Relation.ALLY && player.hasPermission("factionsfly.fly.ally"))) {
                    togglePlayerFlight(player);

                    if (player.getAllowFlight() && econ != null) {
                        double charge = getConfig().getDouble("toggle-cost");

                        if (charge > 0 && !Double.isNaN(charge)) {
                            if (econ.getBalance(player) > charge) {
                                econ.withdrawPlayer(player, charge);
                                player.sendMessage(this.color(getConfig().getString("messages.money-taken").replaceAll("\\[money\\]", Double.toString(charge))));
                            } else {
                                player.sendMessage(this.color(getConfig().getString("messages.money")));
                            }
                        }
                    }
                } else {
                    player.sendMessage(this.color(getConfig().getString("messages.cant-fly")));
                }
            }
        } else if (!sender.hasPermission("factionsfly.fly.other")) {
            sender.sendMessage(this.color(getConfig().getString("messages.permission")));
        } else {
            Player target = Bukkit.getPlayer(args[0]);

            if (target == null) {
                sender.sendMessage(this.color(getConfig().getString("messages.target-missing")));
            } else {
                sender.sendMessage(this.color(getConfig().getString("messages.target-toggled").replaceAll("\\[player\\]", target.getName())));
                togglePlayerFlight(target);
            }
        }
        return false;
    }

    /*
     * Toggles a players flight
     */
    private void togglePlayerFlight(Player player) {
        if (player.getAllowFlight()) {
            player.setAllowFlight(false);
            player.sendMessage(this.color(getConfig().getString("messages.toggle-off")));
        } else {
            player.setAllowFlight(true);
            player.sendMessage(this.color(getConfig().getString("messages.toggle-on")));
        }
    }

    /*
     * Color a configuration message
     */
    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
