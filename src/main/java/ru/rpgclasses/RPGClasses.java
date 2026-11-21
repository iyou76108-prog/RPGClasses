package ru.rpgclasses;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.*;
import java.io.File;
import java.util.*;

@SuppressWarnings("deprecation")
public final class RPGClasses extends JavaPlugin implements Listener, CommandExecutor {
    private final Map<UUID, Profile> profiles = new HashMap<>();
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();
    private final Set<UUID> awaitingPartyName = new HashSet<>();
    private NamespacedKey bookKey;

    private static final String MAIN = "§8RPG §7| §fПерсонаж";
    private static final String CLASSES = "§8Выбор класса";
    private static final String ATTRIBUTES = "§8Атрибуты";
    private static final String ABILITIES = "§8Способности";
    private static final String INFO = "§8Информация";
    private static final String PARTY = "§8Пати";

    enum Stat { AGILITY, STRENGTH, INTELLIGENCE }
    enum RpgClass {
        BERSERK("Берсерк", Material.IRON_AXE, "Чем меньше HP, тем больше урон; ярость"),
        KNIGHT("Рыцарь", Material.SHIELD, "Тяжёлая броня и защита союзников"),
        DUELIST("Дуэлянт", Material.IRON_SWORD, "Уклонения и контратаки"),
        ASSASSIN("Ассасин", Material.ENDER_PEARL, "Скрытность и критические атаки"),
        HUNTER("Охотник", Material.BOW, "Лук, метки и ловушки"),
        FIRE_MAGE("Маг огня", Material.BLAZE_POWDER, "Огонь и метеор"),
        ICE_MAGE("Ледяной маг", Material.PACKED_ICE, "Замедление и заморозка"),
        NECROMANCER("Некромант", Material.WITHER_SKELETON_SKULL, "Проклятия и нежить"),
        LIGHTNING_MAGE("Маг молний", Material.LIGHTNING_ROD, "Цепная молния"),
        BLOOD_MAGE("Кровавый маг", Material.REDSTONE, "Здоровье как ресурс"),
        HEALER("Целитель", Material.GOLDEN_APPLE, "Лечение и очищение"),
        PALADIN("Паладин", Material.GOLDEN_SWORD, "Светлая магия и щиты"),
        BARD("Бард", Material.NOTE_BLOCK, "Музыкальные усиления"),
        ENGINEER("Инженер", Material.DISPENSER, "Турели и мины"),
        ALCHEMIST("Алхимик", Material.BREWING_STAND, "Зелья, бомбы и яды"),
        SUMMONER("Призыватель", Material.EVOKER_SPAWN_EGG, "Призыв существ"),
        VAMPIRE("Вампир", Material.FERMENTED_SPIDER_EYE, "Вампиризм и сила ночи"),
        DRUID("Друид", Material.OAK_SAPLING, "Природа и звери"),
        MONK("Монах", Material.STICK, "Комбо и внутренняя энергия"),
        SHADOW("Теневик", Material.ENDER_EYE, "Тени и телепорты");

        final String display, description;
        final Material icon;
        RpgClass(String display, Material icon, String description) {
            this.display = display; this.icon = icon; this.description = description;
        }
    }

    static final class Profile {
        RpgClass selectedClass;
        int level = 1, xp, attributePoints, abilityPoints;
        final EnumMap<Stat, Integer> stats = new EnumMap<>(Stat.class);
        final Map<String, Integer> abilityLevels = new HashMap<>();
        UUID partyId;
        Profile() { for (Stat stat : Stat.values()) stats.put(stat, 0); }
    }

