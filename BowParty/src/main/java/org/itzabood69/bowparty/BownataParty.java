package org.itzabood69.bowparty;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BownataParty extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private File configFile;
    private final Map<UUID, BowmateData> activeBowmates = new HashMap<>();
    private final Random random = new Random();
    private boolean adShown = false;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Integer> playerLuck = new HashMap<>();
    private final Map<UUID, Integer> leaderboard = new HashMap<>();
    private final Map<UUID, Long> lastHitTimes = new HashMap<>();

    @Override
    public void onEnable() {
        // Create data folder
        if (!getDataFolder().exists()) {
            if (!getDataFolder().mkdirs()) {
                getLogger().severe("Failed to create plugin directory!");
                return;
            }
        }

        // Load configuration
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        reloadConfig();

        // Register events and commands
        getServer().getPluginManager().registerEvents(this, this);

        // Get command with aliases using PluginCommand
        PluginCommand command = getCommand("bownata");
        if (command != null) {
            command.setExecutor(this);
        } else {
            // Try aliases if main command not found
            command = getCommand("bp");
            if (command != null) {
                command.setExecutor(this);
            } else {
                command = getCommand("bowparty");
                if (command != null) {
                    command.setExecutor(this);
                } else {
                    getLogger().severe("Failed to register command! Check plugin.yml");
                }
            }
        }

        // Ad scheduler
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!adShown) {
                    Bukkit.broadcastMessage(ChatColor.GOLD + "[Bownata] " + ChatColor.GREEN +
                            "This server uses Bownata Party v1.5 by " + ChatColor.RED + "ItzAbood69");
                    adShown = true;
                }
            }
        }.runTaskTimer(this, 100, 20 * 60 * 10);
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    @Override
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            getLogger().severe("Could not save config: " + e.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String commandName = cmd.getName().toLowerCase();
        if (!commandName.equals("bownata") && !commandName.equals("bp") && !commandName.equals("bowparty")) {
            return false;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                return true;
            case "spawn":
                handleSpawn(sender);
                return true;
            case "luck":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " luck <player> <amount>");
                    return true;
                }
                return handleLuck(sender, args[1], args[2]);
            case "clone":
                handleClone(sender);
                return true;
            case "help":
                sendHelp(sender);
                return true;
            case "setmob":
                return handleSetMob(sender, args);
            case "sethits":
                return handleSetHits(sender, args);
            case "setcooldown":
                return handleSetCooldown(sender, args);
            case "setbossbar":
                handleSetBossBar(sender, args);
                return true;
            case "setdisplayname":
                handleSetDisplayName(sender, args);
                return true;
            case "seteffects":
                return handleSetEffects(sender, args);
            case "setrewards":
                return handleSetRewards(sender, args);
            case "setmovement":
                return handleSetMovement(sender, args);
            case "list":
                handleList(sender);
                return true;
            case "remove":
                return handleRemove(sender, args);
            case "tp":
                return handleTeleport(sender, args);
            case "giveitem":
                handleGiveItem(sender, args);
                return true;
            case "resetluck":
                return handleResetLuck(sender, args);
            case "top":
                return handleTop(sender);
            case "info":
                return handleInfo(sender);
            case "regen":
                return handleRegen(sender, args);
            case "setcolor":
                return handleSetColor(sender, args);
            case "setglow":
                return handleSetGlow(sender, args);
            case "setad":
                handleSetAd(sender, args);
                return true;
            case "setglobal":
                return handleSetGlobal(sender, args);
            case "sethitrewards":
                return handleSetHitRewards(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Bownata Party " + ChatColor.GREEN + "v1.5");
        sender.sendMessage(ChatColor.YELLOW + "Commands:");
        sender.sendMessage(ChatColor.GREEN + "/bownata spawn " + ChatColor.GRAY + "- Spawn a Bownata");
        sender.sendMessage(ChatColor.GREEN + "/bownata reload " + ChatColor.GRAY + "- Reload configuration");
        sender.sendMessage(ChatColor.GREEN + "/bownata luck <player> <amount> " + ChatColor.GRAY + "- Set player luck");
        sender.sendMessage(ChatColor.GREEN + "/bownata clone " + ChatColor.GRAY + "- Clone nearby Bownata");
        sender.sendMessage(ChatColor.GREEN + "/bownata setmob <type> " + ChatColor.GRAY + "- Set default mob type");
        sender.sendMessage(ChatColor.GREEN + "/bownata sethits <number> " + ChatColor.GRAY + "- Set required hits");
        sender.sendMessage(ChatColor.GREEN + "/bownata setcooldown <seconds> " + ChatColor.GRAY + "- Set spawn cooldown");
        sender.sendMessage(ChatColor.GREEN + "/bownata setbossbar <title> " + ChatColor.GRAY + "- Set bossbar title");
        sender.sendMessage(ChatColor.GREEN + "/bownata setdisplayname <name> " + ChatColor.GRAY + "- Set display name");
        sender.sendMessage(ChatColor.GREEN + "/bownata seteffects <chance> <power> " + ChatColor.GRAY + "- Configure hit effects");
        sender.sendMessage(ChatColor.GREEN + "/bownata setrewards <xp> <items> " + ChatColor.GRAY + "- Configure rewards");
        sender.sendMessage(ChatColor.GREEN + "/bownata setmovement <speed> <jump> " + ChatColor.GRAY + "- Configure movement");
        sender.sendMessage(ChatColor.GREEN + "/bownata list " + ChatColor.GRAY + "- List active Bownatas");
        sender.sendMessage(ChatColor.GREEN + "/bownata remove <id> " + ChatColor.GRAY + "- Remove a Bownata");
        sender.sendMessage(ChatColor.GREEN + "/bownata tp <id> " + ChatColor.GRAY + "- Teleport to Bownata");
        sender.sendMessage(ChatColor.GREEN + "/bownata giveitem <player> " + ChatColor.GRAY + "- Give luck item");
        sender.sendMessage(ChatColor.GREEN + "/bownata resetluck <player> " + ChatColor.GRAY + "- Reset player luck");
        sender.sendMessage(ChatColor.GREEN + "/bownata top " + ChatColor.GRAY + "- Show leaderboard");
        sender.sendMessage(ChatColor.GREEN + "/bownata info " + ChatColor.GRAY + "- Show plugin info");
        sender.sendMessage(ChatColor.GREEN + "/bownata regen <amount> " + ChatColor.GRAY + "- Set health regen");
        sender.sendMessage(ChatColor.GREEN + "/bownata setcolor <color> " + ChatColor.GRAY + "- Set bossbar color");
        sender.sendMessage(ChatColor.GREEN + "/bownata setglow <true/false> " + ChatColor.GRAY + "- Toggle glow effect");
        sender.sendMessage(ChatColor.GREEN + "/bownata setad <message> " + ChatColor.GRAY + "- Set ad message");
        sender.sendMessage(ChatColor.GREEN + "/bownata setglobal <true/false> " + ChatColor.GRAY + "- Toggle global bossbar");
        sender.sendMessage(ChatColor.GREEN + "/bownata sethitrewards <chance> <item|cmd> <value> " + ChatColor.GRAY + "- Configure hit rewards");
        sender.sendMessage(ChatColor.GREEN + "/bownata help " + ChatColor.GRAY + "- Show this help");
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("bownata.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission!");
            return;
        }
        reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "Configuration reloaded!");
    }

    private void handleSpawn(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can spawn a Bownata!");
            return;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("bownata.spawn")) {
            player.sendMessage(ChatColor.RED + "You don't have permission!");
            return;
        }

        // Handle cooldown
        long cooldownTime = config.getLong("cooldown", 60) * 1000;
        if (cooldowns.containsKey(player.getUniqueId())) {
            long elapsed = System.currentTimeMillis() - cooldowns.get(player.getUniqueId());
            if (elapsed < cooldownTime && !player.hasPermission("bownata.bypass.cooldown")) {
                long secondsLeft = (cooldownTime - elapsed) / 1000;
                String msg = config.getString("messages.cooldown", "&cPlease wait %time% seconds!");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        msg.replace("%time%", String.valueOf(secondsLeft))));
                return;
            }
        }

        spawnBowmate(player.getLocation(), player);
        player.sendMessage(ChatColor.GREEN + "Bownata spawned!");
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private boolean handleLuck(CommandSender sender, String playerName, String amountStr) {
        if (!sender.hasPermission("bownata.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission!");
            return true;
        }

        Player target = getServer().getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        try {
            int amount = Integer.parseInt(amountStr);
            playerLuck.put(target.getUniqueId(), amount);
            sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s luck to " + amount);
            sendActionBar(target, ChatColor.GOLD + "Your luck is now " + amount + "!");
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format!");
            return false;
        }
    }

    private void handleClone(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("bownata.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission!");
            return;
        }

        // Find nearest Bownata
        BowmateData nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (BowmateData data : activeBowmates.values()) {
            double distance = player.getLocation().distance(data.getEntity().getLocation());
            if (distance < minDistance && distance < 20) {
                minDistance = distance;
                nearest = data;
            }
        }

        if (nearest == null) {
            player.sendMessage(ChatColor.RED + "No Bownata found nearby!");
            return;
        }

        spawnBowmate(nearest.getEntity().getLocation(), player);
        player.sendMessage(ChatColor.GREEN + "Bownata cloned!");
    }

    private boolean handleSetMob(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bownata.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /bownata setmob <entityType>");
            return true;
        }

        try {
            EntityType type = EntityType.valueOf(args[1].toUpperCase());
            config.set("mob-type", type.name());
            saveConfig();
            sender.sendMessage(ChatColor.GREEN + "Default mob type set to " + type.name());
            return true;
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid entity type! Valid types: " +
                    Arrays.toString(EntityType.values()));
            return false;
        }
    }

    private boolean handleSetHits(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bownata.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /bownata sethits <number>");
            return true;
        }

        try {
            int hits = Integer.parseInt(args[1]);
            config.set("required-hits", hits);
            saveConfig();
            sender.sendMessage(ChatColor.GREEN + "Required hits set to " + hits);
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format!");
            return false;
        }
    }

    private boolean handleSetCooldown(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bownata.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /bownata setcooldown <seconds>");
            return true;
        }

        try {
            int seconds = Integer.parseInt(args[1]);
            config.set("cooldown", seconds);
            saveConfig();
            sender.sendMessage(ChatColor.GREEN + "Cooldown set to " + seconds + " seconds");
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format!");
            return false;
        }
    }

    private void handleSetBossBar(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bownata.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /bownata setbossbar <title>");
            return;
        }

        StringBuilder title = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            title.append(args[i]).append(" ");
        }

        config.set("bossbar-title", title.toString().trim());
        saveConfig();
        sender.sendMessage(ChatColor.GREEN + "Bossbar title set to: " + title.toString().trim());
    }

    private void handleSetDisplayName(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bownata.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /bownata setdisplayname <name>");
            return;
        }

        StringBuilder name = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            name.append(args[i]).append(" ");
        }

        config.set("display-name", name.toString().trim());
        saveConfig();
        sender.sendMessage(ChatColor.GREEN + "Display name set to: " + name.toString().trim());
    }

    private boolean handleSetEffects(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bownata.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission!");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /bownata seteffects <jump-chance> <jump-power>");
            return true;
        }

        try {
            double chance = Double.parseDouble(args[1]);
            double power = Double.parseDouble(args[2]);

            config.set("on-hit.big-jump.chance", chance);
            config.set("on-hit.big-jump.power", power);
            saveConfig();

            sender.sendMessage(ChatColor.GREEN + "Hit effects configured: " + chance + "% chance, " + power + " power");
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format!");
            return false;
        }
    }

    private boolean handleSetRewards(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bownata.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission!");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /bownata setrewards <xp> <items>");
            sender.sendMessage(ChatColor.GRAY + "Example: /bownata setrewards 50 DIAMOND,EMERALD,GOLD_INGOT");
            return true;
        }

        try {
            int xp = Integer.parseInt(args[1]);
            String[] items = args[2].split(",");

            config.set("final-reward.xp", xp);
            config.set("final-reward.items", Arrays.asList(items));
            saveConfig();

            sender.sendMessage(ChatColor.GREEN + "Rewards configured: " + xp + " XP, Items: " + Arrays.toString(items));
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format for XP!");
            return false;
        }
    }

    private boolean handleSetMovement(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bownata.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission!");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /bownata setmovement <speed> <jump-chance>");
            return true;
        }

        try {
            double speed = Double.parseDouble(args[1]);
            double jumpChance = Double.parseDouble(args[2]);

            config.set("movement.speed", speed);
            config.set("movement.jump-chance", jumpChance);
            saveConfig();

            sender.sendMessage(ChatColor.GREEN + "Movement configured: Speed " + speed + ", Jump chance " + jumpChance);
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format!");
            return false;
        }
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("bownata.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission!");
            return;
        }

        if (activeBowmates.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No active Bownatas");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "Active Bownatas (" + activeBowmates.size() + "):");
        int i = 1;
        for (BowmateData data : activeBowmates.values()) {
            Location loc = data.getEntity().getLocation();
            String world = loc.getWorld() != null ? loc.getWorld().getName() : "Unknown";
            String coords = String.format("%d, %d, %d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            String progress = String.format("%.1f%%", (1.0 - data.getBossBar().getProgress()) * 100);
            sender.sendMessage(ChatColor.GREEN + String.valueOf(i++) + ". " +
                    ChatColor.YELLOW + "ID: " + data.getEntity().getUniqueId() +
                    ChatColor.GRAY + " | " + ChatColor.AQUA + world +
                    ChatColor.WHITE + " @ " + ChatColor.GREEN + coords +
                    ChatColor.GRAY + " | " + ChatColor.LIGHT_PURPLE + progress + " damaged");
        }
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bownata.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /bownata remove <id>");
            return true;
        }

        try {
            UUID id = UUID.fromString(args[1]);
            BowmateData data = activeBowmates.get(id);
            if (data == null) {
                sender.sendMessage(ChatColor.RED + "Bownata not found!");
                return true;
            }

            // Clean up
            data.getBossBar().removeAll();
            if (data.getMovementTask() != null) {
                data.getMovementTask().cancel();
            }
            if (data.getRegenTask() != null) {
                data.getRegenTask().cancel();
            }
            data.getEntity().remove();
            activeBowmates.remove(id);

            sender.sendMessage(ChatColor.GREEN + "Bownata removed!");
            return true;
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid UUID format!");
            return false;
        }
    }

    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("bownata.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission!");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /bownata tp <id>");
            return true;
        }

        try {
            UUID id = UUID.fromString(args[1]);
            BowmateData data = activeBowmates.get(id);
            if (data == null) {
                player.sendMessage(ChatColor.RED + "Bownata not found!");
                return true;
            }

            player.teleport(data.getEntity().getLocation());
            player.sendMessage(ChatColor.GREEN + "Teleported to Bownata!");
            return true;
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid UUID format!");
            return false;
        }
    }

    private void handleGiveItem(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bownata.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /bownata giveitem <player>");
            return;
        }

        Player target = getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return;
        }

        Material luckItem = Material.matchMaterial(config.getString("luck.item", "GOLD_NUGGET"));
        if (luckItem == null) luckItem = Material.GOLD_NUGGET;

        ItemStack item = new ItemStack(luckItem);
        target.getInventory().addItem(item);
        sender.sendMessage(ChatColor.GREEN + "Gave luck item to " + target.getName());
    }

    private boolean handleResetLuck(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bownata.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /bownata resetluck <player>");
            return true;
        }

        Player target = getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        playerLuck.remove(target.getUniqueId());
        sender.sendMessage(ChatColor.GREEN + "Reset luck for " + target.getName());
        sendActionBar(target, ChatColor.RED + "Your luck has been reset!");
        return true;
    }

    private boolean handleTop(CommandSender sender) {
        if (leaderboard.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No hits recorded yet!");
            return true;
        }

        // Sort leaderboard
        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(leaderboard.entrySet());
        sorted.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        sender.sendMessage(ChatColor.GOLD + "Bownata Leaderboard:");
        for (int i = 0; i < Math.min(10, sorted.size()); i++) {
            Map.Entry<UUID, Integer> entry = sorted.get(i);
            Player player = getServer().getPlayer(entry.getKey());
            String name = player != null ? player.getName() : "Unknown";
            sender.sendMessage(ChatColor.GREEN + String.valueOf(i+1) + ". " +
                    ChatColor.AQUA + name + ChatColor.GRAY + " - " +
                    ChatColor.YELLOW + entry.getValue() + " hits");
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Bownata Party v1.5");
        sender.sendMessage(ChatColor.YELLOW + "Created by ItzAbood69");
        sender.sendMessage(ChatColor.GREEN + "Active Bownatas: " + activeBowmates.size());
        sender.sendMessage(ChatColor.AQUA + "Total hits recorded: " + leaderboard.values().stream().mapToInt(Integer::intValue).sum());
        return true;
    }

    private boolean handleRegen(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bownata.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /bownata regen <amount>");
            return true;
        }

        try {
            int amount = Integer.parseInt(args[1]);
            config.set("regen.amount", amount);
            saveConfig();
            sender.sendMessage(ChatColor.GREEN + "Health regen set to " + amount + " hits per minute");
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format!");
            return false;
        }
    }

    private boolean handleSetColor(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bownata.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /bownata setcolor <color>");
            sender.sendMessage(ChatColor.GRAY + "Available colors: PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE");
            return true;
        }

        try {
            BarColor color = BarColor.valueOf(args[1].toUpperCase());
            config.set("bossbar-color", color.name());
            saveConfig();
            sender.sendMessage(ChatColor.GREEN + "Bossbar color set to " + color.name());
            return true;
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid color! Use one of: " +
                    Arrays.toString(BarColor.values()));
            return false;
        }
    }

    private boolean handleSetGlow(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bownata.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /bownata setglow <true/false>");
            return true;
        }

        boolean glow = Boolean.parseBoolean(args[1]);
        config.set("glow-effect", glow);
        saveConfig();
        sender.sendMessage(ChatColor.GREEN + "Glow effect " + (glow ? "enabled" : "disabled"));
        return true;
    }

    private void handleSetAd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bownata.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /bownata setad <message>");
            return;
        }

        StringBuilder ad = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            ad.append(args[i]).append(" ");
        }

        config.set("ad-message", ad.toString().trim());
        saveConfig();
        sender.sendMessage(ChatColor.GREEN + "Ad message set to: " + ad.toString().trim());
    }

    private boolean handleSetGlobal(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bownata.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /bownata setglobal <true/false>");
            return true;
        }

        boolean global = Boolean.parseBoolean(args[1]);
        config.set("global-bossbar", global);
        saveConfig();
        sender.sendMessage(ChatColor.GREEN + "Global bossbar " + (global ? "enabled" : "disabled"));
        return true;
    }

    private boolean handleSetHitRewards(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bownata.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission!");
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /bownata sethitrewards <chance> <item|cmd> <value>");
            sender.sendMessage(ChatColor.GRAY + "Examples:");
            sender.sendMessage(ChatColor.GRAY + "/bownata sethitrewards 0.25 item DIAMOND");
            sender.sendMessage(ChatColor.GRAY + "/bownata sethitrewards 0.1 cmd \"give %player% diamond 1\"");
            return true;
        }

        try {
            double chance = Double.parseDouble(args[1]);
            String type = args[2].toLowerCase();
            String value = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

            // Validate type
            if (!type.equals("item") && !type.equals("cmd")) {
                sender.sendMessage(ChatColor.RED + "Invalid type! Must be 'item' or 'cmd'");
                return false;
            }

            // Create reward section
            ConfigurationSection hitRewards = config.getConfigurationSection("hit-rewards");
            if (hitRewards == null) {
                hitRewards = config.createSection("hit-rewards");
            }

            // Get next available ID
            int id = 1;
            while (hitRewards.contains("reward-" + id)) {
                id++;
            }

            // Create reward entry
            ConfigurationSection reward = hitRewards.createSection("reward-" + id);
            reward.set("chance", chance);
            reward.set("type", type);
            reward.set("value", value);

            saveConfig();
            sender.sendMessage(ChatColor.GREEN + "Added hit reward: " + chance + " chance, " + type + " reward");
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid chance format! Use decimal (e.g., 0.25)");
            return false;
        }
    }

    private void spawnBowmate(Location location, Player spawner) {
        // Get mob type
        String mobTypeStr = config.getString("mob-type", "SHEEP");
        EntityType type;
        try {
            type = EntityType.valueOf(mobTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = EntityType.SHEEP;
        }

        // Spawn entity
        Entity entity = location.getWorld().spawnEntity(location, type);
        entity.setCustomName(ChatColor.translateAlternateColorCodes('&',
                config.getString("display-name", "&e&lBownata - Hit with Arrows!")));
        entity.setCustomNameVisible(true);
        entity.setInvulnerable(true);

        // Glow effect
        if (config.getBoolean("glow-effect", true)) {
            entity.setGlowing(true);
        }

        // Apply speed if configured
        if (config.contains("mob-speed") && entity instanceof LivingEntity) {
            double speed = config.getDouble("mob-speed", 0.25);
            ((LivingEntity) entity).getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
        }

        // Create boss bar
        BarColor barColor;
        try {
            barColor = BarColor.valueOf(config.getString("bossbar-color", "YELLOW").toUpperCase());
        } catch (IllegalArgumentException e) {
            barColor = BarColor.YELLOW;
        }

        BossBar bossBar = Bukkit.createBossBar(
                ChatColor.translateAlternateColorCodes('&', config.getString("bossbar-title", "&eBownata")),
                barColor,
                BarStyle.SOLID
        );
        bossBar.setVisible(true);
        bossBar.setProgress(1.0);

        // Track bowmate
        BowmateData data = new BowmateData(entity, bossBar, config.getInt("required-hits", 15));
        activeBowmates.put(entity.getUniqueId(), data);

        // Add players to boss bar
        for (Player player : entity.getWorld().getPlayers()) {
            if (config.getBoolean("global-bossbar", false) ||
                    player.getLocation().distanceSquared(entity.getLocation()) <= 2500) {
                bossBar.addPlayer(player);
            }
        }

        // Start movement task
        if (config.getBoolean("movement.enabled", true)) {
            startEnhancedMovement(data);
        }

        // Start regeneration task
        if (config.getInt("regen.amount", 0) > 0) {
            startRegenTask(data);
        }

        // Send ad
        String adMsg = config.getString("ad-message", "BownataParty v1.5 by ItzAbood69");
        spawner.sendMessage(ChatColor.translateAlternateColorCodes('&', adMsg));
    }

    private void startEnhancedMovement(BowmateData data) {
        BukkitRunnable movementTask = new BukkitRunnable() {
            private int jumpCooldown = 0;
            private int directionChangeCooldown = 0;
            private Vector currentDirection = new Vector(
                    (random.nextDouble() - 0.5) * 0.5,
                    0,
                    (random.nextDouble() - 0.5) * 0.5
            ).normalize();

            @Override
            public void run() {
                if (!data.getEntity().isValid() || data.getHits() >= data.getRequiredHits()) {
                    cancel();
                    return;
                }

                Entity entity = data.getEntity();
                Location loc = entity.getLocation();
                double baseSpeed = config.getDouble("movement.speed", 0.35);
                double speedMultiplier = config.getDouble("movement.speed-multiplier", 1.2);
                double jumpChance = config.getDouble("movement.jump-chance", 0.25);
                double jumpPower = config.getDouble("movement.jump-power", 0.65);

                // Change direction periodically
                if (directionChangeCooldown <= 0) {
                    currentDirection = new Vector(
                            (random.nextDouble() - 0.5) * 2,
                            0,
                            (random.nextDouble() - 0.5) * 2
                    ).normalize();
                    directionChangeCooldown = random.nextInt(40) + 20;
                } else {
                    directionChangeCooldown--;
                }

                // Apply movement
                Vector velocity = entity.getVelocity();
                velocity.add(currentDirection.clone().multiply(baseSpeed * speedMultiplier));

                // Jump logic
                if (jumpCooldown <= 0 && entity.isOnGround() && random.nextDouble() < jumpChance) {
                    velocity.setY(jumpPower);
                    jumpCooldown = random.nextInt(30) + 20;
                    loc.getWorld().spawnParticle(Particle.CLOUD, loc, 10, 0.2, 0.2, 0.2, 0.02);
                } else if (jumpCooldown > 0) {
                    jumpCooldown--;
                }

                // Apply slight random vertical movement
                velocity.setY(velocity.getY() + ((random.nextDouble() - 0.5) * 0.1));

                entity.setVelocity(velocity);

                // Visual effects for movement
                if (random.nextDouble() < 0.3) {
                    loc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc.add(0, 0.5, 0), 3, 0.2, 0.2, 0.2, 0.01);
                }
            }
        };

        movementTask.runTaskTimer(this, 0, config.getInt("movement.interval", 15));
        data.setMovementTask(movementTask);
    }

    private void startRegenTask(BowmateData data) {
        BukkitRunnable regenTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!data.getEntity().isValid() || data.getHits() >= data.getRequiredHits()) {
                    cancel();
                    return;
                }

                // Don't regen if recently hit
                Long lastHit = lastHitTimes.get(data.getEntity().getUniqueId());
                if (lastHit != null && System.currentTimeMillis() - lastHit < 15000) {
                    return;
                }

                int regenAmount = config.getInt("regen.amount", 1);
                if (regenAmount > 0 && data.getHits() > 0) {
                    int newHits = Math.max(0, data.getHits() - regenAmount);
                    data.setHits(newHits);

                    // Update boss bar
                    double progress = 1.0 - ((double) newHits / data.getRequiredHits());
                    data.getBossBar().setProgress(progress);

                    // Regen effect
                    Location loc = data.getEntity().getLocation();
                    loc.getWorld().spawnParticle(Particle.HEART, loc.add(0, 1, 0), 5, 0.5, 0.5, 0.5);
                }
            }
        };

        regenTask.runTaskTimer(this, 20 * 30, 20 * 30); // Run every 30 seconds
        data.setRegenTask(regenTask);
    }

    // Version-safe action bar sender
    private void sendActionBar(Player player, String message) {
        try {
            player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message))
            );
        } catch (NoSuchMethodError e) {
            // Fallback for older versions
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        BowmateData data = activeBowmates.get(entity.getUniqueId());
        if (data == null) return;

        if (event.getDamager() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getDamager();
            ProjectileSource source = arrow.getShooter();
            if (source instanceof Player) {
                Player player = (Player) source;

                if (!player.hasPermission("bownata.hit")) {
                    player.sendMessage(ChatColor.RED + "No permission!");
                    event.setCancelled(true);
                    return;
                }

                event.setCancelled(true);
                onBowmateHit(data, player);
                arrow.remove();
            }
        } else if (event.getDamager() instanceof Player) {
            event.setCancelled(true);
            ((Player) event.getDamager()).sendMessage(ChatColor.RED + "You Can just hi t by Useing arrows!");
        }
    }

    private void onBowmateHit(BowmateData data, Player player) {
        // Track last hit time for regeneration
        lastHitTimes.put(data.getEntity().getUniqueId(), System.currentTimeMillis());

        // Update hit count
        data.setHits(data.getHits() + 1);
        int hits = data.getHits();
        int required = data.getRequiredHits();

        // Update leaderboard
        leaderboard.put(player.getUniqueId(), leaderboard.getOrDefault(player.getUniqueId(), 0) + 1);

        // Update boss bar
        double progress = 1.0 - ((double) hits / required);
        data.getBossBar().setProgress(progress);
        data.getBossBar().setTitle(ChatColor.translateAlternateColorCodes('&',
                config.getString("bossbar-title", "&eBownata") + " &7" + hits + "&8/&7" + required));

        // Play sound
        player.playSound(data.getEntity().getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1, 1);

        // RANDOM BIG JUMP (10% chance)
        if (random.nextDouble() < 0.10) {
            double power = config.getDouble("on-hit.big-jump.power", 1.5);
            Vector velocity = data.getEntity().getVelocity();
            velocity.setY(power);
            data.getEntity().setVelocity(velocity);

            // Effects
            Location loc = data.getEntity().getLocation();
            loc.getWorld().spawnParticle(Particle.CLOUD, loc, 30);
            loc.getWorld().playSound(loc, Sound.ENTITY_BAT_TAKEOFF, 1, 0.5f);
            sendActionBar(player, ChatColor.GOLD + "BIG JUMP!");
        }

        // Apply luck effects
        applyLuckSystem(data, player);

        // HIT REWARDS
        giveHitRewards(player, data.getEntity().getLocation());

        // Check if destroyed
        if (hits >= required) {
            destroyBowmate(data, player);
        }
    }

    private void applyLuckSystem(BowmateData data, Player player) {
        Location loc = data.getEntity().getLocation();
        double luckMultiplier = 1.0;

        if (playerLuck.containsKey(player.getUniqueId())) {
            int luck = playerLuck.get(player.getUniqueId());
            luckMultiplier = 1.0 + (luck * config.getDouble("luck.multiplier-per-point", 0.05));
        }

        // Rare effect
        if (random.nextDouble() * 100 < config.getDouble("on-hit.rare.chance", 1.0) * luckMultiplier) {
            String command = config.getString("on-hit.rare.command", "give %player% golden_apple 1");
            command = command.replace("%player%", player.getName());
            getServer().dispatchCommand(getServer().getConsoleSender(), command);
            sendActionBar(player, ChatColor.GOLD + "Lucky hit! +3 Luck ✨");
        }
    }

    private void giveHitRewards(Player player, Location loc) {
        ConfigurationSection hitRewards = config.getConfigurationSection("hit-rewards");
        if (hitRewards == null) return;

        for (String key : hitRewards.getKeys(false)) {
            ConfigurationSection reward = hitRewards.getConfigurationSection(key);
            if (reward == null) continue;

            double chance = reward.getDouble("chance", 0.0);
            String type = reward.getString("type", "");
            String value = reward.getString("value", "");

            if (random.nextDouble() > chance) continue;

            if ("item".equalsIgnoreCase(type)) {
                Material material = Material.matchMaterial(value);
                if (material != null) {
                    ItemStack rewardItem = new ItemStack(material);
                    Item item = loc.getWorld().dropItem(loc, rewardItem);
                    item.setVelocity(new Vector(
                            (random.nextDouble() - 0.5) * 0.5,
                            random.nextDouble() * 0.75,
                            (random.nextDouble() - 0.5) * 0.5
                    ));

                    // Visual effects
                    loc.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, loc, 15, 0.5, 0.5, 0.5, 0.15);
                    player.playSound(loc, Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.5f);
                }
            }
            else if ("cmd".equalsIgnoreCase(type)) {
                String command = value.replace("%player%", player.getName());
                getServer().dispatchCommand(getServer().getConsoleSender(), command);
                player.sendMessage(ChatColor.GOLD + "Reward command executed!");
            }
        }
    }

    private void destroyBowmate(BowmateData data, Player breaker) {
        Entity entity = data.getEntity();
        Location loc = entity.getLocation();

        // Clean up
        data.getBossBar().removeAll();
        if (data.getMovementTask() != null) {
            data.getMovementTask().cancel();
        }
        if (data.getRegenTask() != null) {
            data.getRegenTask().cancel();
        }
        entity.remove();
        activeBowmates.remove(entity.getUniqueId());

        // Final rewards with professional scattering
        distributeFinalRewards(loc, breaker);

        // SPECIAL BREAK EFFECTS
        loc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, loc, 1);
        loc.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, loc, 100, 1, 1, 1, 0.5);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);

        // Broadcast
        String message = config.getString("final-reward.message", "&e🎉 %player% broke the Bownata!");
        message = ChatColor.translateAlternateColorCodes('&', message.replace("%player%", breaker.getName()));
        Bukkit.broadcastMessage(message);

        // Ad
        String adMsg = config.getString("ad-message", "BownataParty v1.5 by ItzAbood69");
        breaker.sendMessage(ChatColor.translateAlternateColorCodes('&', adMsg));
    }

    private void distributeFinalRewards(Location center, Player breaker) {
        // XP
        int xp = config.getInt("final-reward.xp", 30);
        ExperienceOrb orb = center.getWorld().spawn(center, ExperienceOrb.class);
        orb.setExperience(xp);
        orb.setVelocity(new Vector(0, 0.5, 0));

        // Items with scattering effect
        List<String> items = config.getStringList("final-reward.items");
        double radius = 2.5;

        for (int i = 0; i < items.size(); i++) {
            Material material = Material.matchMaterial(items.get(i));
            if (material == null) continue;

            double angle = 2 * Math.PI * i / items.size();
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location dropLoc = new Location(center.getWorld(), x, center.getY() + 1, z);

            Item item = dropLoc.getWorld().dropItem(dropLoc, new ItemStack(material));
            item.setVelocity(new Vector(
                    (random.nextDouble() - 0.5) * 0.5,
                    random.nextDouble() * 0.75 + 0.25,
                    (random.nextDouble() - 0.5) * 0.5
            ));
        }

        // Fireworks
        if (config.getBoolean("final-reward.fireworks", true)) {
            for (int i = 0; i < 7; i++) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Firework firework = (Firework) center.getWorld().spawnEntity(
                                center.clone().add(
                                        (random.nextDouble() - 0.5) * 3,
                                        1,
                                        (random.nextDouble() - 0.5) * 3
                                ),
                                EntityType.FIREWORK
                        );
                        FireworkMeta meta = firework.getFireworkMeta();
                        meta.addEffect(FireworkEffect.builder()
                                .withColor(Color.RED, Color.YELLOW, Color.ORANGE, Color.GREEN, Color.BLUE)
                                .with(FireworkEffect.Type.BURST)
                                .withFlicker()
                                .withTrail()
                                .build());
                        meta.setPower(1);
                        firework.setFireworkMeta(meta);
                    }
                }.runTaskLater(this, i * 5);
            }
        }

        // Commands
        for (String command : config.getStringList("final-reward.commands")) {
            command = command.replace("%player%", breaker.getName());
            getServer().dispatchCommand(getServer().getConsoleSender(), command);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!adShown) {
            String adMsg = config.getString("ad-message", "BownataParty v1.5 by ItzAbood69");
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', adMsg));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction().name().contains("RIGHT_CLICK")) {
            ItemStack item = event.getItem();
            if (item != null) {
                Material luckItem = Material.matchMaterial(config.getString("luck.item", "GOLD_NUGGET"));
                if (luckItem == null) luckItem = Material.GOLD_NUGGET;

                if (item.getType() == luckItem) {
                    Player player = event.getPlayer();
                    if (player.hasPermission("bownata.luck.use")) {
                        int currentLuck = playerLuck.getOrDefault(player.getUniqueId(), 0);
                        int add = config.getInt("luck.item-value", 5);
                        playerLuck.put(player.getUniqueId(), currentLuck + add);

                        sendActionBar(player, ChatColor.YELLOW + "Luck: " + (currentLuck + add) + " ✨");
                        item.setAmount(item.getAmount() - 1);
                    }
                }
            }
        }
    }

    private static class BowmateData {
        private final Entity entity;
        private final BossBar bossBar;
        private final int requiredHits;
        private int hits;
        private BukkitRunnable movementTask;
        private BukkitRunnable regenTask;

        public BowmateData(Entity entity, BossBar bossBar, int requiredHits) {
            this.entity = entity;
            this.bossBar = bossBar;
            this.requiredHits = requiredHits;
            this.hits = 0;
        }

        public Entity getEntity() { return entity; }
        public BossBar getBossBar() { return bossBar; }
        public int getHits() { return hits; }
        public void setHits(int hits) { this.hits = hits; }
        public int getRequiredHits() { return requiredHits; }
        public BukkitRunnable getMovementTask() { return movementTask; }
        public void setMovementTask(BukkitRunnable task) { this.movementTask = task; }
        public BukkitRunnable getRegenTask() { return regenTask; }
        public void setRegenTask(BukkitRunnable task) { this.regenTask = task; }
    }
}