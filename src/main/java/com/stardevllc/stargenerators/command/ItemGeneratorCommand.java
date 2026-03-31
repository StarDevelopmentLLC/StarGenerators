package com.stardevllc.stargenerators.command;

import com.stardevllc.Position;
import com.stardevllc.colors.StarColorsV2;
import com.stardevllc.command.flags.CmdFlags;
import com.stardevllc.command.flags.FlagResult;
import com.stardevllc.command.flags.type.PresenceFlag;
import com.stardevllc.command.params.*;
import com.stardevllc.itembuilder.common.ItemBuilder;
import com.stardevllc.plugin.ExtendedJavaPlugin;
import com.stardevllc.registry.PluginKey;
import com.stardevllc.smaterial.SMaterial;
import com.stardevllc.stargenerators.model.ItemEntry;
import com.stardevllc.stargenerators.model.ItemEntry.Flag;
import com.stardevllc.stargenerators.model.ItemGenerator;
import com.stardevllc.staritems.ItemBuilders;
import com.stardevllc.starlib.objects.key.Key;
import com.stardevllc.starlib.objects.key.Keys;
import com.stardevllc.starlib.objects.key.impl.StringKey;
import com.stardevllc.starlib.time.TimeFormat;
import com.stardevllc.starlib.time.TimeParser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

import static com.stardevllc.stargenerators.StarGenerators.ITEM_GENERATORS;

public class ItemGeneratorCommand implements CommandExecutor, Listener {
    
    private static final TimeFormat TIME_FORMAT = new TimeFormat("%*##y%%*##mo%%*##d%%*##h%%*##m%%*##s%%*##t%%*##ms%");
    
    private ExtendedJavaPlugin plugin;
    
    /**
     * The generator selections based on each player <br>
     * It is stored as the generator id to make sure that memory leak chances are minimal
     */
    private Map<UUID, Key> selectedGenerators = new HashMap<>();
    private Map<UUID, Selection> positionSelection = new HashMap<>();
    
    private final CmdFlags cmdFlags;
    private final CmdParams cmdParams;
    
    private static final TimeFormat timeFormat = new TimeFormat("%*##h%%*##m%%*##s%%*##ms%");
    
    private static final class Flags {
        private static final PresenceFlag DEBUG = new PresenceFlag("d", "Debug");
        
        private static final class Create {
            private static final PresenceFlag SELECT = new PresenceFlag("se", "Select");
            private static final PresenceFlag INIT = new PresenceFlag("i", "Init");
            private static final PresenceFlag START = new PresenceFlag("st", "start");
        }
    }
    
    private static final class Params {
        private static final class Item {
            private static final Param<String> ID = Param.builder(String.class).id("id").name("ID").defaultValue("").build();
            private static final Param<String> MATERIAL = Param.builder(String.class).id("mat").name("Material").defaultValue(SMaterial.AIR.name()).build();
            private static final Param<String> COOLDOWN = Param.builder(String.class).id("cd").name("Cooldown").defaultValue("1ms").build();
            private static final Param<Boolean> INVULNERABLE = Param.builder(Boolean.class).id("iv").name("Invulnerable").defaultValue(true).build();
            private static final Param<Boolean> INVENTORY_PICKUP = Param.builder(Boolean.class).id("ip").name("Inventory Pickup").defaultValue(false).build();
            private static final Param<Boolean> PERSISTENT = Param.builder(Boolean.class).id("pst").name("Persistent").defaultValue(true).build();
            private static final Param<Integer> MAX_COUNT = Param.builder(Integer.class).id("mi").name("Max Items").defaultValue(64).build();
            private static final Param<Integer> STACK_SIZE = Param.builder(Integer.class).id("ss").name("Stack Size").defaultValue(1).build();
            
            private static final List<Param<?>> REQUIRED = List.of(MATERIAL);
        }
    }
    
