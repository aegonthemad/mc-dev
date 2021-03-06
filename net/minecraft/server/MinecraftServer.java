package net.minecraft.server;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.security.KeyPair;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class MinecraftServer implements Runnable, IMojangStatistics, ICommandListener {

    public static Logger log = Logger.getLogger("Minecraft");
    private static MinecraftServer l = null;
    private final Convertable convertable;
    private final MojangStatisticsGenerator n = new MojangStatisticsGenerator("server", this);
    private final File universe;
    private final List p = new ArrayList();
    private final ICommandHandler q;
    public final MethodProfiler methodProfiler = new MethodProfiler();
    private String serverIp;
    private int s = -1;
    public WorldServer[] worldServer;
    private ServerConfigurationManagerAbstract t;
    private boolean isRunning = true;
    private boolean isStopped = false;
    private int ticks = 0;
    public String d;
    public int e;
    private boolean onlineMode;
    private boolean spawnAnimals;
    private boolean spawnNPCs;
    private boolean pvpMode;
    private boolean allowFlight;
    private String motd;
    private int D;
    private long E;
    private long F;
    private long G;
    private long H;
    public final long[] f = new long[100];
    public final long[] g = new long[100];
    public final long[] h = new long[100];
    public final long[] i = new long[100];
    public final long[] j = new long[100];
    public long[][] k;
    private KeyPair I;
    private String J;
    private String K;
    private boolean demoMode;
    private boolean N;
    private boolean O;
    private String P = "";
    private boolean Q = false;
    private long R;
    private String S;
    private boolean T;

    public MinecraftServer(File file1) {
        l = this;
        this.universe = file1;
        this.q = new CommandDispatcher();
        this.convertable = new WorldLoaderServer(file1);
    }

    protected abstract boolean init();

    protected void c(String s) {
        if (this.getConvertable().isConvertable(s)) {
            log.info("Converting map!");
            this.d("menu.convertingLevel");
            this.getConvertable().convert(s, new ConvertProgressUpdater(this));
        }
    }

    protected synchronized void d(String s) {
        this.S = s;
    }

    protected void a(String s, String s1, long i, WorldType worldtype) {
        this.c(s);
        this.d("menu.loadingLevel");
        this.worldServer = new WorldServer[3];
        this.k = new long[this.worldServer.length][100];
        IDataManager idatamanager = this.convertable.a(s, true);
        WorldData worlddata = idatamanager.getWorldData();
        WorldSettings worldsettings;

        if (worlddata == null) {
            worldsettings = new WorldSettings(i, this.getGamemode(), this.getGenerateStructures(), this.isHardcore(), worldtype);
        } else {
            worldsettings = new WorldSettings(worlddata);
        }

        if (this.N) {
            worldsettings.a();
        }

        for (int j = 0; j < this.worldServer.length; ++j) {
            byte b0 = 0;

            if (j == 1) {
                b0 = -1;
            }

            if (j == 2) {
                b0 = 1;
            }

            if (j == 0) {
                if (this.L()) {
                    this.worldServer[j] = new DemoWorldServer(this, idatamanager, s1, b0, this.methodProfiler);
                } else {
                    this.worldServer[j] = new WorldServer(this, idatamanager, s1, b0, worldsettings, this.methodProfiler);
                }
            } else {
                this.worldServer[j] = new SecondaryWorldServer(this, idatamanager, s1, b0, worldsettings, this.worldServer[0], this.methodProfiler);
            }

            this.worldServer[j].addIWorldAccess(new WorldManager(this, this.worldServer[j]));
            if (!this.H()) {
                this.worldServer[j].getWorldData().setGameType(this.getGamemode());
            }

            this.t.setPlayerFileData(this.worldServer);
        }

        this.c(this.getDifficulty());
        this.d();
    }

    protected void d() {
        short short1 = 196;
        long i = System.currentTimeMillis();

        this.d("menu.generatingTerrain");

        for (int j = 0; j < 1; ++j) {
            log.info("Preparing start region for level " + j);
            WorldServer worldserver = this.worldServer[j];
            ChunkCoordinates chunkcoordinates = worldserver.getSpawn();

            for (int k = -short1; k <= short1 && this.isRunning(); k += 16) {
                for (int l = -short1; l <= short1 && this.isRunning(); l += 16) {
                    long i1 = System.currentTimeMillis();

                    if (i1 < i) {
                        i = i1;
                    }

                    if (i1 > i + 1000L) {
                        int j1 = (short1 * 2 + 1) * (short1 * 2 + 1);
                        int k1 = (k + short1) * (short1 * 2 + 1) + l + 1;

                        this.a_("Preparing spawn area", k1 * 100 / j1);
                        i = i1;
                    }

                    worldserver.chunkProviderServer.getChunkAt(chunkcoordinates.x + k >> 4, chunkcoordinates.z + l >> 4);

                    while (worldserver.updateLights() && this.isRunning()) {
                        ;
                    }
                }
            }
        }

        this.i();
    }

    public abstract boolean getGenerateStructures();

    public abstract EnumGamemode getGamemode();

    public abstract int getDifficulty();

    public abstract boolean isHardcore();

    protected void a_(String s, int i) {
        this.d = s;
        this.e = i;
        log.info(s + ": " + i + "%");
    }

    protected void i() {
        this.d = null;
        this.e = 0;
    }

    protected void saveChunks(boolean flag) {
        if (!this.O) {
            WorldServer[] aworldserver = this.worldServer;
            int i = aworldserver.length;

            for (int j = 0; j < i; ++j) {
                WorldServer worldserver = aworldserver[j];

                if (worldserver != null) {
                    if (!flag) {
                        log.info("Saving chunks for level \'" + worldserver.getWorldData().getName() + "\'/" + worldserver.worldProvider);
                    }

                    try {
                        worldserver.save(true, (IProgressUpdate) null);
                    } catch (ExceptionWorldConflict exceptionworldconflict) {
                        log.warning(exceptionworldconflict.getMessage());
                    }
                }
            }
        }
    }

    public void stop() {
        if (!this.O) {
            log.info("Stopping server");
            if (this.ac() != null) {
                this.ac().a();
            }

            if (this.t != null) {
                log.info("Saving players");
                this.t.savePlayers();
                this.t.r();
            }

            log.info("Saving worlds");
            this.saveChunks(false);
            WorldServer[] aworldserver = this.worldServer;
            int i = aworldserver.length;

            for (int j = 0; j < i; ++j) {
                WorldServer worldserver = aworldserver[j];

                worldserver.saveLevel();
            }

            if (this.n != null && this.n.d()) {
                this.n.e();
            }
        }
    }

    public String getServerIp() {
        return this.serverIp;
    }

    public void e(String s) {
        this.serverIp = s;
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    public void safeShutdown() {
        this.isRunning = false;
    }

    public void run() {
        try {
            if (this.init()) {
                long i = System.currentTimeMillis();

                for (long j = 0L; this.isRunning; this.Q = true) {
                    long k = System.currentTimeMillis();
                    long l = k - i;

                    if (l > 2000L && i - this.R >= 15000L) {
                        log.warning("Can\'t keep up! Did the system time change, or is the server overloaded?");
                        l = 2000L;
                        this.R = i;
                    }

                    if (l < 0L) {
                        log.warning("Time ran backwards! Did the system time change?");
                        l = 0L;
                    }

                    j += l;
                    i = k;
                    if (this.worldServer[0].everyoneDeeplySleeping()) {
                        this.p();
                        j = 0L;
                    } else {
                        while (j > 50L) {
                            j -= 50L;
                            this.p();
                        }
                    }

                    Thread.sleep(1L);
                }
            } else {
                this.a((CrashReport) null);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            log.log(Level.SEVERE, "Encountered an unexpected exception " + throwable.getClass().getSimpleName(), throwable);
            CrashReport crashreport = null;

            if (throwable instanceof ReportedException) {
                crashreport = this.b(((ReportedException) throwable).a());
            } else {
                crashreport = this.b(new CrashReport("Exception in server tick loop", throwable));
            }

            File file1 = new File(new File(this.n(), "crash-reports"), "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-server.txt");

            if (crashreport.a(file1)) {
                log.severe("This crash report has been saved to: " + file1.getAbsolutePath());
            } else {
                log.severe("We were unable to save this crash report to disk.");
            }

            this.a(crashreport);
        } finally {
            try {
                this.stop();
                this.isStopped = true;
            } catch (Throwable throwable1) {
                throwable1.printStackTrace();
            } finally {
                this.o();
            }
        }
    }

    protected File n() {
        return new File(".");
    }

    protected void a(CrashReport crashreport) {}

    protected void o() {}

    protected void p() {
        long i = System.nanoTime();

        AxisAlignedBB.a().a();
        Vec3D.a().a();
        ++this.ticks;
        if (this.T) {
            this.T = false;
            this.methodProfiler.a = true;
            this.methodProfiler.a();
        }

        this.methodProfiler.a("root");
        this.q();
        if (this.ticks % 900 == 0) {
            this.methodProfiler.a("save");
            this.t.savePlayers();
            this.saveChunks(true);
            this.methodProfiler.b();
        }

        this.methodProfiler.a("tallying");
        this.j[this.ticks % 100] = System.nanoTime() - i;
        this.f[this.ticks % 100] = Packet.p - this.E;
        this.E = Packet.p;
        this.g[this.ticks % 100] = Packet.q - this.F;
        this.F = Packet.q;
        this.h[this.ticks % 100] = Packet.n - this.G;
        this.G = Packet.n;
        this.i[this.ticks % 100] = Packet.o - this.H;
        this.H = Packet.o;
        this.methodProfiler.b();
        this.methodProfiler.a("snooper");
        if (!this.n.d() && this.ticks > 100) {
            this.n.a();
        }

        if (this.ticks % 6000 == 0) {
            this.n.b();
        }

        this.methodProfiler.b();
        this.methodProfiler.b();
    }

    public void q() {
        this.methodProfiler.a("levels");

        for (int i = 0; i < this.worldServer.length; ++i) {
            long j = System.nanoTime();

            if (i == 0 || this.getAllowNether()) {
                WorldServer worldserver = this.worldServer[i];

                this.methodProfiler.a(worldserver.getWorldData().getName());
                if (this.ticks % 20 == 0) {
                    this.methodProfiler.a("timeSync");
                    this.t.a(new Packet4UpdateTime(worldserver.getTime()), worldserver.worldProvider.dimension);
                    this.methodProfiler.b();
                }

                this.methodProfiler.a("tick");
                worldserver.doTick();
                this.methodProfiler.c("lights");

                while (true) {
                    if (!worldserver.updateLights()) {
                        this.methodProfiler.b();
                        if (!worldserver.players.isEmpty()) {
                            worldserver.tickEntities();
                        }

                        this.methodProfiler.a("tracker");
                        worldserver.getTracker().updatePlayers();
                        this.methodProfiler.b();
                        this.methodProfiler.b();
                        break;
                    }
                }
            }

            this.k[i][this.ticks % 100] = System.nanoTime() - j;
        }

        this.methodProfiler.c("connection");
        this.ac().b();
        this.methodProfiler.c("players");
        this.t.tick();
        this.methodProfiler.c("tickables");
        Iterator iterator = this.p.iterator();

        while (iterator.hasNext()) {
            IUpdatePlayerListBox iupdateplayerlistbox = (IUpdatePlayerListBox) iterator.next();

            iupdateplayerlistbox.a();
        }

        this.methodProfiler.b();
    }

    public boolean getAllowNether() {
        return true;
    }

    public void a(IUpdatePlayerListBox iupdateplayerlistbox) {
        this.p.add(iupdateplayerlistbox);
    }

    public static void main(String[] astring) {
        StatisticList.a();

        try {
            boolean flag = !GraphicsEnvironment.isHeadless();
            String s = null;
            String s1 = ".";
            String s2 = null;
            boolean flag1 = false;
            boolean flag2 = false;
            int i = -1;

            for (int j = 0; j < astring.length; ++j) {
                String s3 = astring[j];
                String s4 = j == astring.length - 1 ? null : astring[j + 1];
                boolean flag3 = false;

                if (!s3.equals("nogui") && !s3.equals("--nogui")) {
                    if (s3.equals("--port") && s4 != null) {
                        flag3 = true;

                        try {
                            i = Integer.parseInt(s4);
                        } catch (NumberFormatException numberformatexception) {
                            ;
                        }
                    } else if (s3.equals("--singleplayer") && s4 != null) {
                        flag3 = true;
                        s = s4;
                    } else if (s3.equals("--universe") && s4 != null) {
                        flag3 = true;
                        s1 = s4;
                    } else if (s3.equals("--world") && s4 != null) {
                        flag3 = true;
                        s2 = s4;
                    } else if (s3.equals("--demo")) {
                        flag1 = true;
                    } else if (s3.equals("--bonusChest")) {
                        flag2 = true;
                    }
                } else {
                    flag = false;
                }

                if (flag3) {
                    ++j;
                }
            }

            DedicatedServer dedicatedserver = new DedicatedServer(new File(s1));

            if (s != null) {
                dedicatedserver.l(s);
            }

            if (s2 != null) {
                dedicatedserver.m(s2);
            }

            if (i >= 0) {
                dedicatedserver.setPort(i);
            }

            if (flag1) {
                dedicatedserver.b(true);
            }

            if (flag2) {
                dedicatedserver.c(true);
            }

            if (flag) {
                dedicatedserver.aj();
            }

            dedicatedserver.s();
            Runtime.getRuntime().addShutdownHook(new ThreadShutdown(dedicatedserver));
        } catch (Exception exception) {
            log.log(Level.SEVERE, "Failed to start the minecraft server", exception);
        }
    }

    public void s() {
        (new ThreadServerApplication(this, "Server thread")).start();
    }

    public File f(String s) {
        return new File(this.n(), s);
    }

    public void info(String s) {
        log.info(s);
    }

    public void warning(String s) {
        log.warning(s);
    }

    public WorldServer getWorldServer(int i) {
        return i == -1 ? this.worldServer[1] : (i == 1 ? this.worldServer[2] : this.worldServer[0]);
    }

    public String t() {
        return this.serverIp;
    }

    public int u() {
        return this.s;
    }

    public String v() {
        return this.motd;
    }

    public String getVersion() {
        return "1.3.1";
    }

    public int x() {
        return this.t.getPlayerCount();
    }

    public int y() {
        return this.t.getMaxPlayers();
    }

    public String[] getPlayers() {
        return this.t.d();
    }

    public String getPlugins() {
        return "";
    }

    public String i(String s) {
        RemoteControlCommandListener.instance.b();
        this.q.a(RemoteControlCommandListener.instance, s);
        return RemoteControlCommandListener.instance.c();
    }

    public boolean isDebugging() {
        return false;
    }

    public void j(String s) {
        log.log(Level.SEVERE, s);
    }

    public void k(String s) {
        if (this.isDebugging()) {
            log.log(Level.INFO, s);
        }
    }

    public String getServerModName() {
        return "vanilla";
    }

    public CrashReport b(CrashReport crashreport) {
        crashreport.a("Is Modded", (Callable) (new CrashReportModded(this)));
        crashreport.a("Profiler Position", (Callable) (new CrashReportProfilerPosition(this)));
        if (this.t != null) {
            crashreport.a("Player Count", (Callable) (new CrashReportPlayerCount(this)));
        }

        if (this.worldServer != null) {
            WorldServer[] aworldserver = this.worldServer;
            int i = aworldserver.length;

            for (int j = 0; j < i; ++j) {
                WorldServer worldserver = aworldserver[j];

                if (worldserver != null) {
                    worldserver.a(crashreport);
                }
            }
        }

        return crashreport;
    }

    public List a(ICommandListener icommandlistener, String s) {
        ArrayList arraylist = new ArrayList();

        if (s.startsWith("/")) {
            s = s.substring(1);
            boolean flag = !s.contains(" ");
            List list = this.q.b(icommandlistener, s);

            if (list != null) {
                Iterator iterator = list.iterator();

                while (iterator.hasNext()) {
                    String s1 = (String) iterator.next();

                    if (flag) {
                        arraylist.add("/" + s1);
                    } else {
                        arraylist.add(s1);
                    }
                }
            }

            return arraylist;
        } else {
            String[] astring = s.split(" ", -1);
            String s2 = astring[astring.length - 1];
            String[] astring1 = this.t.d();
            int i = astring1.length;

            for (int j = 0; j < i; ++j) {
                String s3 = astring1[j];

                if (CommandAbstract.a(s2, s3)) {
                    arraylist.add(s3);
                }
            }

            return arraylist;
        }
    }

    public static MinecraftServer getServer() {
        return l;
    }

    public String getName() {
        return "Server";
    }

    public void sendMessage(String s) {
        log.info(StripColor.a(s));
    }

    public boolean b(String s) {
        return true;
    }

    public String a(String s, Object... aobject) {
        return LocaleLanguage.a().a(s, aobject);
    }

    public ICommandHandler getCommandHandler() {
        return this.q;
    }

    public KeyPair E() {
        return this.I;
    }

    public int F() {
        return this.s;
    }

    public void setPort(int i) {
        this.s = i;
    }

    public String G() {
        return this.J;
    }

    public void l(String s) {
        this.J = s;
    }

    public boolean H() {
        return this.J != null;
    }

    public String I() {
        return this.K;
    }

    public void m(String s) {
        this.K = s;
    }

    public void a(KeyPair keypair) {
        this.I = keypair;
    }

    public void c(int i) {
        for (int j = 0; j < this.worldServer.length; ++j) {
            WorldServer worldserver = this.worldServer[j];

            if (worldserver != null) {
                if (worldserver.getWorldData().isHardcore()) {
                    worldserver.difficulty = 3;
                    worldserver.setSpawnFlags(true, true);
                } else if (this.H()) {
                    worldserver.difficulty = i;
                    worldserver.setSpawnFlags(worldserver.difficulty > 0, true);
                } else {
                    worldserver.difficulty = i;
                    worldserver.setSpawnFlags(this.getSpawnMonsters(), this.spawnAnimals);
                }
            }
        }
    }

    protected boolean getSpawnMonsters() {
        return true;
    }

    public boolean L() {
        return this.demoMode;
    }

    public void b(boolean flag) {
        this.demoMode = flag;
    }

    public void c(boolean flag) {
        this.N = flag;
    }

    public Convertable getConvertable() {
        return this.convertable;
    }

    public void O() {
        this.O = true;
        this.getConvertable().d();

        for (int i = 0; i < this.worldServer.length; ++i) {
            WorldServer worldserver = this.worldServer[i];

            if (worldserver != null) {
                worldserver.saveLevel();
            }
        }

        this.getConvertable().e(this.worldServer[0].getDataManager().g());
        this.safeShutdown();
    }

    public String getTexturePack() {
        return this.P;
    }

    public void setTexturePack(String s) {
        this.P = s;
    }

    public void a(MojangStatisticsGenerator mojangstatisticsgenerator) {
        mojangstatisticsgenerator.a("whitelist_enabled", Boolean.valueOf(false));
        mojangstatisticsgenerator.a("whitelist_count", Integer.valueOf(0));
        mojangstatisticsgenerator.a("players_current", Integer.valueOf(this.x()));
        mojangstatisticsgenerator.a("players_max", Integer.valueOf(this.y()));
        mojangstatisticsgenerator.a("players_seen", Integer.valueOf(this.t.getSeenPlayers().length));
        mojangstatisticsgenerator.a("uses_auth", Boolean.valueOf(this.onlineMode));
        mojangstatisticsgenerator.a("gui_state", this.ae() ? "enabled" : "disabled");
        mojangstatisticsgenerator.a("avg_tick_ms", Integer.valueOf((int) (MathHelper.a(this.j) * 1.0E-6D)));
        mojangstatisticsgenerator.a("avg_sent_packet_count", Integer.valueOf((int) MathHelper.a(this.f)));
        mojangstatisticsgenerator.a("avg_sent_packet_size", Integer.valueOf((int) MathHelper.a(this.g)));
        mojangstatisticsgenerator.a("avg_rec_packet_count", Integer.valueOf((int) MathHelper.a(this.h)));
        mojangstatisticsgenerator.a("avg_rec_packet_size", Integer.valueOf((int) MathHelper.a(this.i)));
        int i = 0;

        for (int j = 0; j < this.worldServer.length; ++j) {
            if (this.worldServer[j] != null) {
                WorldServer worldserver = this.worldServer[j];
                WorldData worlddata = worldserver.getWorldData();

                mojangstatisticsgenerator.a("world[" + i + "][dimension]", Integer.valueOf(worldserver.worldProvider.dimension));
                mojangstatisticsgenerator.a("world[" + i + "][mode]", worlddata.getGameType());
                mojangstatisticsgenerator.a("world[" + i + "][difficulty]", Integer.valueOf(worldserver.difficulty));
                mojangstatisticsgenerator.a("world[" + i + "][hardcore]", Boolean.valueOf(worlddata.isHardcore()));
                mojangstatisticsgenerator.a("world[" + i + "][generator_name]", worlddata.getType().name());
                mojangstatisticsgenerator.a("world[" + i + "][generator_version]", Integer.valueOf(worlddata.getType().getVersion()));
                mojangstatisticsgenerator.a("world[" + i + "][height]", Integer.valueOf(this.D));
                mojangstatisticsgenerator.a("world[" + i + "][chunks_loaded]", Integer.valueOf(worldserver.F().getLoadedChunks()));
                ++i;
            }
        }

        mojangstatisticsgenerator.a("worlds", Integer.valueOf(i));
    }

    public void b(MojangStatisticsGenerator mojangstatisticsgenerator) {
        mojangstatisticsgenerator.a("singleplayer", Boolean.valueOf(this.H()));
        mojangstatisticsgenerator.a("server_brand", this.getServerModName());
        mojangstatisticsgenerator.a("gui_supported", GraphicsEnvironment.isHeadless() ? "headless" : "supported");
        mojangstatisticsgenerator.a("dedicated", Boolean.valueOf(this.S()));
    }

    public boolean getSnooperEnabled() {
        return true;
    }

    public int R() {
        return 16;
    }

    public abstract boolean S();

    public boolean getOnlineMode() {
        return this.onlineMode;
    }

    public void setOnlineMode(boolean flag) {
        this.onlineMode = flag;
    }

    public boolean getSpawnAnimals() {
        return this.spawnAnimals;
    }

    public void setSpawnAnimals(boolean flag) {
        this.spawnAnimals = flag;
    }

    public boolean getSpawnNPCs() {
        return this.spawnNPCs;
    }

    public void setSpawnNPCs(boolean flag) {
        this.spawnNPCs = flag;
    }

    public boolean getPvP() {
        return this.pvpMode;
    }

    public void setPvP(boolean flag) {
        this.pvpMode = flag;
    }

    public boolean getAllowFlight() {
        return this.allowFlight;
    }

    public void setAllowFlight(boolean flag) {
        this.allowFlight = flag;
    }

    public String getMotd() {
        return this.motd;
    }

    public void setMotd(String s) {
        this.motd = s;
    }

    public int getMaxBuildHeight() {
        return this.D;
    }

    public void d(int i) {
        this.D = i;
    }

    public boolean isStopped() {
        return this.isStopped;
    }

    public ServerConfigurationManagerAbstract getServerConfigurationManager() {
        return this.t;
    }

    public void a(ServerConfigurationManagerAbstract serverconfigurationmanagerabstract) {
        this.t = serverconfigurationmanagerabstract;
    }

    public void a(EnumGamemode enumgamemode) {
        for (int i = 0; i < this.worldServer.length; ++i) {
            getServer().worldServer[i].getWorldData().setGameType(enumgamemode);
        }
    }

    public abstract ServerConnection ac();

    public boolean ae() {
        return false;
    }

    public abstract String a(EnumGamemode enumgamemode, boolean flag);

    public int af() {
        return this.ticks;
    }

    public void ag() {
        this.T = true;
    }

    public static ServerConfigurationManagerAbstract a(MinecraftServer minecraftserver) {
        return minecraftserver.t;
    }
}
