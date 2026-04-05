package com.stardevllc.stargenerators.command;

import com.stardevllc.minecraft.Position;
import com.stardevllc.minecraft.colors.StarColorsV2;
import com.stardevllc.minecraft.command.flags.CmdFlags;
import com.stardevllc.minecraft.command.flags.FlagResult;
import com.stardevllc.minecraft.command.flags.type.PresenceFlag;
import com.stardevllc.minecraft.command.params.*;
import com.stardevllc.minecraft.itembuilder.ItemBuilder;
import com.stardevllc.minecraft.plugin.ExtendedJavaPlugin;
import com.stardevllc.minecraft.registry.PluginKey;
import com.stardevllc.minecraft.registry.PluginRegisterer;
import com.stardevllc.minecraft.smaterial.SMaterial;
import com.stardevllc.stargenerators.model.ItemEntry;
import com.stardevllc.stargenerators.model.ItemEntry.Flag;
import com.stardevllc.stargenerators.model.ItemGenerator;
import com.stardevllc.starcore.ItemBuilders;
import com.stardevllc.starlib.objects.key.Key;
import com.stardevllc.starlib.objects.key.Keys;
import com.stardevllc.starlib.registry.RegistryObject;
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

import static com.stardevllc.stargenerators.StarGenerators.ITEMS;
import static com.stardevllc.stargenerators.StarGenerators.ITEM_GENERATORS;

public class ItemGeneratorCommandOld implements CommandExecutor, Listener {
    
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
            private static final Param<Boolean> KEEP_DATA = Param.builder(Boolean.class).id("kd").name("Keep Data").defaultValue(false).build();
            
