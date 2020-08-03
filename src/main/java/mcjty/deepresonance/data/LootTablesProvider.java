package mcjty.deepresonance.data;

import elec332.core.data.AbstractLootTableProvider;
import elec332.core.data.loottable.AbstractBlockLootTables;
import mcjty.deepresonance.DeepResonance;
import mcjty.deepresonance.modules.core.CoreModule;
import mcjty.deepresonance.modules.generator.GeneratorModule;
import mcjty.deepresonance.modules.machines.MachinesModule;
import mcjty.deepresonance.modules.pulser.PulserModule;
import mcjty.deepresonance.modules.radiation.RadiationModule;
import mcjty.deepresonance.modules.tank.TankModule;
import net.minecraft.data.DataGenerator;

/**
 * Created by Elec332 on 10-1-2020
 */
public class LootTablesProvider extends AbstractLootTableProvider {

    LootTablesProvider(DataGenerator dataGeneratorIn) {
        super(dataGeneratorIn);
    }

    @Override
    protected void registerLootTables() {
        addBlockLootTable(new AbstractBlockLootTables(DeepResonance.MODID) {

            @Override
            protected void registerBlockTables() {
                registerDropSelfLootTable(CoreModule.RESONATING_ORE_STONE_BLOCK);
                registerDropSelfLootTable(CoreModule.RESONATING_ORE_NETHER_BLOCK);
                registerDropSelfLootTable(CoreModule.RESONATING_ORE_END_BLOCK);
                registerDropSelfLootTable(CoreModule.RESONATING_PLATE_BLOCK_BLOCK);

                registerDropSelfLootTable(RadiationModule.POISONED_DIRT_BLOCK);
                registerSilkTouch(RadiationModule.DENSE_GLASS_BLOCK.get());
                registerDropSelfLootTable(RadiationModule.DENSE_OBSIDIAN_BLOCK);

                registerDropSelfLootTable(MachinesModule.VALVE_BLOCK);
                registerDropSelfLootTable(MachinesModule.SMELTER_BLOCK);
                registerDropSelfLootTable(MachinesModule.PURIFIER_BLOCK);
                registerDropSelfLootTable(PulserModule.PULSER_BLOCK);
                registerEmptyLootTable(MachinesModule.LENS_BLOCK.get());
                registerDropSelfLootTable(MachinesModule.LASER_BLOCK);
                registerDropSelfLootTable(MachinesModule.CRYSTALLIZER_BLOCK);
                registerDropSelfLootTable(GeneratorModule.ENERGY_COLLECTOR_BLOCK);
                registerDropSelfLootTable(GeneratorModule.GENERATOR_CONTROLLER_BLOCK);
                registerDropSelfLootTable(GeneratorModule.GENERATOR_PART_BLOCK);

                //todo: tank & crystal
                registerEmptyLootTable(CoreModule.RESONATING_CRYSTAL_BLOCK.get());
                registerEmptyLootTable(TankModule.TANK_BLOCK.get());
            }

        });
    }

}