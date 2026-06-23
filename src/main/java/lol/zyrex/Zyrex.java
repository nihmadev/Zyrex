package lol.zyrex;

import antidaunleak.api.UserProfile;
import lol.zyrex.commands.manager.CommandRepository;
import lol.zyrex.utils.client.managers.file.exception.FileProcessingException;
import lol.zyrex.utils.client.chat.ChatMessage;
import lol.zyrex.utils.client.logs.Logger;
import lol.zyrex.utils.connection.auracheckft.FTCheckClient;
import lol.zyrex.utils.connection.irc.IRCManager;
import lol.zyrex.utils.connection.tps.TPSCalculate;
import lol.zyrex.utils.display.scissor.ScissorAssist;
import net.fabricmc.api.ModInitializer;
import lol.zyrex.common.repository.box.BoxESPRepository;
import lol.zyrex.common.repository.rct.RCTRepository;
import lol.zyrex.common.repository.way.WayRepository;
import lol.zyrex.common.discord.DiscordManager;
import lol.zyrex.utils.client.managers.api.draggable.DraggableRepository;
import lol.zyrex.utils.client.managers.file.*;
import lol.zyrex.common.repository.macro.MacroRepository;
import lol.zyrex.utils.client.managers.event.EventManager;
import lol.zyrex.features.module.ModuleProvider;
import lol.zyrex.features.module.ModuleRepository;
import lol.zyrex.features.module.ModuleSwitcher;
import lol.zyrex.utils.client.sound.SoundManager;
import lol.zyrex.display.screens.clickgui.MenuScreen;
import lol.zyrex.utils.connection.cloud.CloudConfigWebSocketClient;
import lol.zyrex.main.client.ClientInfo;
import lol.zyrex.main.client.ClientInfoProvider;
import lol.zyrex.main.listener.ListenerRepository;
import lol.zyrex.commands.CommandDispatcher;
import lol.zyrex.utils.features.aura.neuro.NeuroRepository;
import lol.zyrex.utils.features.aura.striking.StrikerConstructor;
import lol.zyrex.utils.client.ModAutoLoader;
import com.google.common.eventbus.EventBus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.MinecraftClient;
import java.io.File;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lol.zyrex.display.screens.mainmenu.altscreen.impl.AccountRepository;
import lol.zyrex.utils.client.managers.file.impl.AccountFile;
import lol.zyrex.utils.client.managers.file.impl.AutoBuyConfigFile;
import lol.zyrex.display.screens.mainmenu.altscreen.impl.Account;
import lol.zyrex.mixins.client.IMinecraftClient;
import net.minecraft.client.session.Session;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.ReporterEnvironment;
import com.mojang.authlib.minecraft.UserApiService;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Zyrex implements ModInitializer {
    @Getter
    static Zyrex instance;
    EventManager eventManager = new EventManager();
    EventBus eventBus = new EventBus();
    ModuleRepository moduleRepository;
    ModuleSwitcher moduleSwitcher;
    CommandRepository commandRepository;
    CommandDispatcher commandDispatcher;
    BoxESPRepository boxESPRepository = new BoxESPRepository(eventManager);
    MacroRepository macroRepository = new MacroRepository(eventManager);
    WayRepository wayRepository = new WayRepository(eventManager);
    RCTRepository RCTRepository = new RCTRepository(eventManager);
    ModuleProvider moduleProvider;
    DraggableRepository draggableRepository;
    DiscordManager discordManager;
    FileRepository fileRepository;
    FileController fileController;
    ScissorAssist scissorManager = new ScissorAssist();
    ClientInfoProvider clientInfoProvider;
    ListenerRepository listenerRepository;
    StrikerConstructor attackPerpetrator = new StrikerConstructor();
    CloudConfigWebSocketClient cloudConfigClient;
    FTCheckClient ftCheckClient;
    IRCManager ircManager = new IRCManager();
    AccountRepository accountRepository;
    TPSCalculate tpsCalculate;
    boolean initialized;
    boolean showIrcMessages = false;
    ScheduledExecutorService reconnectScheduler;
    boolean reconnecting = false;

    @Override
    public void onInitialize() {
        instance = this;
        ModAutoLoader.checkAndDownloadMods();
        initClientInfoProvider();
        initModules();
        initDraggable();
        initFileManager();
        initCommands();
        initListeners();
        // initDiscordRPC();       // Отключено — discordManager создаётся без init()
        discordManager = new DiscordManager();
        // initWebSocketClient();  // Отключено
        // initFTCheckClient();    // Отключено
        // ircManager.connect();   // Отключено
        // startReconnectTask();   // Отключено
        SoundManager.init();
        loadCurrentAccount();

        MenuScreen menuScreen = new MenuScreen();
        menuScreen.initialize();
        initialized = true;

        new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (Exception e) {
            }
        }).start();
    }


    private void loadCurrentAccount() {
        if (accountRepository.currentAccount != null && !accountRepository.currentAccount.isEmpty()) {
            Account currentAcc = accountRepository.accountList.stream()
                    .filter(acc -> acc.name.equals(accountRepository.currentAccount))
                    .findFirst()
                    .orElse(null);

            if (currentAcc != null) {
                setSession(currentAcc);
            }
        }
    }

    private void setSession(Account account) {
        Session newSession = new Session(account.name, UUID.fromString(account.uuid), "0", Optional.empty(), Optional.empty(), Session.AccountType.MOJANG);
        IMinecraftClient mca = (IMinecraftClient) MinecraftClient.getInstance();
        mca.setSessionT(newSession);
        MinecraftClient.getInstance().getGameProfile().getProperties().clear();
        UserApiService apiService = UserApiService.OFFLINE;
        mca.setUserApiService(apiService);
        mca.setSocialInteractionsManagerT(new SocialInteractionsManager(MinecraftClient.getInstance(), apiService));
        mca.setProfileKeys(ProfileKeys.create(apiService, newSession, MinecraftClient.getInstance().runDirectory.toPath()));
        mca.setAbuseReportContextT(AbuseReportContext.create(ReporterEnvironment.ofIntegratedServer(), apiService));
    }

    private void initWebSocketClient() {
        // Отключено - соединение с внешним сервером
        // try {
        //     cloudConfigClient = new CloudConfigWebSocketClient(new URI("ws://45.155.205.202:8080"));
        //     cloudConfigClient.connect();
        // } catch (Exception e) {
        // }
    }

    private void initFTCheckClient() {
        // Отключено - соединение с внешним сервером 
        // try {
        //     ftCheckClient = new FTCheckClient(new URI("ws://45.155.205.202:6312"));
        //     ftCheckClient.connect();
        // } catch (Exception e) {
        // }
    }

    private void initDraggable() {
        draggableRepository = new DraggableRepository();
        draggableRepository.setup();
    }

    private void initModules() {
        moduleRepository = new ModuleRepository();
        moduleRepository.setup();
        moduleProvider = new ModuleProvider(moduleRepository.modules());
        moduleSwitcher = new ModuleSwitcher(moduleRepository.modules(), eventManager);
    }

    private void initCommands() {
        commandRepository = new CommandRepository();
        commandDispatcher = new CommandDispatcher(eventManager);
    }

    private void initClientInfoProvider() {
        File clientDirectory = new File(MinecraftClient.getInstance().runDirectory, "\\Zyrex\\");
        File filesDirectory = new File(clientDirectory, "\\Files\\");
        clientInfoProvider = new ClientInfo("Zyrex", "nihmadev, without sleep:(", "Developer", clientDirectory, filesDirectory);
    }

    private void initFileManager() {
        DirectoryCreator directoryCreator = new DirectoryCreator();
        directoryCreator.createDirectories(clientInfoProvider.clientDir(), clientInfoProvider.filesDir());

        File autoBuyDir = new File(clientInfoProvider.clientDir(), "AutoBuy");
        if (!autoBuyDir.exists()) {
            autoBuyDir.mkdirs();
        }

        File customDir = new File(clientInfoProvider.clientDir(), "Custom");
        if (!customDir.exists()) {
            customDir.mkdirs();
        }

        fileRepository = new FileRepository();
        fileRepository.setup(this);
        accountRepository = new AccountRepository();
        fileRepository.getClientFiles().add(new AccountFile(accountRepository));
        fileRepository.getClientFiles().add(new AutoBuyConfigFile());
        fileController = new FileController(fileRepository.getClientFiles(), clientInfoProvider.filesDir());
        try {
            fileController.loadFiles();
        } catch (FileProcessingException e) {
            Logger.error("Failed to load files: " + e.getMessage());
        }
    }

    private void initListeners() {
        listenerRepository = new ListenerRepository();
        listenerRepository.setup();
        tpsCalculate = new TPSCalculate();
    }

    private void startReconnectTask() {
        reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
        reconnectScheduler.scheduleAtFixedRate(() -> {
            if ((ircManager.getClient() == null || !ircManager.getClient().isOpen()) && !reconnecting) {
                reconnecting = true;
                try {
                    ircManager.connect();
                } catch (Exception e) {
                    if (showIrcMessages) {
                        ChatMessage.ircmessageWithRed("Переподключение к серверу IRC не удалось");
                    }
                } finally {
                    reconnecting = false;
                }
            }
        }, 10, 10, TimeUnit.SECONDS);
    }
}