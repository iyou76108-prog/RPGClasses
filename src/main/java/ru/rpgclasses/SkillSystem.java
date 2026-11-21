package ru.rpgclasses;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;
import java.util.*;

@SuppressWarnings("deprecation")
final class SkillSystem implements Listener {
    private final RPGClasses plugin;
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Integer> editing = new HashMap<>();
    private final Map<RPGClasses.RpgClass, Skill[]> trees = new EnumMap<>(RPGClasses.RpgClass.class);
    private static final String PICK = "§8Выбор способностей";
    private static final String BIND = "§8Назначение клавиши";
    private static final String TREE = "§8Древо навыка";

    private enum Kind { BLADE, PROJECTILE, BURST, DASH, TRAP, HEAL, SHIELD, BLINK, SUMMON, DRAIN, ROOT, BUFF }
    private record Skill(String name, String lore, Kind kind, Material icon, double power, int cooldown) {}

    SkillSystem(RPGClasses plugin) {
        this.plugin = plugin;
        buildTrees();
    }

    private void put(RPGClasses.RpgClass c, String[] names, String[] lore, Kind[] kinds, Material[] icons, double[] power, int[] cd) {
        Skill[] s = new Skill[4];
        for (int i=0;i<4;i++) s[i] = new Skill(names[i], lore[i], kinds[i], icons[i], power[i], cd[i]);
        trees.put(c, s);
    }