    @Override public void onEnable() {
        bookKey = new NamespacedKey(this, "character_book");
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("rpg")).setExecutor(this);
        loadProfiles();
        getLogger().info("RPGClasses enabled");
    }

    @Override public void onDisable() { saveProfiles(); }

    private Profile profile(Player player) {
        return profiles.computeIfAbsent(player.getUniqueId(), ignored -> new Profile());
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Эта команда доступна только игроку."); return true;
        }
        giveBook(player); openMain(player); return true;
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        profile(player);
        giveBook(player);
        giveStarterArmor(player);
        applyAttributes(player);
    }

    private void giveStarterArmor(Player player) {
        PlayerInventory inv = player.getInventory();
        if (inv.getHelmet() == null) inv.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        if (inv.getChestplate() == null) inv.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        if (inv.getLeggings() == null) inv.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        if (inv.getBoots() == null) inv.setBoots(new ItemStack(Material.DIAMOND_BOOTS));
    }

    private boolean isBook(ItemStack item) {
        return item != null && item.getType() == Material.BOOK && item.hasItemMeta()
            && item.getItemMeta().getPersistentDataContainer().has(bookKey, PersistentDataType.BYTE);
    }

    private void giveBook(Player player) {
        for (ItemStack item : player.getInventory().getContents()) if (isBook(item)) return;
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        meta.setDisplayName("§6Книга персонажа §7(ПКМ/ЛКМ)");
        meta.setLore(List.of("§7Класс • атрибуты • способности", "§7Информация • пати"));
        meta.getPersistentDataContainer().set(bookKey, PersistentDataType.BYTE, (byte) 1);
        book.setItemMeta(meta);
        player.getInventory().addItem(book);
    }

    @EventHandler public void onBookUse(PlayerInteractEvent event) {
        if (isBook(event.getItem())) { event.setCancelled(true); openMain(event.getPlayer()); }
    }
    @EventHandler public void onBookDrop(PlayerDropItemEvent event) {
        if (isBook(event.getItemDrop().getItemStack())) event.setCancelled(true);
    }
    @EventHandler public void onBookSwap(PlayerSwapHandItemsEvent event) {
        if (isBook(event.getMainHandItem()) || isBook(event.getOffHandItem())) event.setCancelled(true);
    }

    private ItemStack icon(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name); meta.setLore(Arrays.asList(lore)); item.setItemMeta(meta);
        return item;
    }

    private void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MAIN);
        inv.setItem(10, icon(Material.NETHER_STAR, "§aВыбор класса"));
        inv.setItem(12, icon(Material.AMETHYST_SHARD, "§bАтрибуты", "§7Очки: " + profile(player).attributePoints));
        inv.setItem(14, icon(Material.ENCHANTED_BOOK, "§dСпособности", "§7Очки: " + profile(player).abilityPoints));
        inv.setItem(16, icon(Material.PLAYER_HEAD, "§eИнформация"));
        inv.setItem(22, icon(Material.WHITE_BANNER, "§6Пати"));
        player.openInventory(inv);
    }

    private void openClasses(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, CLASSES);
        int slot = 0;
        for (RpgClass clazz : RpgClass.values())
            inv.setItem(slot++, icon(clazz.icon, "§e" + clazz.display, "§7" + clazz.description, "§aНажми для выбора"));
        player.openInventory(inv);
    }

    private void openAttributes(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ATTRIBUTES);
        Profile p = profile(player);
        Stat[] stats = Stat.values();
        Material[] icons = {Material.FEATHER, Material.IRON_SWORD, Material.BOOK};
        String[] names = {"Ловкость", "Сила", "Интеллект"};
        for (int i = 0; i < stats.length; i++)
            inv.setItem(10 + i * 3, icon(icons[i], "§b" + names[i] + ": §f" + p.stats.get(stats[i]),
                "§7Свободные очки: " + p.attributePoints, "§aНажми, чтобы вложить 1 очко"));
        player.openInventory(inv);
    }

    private void openAbilities(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ABILITIES);
        Profile p = profile(player);
        if (p.selectedClass == null) inv.setItem(13, icon(Material.BARRIER, "§cСначала выбери класс"));
        else {
            String key = p.selectedClass.name() + "_core";
            int level = p.abilityLevels.getOrDefault(key, 0);
            inv.setItem(13, icon(p.selectedClass.icon, "§d" + p.selectedClass.display + " — способность",
                "§7Уровень: " + level + "/100", "§7Очки: " + p.abilityPoints, "§aНажми, чтобы улучшить"));
        }
        player.openInventory(inv);
    }

    private void openInfo(Player player) {
        Profile p = profile(player);
        Inventory inv = Bukkit.createInventory(null, 27, INFO);
        inv.setItem(11, icon(Material.PLAYER_HEAD, "§e" + player.getName(),
            "§7Класс: " + (p.selectedClass == null ? "Не выбран" : p.selectedClass.display),
            "§7Уровень: " + p.level, "§7Опыт: " + p.xp + "/" + xpRequired(p.level)));
        inv.setItem(13, icon(Material.RED_DYE, "§cHP: " + (int) player.getHealth() + "/" +
            (int) player.getAttribute(Attribute.MAX_HEALTH).getValue(),
            "§7Броня: " + player.getAttribute(Attribute.ARMOR).getValue()));
        inv.setItem(15, icon(Material.AMETHYST_SHARD, "§bОчки атрибутов: " + p.attributePoints,
            "§dОчки способностей: " + p.abilityPoints));
        player.openInventory(inv);
    }

    private void openParty(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, PARTY);
        Profile p = profile(player);
        inv.setItem(11, icon(Material.NAME_TAG, "§aПригласить игрока", "§7Нажми и введи ник в чат"));
        inv.setItem(13, icon(Material.PLAYER_HEAD, "§eСостав пати: " + partyMembers(p)));
        inv.setItem(15, icon(Material.BARRIER, "§cПокинуть пати"));
        player.openInventory(inv);
    }

    private String partyMembers(Profile p) {
        if (p.partyId == null) return "нет";
        List<String> names = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers())
            if (p.partyId.equals(profile(online).partyId)) names.add(online.getName());
        return String.join(", ", names);
    }

    @EventHandler public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!Set.of(MAIN, CLASSES, ATTRIBUTES, ABILITIES, INFO, PARTY).contains(title)) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;
        Profile p = profile(player);

        if (title.equals(MAIN)) {
            switch (slot) {
                case 10 -> openClasses(player);
                case 12 -> openAttributes(player);
                case 14 -> openAbilities(player);
                case 16 -> openInfo(player);
                case 22 -> openParty(player);
            }
        } else if (title.equals(CLASSES)) {
            RpgClass[] classes = RpgClass.values();
            if (slot < classes.length) {
                p.selectedClass = classes[slot];
                ItemStack classItem = new ItemStack(p.selectedClass.icon);
                ItemMeta classMeta = classItem.getItemMeta();
                classMeta.setDisplayName("§eПредмет класса: " + p.selectedClass.display);
                classMeta.setLore(List.of("§7Персональный предмет выбранного класса"));
                classItem.setItemMeta(classMeta);
                player.getInventory().addItem(classItem);
                player.sendMessage("§aВыбран класс: §e" + p.selectedClass.display);
                player.closeInventory(); saveProfiles();
            }
        } else if (title.equals(ATTRIBUTES) && p.attributePoints > 0) {
            Stat stat = switch (slot) { case 10 -> Stat.AGILITY; case 13 -> Stat.STRENGTH; case 16 -> Stat.INTELLIGENCE; default -> null; };
            if (stat != null) {
                p.stats.put(stat, p.stats.get(stat) + 1); p.attributePoints--;
                applyAttributes(player); openAttributes(player); saveProfiles();
            }
        } else if (title.equals(ABILITIES) && slot == 13 && p.selectedClass != null && p.abilityPoints > 0) {
            String key = p.selectedClass.name() + "_core";
            int level = p.abilityLevels.getOrDefault(key, 0);
            if (level < 100) {
                p.abilityLevels.put(key, level + 1); p.abilityPoints--;
                openAbilities(player); saveProfiles();
            } else player.sendMessage("§cМаксимальный уровень способности — 100.");
        } else if (title.equals(PARTY)) {
            if (slot == 11) {
                awaitingPartyName.add(player.getUniqueId()); player.closeInventory();
                player.sendMessage("§eВведи ник игрока в чат (cancel — отмена).");
            } else if (slot == 15) {
                p.partyId = null; saveProfiles(); openParty(player);
            }
        }
    }

    private void applyAttributes(Player player) {
        Profile p = profile(player);
        player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.1 + p.stats.get(Stat.AGILITY) * 0.001);
        player.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(1.0 + p.stats.get(Stat.STRENGTH) * 0.5);
        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(Math.min(40.0, 20.0 + p.stats.get(Stat.INTELLIGENCE) * 0.1));
    }

    private int xpRequired(int level) { return 100 + level * 25; }

    @EventHandler public void onMobKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null || event.getEntity() instanceof Player) return;
        Profile p = profile(killer);
        p.xp += 10;
        while (p.xp >= xpRequired(p.level)) {
            p.xp -= xpRequired(p.level); p.level++; p.attributePoints += 3; p.abilityPoints++;
            killer.sendMessage("§6Уровень " + p.level + "! §a+3 очка атрибутов, +1 очко способностей.");
        }
        saveProfiles();
    }

    @EventHandler public void onFriendlyFire(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = null;
        if (event.getDamager() instanceof Player p) attacker = p;
        else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player p) attacker = p;
        if (attacker != null && sameParty(attacker, victim)) event.setCancelled(true);
    }

    public boolean sameParty(Player first, Player second) {
        UUID a = profile(first).partyId, b = profile(second).partyId;
        return a != null && a.equals(b);
    }
    public boolean canAffect(Player caster, Player target) {
        return caster.equals(target) || sameParty(caster, target);
    }

    @EventHandler public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        String message = event.getMessage().trim();
        if (awaitingPartyName.remove(id)) {
            event.setCancelled(true);
            if (message.equalsIgnoreCase("cancel")) { player.sendMessage("§7Отменено."); return; }
            Bukkit.getScheduler().runTask(this, () -> sendPartyInvite(player, message));
            return;
        }
        UUID leaderId = pendingInvites.get(id);
        if (leaderId != null && (message.equalsIgnoreCase("accept") || message.equalsIgnoreCase("ignore"))) {
            event.setCancelled(true);
            pendingInvites.remove(id);
            Bukkit.getScheduler().runTask(this, () -> {
                Player leader = Bukkit.getPlayer(leaderId);
                if (message.equalsIgnoreCase("accept") && leader != null) {
                    UUID partyId = profile(leader).partyId == null ? leaderId : profile(leader).partyId;
                    profile(leader).partyId = partyId; profile(player).partyId = partyId;
                    leader.sendMessage("§a" + player.getName() + " вступил в пати.");
                    player.sendMessage("§aТы вступил в пати.");
                } else player.sendMessage("§7Приглашение отклонено.");
                saveProfiles();
            });
        }
    }

    private void sendPartyInvite(Player inviter, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null || target.equals(inviter)) { inviter.sendMessage("§cИгрок не найден."); return; }
        pendingInvites.put(target.getUniqueId(), inviter.getUniqueId());
        target.sendMessage("§e" + inviter.getName() + " приглашает тебя в пати. Напиши §aaccept §eили §cignore§e.");
        inviter.sendMessage("§aПриглашение отправлено игроку " + target.getName() + ".");
    }

    private void loadProfiles() {
        File file = new File(getDataFolder(), "players.yml");
        if (!file.exists()) return;
        var yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) try {
            UUID id = UUID.fromString(key); Profile p = new Profile();
            String clazz = yaml.getString(key + ".class");
            if (clazz != null) p.selectedClass = RpgClass.valueOf(clazz);
            p.level = yaml.getInt(key + ".level", 1); p.xp = yaml.getInt(key + ".xp");
            p.attributePoints = yaml.getInt(key + ".attributePoints"); p.abilityPoints = yaml.getInt(key + ".abilityPoints");
            for (Stat stat : Stat.values()) p.stats.put(stat, yaml.getInt(key + ".stats." + stat.name()));
            var section = yaml.getConfigurationSection(key + ".abilities");
            if (section != null) for (String ability : section.getKeys(false))
                p.abilityLevels.put(ability, yaml.getInt(key + ".abilities." + ability));
            String party = yaml.getString(key + ".party");
            if (party != null) p.partyId = UUID.fromString(party);
            profiles.put(id, p);
        } catch (Exception ex) { getLogger().warning("Could not load player profile " + key); }
    }

    private void saveProfiles() {
        var yaml = new org.bukkit.configuration.file.YamlConfiguration();
        for (var entry : profiles.entrySet()) {
            String id = entry.getKey().toString(); Profile p = entry.getValue();
            if (p.selectedClass != null) yaml.set(id + ".class", p.selectedClass.name());
            yaml.set(id + ".level", p.level); yaml.set(id + ".xp", p.xp);
            yaml.set(id + ".attributePoints", p.attributePoints); yaml.set(id + ".abilityPoints", p.abilityPoints);
            for (Stat stat : Stat.values()) yaml.set(id + ".stats." + stat.name(), p.stats.get(stat));
            for (var ability : p.abilityLevels.entrySet()) yaml.set(id + ".abilities." + ability.getKey(), ability.getValue());
            if (p.partyId != null) yaml.set(id + ".party", p.partyId.toString());
        }
        try { if (!getDataFolder().exists()) getDataFolder().mkdirs(); yaml.save(new File(getDataFolder(), "players.yml")); }
        catch (Exception ex) { getLogger().severe("Could not save profiles: " + ex.getMessage()); }
    }
}
