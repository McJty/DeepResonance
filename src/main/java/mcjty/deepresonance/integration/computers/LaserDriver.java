package mcjty.deepresonance.integration.computers;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.prefab.ManagedEnvironment;
import mcjty.deepresonance.blocks.laser.LaserTileEntity;
import mcjty.deepresonance.config.ConfigMachines;
import mcjty.lib.integration.computers.AbstractOCDriver;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LaserDriver {
    public static class OCDriver extends AbstractOCDriver {
        public OCDriver() {
            super("deepresonance_laser", LaserTileEntity.class);
        }

        public static class InternalManagedEnvironment extends AbstractOCDriver.InternalManagedEnvironment<LaserTileEntity> {
            public InternalManagedEnvironment(LaserTileEntity tile) {
                super(tile, "deepresonance_laser");
            }

            @Callback(doc="function():number; Get the currently stored energy")
            public Object[] getEnergy(Context c, Arguments a) {
                return new Object[]{tile.getEnergyStored(EnumFacing.DOWN)};
            }

            @Callback(doc="function():number; Get the maximum stored energy")
            public Object[] getMaxEnergy(Context c, Arguments a) {
                return new Object[]{tile.getMaxEnergyStored(EnumFacing.DOWN)};
            }

            @Callback(doc="function():number; Get the currently stored liquid crystal")
            public Object[] getCrystalLiquid(Context c, Arguments a) {
                return new Object[]{tile.getCrystalLiquid()};
            }

            @Callback(doc="function():number; Get the maximum liquid crystal capacity")
            public Object[] getMaxCrystalLiquid(Context c, Arguments a) {
                return new Object[]{ConfigMachines.Laser.crystalLiquidMaximum};
            }

            @Override
            public int priority() {
                return 4;
            }
        }

        @Override
        public ManagedEnvironment createEnvironment(World world, BlockPos pos, EnumFacing side, TileEntity tile) {
            return new InternalManagedEnvironment((LaserTileEntity)tile);
        }
    }
}