    private void buildTrees() {
        put(RPGClasses.RpgClass.BERSERK,new String[]{"Рассечение","Кровавый вихрь","Ярость","Неукротимость"},new String[]{"Мощный удар перед собой","Вихрь клинка вокруг себя","На время повышает урон","Сопротивление и лечение при низком HP"},new Kind[]{Kind.BLADE,Kind.BURST,Kind.BUFF,Kind.SHIELD},new Material[]{Material.IRON_AXE,Material.NETHERITE_AXE,Material.BLAZE_POWDER,Material.GOLDEN_APPLE},new double[]{7,5,0.35,0},new int[]{5,12,25,35});
        put(RPGClasses.RpgClass.KNIGHT,new String[]{"Щитовой удар","Рассекающий выпад","Стойка стража","Клятва защиты"},new String[]{"Удар щитом и отбрасывание","Рывок с ударом","Сопротивление урону","Щит для ближайших союзников"},new Kind[]{Kind.BLADE,Kind.DASH,Kind.SHIELD,Kind.BUFF},new Material[]{Material.SHIELD,Material.IRON_SWORD,Material.IRON_CHESTPLATE,Material.TOTEM_OF_UNDYING},new double[]{6,8,0,0.2},new int[]{6,10,22,35});
        put(RPGClasses.RpgClass.DUELIST,new String[]{"Контрудар","Смертельный выпад","Парирование","Идеальный ритм"},new String[]{"Точный удар по цели","Быстрый рывок с уроном","Короткая защита от атак","Скорость и сила на время"},new Kind[]{Kind.BLADE,Kind.DASH,Kind.SHIELD,Kind.BUFF},new Material[]{Material.IRON_SWORD,Material.GOLDEN_SWORD,Material.SHIELD,Material.FEATHER},new double[]{7,9,0,0.25},new int[]{5,9,18,28});
        put(RPGClasses.RpgClass.ASSASSIN,new String[]{"Теневой порез","Метательный клинок","Исчезновение","За спину"},new String[]{"Критический удар перед собой","Метает быстрый снаряд","Невидимость и скорость","Телепорт за ближайшую цель"},new Kind[]{Kind.BLADE,Kind.PROJECTILE,Kind.BLINK,Kind.BLINK},new Material[]{Material.IRON_SWORD,Material.IRON_INGOT,Material.ENDER_EYE,Material.ENDER_PEARL},new double[]{8,6,0,7},new int[]{5,8,24,18});
        put(RPGClasses.RpgClass.HUNTER,new String[]{"Прицельный выстрел","Взрывная стрела","Капкан","Орлиный глаз"},new String[]{"Сильный выстрел по направлению взгляда","Взрывной снаряд","Замедляет врагов рядом","Скорость и ночное зрение"},new Kind[]{Kind.PROJECTILE,Kind.BURST,Kind.TRAP,Kind.BUFF},new Material[]{Material.BOW,Material.TNT,Material.TRIPWIRE_HOOK,Material.SPYGLASS},new double[]{8,7,4,0},new int[]{4,14,18,30});
        put(RPGClasses.RpgClass.FIRE_MAGE,new String[]{"Огненный шар","Огненный дождь","Пылающий щит","Метеор"},new String[]{"Огненный снаряд","Взрыв огня вокруг цели","Поджигает атакующих","Мощный взрыв перед магом"},new Kind[]{Kind.PROJECTILE,Kind.BURST,Kind.SHIELD,Kind.BURST},new Material[]{Material.FIRE_CHARGE,Material.BLAZE_POWDER,Material.MAGMA_CREAM,Material.MAGMA_BLOCK},new double[]{7,6,0,12},new int[]{5,14,25,35});
        put(RPGClasses.RpgClass.ICE_MAGE,new String[]{"Ледяная стрела","Кольцо метели","Ледяная броня","Ледяной шип"},new String[]{"Замедляет поражённого врага","Замедляет врагов вокруг","Защита и сопротивление","Сильный ледяной удар"},new Kind[]{Kind.PROJECTILE,Kind.BURST,Kind.SHIELD,Kind.BLADE},new Material[]{Material.PACKED_ICE,Material.SNOWBALL,Material.BLUE_ICE,Material.ICE},new double[]{6,4,0,10},new int[]{5,14,25,30});
        put(RPGClasses.RpgClass.NECROMANCER,new String[]{"Проклятый снаряд","Вытягивание души","Костяная стража","Порча"},new String[]{"Снаряд тьмы","Похищает здоровье у цели","Призывает союзного скелета","Ослабляет ближайших врагов"},new Kind[]{Kind.PROJECTILE,Kind.DRAIN,Kind.SUMMON,Kind.BURST},new Material[]{Material.WITHER_ROSE,Material.SOUL_SAND,Material.SKELETON_SKULL,Material.WITHER_SKELETON_SKULL},new double[]{7,6,0,4},new int[]{5,16,35,22});
        put(RPGClasses.RpgClass.LIGHTNING_MAGE,new String[]{"Громовой разряд","Цепная молния","Шаг молнии","Грозовой купол"},new String[]{"Молния по цели","Поражает ближайших врагов","Мгновенный рывок вперёд","Молнии вокруг мага"},new Kind[]{Kind.PROJECTILE,Kind.BURST,Kind.DASH,Kind.BURST},new Material[]{Material.LIGHTNING_ROD,Material.COPPER_INGOT,Material.RABBIT_FOOT,Material.END_ROD},new double[]{8,6,0,7},new int[]{4,14,18,30});
        put(RPGClasses.RpgClass.BLOOD_MAGE,new String[]{"Кровавое копьё","Жатва крови","Кровавый барьер","Жертвенный ритуал"},new String[]{"Снаряд с уроном","Урон врагам и лечение мага","Поглощает часть входящего урона","Меняет часть HP на усиление"},new Kind[]{Kind.PROJECTILE,Kind.DRAIN,Kind.SHIELD,Kind.BUFF},new Material[]{Material.REDSTONE,Material.RED_DYE,Material.RED_STAINED_GLASS,Material.GHAST_TEAR},new double[]{8,5,0,0.4},new int[]{5,15,25,35});
        put(RPGClasses.RpgClass.HEALER,new String[]{"Карающий свет","Сияющий импульс","Исцеление","Очищение"},new String[]{"Световой снаряд во врага","Урон врагам вокруг","Лечит себя и союзников","Снимает негативные эффекты"},new Kind[]{Kind.PROJECTILE,Kind.BURST,Kind.HEAL,Kind.HEAL},new Material[]{Material.GLOWSTONE_DUST,Material.SUNFLOWER,Material.GOLDEN_APPLE,Material.MILK_BUCKET},new double[]{6,4,8,0},new int[]{5,14,18,28});
        put(RPGClasses.RpgClass.PALADIN,new String[]{"Карающий удар","Световое копьё","Священный щит","Аура света"},new String[]{"Удар перед собой","Световой снаряд","Защита и сопротивление","Лечит союзников рядом"},new Kind[]{Kind.BLADE,Kind.PROJECTILE,Kind.SHIELD,Kind.HEAL},new Material[]{Material.GOLDEN_SWORD,Material.END_ROD,Material.SHIELD,Material.BEACON},new double[]{7,7,0,5},new int[]{5,8,24,30});
        put(RPGClasses.RpgClass.BARD,new String[]{"Звуковая волна","Диссонанс","Боевой марш","Гимн восстановления"},new String[]{"Волна наносит урон","Оглушающий импульс замедляет врагов","Ускоряет союзников","Лечит союзников рядом"},new Kind[]{Kind.BURST,Kind.BURST,Kind.BUFF,Kind.HEAL},new Material[]{Material.NOTE_BLOCK,Material.GOAT_HORN,Material.JUKEBOX,Material.MUSIC_DISC_13},new double[]{5,4,0,5},new int[]{6,14,24,30});
        put(RPGClasses.RpgClass.ENGINEER,new String[]{"Импульсная мина","Турельный залп","Ремкомплект","Перегрузка"},new String[]{"Взрыв перед инженером","Серия снарядов","Восстанавливает здоровье","Взрыв энергии вокруг"},new Kind[]{Kind.BURST,Kind.PROJECTILE,Kind.HEAL,Kind.BURST},new Material[]{Material.TNT_MINECART,Material.DISPENSER,Material.IRON_INGOT,Material.REDSTONE_BLOCK},new double[]{7,6,6,9},new int[]{8,16,20,32});
        put(RPGClasses.RpgClass.ALCHEMIST,new String[]{"Кислотная колба","Ядовитый туман","Эликсир жизни","Катализатор"},new String[]{"Кислотный снаряд","Отравляет врагов рядом","Лечит и снимает яд","Усиливает скорость и силу"},new Kind[]{Kind.PROJECTILE,Kind.TRAP,Kind.HEAL,Kind.BUFF},new Material[]{Material.SPLASH_POTION,Material.SPIDER_EYE,Material.POTION,Material.BREWING_STAND},new double[]{6,4,7,0.25},new int[]{5,15,20,30});
        put(RPGClasses.RpgClass.SUMMONER,new String[]{"Эфирный болт","Взрыв призыва","Дух-защитник","Портал призыва"},new String[]{"Магический снаряд","Взрыв перед призывателем","Призывает волка","Призывает второго волка"},new Kind[]{Kind.PROJECTILE,Kind.BURST,Kind.SUMMON,Kind.SUMMON},new Material[]{Material.AMETHYST_SHARD,Material.END_CRYSTAL,Material.BONE,Material.END_PORTAL_FRAME},new double[]{7,6,0,0},new int[]{5,15,30,45});
        put(RPGClasses.RpgClass.VAMPIRE,new String[]{"Клыки","Кровавый бросок","Ночная форма","Похищение жизни"},new String[]{"Быстрый удар и лечение","Снаряд крови","Скорость и невидимость","Урон цели лечит вампира"},new Kind[]{Kind.DRAIN,Kind.PROJECTILE,Kind.BUFF,Kind.DRAIN},new Material[]{Material.FERMENTED_SPIDER_EYE,Material.REDSTONE,Material.BLACK_DYE,Material.GHAST_TEAR},new double[]{7,7,0,9},new int[]{5,8,25,22});
        put(RPGClasses.RpgClass.DRUID,new String[]{"Корни","Шип лозы","Природное исцеление","Форма зверя"},new String[]{"Корни замедляют врагов","Снаряд из лозы","Лечит себя и союзников","Скорость и сила на время"},new Kind[]{Kind.ROOT,Kind.PROJECTILE,Kind.HEAL,Kind.BUFF},new Material[]{Material.VINE,Material.POISONOUS_POTATO,Material.OAK_SAPLING,Material.WOLF_SPAWN_EGG},new double[]{5,7,7,0.25},new int[]{8,6,20,30});
        put(RPGClasses.RpgClass.MONK,new String[]{"Ладонь ци","Воздушный удар","Медитация","Путь ветра"},new String[]{"Ударная волна перед собой","Рывок с уроном","Регенерация и сопротивление","Быстрый рывок вперёд"},new Kind[]{Kind.BLADE,Kind.DASH,Kind.HEAL,Kind.DASH},new Material[]{Material.RABBIT_FOOT,Material.FEATHER,Material.BAMBOO,Material.PHANTOM_MEMBRANE},new double[]{6,8,5,0},new int[]{5,10,25,16});
        put(RPGClasses.RpgClass.SHADOW,new String[]{"Разлом тени","Теневой снаряд","Шаг в пустоту","Поглощение тьмы"},new String[]{"Режущий удар","Снаряд тьмы","Телепорт вперёд","Урон врагам и лечение"},new Kind[]{Kind.BLADE,Kind.PROJECTILE,Kind.BLINK,Kind.DRAIN},new Material[]{Material.OBSIDIAN,Material.ENDER_PEARL,Material.ECHO_SHARD,Material.SCULK},new double[]{7,7,0,6},new int[]{5,7,18,20});
    }

