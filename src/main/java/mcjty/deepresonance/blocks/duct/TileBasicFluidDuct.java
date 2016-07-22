package mcjty.deepresonance.blocks.duct;

import mcjty.deepresonance.DeepResonance;
import mcjty.deepresonance.blocks.base.ElecTileBase;
import mcjty.deepresonance.grid.fluid.DRFluidDuctGrid;
import mcjty.deepresonance.grid.fluid.event.FluidTileEvent;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * Created by Elec332 on 3-8-2015.
 */
public class TileBasicFluidDuct extends ElecTileBase {

    public FluidStack intTank;
    public Fluid lastSeenFluid;

    @Override
    public void onTileLoaded() {
        super.onTileLoaded();
        if (!worldObj.isRemote) {
            MinecraftForge.EVENT_BUS.post(new FluidTileEvent.Load(this));
            //addAllAdjacentTanks();
        }
    }

    @Override
    public void onTileUnloaded() {
        super.onTileUnloaded();
        if (!worldObj.isRemote) {
            MinecraftForge.EVENT_BUS.post(new FluidTileEvent.Unload(this));
            //removeAllAdjacentTanks();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
        if (tagCompound.hasKey("fluid"))
            this.intTank = FluidStack.loadFluidStackFromNBT(tagCompound.getCompoundTag("fluid"));
        if (tagCompound.hasKey("lastSeenFluid"))
            this.lastSeenFluid = FluidRegistry.getFluid(tagCompound.getString("lastSeenFluid"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);
        if (getGrid() != null) {
            intTank = getGrid().getFluidShare(this);
            lastSeenFluid = getGrid().getFluid();
        }
        if (intTank != null) {
            NBTTagCompound fluidTag = new NBTTagCompound();
            intTank.writeToNBT(fluidTag);
            tagCompound.setTag("fluid", fluidTag);
        }
        if (lastSeenFluid != null)
            tagCompound.setString("lastSeenFluid", FluidRegistry.getFluidName(lastSeenFluid));
        return tagCompound;
    }

    public int getTankStorageMax(){
        return 200;
    }

    public DRFluidDuctGrid getGrid(){
        if (!worldObj.isRemote) {
            return DeepResonance.worldGridRegistry.getFluidRegistry().get(worldObj).getPowerTile(getPos()).getGrid();
        }
        return null;
    }

}
