package sudark.supermarket;

import it.unimi.dsi.fastutil.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ConcurrentHashMap;

public class PlayerTell implements Listener {

    Supermarket spm = new Supermarket();

    @EventHandler
    public void onPlayerTell(PlayerChatEvent e) {
        Player pl = e.getPlayer();
        ConcurrentHashMap<String, Pair<Material, Pair<Integer, Integer>>> store = spm.getStore();
        int purse = pl.getScoreboard().getObjective("mim").getScore(pl.getName()).getScore();
        if (store.containsKey(e.getPlayer().getName()) && ((Pair)store.get(e.getPlayer().getName())).left() != Material.AIR) {
            String msg = e.getMessage();
            int price = (Integer)((Pair)((Pair)store.get(e.getPlayer().getName())).right()).left();
            int account = (Integer)((Pair)((Pair)store.get(e.getPlayer().getName())).right()).right();

            int num;
            try {
                num = Integer.parseInt(msg);
            } catch (NumberFormatException var10) {
                pl.sendMessage("你输入的内容不是一个阿拉伯数字");
                return;
            }

            if (num > account * purse / price) {
                pl.sendMessage("数字过大 已按照最大数量提供");
                num = account * purse / price;
            }

            ItemStack item = new ItemStack((Material)((Pair)store.get(e.getPlayer().getName())).left(), num);
            Bukkit.getWorld("BEEF-Main").dropItem(pl.getLocation().add(0.0, 1.0, 0.0), item);
            int cost = num / account * price;
            pl.getScoreboard().getObjective("mim").getScore(pl.getName()).setScore(purse - cost);
            pl.sendActionBar("§e§lMIM : §r§6" + (purse - cost) + "§f | §eCOST : " + cost + "§f | §bNUM : " + num);
            pl.playSound(pl.getLocation(), Sound.ENTITY_PIGLIN_ADMIRING_ITEM, 1.0F, 1.0F);
            store.put(pl.getName(), Pair.of(Material.AIR, Pair.of(0, 0)));
        }

    }
}