    @EventHandler public void onSwap(PlayerSwapHandItemsEvent e) {
        e.setCancelled(true);
        if (plugin.profile(e.getPlayer()).selectedClass == null) { e.getPlayer().sendMessage("§cСначала выбери класс."); return; }
        openPicker(e.getPlayer());
    }

    private Inventory menu(String title) { return Bukkit.createInventory(null, 27, title); }
    private ItemStack item(Material mat, String name, String... lore) {
        ItemStack i = new ItemStack(mat); ItemMeta m=i.getItemMeta(); m.setDisplayName(name); m.setLore(Arrays.asList(lore)); i.setItemMeta(m); return i;
    }
    private void openPicker(Player p) {
        Inventory inv=menu(PICK); Skill[] skills=trees.get(plugin.profile(p).selectedClass);
        for(int i=0;i<4;i++) inv.setItem(10+i*2,item(skills[i].icon(),"§d"+(i+1)+". "+skills[i].name(),"§7"+skills[i].lore,"§eНажми, чтобы назначить клавишу"));
        p.openInventory(inv);
    }
    private void openBind(Player p,int skill) {
        editing.put(p.getUniqueId(),skill); Inventory inv=Bukkit.createInventory(null,27,BIND);
        for(int i=0;i<9;i++) inv.setItem(9+i,item(Material.PAPER,"§eКлавиша "+(i+1),"§7Нажми, чтобы назначить навык"));
        p.openInventory(inv);
    }
    private void openTree(Player p,int skill) {
        Skill s=trees.get(plugin.profile(p).selectedClass)[skill]; int lvl=plugin.profile(p).abilityLevels.getOrDefault(key(plugin.profile(p),skill),0);
        Inventory inv=menu(TREE); inv.setItem(13,item(s.icon(),"§d"+s.name(),"§7"+s.lore,"§7Уровень: "+lvl+"/10","§7Очки навыков: "+plugin.profile(p).abilityPoints,"§aНажми, чтобы улучшить","§8Улучшения усиливают эффект и сокращают перезарядку"));
        inv.setItem(22,item(Material.ARROW,"§eНазад")); p.openInventory(inv);
    }
    private String key(RPGClasses.Profile p,int skill) { return p.selectedClass.name()+"_skill_"+skill; }
    private String bindKey(int slot) { return "bind_"+slot; }

