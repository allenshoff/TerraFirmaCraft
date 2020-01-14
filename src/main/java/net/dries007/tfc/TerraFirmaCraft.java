/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import net.dries007.tfc.api.capability.damage.CapabilityDamageResistance;
import net.dries007.tfc.api.capability.egg.CapabilityEgg;
import net.dries007.tfc.api.capability.food.CapabilityFood;
import net.dries007.tfc.api.capability.food.FoodHandler;
import net.dries007.tfc.api.capability.forge.CapabilityForgeable;
import net.dries007.tfc.api.capability.heat.CapabilityItemHeat;
import net.dries007.tfc.api.capability.metal.CapabilityMetalItem;
import net.dries007.tfc.api.capability.player.CapabilityPlayerData;
import net.dries007.tfc.api.capability.size.CapabilityItemSize;
import net.dries007.tfc.api.util.TFCConstants;
import net.dries007.tfc.client.ClientEvents;
import net.dries007.tfc.client.TFCGuiHandler;
import net.dries007.tfc.client.TFCKeybindings;
import net.dries007.tfc.client.gui.overlay.PlayerDataOverlay;
import net.dries007.tfc.client.render.animal.RenderAnimalTFCFamiliarity;
import net.dries007.tfc.command.*;
import net.dries007.tfc.network.*;
import net.dries007.tfc.objects.LootTablesTFC;
import net.dries007.tfc.objects.entity.EntitiesTFC;
import net.dries007.tfc.objects.items.ItemsTFC;
import net.dries007.tfc.proxy.IProxy;
import net.dries007.tfc.util.calendar.CalendarTFC;
import net.dries007.tfc.util.fuel.FuelManager;
import net.dries007.tfc.world.classic.WorldTypeTFC;
import net.dries007.tfc.world.classic.chunkdata.CapabilityChunkData;
import net.dries007.tfc.world.classic.worldgen.vein.VeinRegistry;

import static net.dries007.tfc.api.util.TFCConstants.MOD_ID;

@SuppressWarnings({"DefaultAnnotationParam", "unused"})
@Mod(modid = MOD_ID, name = TFCConstants.MOD_NAME, useMetadata = true, guiFactory = Constants.GUI_FACTORY, canBeDeactivated = false, certificateFingerprint = TFCConstants.SIGNING_KEY)
@Mod.EventBusSubscriber
public final class TerraFirmaCraft
{
    @Mod.Instance
    private static TerraFirmaCraft instance = null;

    @Mod.Metadata
    private static ModMetadata metadata = null;

    @SidedProxy(modId = MOD_ID, clientSide = "net.dries007.tfc.proxy.ClientProxy", serverSide = "net.dries007.tfc.proxy.ServerProxy")
    private static IProxy proxy = null;

    static
    {
        FluidRegistry.enableUniversalBucket();
    }

    public static Logger getLog()
    {
        return instance.log;
    }

    public static IProxy getProxy()
    {
        return proxy;
    }

    public static String getVersion()
    {
        return metadata.version;
    }

    public static WorldTypeTFC getWorldTypeTFC()
    {
        return instance.worldTypeTFC;
    }

    public static SimpleNetworkWrapper getNetwork()
    {
        return instance.network;
    }

    public static TerraFirmaCraft getInstance()
    {
        return instance;
    }

    public static LoaderState.ModState getState()
    {
        return Loader.instance().getModState(Loader.instance().getModObjectList().inverse().get(instance));
    }

    public static boolean pastState(LoaderState.ModState state)
    {
        return TerraFirmaCraft.getState().ordinal() >= state.ordinal();
    }

    private final Logger log = LogManager.getLogger(MOD_ID);
    private boolean isSignedBuild = true;
    private WorldTypeTFC worldTypeTFC;
    private SimpleNetworkWrapper network;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        log.debug("If you can see this, debug logging is working :)");
        if (!isSignedBuild)
        {
            log.warn("You are not running an official build. Please do not use this and then report bugs or issues.");
        }

        // No need to sync config here, forge magic

        NetworkRegistry.INSTANCE.registerGuiHandler(this, new TFCGuiHandler());
        network = NetworkRegistry.INSTANCE.newSimpleChannel(MOD_ID);
        int id = 0;
        // Received on server
        network.registerMessage(new PacketGuiButton.Handler(), PacketGuiButton.class, ++id, Side.SERVER);
        network.registerMessage(new PacketPlaceBlockSpecial.Handler(), PacketPlaceBlockSpecial.class, ++id, Side.SERVER);
        network.registerMessage(new PacketSwitchPlayerInventoryTab.Handler(), PacketSwitchPlayerInventoryTab.class, ++id, Side.SERVER);
        network.registerMessage(new PacketOpenCraftingGui.Handler(), PacketOpenCraftingGui.class, ++id, Side.SERVER);
        network.registerMessage(new PacketCycleItemMode.Handler(), PacketCycleItemMode.class, ++id, Side.SERVER);

