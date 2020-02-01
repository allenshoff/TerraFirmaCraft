/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.world.classic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import net.dries007.tfc.api.types.ICreatureTFC;
import net.dries007.tfc.util.climate.ClimateTFC;
import net.dries007.tfc.world.classic.chunkdata.ChunkDataTFC;

/*
 * TFC entity spawning mechanics
 * Only works in tfc type worlds
 */
@SuppressWarnings("WeakerAccess")
public final class WorldEntitySpawnerTFC
{
    //todo customization for spawning other mods' creatures. maybe a Map<Entity, SpawnRules>, where SpawnRules is a new class with biomes, grouping and spawn weight


    /**
     * **Modified version from vanilla's {@link net.minecraft.world.WorldEntitySpawner}
     * Called during chunk generation to spawn initial creatures.
     * Spawns group of animals together
     *
     * @param centerX   The X coordinate of the point to spawn mobs around.
     * @param centerZ   The Z coordinate of the point to spawn mobs around.
     * @param diameterX The X diameter of the rectangle to spawn mobs in
     * @param diameterZ The Z diameter of the rectangle to spawn mobs in
     */
    public static void performWorldGenSpawning(World worldIn, Biome biomeIn, int centerX, int centerZ, int diameterX, int diameterZ, Random randomIn)
    {
        BlockPos chunkBlockPos = new BlockPos(centerX, 0, centerZ);

        float temperature = ClimateTFC.getAvgTemp(worldIn, chunkBlockPos);
        float rainfall = ChunkDataTFC.getRainfall(worldIn, chunkBlockPos);

        // Spawns only one group
        ForgeRegistries.ENTITIES.getValuesCollection().stream()
            .filter(x -> {
                if (ICreatureTFC.class.isAssignableFrom(x.getEntityClass()))
                {
                    int weight = ((ICreatureTFC) x.newInstance(worldIn)).getSpawnWeight(biomeIn, temperature, rainfall);
                    return weight > 0 && randomIn.nextInt(weight) == 0;
                }
                return false;
            }).findFirst()
            .ifPresent(entityEntry ->
            {
                List<EntityLiving> group = new ArrayList<>();
                EntityLiving creature = (EntityLiving) entityEntry.newInstance(worldIn);
                ICreatureTFC creatureTFC = (ICreatureTFC) creature;
                int fallback = 5; // Fallback measure if some mod completely deny this entity spawn
                int individuals = Math.max(1, creatureTFC.getMinGroupSize()) + randomIn.nextInt(creatureTFC.getMaxGroupSize() - Math.max(0, creatureTFC.getMinGroupSize() - 1));
                while (individuals > 0)
                {
                    int j = centerX + randomIn.nextInt(diameterX);
                    int k = centerZ + randomIn.nextInt(diameterZ);
                    BlockPos blockpos = worldIn.getTopSolidOrLiquidBlock(new BlockPos(j, 0, k));
                    creature.setLocationAndAngles((float) j + 0.5F, blockpos.getY(), (float) k + 0.5F, randomIn.nextFloat() * 360.0F, 0.0F);
                    if (creature.getCanSpawnHere()) // fix entities spawning inside walls
                    {
                        if (net.minecraftforge.event.ForgeEventFactory.canEntitySpawn(creature, worldIn, j + 0.5f, (float) blockpos.getY(), k + 0.5f, null) == net.minecraftforge.fml.common.eventhandler.Event.Result.DENY)
                        {
                            if (--fallback > 0)
                            {
                                continue;
                            }
                            else
                            {
                                break; // Someone doesn't want me to spawn :(
                            }
                        }
                        fallback = 5;
                        // Spawn pass! let's continue
                        worldIn.spawnEntity(creature);
                        group.add(creature);
                        creature.onInitialSpawn(worldIn.getDifficultyForLocation(new BlockPos(creature)), null);
                        if (--individuals > 0)
                        {
                            //We still need to spawn more
                            creature = (EntityLiving) entityEntry.newInstance(worldIn);
                            creatureTFC = (ICreatureTFC) creature;
                        }
                    }
                    else
                    {
                        if (--fallback <= 0) //Trying to spawn in water or inside walls too many times, let's break
                        {
                            break;
                        }
                    }
                }
                // Apply the group spawning mechanics!
                creatureTFC.getGroupingRules().accept(group, randomIn);
            });
    }
}
