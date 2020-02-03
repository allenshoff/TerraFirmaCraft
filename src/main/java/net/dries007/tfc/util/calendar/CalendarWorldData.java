/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.util.calendar;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import static net.dries007.tfc.TerraFirmaCraft.MOD_ID;

@ParametersAreNonnullByDefault
public class CalendarWorldData extends WorldSavedData
{
    private static final String NAME = MOD_ID + ":calendar";

    @Nonnull
    public static CalendarWorldData get(@Nonnull World world)
    {
        MapStorage mapStorage = world.getMapStorage();
        if (mapStorage != null)
        {
            CalendarWorldData data = (CalendarWorldData) mapStorage.getOrLoadData(CalendarWorldData.class, NAME);
            if (data == null)
            {
                // Unable to load data, so assign default values
                data = new CalendarWorldData();
                data.markDirty();
                mapStorage.setData(NAME, data);
            }
            return data;
        }
        throw new IllegalStateException("Unable to access calendar data - everything is wrong now");
    }

    private final CalendarTFC calendar;

    @SuppressWarnings("WeakerAccess")
    public CalendarWorldData()
    {
        super(NAME);
        this.calendar = new CalendarTFC();
    }

    @SuppressWarnings("unused")
    public CalendarWorldData(String name)
    {
        super(name);
        this.calendar = new CalendarTFC();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        calendar.deserializeNBT(nbt.getCompoundTag("calendar"));
    }

    @Override
    @Nonnull
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        nbt.setTag("calendar", CalendarTFC.INSTANCE.serializeNBT());
        return nbt;
    }

    /**
     * Since this updates every tick, and doesn't store a local copy always assume it needs saving to disk
     */
    @Override
    public boolean isDirty()
    {
        return true;
    }

    @Nonnull
    public CalendarTFC getCalendar()
    {
        return calendar;
    }
}
