/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.objects.blocks;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.dries007.tfc.Constants;
import net.dries007.tfc.api.capability.food.FoodStatsTFC;
import net.dries007.tfc.objects.fluids.FluidsTFC;

public class BlockFluidHotWater extends BlockFluidTFC
{
    public BlockFluidHotWater()
    {
        super(FluidsTFC.HOT_WATER.get(), Material.WATER, false);

        setLightOpacity(3);
        disableStats();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand)
    {
        if (rand.nextInt(4) == 0)
        {
            worldIn.spawnParticle(EnumParticleTypes.WATER_BUBBLE, (double) (pos.getX() + rand.nextFloat()), pos.getY() + 0.50D, (double) (pos.getZ() + rand.nextFloat()), 0.0D, 0.0D, 0.0D, Block.getStateId(stateIn));
        }

        //steam clouds
        if (worldIn.getBlockState(pos.up()).getBlock() ==  Blocks.AIR) //only instantiate from the top level of fluid
        {
            worldIn.spawnParticle(EnumParticleTypes.CLOUD, (double) (pos.getX() + rand.nextFloat()), (double) (pos.getY() + rand.nextFloat() * 4.0D), (double) (pos.getZ() + rand.nextFloat()), 0.0D, 0.2D, 0.0D, Block.getStateId(stateIn));
        }
    }

    @Override
    public void onEntityCollision(World worldIn, BlockPos pos, IBlockState state, Entity entityIn)
    {
        super.onEntityCollision(worldIn, pos, state, entityIn);
        if (entityIn instanceof EntityLivingBase)
        {
            EntityLivingBase entityLiving = (EntityLivingBase) entityIn;
            if (Constants.RNG.nextInt(10) == 0 && entityLiving.getHealth() < entityLiving.getMaxHealth())
            {
                entityLiving.heal(FoodStatsTFC.PASSIVE_HEAL_AMOUNT * 7f);
            }
        }
    }
}
