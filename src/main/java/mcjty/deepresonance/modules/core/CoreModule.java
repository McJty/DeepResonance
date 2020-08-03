package mcjty.deepresonance.modules.core;

import elec332.core.api.module.ElecModule;
import mcjty.deepresonance.DeepResonance;
import mcjty.deepresonance.modules.core.block.BlockCrystal;
import mcjty.deepresonance.modules.core.block.BlockResonatingPlate;
import mcjty.deepresonance.modules.core.item.ItemLiquidInjector;
import mcjty.deepresonance.modules.core.util.CrystalConfig;
import mcjty.deepresonance.modules.core.util.ResonatingPlateBlockConfig;
import mcjty.deepresonance.util.ItemWithTooltip;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.fml.RegistryObject;

/**
 * Created by Elec332 on 10-1-2020
 */
@ElecModule(owner = DeepResonance.MODID, name = "Core", alwaysEnabled = true)
public class CoreModule {

    public static final Block.Properties ORE_PROPERTIES = Block.Properties.create(Material.ROCK).hardnessAndResistance(3, 5).harvestLevel(3).harvestTool(ToolType.PICKAXE);

    public static final RegistryObject<BlockCrystal> RESONATING_CRYSTAL_BLOCK = DeepResonance.BLOCKS.register("resonating_crystal", BlockCrystal::new);
    public static final RegistryObject<Block> RESONATING_ORE_STONE_BLOCK = DeepResonance.BLOCKS.register("resonating_ore_stone", () -> new Block(ORE_PROPERTIES));
    public static final RegistryObject<Block> RESONATING_ORE_NETHER_BLOCK = DeepResonance.BLOCKS.register("resonating_ore_nether", () -> new Block(ORE_PROPERTIES));
    public static final RegistryObject<Block> RESONATING_ORE_END_BLOCK = DeepResonance.BLOCKS.register("resonating_ore_end", () -> new Block(ORE_PROPERTIES));
    public static final RegistryObject<Block> RESONATING_PLATE_BLOCK_BLOCK = DeepResonance.BLOCKS.register("resonating_plate_block", () -> new BlockResonatingPlate(Block.Properties.create(Material.ROCK).hardnessAndResistance(3, 5).harvestLevel(2).harvestTool(ToolType.PICKAXE)));

    public static final RegistryObject<Item> RESONATING_PLATE_ITEM = DeepResonance.ITEMS.register("resonating_plate", () -> new Item(DeepResonance.createStandardProperties()));
    public static final RegistryObject<Item> FILTER_MATERIAL_ITEM = DeepResonance.ITEMS.register("filter_material", () -> new ItemWithTooltip(DeepResonance.createStandardProperties().maxStackSize(1)));
    public static final RegistryObject<Item> SPENT_FILTER_ITEM = DeepResonance.ITEMS.register("spent_filter_material", () -> new Item(DeepResonance.createStandardProperties().maxStackSize(1)));
    public static final RegistryObject<Item> LIQUID_INJECTOR_ITEM = DeepResonance.ITEMS.register("liquid_injector", () -> new ItemLiquidInjector(DeepResonance.createStandardProperties()));
    public static final RegistryObject<Item> MACHINE_FRAME_ITEM = DeepResonance.ITEMS.register("machine_frame", () -> new Item(DeepResonance.createStandardProperties()));
    public static final RegistryObject<Item> RESONATING_CRYSTAL_ITEM = DeepResonance.fromBlock(RESONATING_CRYSTAL_BLOCK);
    public static final RegistryObject<Item> RESONATING_ORE_STONE_ITEM = DeepResonance.fromBlock(RESONATING_ORE_STONE_BLOCK);
    public static final RegistryObject<Item> RESONATING_ORE_NETHER_ITEM = DeepResonance.fromBlock(RESONATING_ORE_NETHER_BLOCK);
    public static final RegistryObject<Item> RESONATING_ORE_END_ITEM = DeepResonance.fromBlock(RESONATING_ORE_END_BLOCK);
    public static final RegistryObject<Item> RESONATING_PLATE_BLOCK_ITEM = DeepResonance.fromBlock(RESONATING_PLATE_BLOCK_BLOCK);

    public CoreModule() {
        DeepResonance.config.configureSubConfig("core_module", "Core module settings", config -> {
            config.registerConfigurableElement(new CrystalConfig(), "resonating_crystal", "Resonating Crystal settings");
            config.registerConfigurableElement(new ResonatingPlateBlockConfig());
        });
    }


}