package fr.maxlego08.essentials;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.impl.ServerImplementation;
import fr.maxlego08.essentials.api.ConfigurationFile;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.modules.ModuleManager;
import fr.maxlego08.essentials.api.user.User;
import fr.maxlego08.essentials.api.commands.CommandManager;
import fr.maxlego08.essentials.api.storage.Persist;
import fr.maxlego08.essentials.api.storage.StorageManager;
import fr.maxlego08.essentials.api.storage.adapter.LocationAdapter;
import fr.maxlego08.essentials.commands.CommandLoader;
import fr.maxlego08.essentials.commands.ZCommandManager;
import fr.maxlego08.essentials.commands.commands.essentials.CommandEssentials;
import fr.maxlego08.essentials.messages.MessageLoader;
import fr.maxlego08.essentials.module.ZModuleManager;
import fr.maxlego08.essentials.storage.ZStorageManager;
import fr.maxlego08.essentials.storage.ZUser;
import fr.maxlego08.essentials.storage.adapter.UserTypeAdapter;
import fr.maxlego08.essentials.zutils.ZPlugin;
import org.bukkit.Location;

import java.lang.reflect.Modifier;
import java.util.List;

public final class ZEssentialsPlugin extends ZPlugin implements EssentialsPlugin {

    @Override
    public void onEnable() {

        this.saveDefaultConfig();

        FoliaLib foliaLib = new FoliaLib(this);
        this.serverImplementation = foliaLib.getImpl();

        this.moduleManager = new ZModuleManager(this);

        this.gson = getGsonBuilder().create();
        this.persist = new Persist(this);

        // Configurations files
        this.registerConfiguration(new MessageLoader(this));

        // Load configuration files
        this.configurationFiles.forEach(ConfigurationFile::load);

        // Commands
        this.commandManager = new ZCommandManager(this);
        this.registerCommand("zessentials", new CommandEssentials(this), "ess");

        CommandLoader commandLoader = new CommandLoader(this);
        commandLoader.loadCommands(this.commandManager);

        this.getLogger().info("Create " + this.commandManager.countCommands() + " commands.");

        // Storage
        this.storageManager = new ZStorageManager(this);
        this.registerListener(this.storageManager);
        this.storageManager.onEnable();

        this.moduleManager.loadModules();
    }

    @Override
    public void onDisable() {

        // Storage
        this.storageManager.onDisable();
    }

    @Override
    public CommandManager getCommandManager() {
        return this.commandManager;
    }

    @Override
    public List<ConfigurationFile> getConfigurationFiles() {
        return this.configurationFiles;
    }

    @Override
    public Gson getGson() {
        return this.gson;
    }

    @Override
    public Persist getPersist() {
        return this.persist;
    }

    @Override
    public ServerImplementation getScheduler() {
        return this.serverImplementation;
    }

    @Override
    public ModuleManager getModuleManager() {
        return this.moduleManager;
    }

    @Override
    public StorageManager getStorageManager() {
        return this.storageManager;
    }

    private GsonBuilder getGsonBuilder() {
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.VOLATILE)
                .registerTypeAdapter(Location.class, new LocationAdapter(this))
                .registerTypeAdapter(User.class, new UserTypeAdapter(this))
                .registerTypeAdapter(ZUser.class, new UserTypeAdapter(this))
                ;
    }
}
