package fr.maxlego08.essentials.module.modules;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.cache.ExpiringCache;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.api.sanction.Sanction;
import fr.maxlego08.essentials.api.sanction.SanctionType;
import fr.maxlego08.essentials.api.server.EssentialsServer;
import fr.maxlego08.essentials.api.storage.IStorage;
import fr.maxlego08.essentials.api.user.Option;
import fr.maxlego08.essentials.api.user.User;
import fr.maxlego08.essentials.module.ZModule;
import fr.maxlego08.essentials.user.ZUser;
import fr.maxlego08.essentials.zutils.utils.TimerBuilder;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class SanctionModule extends ZModule {

    private final ExpiringCache<UUID, User> expiringCache = new ExpiringCache<>(1000 * 60 * 60); // 1 hour cache
    private SimpleDateFormat simpleDateFormat;
    // Default messages for kick and ban
    private String kickDefaultReason = "";
    private String banDefaultReason = "";
    private String muteDefaultReason = "";
    private String unmuteDefaultReason = "";
    private String unbanDefaultReason = "";
    private String dateFormat = "yyyy-MM-dd HH:mm:ss";
    private Material kickMaterial = Material.BOOK;
    private Material banMaterial = Material.BOOK;
    private Material muteMaterial = Material.BOOK;
    private Material unbanMaterial = Material.BOOK;
    private Material unmuteMaterial = Material.BOOK;
    private Material warnMaterial = Material.BOOK;
    private Material currentMuteMaterial = Material.BOOKSHELF;
    private Material currentBanMaterial = Material.BOOKSHELF;
    private List<String> protections = new ArrayList<>();


    public SanctionModule(ZEssentialsPlugin plugin) {
        super(plugin, "sanction");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        this.loadInventory("sanction");
        this.loadInventory("sanction_history");
        this.loadInventory("sanctions");
        this.simpleDateFormat = new SimpleDateFormat(this.dateFormat);
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public String getKickDefaultReason() {
        return kickDefaultReason;
    }

    public String getBanDefaultReason() {
        return banDefaultReason;
    }

    public String getMuteDefaultReason() {
        return muteDefaultReason;
    }

    public String getUnbanDefaultReason() {
        return unbanDefaultReason;
    }

    public String getUnmuteDefaultReason() {
        return unmuteDefaultReason;
    }

    public Material getSanctionMaterial(SanctionType sanctionType, boolean isActive) {
        return switch (sanctionType) {
            case KICK -> kickMaterial;
            case MUTE -> isActive ? currentMuteMaterial : muteMaterial;
            case BAN -> isActive ? currentBanMaterial : banMaterial;
            case UNBAN -> unbanMaterial;
            case UNMUTE -> unmuteMaterial;
            case WARN -> warnMaterial;
        };
    }


    // Get the UUID of the sender (player or console)
    private UUID getSenderUniqueId(CommandSender sender) {
        return sender instanceof Player player ? player.getUniqueId() : this.plugin.getConsoleUniqueId();
    }

    /**
     * Kick a player with a specified reason.
     *
     * @param sender     The command sender.
     * @param uuid       The UUID of the player to kick.
     * @param playerName The name of the player to kick.
     * @param reason     The reason for the kick.
     */
    public void kick(CommandSender sender, UUID uuid, String playerName, String reason) {

        if (isProtected(playerName)) {
            message(sender, Message.COMMAND_SANCTION_ERROR);
            return;
        }

        EssentialsServer server = plugin.getEssentialsServer();
        IStorage iStorage = plugin.getStorageManager().getStorage();

        // Create and save the sanction
        Sanction sanction = Sanction.kick(uuid, getSenderUniqueId(sender), reason);
        iStorage.insertSanction(sanction, sanction::setId);
        this.expiringCache.clear(uuid);

        // Kick the player with the specified reason
        server.kickPlayer(uuid, Message.MESSAGE_KICK, "%reason%", reason);

        // Broadcast a notification message to players with the kick notify permission
        server.broadcastMessage(Permission.ESSENTIALS_KICK_NOTIFY, Message.COMMAND_KICK_NOTIFY, "%player%", sender.getName(), "%target%", playerName, "%reason%", reason, "%sender%", getSanctionBy(sender), "%created_at%", this.simpleDateFormat.format(new Date()));
    }

    /**
     * Ban a player for a specified duration with a reason.
     *
     * @param sender     The command sender.
     * @param uuid       The UUID of the player to ban.
     * @param playerName The name of the player to ban.
     * @param duration   The duration of the ban.
     * @param reason     The reason for the ban.
     */
    public void ban(CommandSender sender, UUID uuid, String playerName, Duration duration, String reason) {

        if (isProtected(playerName)) {
            message(sender, Message.COMMAND_SANCTION_ERROR);
            return;
        }

        EssentialsServer server = plugin.getEssentialsServer();
        IStorage iStorage = plugin.getStorageManager().getStorage();

        // Check if the ban duration is valid
        if (duration.isZero()) {
            message(sender, Message.COMMAND_BAN_ERROR_DURATION);
            return;
        }

        // Calculate the ban finish date
        Date finishAt = new Date(System.currentTimeMillis() + duration.toMillis());

        // Create and save the sanction
        Sanction sanction = Sanction.ban(uuid, getSenderUniqueId(sender), reason, duration, finishAt);
        iStorage.insertSanction(sanction, index -> {
            sanction.setId(index);
            iStorage.updateUserBan(uuid, index);
        });
        this.expiringCache.clear(uuid);

        String durationString = TimerBuilder.getStringTime(duration.toMillis());
        // Ban the player with the specified reason and duration
        server.kickPlayer(uuid, Message.MESSAGE_BAN, "%reason%", reason, "%duration%", TimerBuilder.getStringTime(duration.toMillis()));

        // Broadcast a notification message to players with the ban notify permission
        server.broadcastMessage(Permission.ESSENTIALS_BAN_NOTIFY, Message.COMMAND_BAN_NOTIFY, "%player%", sender.getName(), "%target%", playerName, "%reason%", reason, "%duration%", durationString, "%sender%", getSanctionBy(sender), "%created_at%", this.simpleDateFormat.format(new Date()), "%expired_at%", this.simpleDateFormat.format(sanction.getExpiredAt()));
    }

    /**
     * Mute a player for a specified duration with a reason.
     *
     * @param sender     The command sender.
     * @param uuid       The UUID of the player to mute.
     * @param playerName The name of the player to mute.
     * @param duration   The duration of the mute.
     * @param reason     The reason for the mute.
     */
    public void mute(CommandSender sender, UUID uuid, String playerName, Duration duration, String reason) {

        if (isProtected(playerName)) {
            message(sender, Message.COMMAND_SANCTION_ERROR);
            return;
        }

        EssentialsServer server = plugin.getEssentialsServer();
        IStorage iStorage = plugin.getStorageManager().getStorage();

        // Check if the mute duration is valid
        if (duration.isZero()) {
            message(sender, Message.COMMAND_MUTE_ERROR_DURATION);
            return;
        }

        // Calculate the mute finish date
        Date finishAt = new Date(System.currentTimeMillis() + duration.toMillis());

        // Create and save the sanction
        Sanction sanction = Sanction.mute(uuid, getSenderUniqueId(sender), reason, duration, finishAt);
        iStorage.insertSanction(sanction, index -> {
            sanction.setId(index);
            iStorage.updateUserMute(uuid, index);

            User user = iStorage.getUser(uuid);
            if (user != null) {// If user is online, update cache
                user.setMuteSanction(sanction);
            }
        });
        this.expiringCache.clear(uuid);

        // Mute the player with the specified reason and duration
        server.sendMessage(uuid, Message.MESSAGE_MUTE, "%reason%", reason, "%duration%", TimerBuilder.getStringTime(duration.toMillis()));

        // Broadcast a notification message to players with the mute notify permission
        server.broadcastMessage(Permission.ESSENTIALS_MUTE_NOTIFY, Message.COMMAND_MUTE_NOTIFY, "%player%", sender.getName(), "%target%", playerName, "%reason%", reason, "%duration%", TimerBuilder.getStringTime(duration.toMillis()), "%sender%", getSanctionBy(sender), "%created_at%", this.simpleDateFormat.format(new Date()), "%expired_at%", this.simpleDateFormat.format(sanction.getExpiredAt()));
    }

    /**
     * UnMute a player with a reason.
     *
     * @param sender     The command sender.
     * @param uuid       The UUID of the player to unmute.
     * @param playerName The name of the player to unmute.
     * @param reason     The reason for to unmute.
     */
    public void unmute(CommandSender sender, UUID uuid, String playerName, String reason) {

        if (isProtected(playerName)) {
            message(sender, Message.COMMAND_SANCTION_ERROR);
            return;
        }

        IStorage iStorage = plugin.getStorageManager().getStorage();

        User user = iStorage.getUser(uuid);
        if (user == null) {
            // Check is user is mute
            this.plugin.getScheduler().runAsync(wrappedTask -> {
                if (!iStorage.isMute(uuid)) {
                    message(sender, Message.COMMAND_UN_MUTE_ERROR, "%player%", playerName);
                    return;
                }

                processUnmute(sender, uuid, playerName, reason);
            });
        } else {
            // Check is user is mute
            if (!user.isMute()) {
                message(sender, Message.COMMAND_UN_MUTE_ERROR, "%player%", playerName);
                return;
            }

            processUnmute(sender, uuid, playerName, reason);
        }
    }

    private void processUnmute(CommandSender sender, UUID uuid, String playerName, String reason) {

        EssentialsServer server = plugin.getEssentialsServer();
        IStorage iStorage = plugin.getStorageManager().getStorage();

        // Create and save the sanction
        Sanction sanction = Sanction.unmute(uuid, getSenderUniqueId(sender), reason);
        iStorage.insertSanction(sanction, index -> {
            sanction.setId(index);
            iStorage.updateUserMute(uuid, null);

            User user = iStorage.getUser(uuid);
            if (user != null) { // If user is online, update cache
                user.setMuteSanction(null);
            }
        });
        this.expiringCache.clear(uuid);

        // Mute the player with the specified reason and duration
        server.sendMessage(uuid, Message.MESSAGE_UNMUTE, "%reason%", reason);

        // Broadcast a notification message to players with the mute notify permission
        server.broadcastMessage(Permission.ESSENTIALS_UNMUTE_NOTIFY, Message.COMMAND_UNMUTE_NOTIFY, "%player%", sender.getName(), "%target%", playerName, "%reason%", reason, "%sender%", getSanctionBy(sender), "%created_at%", this.simpleDateFormat.format(new Date()));
    }

    /**
     * UnBan a player with a reason.
     *
     * @param sender     The command sender.
     * @param uuid       The UUID of the player to unban.
     * @param playerName The name of the player to unban.
     * @param reason     The reason for the unban.
     */
    public void unban(CommandSender sender, UUID uuid, String playerName, String reason) {

        if (isProtected(playerName)) {
            message(sender, Message.COMMAND_SANCTION_ERROR);
            return;
        }

        EssentialsServer server = plugin.getEssentialsServer();
        IStorage iStorage = plugin.getStorageManager().getStorage();
        if (!iStorage.isBan(uuid)) {
            message(sender, Message.COMMAND_UN_BAN_ERROR, "%player%", playerName);
            return;
        }

        // Create and save the sanction
        Sanction sanction = Sanction.unban(uuid, getSenderUniqueId(sender), reason);
        iStorage.insertSanction(sanction, index -> {
            sanction.setId(index);
            iStorage.updateUserBan(uuid, null);
        });
        this.expiringCache.clear(uuid);

        // Broadcast a notification message to players with the mute notify permission
        server.broadcastMessage(Permission.ESSENTIALS_UNBAN_NOTIFY, Message.COMMAND_UNBAN_NOTIFY, "%player%", sender.getName(), "%target%", playerName, "%reason%", reason, "%sender%", getSanctionBy(sender), "%created_at%", this.simpleDateFormat.format(new Date()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTalk(AsyncChatEvent event) {

        Player player = event.getPlayer();
        IStorage iStorage = plugin.getStorageManager().getStorage();
        User user = iStorage.getUser(player.getUniqueId());
        if (user != null && user.isMute()) {
            event.setCancelled(true);
            Sanction sanction = user.getMuteSanction();
            Duration duration = sanction.getDurationRemaining();
            message(player, Message.MESSAGE_MUTE_TALK, "%reason%", sanction.getReason(), "%duration%", TimerBuilder.getStringTime(duration.toMillis()));
        }
    }

    public void openSanction(User user, UUID uuid, String userName) {

        IStorage iStorage = this.plugin.getStorageManager().getStorage();
        this.plugin.getScheduler().runAsync(wrappedTask -> {

            user.setTargetUser(expiringCache.get(uuid, () -> {
                User fakeUser = ZUser.fakeUser(this.plugin, uuid, userName);
                Sanction muteSanction = iStorage.getMute(uuid);
                fakeUser.setFakeOption(Option.BAN, iStorage.isBan(uuid));
                fakeUser.setFakeOption(Option.MUTE, muteSanction != null && muteSanction.isActive());
                fakeUser.setMuteSanction(muteSanction);
                fakeUser.setBanSanction(iStorage.getBan(uuid));
                fakeUser.setFakeSanctions(iStorage.getSanctions(uuid));
                return fakeUser;
            }));

            this.plugin.openInventory(user.getPlayer(), "sanction");
        });
    }

    public String getSanctionBy(UUID senderUniqueId) {
        return senderUniqueId.equals(this.plugin.getConsoleUniqueId()) ? Message.CONSOLE.getMessage() : Bukkit.getOfflinePlayer(senderUniqueId).getName();
    }

    private String getSanctionBy(CommandSender sender) {
        return sender instanceof Player player ? player.getName() : Message.CONSOLE.getMessage();
    }

    private boolean isProtected(String username) {
        return this.protections.stream().anyMatch(name -> name.equalsIgnoreCase(username));
    }
}