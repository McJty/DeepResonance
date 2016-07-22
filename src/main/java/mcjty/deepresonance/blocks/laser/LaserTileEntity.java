package mcjty.deepresonance.blocks.laser;

import elec332.core.world.WorldHelper;
import mcjty.deepresonance.DeepResonance;
import mcjty.deepresonance.blocks.ModBlocks;
import mcjty.deepresonance.blocks.lens.LensSetup;
import mcjty.deepresonance.blocks.tank.TileTank;
import mcjty.deepresonance.config.ConfigMachines;
import mcjty.deepresonance.fluid.DRFluidRegistry;
import mcjty.deepresonance.fluid.LiquidCrystalFluidTagData;
import mcjty.lib.container.DefaultSidedInventory;
import mcjty.lib.container.InventoryHelper;
import mcjty.lib.entity.GenericEnergyReceiverTileEntity;
import mcjty.lib.network.Argument;
import mcjty.lib.network.PacketRequestIntegerFromServer;
import mcjty.lib.varia.BlockTools;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;

import static mcjty.deepresonance.grid.tank.DRTankMultiBlock.TANK_BUCKETS;

public class LaserTileEntity extends GenericEnergyReceiverTileEntity implements DefaultSidedInventory, ITickable {

    public static final String CMD_GETLIQUID = "getLiquid";
    public static final String CLIENTCMD_GETLIQUID = "getLiquid";

    public static final int COLOR_BLUE = 1;
    public static final int COLOR_RED = 2;
    public static final int COLOR_GREEN = 3;
    public static final int COLOR_YELLOW = 4;       // This is rendered as off in meta

    // Transient
    private int tickCounter = 10;

    private int progressCounter = 0;
    private int color = 0;          // 0 means not active, > 0 means a color laser
    private int crystalLiquid = 0;  // This is not RCL but just liquidified spent crystal

    private static int crystalLiquidClient = 0;

    // Infusing bonus for items. Index is the registry name of the item.
    public static Map<String, InfusingBonus> infusingBonusMap = null;

    private InventoryHelper inventoryHelper = new InventoryHelper(this, LaserContainer.factory, 2);

    public LaserTileEntity() {
        super(ConfigMachines.Laser.rfMaximum, ConfigMachines.Laser.rfPerTick);
    }

    @Override
    protected boolean needsCustomInvWrapper() {
        return true;
    }