    public ItemGeneratorCommand(ExtendedJavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
        
        this.cmdFlags = new CmdFlags(Flags.DEBUG, Flags.Create.SELECT, Flags.Create.INIT, Flags.Create.START);
        this.cmdParams = new CmdParams(Params.Item.ID, Params.Item.MATERIAL, Params.Item.COOLDOWN, Params.Item.INVULNERABLE, Params.Item.INVENTORY_PICKUP, Params.Item.PERSISTENT, Params.Item.MAX_COUNT, Params.Item.STACK_SIZE);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        StarColorsV2 colors = plugin.getColors();
        if (!sender.hasPermission("staritemgenerators.command.itemgenerators")) {
            colors.coloredLegacy(sender, "You do not have permission to perform that command.");
            return true;
        }
        
        FlagResult flagResults = cmdFlags.parse(args);
        args = flagResults.args();
        
        if (!(args.length > 0)) {
            colors.coloredLegacy(sender, "&cYou must provide a sub command.");
            return true;
        }
        
        if (!(sender instanceof Player player)) {
            colors.coloredLegacy(sender, "&cOnly players can use that command.");
            return true;
        }
        
        Location location = player.getLocation();
        
        if (args[0].toLowerCase().startsWith("pos")) {
            this.positionSelection.computeIfAbsent(player.getUniqueId(), u -> new Selection());
            Selection selection = this.positionSelection.get(player.getUniqueId());
            Location loc = player.getLocation().clone();
            int positionNumber;
            if (args[0].equalsIgnoreCase("pos1")) {
                selection.setPos1(loc);
                positionNumber = 1;
            } else if (args[0].equalsIgnoreCase("pos2")) {
                selection.setPos2(loc);
                positionNumber = 2;
            } else {
                colors.coloredLegacy(sender, "&cInvalid position command.");
                return true;
            }
            
            colors.coloredLegacy(sender, "&eSet position &b" + positionNumber + " &eto &b" + loc.getBlockX() + "&e, &b" + loc.getBlockY() + "&e, &b" + loc.getBlockZ() + "&e.");
            return true;
        } else if (args[0].equalsIgnoreCase("create")) {
            if (!(args.length > 1)) {
                colors.coloredLegacy(sender, "&cYou must provide an id for the generator");
                return true;
            }
            
            Key id = PluginKey.of(this.plugin, args[1]);
            
            if (ITEM_GENERATORS.containsKey(id)) {
                colors.coloredLegacy(sender, "&cA generator with that name already exists.");
                return true;
            }
            
            Selection selection = this.positionSelection.get(player.getUniqueId());
            if (selection == null) {
                colors.coloredLegacy(sender, "&cYou do not have a selection. Use /" + label + " pos<#> to create one.");
                return true;
            }
            
            if (selection.getPos1() == null || selection.getPos2() == null) {
                colors.coloredLegacy(sender, "&cYour selection does not have both postions defined.");
                return true;
            }
            
            if (!selection.getPos1().getWorld().getName().equalsIgnoreCase(selection.getPos2().getWorld().getName())) {
                colors.coloredLegacy(sender, "&cThe positions are not in the same world.");
                return true;
            }
            
            Position pos1 = new Position(selection.getPos1().getBlockX(), selection.getPos1().getBlockY(), selection.getPos1().getBlockZ());
            Position pos2 = new Position(selection.getPos2().getBlockX(), selection.getPos2().getBlockY(), selection.getPos2().getBlockZ());
            
            ItemGenerator itemGenerator = new ItemGenerator(id, new ArrayList<>(), pos1, pos2);
            
            ITEM_GENERATORS.register(itemGenerator.getKey(), itemGenerator);
            if (!ITEM_GENERATORS.containsKey(itemGenerator.getKey())) {
                colors.coloredLegacy(sender, "&cFailed to register the new item generator");
                return true;
            }
            
            colors.coloredLegacy(sender, "&eCreated an Item Generator with the id &b" + id);
            
            if (flagResults.isPresent(Flags.Create.INIT)) {
                itemGenerator.init(player.getWorld());
                colors.coloredLegacy(sender, "&eInitialized the Item Generator &b" + id + " &ein world &b" + player.getWorld().getName());
            }
            
            if (flagResults.isPresent(Flags.Create.START)) {
                itemGenerator.start();
                colors.coloredLegacy(sender, "&eStarted the Item Generator &b" + id);
            }
            
            if (flagResults.isPresent(Flags.Create.SELECT)) {
                selectedGenerators.put(player.getUniqueId(), id);
                colors.coloredLegacy(sender, "&eSelected the Item Generator &b" + id);
            }
            
            return true;
        } else if (args[0].equalsIgnoreCase("select")) {
            if (!(args.length > 1)) {
                colors.coloredLegacy(sender, "&cYou must provide an id for the generator to select");
                return true;
            }
            
            String genName = args[1].toLowerCase();
            ItemGenerator generator = ITEM_GENERATORS.get(genName);
            
            List<ItemGenerator> gens = new ArrayList<>();
            if (generator == null) {
                for (ItemGenerator itemGenerator : ITEM_GENERATORS) {
                    if (itemGenerator.getKey().contains(genName)) {
                        gens.add(itemGenerator);
                    }
                }
            }
            
            if (gens.size() == 1) {
                generator = gens.getFirst();
            } else if (gens.size() > 1) {
                colors.coloredLegacy(sender, "&cMultiple Generators match the partial name: \"" + genName + "\"");
                for (ItemGenerator gen : gens) {
                    colors.coloredLegacy(sender, " &8- &c" + gen.getKey());
                }
                return true;
            }
            
            if (generator == null) {
                colors.coloredLegacy(sender, "&cAn Item Generator with the id " + genName + " does not exist.");
                return true;
            }
            
            selectedGenerators.put(player.getUniqueId(), generator.getKey());
            colors.coloredLegacy(sender, "&eSelected the Item Generator &b" + generator.getKey());
            return true;
        } else if (args[0].equalsIgnoreCase("status")) {
            Key genId;
            if (args.length > 1) {
                genId = Keys.of(args[1]);
            } else {
                genId = this.selectedGenerators.get(player.getUniqueId());
            }
            
            if (genId == null && genId.isEmpty()) {
                colors.coloredLegacy(sender, "&cYou must provide a generator id or select a generator to use this command.");
                return true;
            }
            
            ItemGenerator generator = ITEM_GENERATORS.get(genId);
            if (generator == null) {
                colors.coloredLegacy(sender, "&cThe id " + genId + " does not match a valid generator.");
                return true;
            }
            
            List<String> lines = new LinkedList<>();
            lines.add("&6Information for Item Generator &b" + generator.getKey());
            lines.add("&eBounds: ");
            Position min = generator.getBoundsMin();
            lines.add("  &eMin: &b(" + min.getBlockX() + ", " + min.getBlockY() + ", " + min.getBlockZ() + ")");
            Position max = generator.getBoundsMin();
            lines.add("  &eMax: &b(" + max.getBlockX() + ", " + max.getBlockY() + ", " + max.getBlockZ() + ")");
            lines.add("&eInitialized: " + formatBoolean(generator.isInitialized()));
            lines.add("&eRunning: " + formatBoolean(generator.isRunning()));
            lines.add("&eWorld: &b" + (generator.getWorld() != null ? generator.getWorld().getName() : "None"));
            lines.add("&eSpawned Items: &b" + generator.getSpawnedItemsCount());
            lines.add("&eEntries: ");
            for (ItemEntry entry : generator.getEntries()) {
                lines.add("  &e" + entry.getKey() + ":");
                lines.add("    &eCooldown: &b" + timeFormat.format(entry.getCooldown()));
                lines.add("    &eMax items: &b" + entry.getMaxItems());
                List<String> flags = new ArrayList<>();
                for (Flag flag : entry.getFlags()) {
                    flags.add(flag.name().toLowerCase());
                }
                
                String flagsString = String.join(", ", flags);
                lines.add("    &eFlags: &b" + (flagsString.isBlank() ? "None" : flagsString));
                lines.add("    &eSpawned Items: &b" + generator.getSpawnedItemsCount(entry.getKey()));
                Position pos = entry.getSpawnPosition();
                lines.add("    &ePos: &b(" + pos.getBlockX() + ", " + pos.getBlockY() + ", " + pos.getBlockZ() + ")");
                lines.add("    &eNext Spawn: &b" + timeFormat.format(generator.getNextSpawn(entry)));
            }
            
            lines.forEach(line -> colors.coloredLegacy(sender, line));
            return true;
        }
        
        Key genId = this.selectedGenerators.get(player.getUniqueId());
        if (genId == null && genId.isEmpty()) {
            colors.coloredLegacy(sender, "&cYou must have a generator selection to use that command.");
            return true;
        }
        
        ItemGenerator generator = ITEM_GENERATORS.get(genId);
        if (generator == null) {
            colors.coloredLegacy(sender, "&cThe id " + genId + " does not match a valid generator.");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("init")) {
            if (generator.isInitialized()) {
                colors.coloredLegacy(sender, "&cGenerator " + generator.getKey() + " is already initialized.");
                return true;
            }
            
            generator.init(player.getWorld());
            colors.coloredLegacy(sender, "&eInitialized the Item Generator &b" + generator.getKey() + " &ein world &b" + player.getWorld().getName());
        } else if (args[0].equalsIgnoreCase("start")) {
            if (generator.isRunning()) {
                colors.coloredLegacy(sender, "&cGenerator " + generator.getKey() + " is already running.");
                return true;
            }
            
            generator.start();
            colors.coloredLegacy(sender, "&eStarted the Item Generator &b" + generator.getKey());
        } else if (args[0].equalsIgnoreCase("stop")) {
            if (!generator.isRunning()) {
                colors.coloredLegacy(sender, "&cGenerator " + generator.getKey() + " is not running.");
                return true;
            }
            
            generator.stop();
            colors.coloredLegacy(sender, "&eStopped the Item Generator &b" + generator.getKey());
        } else if (args[0].equalsIgnoreCase("additem")) {
            if (!(args.length > 1)) {
                colors.coloredLegacy(sender, "&cUsage: /" + label + " " + args[0] + " <params>");
                List<Param<?>> paramsList = List.of(Params.Item.ID, Params.Item.MATERIAL, Params.Item.COOLDOWN, Params.Item.INVULNERABLE, Params.Item.INVENTORY_PICKUP, Params.Item.PERSISTENT, Params.Item.MAX_COUNT, Params.Item.STACK_SIZE);
                
                List<Param<?>> requiredParams = new LinkedList<>(), optionalParams = new LinkedList<>();
                for (Param<?> param : paramsList) {
                    if (Params.Item.REQUIRED.contains(param)) {
                        requiredParams.add(param);
                    } else {
                        optionalParams.add(param);
                    }
                }
                
                if (!requiredParams.isEmpty()) {
                    colors.coloredLegacy(sender, "  &cRequired Parameters:");
                    for (Param<?> requiredParam : requiredParams) {
                        String aliases;
                        if (requiredParam.aliases() != null) {
                            aliases = ", aliases=" + String.join(", ", requiredParam.aliases());
                        } else {
                            aliases = "";
                        }
                        colors.coloredLegacy(sender, "    &8- &c" + requiredParam.id() + "(name=" + requiredParam.name() + aliases + ")");
                    }
                }
                
                if (!optionalParams.isEmpty()) {
                    colors.coloredLegacy(sender, "  &eOptional Parameters: ");
                    for (Param<?> optionalParam : optionalParams) {
                        String aliases;
                        if (optionalParam.aliases() != null) {
                            aliases = ", aliases=" + String.join(", ", optionalParam.aliases());
                        } else {
                            aliases = "";
                        }
                        
                        colors.coloredLegacy(sender, "    &8- &e" + optionalParam.id() + " (name=" + optionalParam.name() + ", default=\"" + optionalParam.defaultValue() + "\"" + aliases + ")");
                    }
                }
                
                return true;
            }
            
            if (!generator.isInitialized()) {
                colors.coloredLegacy(sender, "&cThe generator must be initialized to add an item");
                return true;
            }
            
            if (!generator.contains(player)) {
                colors.coloredLegacy(sender, "&cYou are not within the generator bounds.");
                return true;
            }
            
            
            ParamResult paramResults = this.cmdParams.parse(args);
            
            Optional<SMaterial> matOpt = SMaterial.matchSMaterial(paramResults.getValue(Params.Item.MATERIAL));
            if (matOpt.isEmpty()) {
                colors.coloredLegacy(sender, "&cYou provided an invalid material " + paramResults.getValue(Params.Item.MATERIAL));
                return true;
            }
            
            SMaterial material = matOpt.get();
            
            if (!material.isSupported()) {
                colors.coloredLegacy(sender, "&cYou provided an invalid material " + paramResults.getValue(Params.Item.MATERIAL));
                return true;
            }
            
            ItemBuilder<?, ?> itemBuilder = ItemBuilders.of(material);
            
            String id = paramResults.getValue(Params.Item.ID);
            
            if (id == null || id.isBlank()) {
                id = material.name().toLowerCase();
            }
            
            if (generator.hasEntry(new StringKey(id))) {
                colors.coloredLegacy(sender, "&cAn item already exists by the name " + material.name().toLowerCase());
                return true;
            }
            
            ItemEntry itemEntry = new ItemEntry(PluginKey.of(plugin, id), itemBuilder);
            generator.addEntry(itemEntry);
            
            setItemEntryValues(itemEntry, paramResults, colors, player);
            
            if (flagResults.isPresent(Flags.DEBUG)) {
                itemEntry.addPickupListener((entity, item, entry, gen) -> colors.coloredLegacy(sender, "&8Picked up " + entry.getKey()));
                itemEntry.addSpawnListener((item, entry, gen) -> colors.coloredLegacy(sender, "&8Spawned " + entry.getKey()));
            }
            
            List<String> msgLines = new LinkedList<>();
            msgLines.add("&eYou added the item &b" + itemEntry.getKey() + " &eto the generator &b" + generator.getKey());
            
            int maxItems = paramResults.getValue(Params.Item.MAX_COUNT);
            if (maxItems == Integer.MAX_VALUE) {
                msgLines.add("  &eMax Items: &bInfinite");
            } else {
                msgLines.add("  &eMax Items: &b" + maxItems);
            }
            
            msgLines.add("  &eCooldown: &b" + TIME_FORMAT.format(itemEntry.getCooldown()));
            msgLines.add("  &ePersistent: &b" + formatBoolean(itemEntry.hasFlag(Flag.PERSISTENT)));
            msgLines.add("  &eInventory Pickup: &b" + formatBoolean(itemEntry.hasFlag(Flag.INVENTORY_PICKUP)));
            msgLines.add("  &eInvulnerable: &b" + formatBoolean(itemEntry.hasFlag(Flag.INVULNERABLE)));
            
            msgLines.forEach(line -> colors.coloredLegacy(sender, line));
        }
        
        return true;
    }
    