    @EventHandler public void click(InventoryClickEvent e) {
        if(!(e.getWhoClicked() instanceof Player p)) return;
        String title=e.getView().getTitle();
        if(!title.equals(PICK)&&!title.equals(BIND)&&!title.equals(TREE)) return;
        e.setCancelled(true); int slot=e.getRawSlot(); if(slot<0||slot>=e.getInventory().getSize()) return;
        RPGClasses.Profile prof=plugin.profile(p);
        if(title.equals(PICK)) {
            int skill=(slot-10)/2; if(slot>=10&&slot<=16&&slot%2==0&&skill<4) { if(e.isShiftClick()) openTree(p,skill); else openBind(p,skill); }
            else if(slot==22) openTree(p,0);
        } else if(title.equals(BIND)&&slot>=9&&slot<=17) {
            int hotbar=slot-9, skill=editing.getOrDefault(p.getUniqueId(),0);
            prof.abilityLevels.put(bindKey(hotbar-1),skill); plugin.saveProfiles();
            p.sendMessage("§aНавык назначен на клавишу "+hotbar+"."); openPicker(p);
        } else if(title.equals(TREE)) {
            if(slot==13&&prof.abilityPoints>0) {
                int skill=editing.getOrDefault(p.getUniqueId(),0); int level=prof.abilityLevels.getOrDefault(key(prof,skill),0);
                if(level<10) { prof.abilityLevels.put(key(prof,skill),level+1); prof.abilityPoints--; plugin.saveProfiles(); openTree(p,skill); }
                else p.sendMessage("§cМаксимальный уровень.");
            } else if(slot==22) openPicker(p);
        }
    }

