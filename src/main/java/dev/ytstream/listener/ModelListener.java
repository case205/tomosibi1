package dev.ytstream.listener;

import dev.ytstream.config.Settings;
import dev.ytstream.model.ServerModel;
import dev.ytstream.model.ServerModel.PlayerEntry;
import dev.ytstream.render.SkinService;
import dev.ytstream.util.TextUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Bukkitイベント → ServerModel への橋渡し。ここでの処理はキューへの追加のみで軽量。
 * Java版/統合版(Geyser/Floodgate)の区別なく、サーバー上のPlayerとして扱う。
 */
public final class ModelListener implements Listener {
    private final ServerModel model;
    private final SkinService skins;
    private volatile Settings settings;

    public ModelListener(ServerModel model, SkinService skins, Settings settings) {
        this.model = model;
        this.skins = skins;
        this.settings = settings;
    }

    public void setSettings(Settings s) {
        this.settings = s;
    }

    private boolean hidden(Player p) {
        if (p.hasPermission("ytstream.hide")) return true;
        if (!settings.hideVanished()) return false;
        for (MetadataValue v : p.getMetadata("vanished")) {
            if (v.asBoolean()) return true;
        }
        return false;
    }

    private void add(Player p, boolean toast) {
        Settings s = settings;
        String name = p.getName();
        boolean bedrock = !s.bedrockPrefix().isEmpty() && name.startsWith(s.bedrockPrefix());
        PlayerEntry entry = new PlayerEntry(p.getUniqueId(), name, bedrock);
        if (!model.playerJoined(entry)) return;
        if (toast && s.showJoinLeave()) model.addToast(true, name, System.currentTimeMillis());
        try {
            PlayerTextures tex = p.getPlayerProfile().getTextures();
            URL skin = tex.getSkin();
            if (skin != null) {
                skins.loadHead(skin.toString(), img -> {
                    entry.head = img;
                    model.bump();
                });
            }
        } catch (RuntimeException ignored) {
            // スキン情報が無い(統合版など)場合はプレースホルダ表示
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        if (hidden(e.getPlayer())) return;
        add(e.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        PlayerEntry removed = model.playerLeft(e.getPlayer().getUniqueId());
        if (removed != null && settings.showJoinLeave()) {
            model.addToast(false, removed.name, System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        Settings s = settings;
        if (!s.showChat()) return;
        Player p = e.getPlayer();
        if (!model.contains(p.getUniqueId())) return; // 非表示プレイヤーのチャットは映さない
        String text = PlainTextComponentSerializer.plainText().serialize(e.message());
        text = TextUtil.sanitize(text, s.chatMaxLength());
        if (text.isBlank()) return;
        for (Pattern pat : s.chatBlocked()) {
            text = pat.matcher(text).replaceAll("***");
        }
        model.addChat(p.getName(), text, System.currentTimeMillis());
    }

    /** 起動時の初期登録と、バニッシュ切替・イベント取りこぼしの補正(メインスレッドで呼ぶ)。 */
    public void sync() {
        model.setMaxPlayers(Bukkit.getMaxPlayers());
        Set<UUID> visible = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (hidden(p)) continue;
            visible.add(p.getUniqueId());
            add(p, false);
        }
        for (PlayerEntry e : model.players()) {
            if (!visible.contains(e.uuid)) model.playerLeft(e.uuid);
        }
    }
}
