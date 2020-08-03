package mcjty.deepresonance.modules.core.client;

import com.google.common.base.Preconditions;
import elec332.core.api.APIHandlerInject;
import elec332.core.api.client.model.IElecRenderingRegistry;
import elec332.core.api.client.model.loading.IModelHandler;
import elec332.core.api.client.model.loading.ModelHandler;
import elec332.core.client.model.WrappedModel;
import elec332.core.client.util.AbstractItemOverrideList;
import elec332.core.item.AbstractItemBlock;
import elec332.core.world.WorldHelper;
import mcjty.deepresonance.modules.core.CoreModule;
import mcjty.deepresonance.modules.core.tile.TileEntityResonatingCrystal;
import mcjty.deepresonance.util.Constants;
import mcjty.deepresonance.util.DeepResonanceResourceLocation;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILightReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created by Elec332 on 19-1-2020
 */
@ModelHandler
@OnlyIn(Dist.CLIENT)
public class ModelLoaderCoreModule implements IModelHandler {

    public static final ResourceLocation RESONATING_CRYSTAL = new DeepResonanceResourceLocation("resonating_crystal_model");

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty EMPTY = BooleanProperty.create("empty");
    public static final BooleanProperty VERY_PURE = BooleanProperty.create("very_pure");

    public static final ModelProperty<Float> POWER = new ModelProperty<>();
    public static final ModelProperty<Float> PURITY = new ModelProperty<>();

    public static StateContainer<Block, BlockState> stateContainer;

    @APIHandlerInject
    @OnlyIn(Dist.CLIENT)
    private static void registerBlockState(IElecRenderingRegistry renderingRegistry) {
        stateContainer = renderingRegistry.registerBlockStateLocation(RESONATING_CRYSTAL, FACING, EMPTY, VERY_PURE);
    }

    @Nonnull
    @Override
    public Collection<ResourceLocation> getHandlerModelLocations() {
        return Collections.singletonList(CoreModule.RESONATING_CRYSTAL_BLOCK.getId());
    }

    @Override
    public void registerBakedModels(Function<ModelResourceLocation, IBakedModel> bakedModelGetter, ModelLoader modelLoader, BiConsumer<ModelResourceLocation, IBakedModel> registry) {
        BlockState state = stateContainer.getBaseState();
        BiFunction<Float, Float, IBakedModel> itemModelGetter_ = null;
        for (Direction dir : FACING.getAllowedValues()) {
            IBakedModel emptyNaturalModel = bakedModelGetter.apply(getLocation(state.with(FACING, dir).with(EMPTY, true).with(VERY_PURE, false)));
            IBakedModel fullNaturalModel = bakedModelGetter.apply(getLocation(state.with(FACING, dir).with(EMPTY, false).with(VERY_PURE, false)));
            IBakedModel emptyPureModel = bakedModelGetter.apply(getLocation(state.with(FACING, dir).with(EMPTY, true).with(VERY_PURE, true)));
            IBakedModel fullPureModel = bakedModelGetter.apply(getLocation(state.with(FACING, dir).with(EMPTY, false).with(VERY_PURE, true)));

            final BiFunction<Float, Float, IBakedModel> modelGetter = (power, purity) -> {
                if (power == null || purity == null) {
                    return fullNaturalModel;
                }
                if (power < Constants.CRYSTAL_MIN_POWER) {
                    if (purity > 30.0f) {
                        return emptyPureModel;
                    } else {
                        return emptyNaturalModel;
                    }
                } else {
                    if (purity > 30.0f) {
                        return fullPureModel;
                    } else {
                        return fullNaturalModel;
                    }
                }
            };
            if (dir == Direction.NORTH) {
                itemModelGetter_ = modelGetter;
            }

            IBakedModel wrapped = new WrappedModel(fullNaturalModel) {

                @Nonnull
                @Override //Todo: Move to tile?
                public IModelData getModelData(@Nonnull ILightReader world, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull IModelData tileData) {
                    if (tileData == EmptyModelData.INSTANCE) {
                        tileData = new ModelDataMap.Builder().build();
                    }
                    TileEntity tile = WorldHelper.getTileAt(world, pos);
                    if (tile instanceof TileEntityResonatingCrystal) {
                        tileData.setData(POWER, ((TileEntityResonatingCrystal) tile).getPower());
                        tileData.setData(PURITY, ((TileEntityResonatingCrystal) tile).getPurity());
                    }
                    return tileData;
                }

                @Nonnull
                @Override
                public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random rand, @Nonnull IModelData extraData) {
                    return modelGetter.apply(extraData.getData(POWER), extraData.getData(PURITY)).getQuads(state, side, rand, extraData);
                }

            };
            registry.accept(BlockModelShapes.getModelLocation(Preconditions.checkNotNull(CoreModule.RESONATING_CRYSTAL_BLOCK.get()).getDefaultState().with(FACING, dir)), wrapped);
        }

        final BiFunction<Float, Float, IBakedModel> itemModelGetter = Preconditions.checkNotNull(itemModelGetter_);
        final ItemOverrideList iol = new AbstractItemOverrideList() {

            @Nullable
            @Override
            protected IBakedModel getModel(@Nonnull IBakedModel model, @Nonnull ItemStack stack, @Nullable World worldIn, @Nullable LivingEntity entityIn) {
                float power = AbstractItemBlock.getTileData(stack).getFloat("power");
                float purity = AbstractItemBlock.getTileData(stack).getFloat("purity");
                return itemModelGetter.apply(power, purity);
            }

        };
        IBakedModel wrapped = new WrappedModel(itemModelGetter.apply(100F, 0F)) { //Full & non-pure (natural)

            @Nonnull
            @Override
            public ItemOverrideList getOverrides() {
                return iol;
            }

        };
        registry.accept(new ModelResourceLocation(CoreModule.RESONATING_CRYSTAL_ITEM.getId(), "inventory"), wrapped);
    }

    private ModelResourceLocation getLocation(BlockState state) {
        return BlockModelShapes.getModelLocation(RESONATING_CRYSTAL, state);
    }

}