    @Override
    public void setPowerInput(int powered) {
        boolean changed = powerLevel != powered;
        super.setPowerInput(powered);
        if (changed) {
            IBlockState state = worldObj.getBlockState(pos);
            worldObj.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    @Override
    public InventoryHelper getInventoryHelper() {
        return inventoryHelper;
    }

    @Override
    public void update() {
        if (!worldObj.isRemote) {
            checkStateServer();
        }
    }

    protected void checkStateServer() {
        tickCounter--;
        if (tickCounter > 0) {
            return;
        }
        tickCounter = 10;

        checkCrystal();

        if (powerLevel == 0) {
            changeColor(0);
            return;
        }

        ItemStack stack = inventoryHelper.getStackInSlot(LaserContainer.SLOT_CATALYST);
        InfusingBonus bonus = getInfusingBonus(stack);
        if (bonus == null) {
            changeColor(0);
            return;
        }

        if (getEnergyStored(null) < ConfigMachines.Laser.rfUsePerCatalyst) {
            changeColor(0);
            return;
        }

        if (crystalLiquid < ConfigMachines.Laser.crystalLiquidPerCatalyst) {
            changeColor(0);
            return;
        }

        BlockPos tankCoordinate = findLens();
        if (tankCoordinate != null) {
            changeColor(bonus.getColor());
        } else {
            changeColor(0);
            return;
        }

        progressCounter--;
        markDirty();
        if (progressCounter > 0) {
            return;
        }
        progressCounter = ConfigMachines.Laser.ticks10PerCatalyst;

        infuseLiquid(tankCoordinate, bonus);
    }

    private boolean validRCLTank(TileTank tank) {
        Fluid fluid = DRFluidRegistry.getFluidFromStack(tank.getFluid());
        return fluid == null || fluid == DRFluidRegistry.liquidCrystal;
    }


    private void infuseLiquid(BlockPos tankCoordinate, InfusingBonus bonus) {
        // We consume stuff even if the tank does not have enough liquid. Player has to be careful
        decrStackSize(LaserContainer.SLOT_CATALYST, 1);
        consumeEnergy(ConfigMachines.Laser.rfUsePerCatalyst);
        crystalLiquid -= ConfigMachines.Laser.crystalLiquidPerCatalyst;

        TileEntity te = WorldHelper.getTileAt(worldObj, tankCoordinate);
        if (te instanceof TileTank) {
            TileTank tileTank = (TileTank) te;
            if (validRCLTank(tileTank)) {


                FluidStack stack = tileTank.drain(null, 1000*TANK_BUCKETS, false);
                if (stack != null) {
                    stack = tileTank.drain(null, 1000*TANK_BUCKETS, true);
                    LiquidCrystalFluidTagData fluidData = LiquidCrystalFluidTagData.fromStack(stack);
                    float factor = 500.0f / stack.amount;
                    float purity = bonus.getPurityModifier().modify(fluidData.getPurity(), fluidData.getQuality(), factor);
                    float strength = bonus.getStrengthModifier().modify(fluidData.getStrength(), fluidData.getQuality(), factor);
                    float efficiency = bonus.getEfficiencyModifier().modify(fluidData.getEfficiency(), fluidData.getQuality(), factor);
                    fluidData.setPurity(purity);
                    fluidData.setStrength(strength);
                    fluidData.setEfficiency(efficiency);
                    FluidStack newStack = fluidData.makeLiquidCrystalStack();
                    if (Math.abs(purity) < 0.01) {
                        newStack.amount -= 200;
                        if (newStack.amount < 0) {
                            newStack.amount = 0;
                        }
                    }
                    if (newStack.amount > 0) {
                        tileTank.fill(null, newStack, true);
                    }
                }
            }
        }
    }

    private void changeColor(int newcolor) {
        if (newcolor != color) {
            color = newcolor;
            int mcolor = color;
            if (color == COLOR_YELLOW) {
                mcolor = COLOR_RED;
            } else if (color == 0) {
                mcolor = 0;    // Off
            }
            worldObj.setBlockState(getPos(), worldObj.getBlockState(getPos()).withProperty(LaserBlock.COLOR, mcolor), 3);
            markDirty();
        }
    }

    public int getColor() {
        return color;
    }

    private void checkCrystal() {
        ItemStack stack = inventoryHelper.getStackInSlot(LaserContainer.SLOT_CRYSTAL);
        if (stack != null) {
            NBTTagCompound tagCompound = stack.getTagCompound();
            float strength = tagCompound == null ? 0 : tagCompound.getFloat("strength") / 100.0f;
            int addAmount = (int) (ConfigMachines.Laser.minCrystalLiquidPerCrystal + strength * (ConfigMachines.Laser.maxCrystalLiquidPerCrystal - ConfigMachines.Laser.minCrystalLiquidPerCrystal));
            int newAmount = crystalLiquid + addAmount;
            if (newAmount > ConfigMachines.Laser.crystalLiquidMaximum) {
                // Not enough room
                return;
            }
            inventoryHelper.decrStackSize(LaserContainer.SLOT_CRYSTAL, 1);
            crystalLiquid = newAmount;
            markDirty();
        }
    }

    public static InfusingBonus getInfusingBonus(ItemStack item) {
        if (item == null) {
            return null;
        }
        String name = item.getItem().getRegistryName().getResourcePath();
//        String name = Item.itemRegistry.getNameForObject(item.getItem()).toString();
        return infusingBonusMap.get(name);
    }

    public static void createDefaultInfusionBonusMap() {
        infusingBonusMap = new HashMap<String, InfusingBonus>();
        infusingBonusMap.put(Items.DIAMOND.getRegistryName().getResourcePath(), new InfusingBonus(
                COLOR_BLUE,
                new InfusingBonus.Modifier(5.0f, 100.0f),
                InfusingBonus.Modifier.NONE,
                InfusingBonus.Modifier.NONE));
        infusingBonusMap.put(Items.EMERALD.getRegistryName().getResourcePath(), new InfusingBonus(
                COLOR_GREEN,
                new InfusingBonus.Modifier(8.0f, 100.0f),
                InfusingBonus.Modifier.NONE,
                InfusingBonus.Modifier.NONE));
        infusingBonusMap.put(Items.ENDER_PEARL.getRegistryName().getResourcePath(), new InfusingBonus(
                COLOR_GREEN,
                new InfusingBonus.Modifier(2.0f, 100.0f),
                InfusingBonus.Modifier.NONE,
                InfusingBonus.Modifier.NONE));
        infusingBonusMap.put(Items.REDSTONE.getRegistryName().getResourcePath(), new InfusingBonus(
                COLOR_RED,
                new InfusingBonus.Modifier(-1.0f, 0.0f),
                new InfusingBonus.Modifier(5.0f, 60.0f),
                InfusingBonus.Modifier.NONE));
        infusingBonusMap.put(Items.GUNPOWDER.getRegistryName().getResourcePath(), new InfusingBonus(
                COLOR_RED,
                new InfusingBonus.Modifier(-5.0f, 0.0f),
                new InfusingBonus.Modifier(8.0f, 70.0f),
                new InfusingBonus.Modifier(4.0f, 60.0f)));
        infusingBonusMap.put(Items.GLOWSTONE_DUST.getRegistryName().getResourcePath(), new InfusingBonus(
                COLOR_YELLOW,
                new InfusingBonus.Modifier(-2.0f, 0.0f),
                new InfusingBonus.Modifier(6.0f, 50.0f),
                new InfusingBonus.Modifier(3.0f, 50.0f)));
        infusingBonusMap.put(Items.BLAZE_POWDER.getRegistryName().getResourcePath(), new InfusingBonus(
                COLOR_YELLOW,
                new InfusingBonus.Modifier(-6.0f, 0.0f),
                new InfusingBonus.Modifier(5.0f, 70.0f),
                new InfusingBonus.Modifier(5.0f, 70.0f)));
        infusingBonusMap.put(Items.QUARTZ.getRegistryName().getResourcePath(), new InfusingBonus(
                COLOR_BLUE,
                new InfusingBonus.Modifier(-1.0f, 0.0f),
                InfusingBonus.Modifier.NONE,
                new InfusingBonus.Modifier(7.0f, 80.0f)));
        infusingBonusMap.put(Items.NETHER_STAR.getRegistryName().getResourcePath(), new InfusingBonus(
                COLOR_RED,
                new InfusingBonus.Modifier(-60.0f, 0.0f),
                new InfusingBonus.Modifier(90.0f, 100.0f),
                new InfusingBonus.Modifier(90.0f, 100.0f)));
        infusingBonusMap.put(Items.GHAST_TEAR.getRegistryName().getResourcePath(), new InfusingBonus(
                COLOR_YELLOW,
                new InfusingBonus.Modifier(-20.0f, 0.0f),
                new InfusingBonus.Modifier(25.0f, 100.0f),
                new InfusingBonus.Modifier(15.0f, 100.0f)));
        infusingBonusMap.put(Items.PRISMARINE_SHARD.getRegistryName().getResourcePath(), new InfusingBonus(
                COLOR_YELLOW,
                InfusingBonus.Modifier.NONE,
                new InfusingBonus.Modifier(3.0f, 30.0f),
                new InfusingBonus.Modifier(3.0f, 30.0f)));
        infusingBonusMap.put(Items.PRISMARINE_CRYSTALS.getRegistryName().getResourcePath(), new InfusingBonus(
                COLOR_YELLOW,
                InfusingBonus.Modifier.NONE,
                new InfusingBonus.Modifier(4.0f, 35.0f),
                new InfusingBonus.Modifier(4.0f, 35.0f)));
        infusingBonusMap.put(Items.SLIME_BALL.getRegistryName().getResourcePath(), new InfusingBonus(
                COLOR_GREEN,
                InfusingBonus.Modifier.NONE,
                InfusingBonus.Modifier.NONE,
                new InfusingBonus.Modifier(-10.0f, 1.0f)));
        infusingBonusMap.put(Items.COAL.getRegistryName().getResourcePath(), new InfusingBonus(
                COLOR_RED,
                new InfusingBonus.Modifier(-1.0f, 0.0f),
                new InfusingBonus.Modifier(-10.0f, 0.0f),
                InfusingBonus.Modifier.NONE));
        infusingBonusMap.put(Items.NETHER_WART.getRegistryName().getResourcePath(), new InfusingBonus(
                COLOR_RED,
                new InfusingBonus.Modifier(-3.0f, 0.0f),
                new InfusingBonus.Modifier(2.0f, 35.0f),
                new InfusingBonus.Modifier(-2.0f, 1.0f)));
        infusingBonusMap.put(Items.GOLD_INGOT.getRegistryName().getResourcePath(), new InfusingBonus(
                COLOR_RED,
                InfusingBonus.Modifier.NONE,
                new InfusingBonus.Modifier(-1.0f, 0.0f),
                new InfusingBonus.Modifier(1.0f, 30.0f)));
        infusingBonusMap.put(Items.IRON_INGOT.getRegistryName().getResourcePath(), new InfusingBonus(
                COLOR_RED,
                InfusingBonus.Modifier.NONE,
                new InfusingBonus.Modifier(-2.0f, 0.0f),
                new InfusingBonus.Modifier(1.0f, 20.0f)));
        infusingBonusMap.put(Items.SNOWBALL.getRegistryName().getResourcePath(), new InfusingBonus(
                COLOR_RED,
                new InfusingBonus.Modifier(1.0f, 30.0f),
                InfusingBonus.Modifier.NONE,
                new InfusingBonus.Modifier(1.0f, 40.0f)));
    }

    private BlockPos findLens() {
        if (!worldObj.isBlockLoaded(getPos())) {
            return null;
        }
        IBlockState state = worldObj.getBlockState(getPos());
        int meta = state.getBlock().getMetaFromState(state);
        EnumFacing direction = BlockTools.getOrientationHoriz(meta);
        BlockPos shouldBeAir = getPos().offset(direction);
        if (!worldObj.isAirBlock(shouldBeAir)) {
            return null;
        }
        BlockPos shouldBeLens = shouldBeAir.offset(direction);
        Block lensBlock = WorldHelper.getBlockAt(worldObj, shouldBeLens);
        if (lensBlock != LensSetup.lensBlock) {
            return null;
        }
        EnumFacing lensDirection = BlockTools.getOrientationHoriz(WorldHelper.getBlockMeta(worldObj, shouldBeLens));
        if (lensDirection != direction) {
            return null;
        }

        return shouldBeLens.offset(direction);
    }

    public void requestCrystalLiquidFromServer() {
        DeepResonance.networkHandler.getNetworkWrapper().sendToServer(new PacketRequestIntegerFromServer(DeepResonance.MODID, pos,
                                                                                                         CMD_GETLIQUID,
                                                                                                         CLIENTCMD_GETLIQUID));
    }

    @SideOnly(Side.CLIENT)
    public static int getCrystalLiquidClient() {
        return crystalLiquidClient;
    }

    @Override
    public Integer executeWithResultInteger(String command, Map<String, Argument> args) {
        Integer rc = super.executeWithResultInteger(command, args);
        if (rc != null) {
            return rc;
        }
        if (CMD_GETLIQUID.equals(command)) {
            return crystalLiquid;
        }
        return null;
    }

    @Override
    public boolean execute(String command, Integer result) {
        boolean rc = super.execute(command, result);
        if (rc) {
            return true;
        }
        if (CLIENTCMD_GETLIQUID.equals(command)) {
            crystalLiquidClient = result;
            return true;
        }
        return false;
    }


    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
        color = tagCompound.getInteger("color");
        progressCounter = tagCompound.getInteger("progress");
    }

    @Override
    public void readRestorableFromNBT(NBTTagCompound tagCompound) {
        super.readRestorableFromNBT(tagCompound);
        readBufferFromNBT(tagCompound, inventoryHelper);
        crystalLiquid = tagCompound.getInteger("liquid");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);
        tagCompound.setInteger("color", color);
        tagCompound.setInteger("progress", progressCounter);
        return tagCompound;
    }