    @EventHandler public void use(PlayerInteractEvent e) {
        if(e.getHand()!=org.bukkit.inventory.EquipmentSlot.HAND) return;
        if(e.getAction()!=org.bukkit.event.block.Action.RIGHT_CLICK_AIR&&e.getAction()!=org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Player p=e.getPlayer(); int slot=p.getInventory().getHeldItemSlot();
        int skill=plugin.profile(p).abilityLevels.getOrDefault(bindKey(slot),-1);
        if(skill<0||plugin.profile(p).selectedClass==null) return;
        e.setCancelled(true); cast(p,skill);
    }

    private void cast(Player p,int index) {
        RPGClasses.Profile prof=plugin.profile(p); Skill s=trees.get(prof.selectedClass)[index];
        int level=prof.abilityLevels.getOrDefault(key(prof,index),0);
        long now=System.currentTimeMillis(); String cooldownKey=p.getUniqueId()+":"+index; long ready=cooldowns.getOrDefault(cooldownKey,0L);
        if(now<ready) { p.sendActionBar("§cПерезарядка: "+((ready-now+999)/1000)+"с"); return; }
        double scale=1.0+level*0.12, power=s.power()*scale;
        cooldowns.put(cooldownKey,now+Math.max(3000,(s.cooldown()-level/2)*1000L));
        LivingEntity target=target(p,12);
        switch(s.kind()) {
            case PROJECTILE -> {
                Snowball b=p.launchProjectile(Snowball.class); b.setVelocity(p.getLocation().getDirection().multiply(1.6)); b.setCustomName("rpg_skill");
                b.setPersistent(true); b.getPersistentDataContainer().set(new NamespacedKey(plugin,"skill_damage"),PersistentDataType.DOUBLE,power);
                p.getWorld().playSound(p.getLocation(),Sound.ENTITY_BLAZE_SHOOT,0.8f,1.2f);
            }
            case BLADE -> { p.getWorld().playSound(p.getLocation(),Sound.ENTITY_PLAYER_ATTACK_SWEEP,1,0.8f); hitCone(p,power,4.2); p.getWorld().spawnParticle(Particle.SWEEP_ATTACK,p.getLocation().add(p.getLocation().getDirection().multiply(1.3)).add(0,1,0),1); }
            case BURST,TRAP,ROOT -> { Location at=target==null?p.getLocation().add(p.getLocation().getDirection().multiply(4)):target.getLocation(); burst(p,at,power, s.kind()==Kind.ROOT?4:4.5); p.getWorld().spawnParticle(s.kind()==Kind.ROOT?Particle.HAPPY_VILLAGER:Particle.EXPLOSION,at.add(0,1,0),24,1,0.7,1,0.05); }
            case DASH -> { Vector v=p.getLocation().getDirection().setY(0).normalize().multiply(1.35); v.setY(0.28); p.setVelocity(v); hitCone(p,power,2.5); p.getWorld().playSound(p.getLocation(),Sound.ENTITY_ENDER_DRAGON_FLAP,0.7f,1.5f); }
            case BLINK -> { Location dest=p.getTargetBlockExact(12)==null?p.getLocation().add(p.getLocation().getDirection().multiply(5)):p.getTargetBlockExact(12).getLocation().add(0,1,0); dest.setDirection(p.getLocation().getDirection()); if(dest.getBlock().isPassable()&&dest.clone().add(0,1,0).getBlock().isPassable()) p.teleport(dest); p.getWorld().spawnParticle(Particle.PORTAL,p.getLocation(),30,0.5,0.8,0.5,0.4); }
            case HEAL -> { for(Player t:Bukkit.getOnlinePlayers()) if(t.getWorld().equals(p.getWorld())&&t.getLocation().distanceSquared(p.getLocation())<=100&&(t.equals(p)||plugin.sameParty(p,t))) t.setHealth(Math.min(t.getAttribute(Attribute.MAX_HEALTH).getValue(),t.getHealth()+Math.max(1,power))); p.getWorld().spawnParticle(Particle.HEART,p.getLocation().add(0,1,0),12,1,0.6,1); p.playSound(p.getLocation(),Sound.BLOCK_AMETHYST_BLOCK_CHIME,1,1.5f); }
            case SHIELD -> { p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,100+level*10,Math.min(2,level/4),true,true)); p.getWorld().spawnParticle(Particle.WAX_ON,p.getLocation().add(0,1,0),25,0.8,1,0.8,0.1); }
            case BUFF -> { p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,160+level*10,Math.min(2,level/4),true,true)); p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,160+level*10,Math.min(2,level/5),true,true)); p.getWorld().spawnParticle(Particle.ENCHANT,p.getLocation().add(0,1,0),35,0.8,1,0.8,0.5); }
            case SUMMON -> { Wolf wolf=(Wolf)p.getWorld().spawnEntity(p.getLocation().add(p.getLocation().getDirection()),EntityType.WOLF); wolf.setTamed(true); wolf.setOwner(p); wolf.setCustomName("§dДух-хранитель"); wolf.setCustomNameVisible(true); wolf.setAdult(); wolf.setRemoveWhenFarAway(true); wolf.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,1200,1)); }
            case DRAIN -> { if(target!=null) { double damage=Math.max(1,power); target.damage(damage,p); p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(),p.getHealth()+damage*0.55)); p.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,target.getLocation().add(0,1,0),10,0.4,0.5,0.4); } }
        }
        p.sendActionBar("§d"+s.name()+" §7• §f"+s.cooldown()+"с");
    }

    private LivingEntity target(Player p,double range) {
        for(Entity e:p.getNearbyEntities(range,range,range)) if(e instanceof LivingEntity l&&!l.equals(p)&&!(l instanceof Player other&&plugin.sameParty(p,other))&&p.hasLineOfSight(l)) return l;
        return null;
    }
    private void hitCone(Player p,double damage,double range) {
        Vector dir=p.getLocation().getDirection().normalize();
        for(Entity e:p.getNearbyEntities(range,2.5,range)) if(e instanceof LivingEntity l&&!l.equals(p)&&!(l instanceof Player other&&plugin.sameParty(p,other))) {
            Vector to=l.getEyeLocation().toVector().subtract(p.getEyeLocation().toVector()).normalize();
            if(dir.dot(to)>0.25&&p.hasLineOfSight(l)) { l.damage(damage,p); l.setVelocity(l.getVelocity().add(dir.multiply(0.35))); }
        }
    }
    private void burst(Player p,Location at,double damage,double radius) {
        for(Entity e:at.getWorld().getNearbyEntities(at,radius,radius,radius)) if(e instanceof LivingEntity l&&!l.equals(p)&&!(l instanceof Player other&&plugin.sameParty(p,other))) {
            if(l.getLocation().distanceSquared(at)<=radius*radius) { l.damage(damage,p); if(l instanceof Player) l.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,50,1)); }
        }
    }

    @EventHandler public void projectileHit(ProjectileHitEvent e) {
        if(!(e.getEntity() instanceof Snowball b)||!b.getPersistentDataContainer().has(new NamespacedKey(plugin,"skill_damage"),PersistentDataType.DOUBLE)) return;
        if(!(b.getShooter() instanceof Player shooter)) return;
        Double damage=b.getPersistentDataContainer().get(new NamespacedKey(plugin,"skill_damage"),PersistentDataType.DOUBLE);
        if(e.getHitEntity() instanceof LivingEntity victim&&!victim.equals(shooter)&&!(victim instanceof Player other&&plugin.sameParty(shooter,other))) victim.damage(damage,shooter);
        b.getWorld().spawnParticle(Particle.CRIT,b.getLocation(),10,0.25,0.25,0.25,0.1);
    }
}