            private static final List<Param<?>> REQUIRED = List.of(MATERIAL);
        }
    }
    
    private final PluginRegisterer<ItemGenerator> generatorRegisterer;
    private final PluginRegisterer<ItemEntry> itemRegisterer;
    
    public ItemGeneratorCommandOld(ExtendedJavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
        
        this.generatorRegisterer = PluginRegisterer.create(ITEM_GENERATORS, plugin);
        this.itemRegisterer = PluginRegisterer.create(ITEMS, plugin);
        
        this.cmdFlags = new CmdFlags(Flags.Create.SELECT, Flags.Create.INIT, Flags.Create.START);
        this.cmdParams = new CmdParams(Params.Item.ID, Params.Item.MATERIAL, Params.Item.COOLDOWN, Params.Item.INVULNERABLE, Params.Item.INVENTORY_PICKUP, Params.Item.PERSISTENT, Params.Item.MAX_COUNT, Params.Item.STACK_SIZE, Params.Item.KEEP_DATA);
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
            
            if (ITEM_GENERATORS.containsKey(PluginKey.of(plugin, args[1]))) {
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
            
            RegistryObject<ItemGenerator> generator = generatorRegisterer.register(args[1], new ItemGenerator(args[1], new ArrayList<>(), pos1, pos2));
            
            if (!generator.isPresent()) {
                colors.coloredLegacy(sender, "&cFailed to register the new item generator");
                return true;
            }
            
            ItemGenerator itemGenerator = generator.get();
            
            colors.coloredLegacy(sender, "&eCreated an Item Generator with the key &b" + generator.getKey());
            
            if (flagResults.isPresent(Flags.Create.INIT)) {
                itemGenerator.init(player.getWorld());
                colors.coloredLegacy(sender, "&eInitialized the Item Generator &b" + generator.getKey() + " &ein world &b" + player.getWorld().getName());
            }
            
            if (flagResults.isPresent(Flags.Create.START)) {
                itemGenerator.start();
                colors.coloredLegacy(sender, "&eStarted the Item Generator &b" + generator.getKey());
            }
            
            if (flagResults.isPresent(Flags.Create.SELECT)) {
                selectedGenerators.put(player.getUniqueId(), generator.getKey());
                colors.coloredLegacy(sender, "&eSelected the Item Generator &b" + generator.getKey());
            }
            
            return true;
        } else if (args[0].equalsIgnoreCase("select")) {
            if (!(args.length > 1)) {
                colors.coloredLegacy(sender, "&cYou must provide an id for the generator to select");
                return true;
            }
            
            String genName = args[1].toLowerCase();
            ItemGenerator generator = ITEM_GENERATORS.get(genName);
            
            if (generator == null) {
                List<Key> partialMatches = ITEM_GENERATORS.getPartial(genName);
                if (partialMatches.size() == 1) {
                    generator = ITEM_GENERATORS.get(partialMatches.getFirst());
                } else if (partialMatches.size() > 1) {
                    colors.coloredLegacy(sender, "&cMultiple Generators match the partial name: \"" + genName + "\"");
                    for (Key gen : partialMatches) {
                        colors.coloredLegacy(sender, " &8- &c" + gen);
                    }
                    return true;
                }
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
                lines.add("    &eCooldown: &b" + timeFormat.format(generator.getEntryCooldown(entry)));
                lines.add("    &eMax items: &b" + generator.getMaxItems(entry));
                List<String> flags = new ArrayList<>();
                for (Flag flag : entry.getFlags()) {
                    flags.add(flag.name().toLowerCase());
                }
                
                String flagsString = String.join(", ", flags);
                lines.add("    &eFlags: &b" + (flagsString.isBlank() ? "None" : flagsString));
                lines.add("    &eSpawned Items: &b" + generator.getSpawnedItemsCount(entry.getKey()));
                Position pos = generator.getSpawnPosition(entry);
                lines.add("    &ePos: &b(" + pos.getBlockX() + ", " + pos.getBlockY() + ", " + pos.getBlockZ() + ")");
                long nextSpawn = generator.getNextSpawn(entry);
                lines.add("    &eNext Spawn: &b" + timeFormat.format(nextSpawn > 0 ? nextSpawn : 0));
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
                        colors.coloredLegacy(sender, "    &8- &c" + requiredParam.id() + " (name=" + requiredParam.name() + aliases + ")");
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
            
            String id = paramResults.getValue(Params.Item.ID);
            
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
            
            if (material == SMaterial.AIR) {
                colors.coloredLegacy(sender, "&cThere was a problem parsing the material \"" + paramResults.getValue(Params.Item.MATERIAL) + "\"");
                return true;
            }
            
            ItemBuilder<?, ?> itemBuilder = ItemBuilders.of(material);
            
            if (id == null || id.isBlank()) {
                id = material.name().toLowerCase();
            }
            
            //This will either register the item, or get an existing registration
            RegistryObject<ItemEntry> item = itemRegisterer.register(id, new ItemEntry(itemBuilder));
            ItemEntry itemEntry = item.get();
            
            if (generator.hasEntry(item.getKey())) {
                colors.coloredLegacy(sender, "&cThe item &e" + item.getKey() + " &calready exists in generator &e" + generator.getKey());
                return true;
            }
            
            long cooldown = TimeParser.parseTime(paramResults.getValue(Params.Item.COOLDOWN));
            generator.setEntryCooldown(itemEntry, cooldown);
            
            int maxItems = paramResults.getValue(Params.Item.MAX_COUNT);
            if (maxItems <= 0) {
                colors.coloredLegacy(sender, "&cInvalid number provided for max count");
                return true;
            }
            
            int stackSize = paramResults.getValue(Params.Item.STACK_SIZE);
            
            if (stackSize < 1 || stackSize > 64) {
                colors.coloredLegacy(sender, "&cStack Size must be between 1 and 64 (inclusive)");
                return true;
            }
            
            generator.addEntry(itemEntry, new Position(location.getBlockX(), location.getBlockY(), location.getBlockZ()), cooldown, maxItems, stackSize);
            
            setItemEntryValues(itemEntry, paramResults);
            
            List<String> msgLines = new LinkedList<>();
            msgLines.add("&eYou added the item &b" + itemEntry.getKey() + " &eto the generator &b" + generator.getKey());
            
            if (maxItems == Integer.MAX_VALUE) {
                msgLines.add("  &eMax Items: &bInfinite");
            } else {
                msgLines.add("  &eMax Items: &b" + maxItems);
            }
            
            msgLines.add("  &eMaterial: &b" + itemEntry.getBuilder().getMaterial().name());
            msgLines.add("  &eStack Size: &b" + generator.getStackSize(itemEntry));
            msgLines.add("  &eCooldown: &b" + TIME_FORMAT.format(generator.getEntryCooldown(itemEntry)));
            msgLines.add("  &ePersistent: &b" + formatBoolean(itemEntry.hasFlag(Flag.PERSISTENT)));
            msgLines.add("  &eInventory Pickup: &b" + formatBoolean(itemEntry.hasFlag(Flag.INVENTORY_PICKUP)));
            msgLines.add("  &eInvulnerable: &b" + formatBoolean(itemEntry.hasFlag(Flag.INVULNERABLE)));
            msgLines.add("  &eKeep Data: &b" + formatBoolean(itemEntry.hasFlag(Flag.KEEP_DATA)));
            
            msgLines.forEach(line -> colors.coloredLegacy(sender, line));
        }
        
        return true;
    }
    
    private static void setItemEntryValues(ItemEntry entry, ParamResult paramResults) {
        if (paramResults.getValue(Params.Item.PERSISTENT)) {
            entry.addFlag(Flag.PERSISTENT);
        }
        
        if (paramResults.getValue(Params.Item.INVULNERABLE)) {
            entry.addFlag(Flag.INVULNERABLE);
        }
        
        if (paramResults.getValue(Params.Item.INVENTORY_PICKUP)) {
            entry.addFlag(Flag.INVENTORY_PICKUP);
        }
        
        if (paramResults.getValue(Params.Item.KEEP_DATA)) {
            entry.addFlag(Flag.KEEP_DATA);
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