    @Override
    public void writeRestorableToNBT(NBTTagCompound tagCompound) {
        super.writeRestorableToNBT(tagCompound);
        writeBufferToNBT(tagCompound, inventoryHelper);
        tagCompound.setInteger("liquid", crystalLiquid);
    }

    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        // @todo needs a better box
        return new AxisAlignedBB(getPos().getX() - 3, getPos().getY() - 1, getPos().getZ() - 3, getPos().getX() + 4, getPos().getY() + 2, getPos().getZ() + 4);
    }

    @Override
    public boolean canExtractItem(int index, ItemStack item, EnumFacing side) {
        return false;
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        return new int[]{LaserContainer.SLOT_CATALYST, LaserContainer.SLOT_CRYSTAL};
    }

    @Override
    public boolean canInsertItem(int index, ItemStack item, EnumFacing side) {
        switch (index) {
            case LaserContainer.SLOT_CRYSTAL:
                return item.isItemEqual(new ItemStack(ModBlocks.resonatingCrystalBlock));
            case LaserContainer.SLOT_CATALYST:
                return true;
        }
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return canPlayerAccess(player);
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (index == LaserContainer.SLOT_CRYSTAL) {
            return stack.getItem() == Item.getItemFromBlock(ModBlocks.resonatingCrystalBlock);
        } else {
            return true;
        }
    }
}
