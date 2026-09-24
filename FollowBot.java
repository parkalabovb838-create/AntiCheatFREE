package com.good.anticheat;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FollowBot {

    private final AntiCheatPlugin plugin;
    private final Map<UUID, BukkitRunnable> tasks = new HashMap<>();
    private final Map<UUID, Boolean> active = new HashMap<>();

    public FollowBot(AntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public void startFollow(Player target) {
        stopFollow(target);

        active.put(target.getUniqueId(), true);

        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!target.isOnline() || !active.getOrDefault(target.getUniqueId(), false)) {
                    cancel();
                    return;
                }

                ticks++;

                Location playerLoc = target.getLocation();

                // Логика follow-бота: телепортация рядом с игроком
                // (заглушка, так как без Citizens NPC не создать)

                if (ticks % 20 == 0) {
                    plugin.getFileLogger().log(target.getName(),
                            "FOLLOW_BOT tracking player at "
                                    + String.format("%.1f, %.1f, %.1f",
                                    playerLoc.getX(), playerLoc.getY(), playerLoc.getZ()));
                }
            }
        };

        task.runTaskTimer(plugin, 0L, 2L);
        tasks.put(target.getUniqueId(), task);

        plugin.getLogger().info("[ANTICHEAT] Follow-бот запущен для " + target.getName());
        target.sendMessage("§c[ANTICHEAT] §fЗа тобой следит бот проверки.");
    }

    public void stopFollow(Player target) {
        active.put(target.getUniqueId(), false);

        BukkitRunnable task = tasks.remove(target.getUniqueId());
        if (task != null) task.cancel();

        active.remove(target.getUniqueId());

        if (target.isOnline()) {
            target.sendMessage("§c[ANTICHEAT] §fБот проверки убран.");
        }
    }

    public boolean hasBot(Player p) {
        return active.getOrDefault(p.getUniqueId(), false);
    }

    public int count() {
        return active.size();
    }
}