        // Received on client
        network.registerMessage(new PacketAnvilUpdate.Handler(), PacketAnvilUpdate.class, ++id, Side.CLIENT);
        network.registerMessage(new PacketCrucibleUpdate.Handler(), PacketCrucibleUpdate.class, ++id, Side.CLIENT);
        network.registerMessage(new PacketChunkData.Handler(), PacketChunkData.class, ++id, Side.CLIENT);
        network.registerMessage(new PacketCapabilityContainerUpdate.Handler(), PacketCapabilityContainerUpdate.class, ++id, Side.CLIENT);
        network.registerMessage(new PacketCalendarUpdate.Handler(), PacketCalendarUpdate.class, ++id, Side.CLIENT);
        network.registerMessage(new PacketBarrelUpdate.Handler(), PacketBarrelUpdate.class, ++id, Side.CLIENT);
        network.registerMessage(new PacketLoomUpdate.Handler(), PacketLoomUpdate.class, ++id, Side.CLIENT);
        network.registerMessage(new PacketBellowsUpdate.Handler(), PacketBellowsUpdate.class, ++id, Side.CLIENT);
        network.registerMessage(new PacketFoodStatsUpdate.Handler(), PacketFoodStatsUpdate.class, ++id, Side.CLIENT);
        network.registerMessage(new PacketFoodStatsReplace.Handler(), PacketFoodStatsReplace.class, ++id, Side.CLIENT);
        network.registerMessage(new PacketPlayerDataUpdate.Handler(), PacketPlayerDataUpdate.class, ++id, Side.CLIENT);
        network.registerMessage(new PacketLargeVesselUpdate.Handler(), PacketLargeVesselUpdate.class, ++id, Side.CLIENT);

        EntitiesTFC.preInit();
        VeinRegistry.INSTANCE.preInit(event.getModConfigurationDirectory());

        CapabilityChunkData.preInit();
        CapabilityItemSize.preInit();
        CapabilityItemHeat.preInit();
        CapabilityForgeable.preInit();
        CapabilityFood.preInit();
        CapabilityEgg.preInit();
        CapabilityPlayerData.preInit();
        CapabilityDamageResistance.preInit();
        CapabilityMetalItem.preInit();

        if (event.getSide().isClient())
        {
            ClientEvents.preInit();
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        if (!isSignedBuild)
        {
            log.warn("You are not running an official build. Please do not use this and then report bugs or issues.");
        }

        ItemsTFC.init();
        LootTablesTFC.init();
        CapabilityFood.init();

        if (event.getSide().isClient())
        {
            TFCKeybindings.init();
            // Enable overlay to render health, thirst and hunger bars, TFC style.
            MinecraftForge.EVENT_BUS.register(PlayerDataOverlay.getInstance());
            // Enable to render animals familiarity
            MinecraftForge.EVENT_BUS.register(RenderAnimalTFCFamiliarity.getInstance());
            GuiIngameForge.renderHealth = false;
            GuiIngameForge.renderArmor = false;
            GuiIngameForge.renderExperiance = false;
        }

        worldTypeTFC = new WorldTypeTFC();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        if (!isSignedBuild)
        {
            log.warn("You are not running an official build. Please do not use this and then report bugs or issues.");
        }

        FuelManager.postInit();
        VeinRegistry.INSTANCE.postInit();
    }

    @Mod.EventHandler
    public void onLoadComplete(FMLLoadCompleteEvent event)
    {
        // This is the latest point that we can possibly stop creating non-decaying stacks on both server + client
        // It should be safe to use as we're only using it internally
        FoodHandler.setNonDecaying(false);
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event)
    {
        if (!isSignedBuild)
        {
            log.warn("You are not running an official build. Please do not use this and then report bugs or issues.");
        }

        event.registerServerCommand(new CommandStripWorld());
        event.registerServerCommand(new CommandHeat());
        event.registerServerCommand(new CommandSkill());
        event.registerServerCommand(new CommandTimeTFC());
        event.registerServerCommand(new CommandFindVeins());
        event.registerServerCommand(new CommandDebugInfo());

        // Initialize calendar for the current server
        CalendarTFC.INSTANCE.init(event.getServer());
    }

    @Mod.EventHandler
    public void onIMC(FMLInterModComms.IMCEvent event)
    {
        //todo: provide nice API here.
    }

    @Mod.EventHandler
    public void onFingerprintViolation(FMLFingerprintViolationEvent event)
    {
        isSignedBuild = false;
        FMLCommonHandler.instance().registerCrashCallable(new ICrashCallable()
        {
            @Override
            public String getLabel()
            {
                return TFCConstants.MOD_NAME;
            }

            @Override
            public String call()
            {
                return "You are not running an official build. Please do not use this and then report bugs or issues.";
            }
        });
    }
}