    private static void setItemEntryValues(ItemEntry entry, ParamResult paramResults, StarColorsV2 colors, Player sender) {
        int stackSize = paramResults.getValue(Params.Item.STACK_SIZE);
        entry.getBuilder().amount(stackSize);
        
        long cooldown = TimeParser.parseTime(paramResults.getValue(Params.Item.COOLDOWN));
        
        int maxItems = paramResults.getValue(Params.Item.MAX_COUNT);
        if (maxItems <= 0) {
            colors.coloredLegacy(sender, "&cInvalid number provided for max count");
            return;
        }
        
        Location location = sender.getLocation();
        entry.setSpawnPosition(new Position(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
        
        if (paramResults.getValue(Params.Item.PERSISTENT)) {
            entry.addFlag(Flag.PERSISTENT);
        }
        
        if (paramResults.getValue(Params.Item.INVULNERABLE)) {
            entry.addFlag(Flag.INVULNERABLE);
        }
        
        if (paramResults.getValue(Params.Item.INVENTORY_PICKUP)) {
            entry.addFlag(Flag.INVENTORY_PICKUP);
        }
    }
    
    private static String formatBoolean(boolean v) {
        return v ? "&atrue" : "&cfalse";
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        selectedGenerators.remove(e.getPlayer().getUniqueId());
    }
    
    private static class Selection {
        private Location pos1, pos2;
        
        public Location getPos1() {
            return pos1;
        }
        
        public void setPos1(Location pos1) {
            this.pos1 = pos1;
        }
        
        public Location getPos2() {
            return pos2;
        }
        
        public void setPos2(Location pos2) {
            this.pos2 = pos2;
        }
    }
}