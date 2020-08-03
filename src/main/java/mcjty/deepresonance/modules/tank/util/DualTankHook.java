package mcjty.deepresonance.modules.tank.util;

import com.google.common.base.Preconditions;
import elec332.core.world.WorldHelper;
import net.minecraft.fluid.Fluid;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.lang.ref.WeakReference;

/**
 * Created by Elec332 on 26-7-2020
 */
public class DualTankHook {

    public DualTankHook(TileEntity tile, Direction dir1, Direction dir2) {
        this.tile = new WeakReference<>(Preconditions.checkNotNull(tile));
        this.dir1 = Preconditions.checkNotNull(dir1);
        this.dir2 = Preconditions.checkNotNull(dir2);
        this.allowDuplicates = false;
        this.timeout = this.timeCounter = 0;
    }

    private final WeakReference<TileEntity> tile;
    private final Direction dir1, dir2;
    private boolean allowDuplicates;
    private int timeout, timeCounter;
    private LazyOptional<IFluidHandler> tank1;
    private LazyOptional<IFluidHandler> tank2;

    /**
     * Allows the 2 tanks to be the same tank (in case of multiblocks)
     */
    public DualTankHook allowDuplicates() {
        this.allowDuplicates = true;
        return this;
    }

    /**
     * Sets a timeout value vor validation, prevent checking every tick when tanks are missing
     *
     * @param timeout The timeout (in the number of {@link DualTankHook#checkTanks()} calls)
     */
    public DualTankHook setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public IFluidHandler getTank1() {
        return tank1.orElseThrow(NullPointerException::new);
    }

    public IFluidHandler getTank2() {
        return tank2.orElseThrow(NullPointerException::new);
    }

    public boolean tank1Present() {
        return tank1 != null && tank1.isPresent();
    }

    public boolean tank2Present() {
        return tank2 != null && tank2.isPresent();
    }

    public boolean checkTankContents(Fluid fluid1, Fluid fluid2) {
        if (!checkTanks()) {
            return false;
        }
        if (fluid1 != null && getTank1().getFluidInTank(0).getFluid() != fluid1) {
            return false;
        }
        return fluid2 == null || getTank2().getFluidInTank(0).getFluid() == fluid2;
    }

    public boolean checkTanks() {
        TileEntity tile_ = this.tile.get();
        if (tile_ == null) {
            throw new IllegalStateException();
        }
        World world = Preconditions.checkNotNull(tile_.getWorld());
        BlockPos pos = tile_.getPos();
        boolean check = false;
        if (!tank1Present()) {
            if (timeCounter > 0) {
                timeCounter--;
                return false;
            }
            check = true;
            TileEntity tile = WorldHelper.getTileAt(world, pos.offset(dir1));
            if (tile != null) {
                LazyOptional<IFluidHandler> f = tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.DOWN);
                if (f.isPresent()) {
                    tank1 = f;
                    if (!allowDuplicates && tank2Present() && getTank2().equals(getTank1())) {
                        tank1 = null; //Do not circle-inject
                        return false;
                    }
                } else {
                    tank1 = null;
                    return false;
                }
            } else {
                return false;
            }
        }
        if (!tank2Present()) {
            if (timeCounter > 0) {
                timeCounter--;
                return false;
            }
            check = true;
            TileEntity tile = WorldHelper.getTileAt(world, pos.offset(dir2));
            if (tile != null) {
                LazyOptional<IFluidHandler> f = tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.UP);
                if (f.isPresent()) {
                    tank2 = f;
                    if (!allowDuplicates && tank1Present() && getTank1().equals(getTank2())) {
                        tank2 = null; //Do not circle-inject
                        return false;
                    }
                } else {
                    tank2 = null;
                    return false;
                }
            } else {
                return false;
            }
        }
        if (check) {
            timeCounter = timeout;
        }
        return true;
    }

}