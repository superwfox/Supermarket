package sudark.supermarket;

import it.unimi.dsi.fastutil.Pair;
import org.bukkit.*;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class Supermarket extends JavaPlugin {

    ConcurrentHashMap<String, Pair<Material, Pair<Integer, Integer>>> store = new ConcurrentHashMap();

    public ConcurrentHashMap<String, Pair<Material, Pair<Integer, Integer>>> getStore() {
        return store;
    }

    @Override
    public void onEnable() {
        this.getLogger().info("\u001b[35mSHOP Enabled 超市开张了\u001b[0m");
        this.getCommand("shop").setExecutor(this);
        this.getCommand("sell").setExecutor(this);
        this.getCommand("enchants").setExecutor(this);
        this.getCommand("deco").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(new PlayerTell(), this);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("shop") && sender instanceof BlockCommandSender block) {
            int amount = Integer.parseInt(args[1]);
            int price = Integer.parseInt(args[2]);
            ItemStack item = new ItemStack(Material.valueOf(args[0].toUpperCase()), amount);

            Player player = block.getBlock().getLocation().add(0.5, 2.0, 0.5).getNearbyPlayers(0.5).iterator().next();

            int lvl = player.getLevel();
            if (lvl >= price) {
                if (player.getPitch() > 89.0F) {
                    store.putIfAbsent(player.getName(), Pair.of(Material.AIR, Pair.of(0, 0)));
                }
                player.sendTitle("[§e定额购买模式§f]", "请在聊天栏中输入你要购买的数量");
                String num;
                if (lvl / price * amount > 640) {
                    num = "640";
                } else {
                    num = "" + lvl / price * amount;
                }

                player.sendMessage("\n§l：最大可购数量为§e " + num + "\n====================");
                store.put(player.getName(), Pair.of(Material.valueOf(args[0].toUpperCase()), Pair.of(price, amount)));
                if (player.getInventory().firstEmpty() != -1) {
                    player.setLevel(lvl - price);
                    player.getInventory().addItem(item);
                    player.sendMessage("§e§lLevel: §r§6" + (lvl - price) + "§f | §eCOST : " + price);
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1.0F, 1.0F);
                } else {
                    player.sendTitle("[§e背包满了§f]", "§7腾个位置出来再买吧", 10, 30, 20);
                }
            } else {
                player.sendTitle("", "§e§lLevel : §c" + lvl);
            }
        }


        if (cmd.getName().equalsIgnoreCase("sell") && sender instanceof BlockCommandSender block) {
            Player player = block.getBlock().getLocation().add(0.5, 2.0, 0.5).getNearbyPlayers(0.5).iterator().next();

            Material item = Material.valueOf(args[0].toUpperCase());
            int amount = Integer.parseInt(args[1]);
            int price = Integer.parseInt(args[2]);
            int limit = player.getScoreboard().getObjective("limit").getScore(player.getName()).getScore();

            if (limit < 400) {
                Boolean has = false;
                ItemStack[] contents = player.getInventory().getContents();
                int result = 0;

                int t;
                for (t = 0; t < contents.length; t++) {
                    if (contents[t] != null && contents[t].getType().equals(item) && contents[t].getAmount() >= amount) {
                        limit = player.getScoreboard().getObjective("limit").getScore(player.getName()).getScore();
                        if (limit >= 400) {
                            break;
                        }

                        int group = contents[t].getAmount() / amount;
                        if (group > 400 - limit) {
                            group = 400 - limit;
                        }

                        contents[t].setAmount(contents[t].getAmount() - amount * group);
                        result += price * group;
                        has = true;
                        if (item.equals(Material.valueOf("JACK_O_LANTERN"))) {
                            group *= 4;
                        }

                        player.getScoreboard().getObjective("limit").getScore(player.getName()).setScore(limit + group);
                    }
                }

                if (has) {
                    player.giveExpLevels(result);
                    player.sendMessage("§e§lLevel : §r§6" + (player.getLevel() + result) + "§f | §eIN : " + result);
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0F, 1.0F);
                    partice(player, result);
                } else {
                    player.sendTitle("[§e没货了§f]", "§7补货再来哦", 0, 20, 35);
                }
            } else {
                player.sendTitle("[§e出售上限§f]", "§7今天出售额度用完了——§f明天§e刷新", 0, 20, 35);
            }
        }


        if (cmd.getName().equalsIgnoreCase("enchants") && sender instanceof BlockCommandSender block) {
            Player player = block.getBlock().getLocation().add(0.5, 2.0, 0.5).getNearbyPlayers(0.5).iterator().next();

            int purse = player.getLevel();
            int price = Integer.parseInt(args[2]);
            if (purse >= price) {
                if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
                    player.sendTitle("[§e附魔错误§f]", "§7您这麒麟臂可不敢随便附魔", 10, 30, 20);
                } else {
                    Enchantment enchant = Enchantment.getByName(args[0].toUpperCase());
                    int level = Integer.parseInt(args[1]);
                    ItemStack item = player.getInventory().getItemInMainHand();
                    ItemMeta meta = item.getItemMeta();
                    if (meta.hasEnchant(enchant) && meta.getEnchantLevel(enchant) >= level) {
                        player.sendTitle("[§e重复附魔§f]", "§7物品存在重复等级附魔", 10, 30, 20);
                    } else {
                        meta.addEnchant(enchant, level, true);
                        item.setItemMeta(meta);
                        player.giveExpLevels(-price);
                        player.sendMessage("§e§lLevel : §r§6" + (purse - price) + "§f | §eCOST : " + price);
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0F, 1.0F);
                    }
                }
            } else {
                player.sendTitle("", "§e§lLevel : §c" + purse);
            }
        }


        if (cmd.getName().equalsIgnoreCase("deco") && sender instanceof BlockCommandSender block) {
            Player player = block.getBlock().getLocation().add(0.5, 2.0, 0.5).getNearbyPlayers(0.5).iterator().next();

            int purse = player.getLevel();
            int price = Integer.parseInt(args[1]);
            String decoration = args[0].toUpperCase();
            if (purse >= price) {
                Material material = player.getInventory().getItemInMainHand().getType();
                ItemStack[] armorContents = player.getInventory().getArmorContents();
                List<Material> materialList = Arrays.asList(Material.DIAMOND, Material.AMETHYST_SHARD, Material.COPPER_INGOT, Material.GOLD_INGOT, Material.EMERALD, Material.IRON_INGOT, Material.LAPIS_LAZULI, Material.NETHERITE_INGOT, Material.QUARTZ, Material.REDSTONE);

                for (int i = 0; i < armorContents.length; i++) {
                    ItemStack item = armorContents[i];
                    if (item != null) {
                        if (!materialList.contains(player.getInventory().getItemInMainHand().getType())) {
                            player.sendTitle("[§e雕刻材质不支持§f]", "§7手持支持的雕刻矿物", 10, 30, 20);
                        } else {
                            ArmorMeta meta = (ArmorMeta) item.getItemMeta();
                            ArmorTrim trim = meta.getTrim();
                            if (trim != null && trim.getPattern().equals(String2Trim(decoration)) && trim.getMaterial().equals(Material2Trim(material))) {
                                player.sendTitle("[§e重复雕刻§f]", "§7换个物品再买哦(不是§f贪钱狐", 10, 30, 20);
                            } else {
                                meta.setTrim(new ArmorTrim(Material2Trim(material), String2Trim(decoration)));
                                item.setItemMeta(meta);
                                player.giveExpLevels(-price);
                                player.sendMessage("§e§lLevel : §r§6" + (purse - price) + "§f | §eCOST : " + price);
                                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1.0F, 1.0F);
                            }
                        }
                    }
                }
            } else {
                player.sendTitle("","§e§lLevel : §c" + purse);
            }
        }


        return true;
    }

    public TrimPattern String2Trim(String deco) {
        String[] keyword = new String[]{"BOLT", "EYE", "COAST", "RIB", "DUNE", "FLOW", "HOST", "RAISER", "SENTRY", "SHAPER", "SILENCE", "SNOUT", "SPIRE", "TIDE", "VEX", "WARD", "WAYFINDER", "WILD"};
        TrimPattern[] pattern = new TrimPattern[]{TrimPattern.BOLT, TrimPattern.EYE, TrimPattern.COAST, TrimPattern.RIB, TrimPattern.DUNE, TrimPattern.FLOW, TrimPattern.HOST, TrimPattern.RAISER, TrimPattern.SENTRY, TrimPattern.SHAPER, TrimPattern.SILENCE, TrimPattern.SNOUT, TrimPattern.SPIRE, TrimPattern.TIDE, TrimPattern.VEX, TrimPattern.WARD, TrimPattern.WAYFINDER, TrimPattern.WILD};

        for (int i = 0; i < keyword.length; ++i) {
            if (deco.equals(keyword[i])) {
                return pattern[i];
            }
        }
        return null;
    }

    public TrimMaterial Material2Trim(Material material) {
        Material[] materialList = new Material[]{Material.DIAMOND, Material.AMETHYST_SHARD, Material.COPPER_INGOT, Material.GOLD_INGOT, Material.EMERALD, Material.IRON_INGOT, Material.LAPIS_LAZULI, Material.NETHERITE_INGOT, Material.QUARTZ, Material.REDSTONE};
        TrimMaterial[] trim = new TrimMaterial[]{TrimMaterial.DIAMOND, TrimMaterial.AMETHYST, TrimMaterial.COPPER, TrimMaterial.GOLD, TrimMaterial.EMERALD, TrimMaterial.IRON, TrimMaterial.LAPIS, TrimMaterial.NETHERITE, TrimMaterial.QUARTZ, TrimMaterial.REDSTONE};

        for (int i = 0; i < materialList.length; ++i) {
            if (material.equals(materialList[i])) {
                return trim[i];
            }
        }
        return null;
    }

    public void partice(final Player pl, int result) {
        final Location boxLoc = pl.getLocation().add(0.5, 1.2, 0.5);
        Particle.DustTransition dustTransition = new Particle.DustTransition(Color.YELLOW, Color.ORANGE, 0.8F);
        final ItemStack goldBlockItem = new ItemStack(Material.GOLD_INGOT);
        pl.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 2, 1));
        pl.playSound(pl.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.0F, 1.0F);
        final int turn = result / 10 + 3;
        new BukkitRunnable() {
            int i = 0;

            public void run() {
                if (this.i == turn) {
                    this.cancel();
                }

                ++this.i;
                final Item gold = pl.getWorld().dropItemNaturally(boxLoc, goldBlockItem);
                gold.setCanPlayerPickup(false);
                (new BukkitRunnable() {
                    public void run() {
                        gold.remove();
                    }
                }).runTaskLater(Supermarket.this, 80L);
            }
        }.runTaskTimer(this, 0L, 2L);
        pl.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, boxLoc, 10, 0.5, 0.5, 0.5, 0.20000000298023224, dustTransition);
    }
}
