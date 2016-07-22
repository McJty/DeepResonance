package mcjty.deepresonance.blocks.valve;

import mcjty.deepresonance.DeepResonance;
import mcjty.lib.base.StyleConfig;
import mcjty.lib.container.GenericGuiContainer;
import mcjty.lib.gui.Window;
import mcjty.lib.gui.events.ButtonEvent;
import mcjty.lib.gui.layout.HorizontalAlignment;
import mcjty.lib.gui.layout.HorizontalLayout;
import mcjty.lib.gui.layout.PositionalLayout;
import mcjty.lib.gui.layout.VerticalLayout;
import mcjty.lib.gui.widgets.Button;
import mcjty.lib.gui.widgets.*;
import mcjty.lib.gui.widgets.Label;
import mcjty.lib.gui.widgets.Panel;
import mcjty.lib.gui.widgets.TextField;
import mcjty.lib.network.Argument;
import mcjty.lib.varia.RedstoneMode;
import net.minecraft.util.ResourceLocation;

import java.awt.*;

public class GuiValve extends GenericGuiContainer<ValveTileEntity> {
    public static final int VALVE_WIDTH = 180;
    public static final int VALVE_HEIGHT = 152;

    private static final ResourceLocation iconLocation = new ResourceLocation(DeepResonance.MODID, "textures/gui/valve.png");
    private static final ResourceLocation iconGuiElements = new ResourceLocation(DeepResonance.MODID, "textures/gui/guielements.png");

    private TextField minPurity;
    private TextField minStrength;
    private TextField minEfficiency;
    private TextField maxAmount;
    private ImageChoiceLabel redstoneMode;

    public GuiValve(ValveTileEntity valveTileEntity, ValveContainer container) {
        super(DeepResonance.instance, DeepResonance.networkHandler.getNetworkWrapper(), valveTileEntity, container, 0, "valve");

        xSize = VALVE_WIDTH;
        ySize = VALVE_HEIGHT;
    }

    private void initRedstoneMode() {
        redstoneMode = new ImageChoiceLabel(mc, this).
                addChoiceEvent((parent, newChoice) -> changeRedstoneMode()).
                addChoice(RedstoneMode.REDSTONE_IGNORED.getDescription(), "Redstone mode:\nIgnored", iconGuiElements, 0, 0).
                addChoice(RedstoneMode.REDSTONE_OFFREQUIRED.getDescription(), "Redstone mode:\nOff to activate", iconGuiElements, 16, 0).
                addChoice(RedstoneMode.REDSTONE_ONREQUIRED.getDescription(), "Redstone mode:\nOn to activate", iconGuiElements, 32, 0);
        redstoneMode.setLayoutHint(new PositionalLayout.PositionalHint(154, 47, 16, 16));
        redstoneMode.setCurrentChoice(tileEntity.getRSMode().ordinal());
    }

    private void changeRedstoneMode() {
        tileEntity.setRSMode(RedstoneMode.values()[redstoneMode.getCurrentChoiceIndex()]);
        sendServerCommand(DeepResonance.networkHandler.getNetworkWrapper(), ValveTileEntity.CMD_RSMODE, new Argument("rs", RedstoneMode.values()[redstoneMode.getCurrentChoiceIndex()].getDescription()));
    }

    @Override
    public void initGui() {
        super.initGui();

        Panel toplevel = new Panel(mc, this).setBackground(iconLocation).setLayout(new PositionalLayout());

        Panel inputPanel = setupInputPanel();
        Panel outputPanel = setupOutputPanel();

        Button applyButton = new Button(mc, this)
                .setText("Apply")
                .setTooltips("Apply the new setting")
                .setLayoutHint(new PositionalLayout.PositionalHint(112, 49, 40, 14))
                .addButtonEvent(new ButtonEvent() {
                    @Override
                    public void buttonClicked(Widget parent) {
                        updateSettings();
                    }
                });

        initRedstoneMode();

        toplevel.addChild(inputPanel).addChild(outputPanel).addChild(applyButton).addChild(redstoneMode);

        toplevel.setBounds(new Rectangle(guiLeft, guiTop, xSize, ySize));

        window = new Window(this, toplevel);
    }

    private Panel setupOutputPanel() {
        maxAmount = new TextField(mc, this).setTooltips("The maximum amount of liquid", "in the bottom tank");
        maxAmount.setText(Integer.toString(tileEntity.getMaxMb())).setDesiredWidth(45).setDesiredHeight(15);

        return new Panel(mc, this).setLayout(new VerticalLayout().setSpacing(1).setVerticalMargin(2))
                .setFilledRectThickness(-2)
                .setFilledBackground(StyleConfig.colorListBackground)
                .addChild(new Label(mc, this).setText("Max mb").setDesiredWidth(50).setDesiredHeight(15))
                .addChild(maxAmount)
                .setLayoutHint(new PositionalLayout.PositionalHint(112, 6, 60, 40));
    }

    private Panel setupInputPanel() {
        minPurity = new TextField(mc, this).setTooltips("The minimum purity % to", "accept the liquid");
        minStrength = new TextField(mc, this).setTooltips("The minimum strength % to", "accept the liquid");
        minEfficiency = new TextField(mc, this).setTooltips("The minimum efficiency % to", "accept the liquid");
        minPurity.setText(Integer.toString((int) (tileEntity.getMinPurity() * 100))).setDesiredWidth(30).setDesiredHeight(15);
        minStrength.setText(Integer.toString((int) (tileEntity.getMinStrength() * 100))).setDesiredWidth(30).setDesiredHeight(15);
        minEfficiency.setText(Integer.toString((int) (tileEntity.getMinEfficiency() * 100))).setDesiredWidth(30).setDesiredHeight(15);
        Panel purityPanel = new Panel(mc, this).setLayout(new HorizontalLayout())
                .setDesiredHeight(16)
                .addChild(new Label(mc, this).setHorizontalAlignment(HorizontalAlignment.ALIGH_LEFT).setText("Purity").setDesiredWidth(50))
                .addChild(minPurity);
        Panel strengthPanel = new Panel(mc, this).setLayout(new HorizontalLayout())
                .setDesiredHeight(16)
                .addChild(new Label(mc, this).setHorizontalAlignment(HorizontalAlignment.ALIGH_LEFT).setText("Strength").setDesiredWidth(50))
                .addChild(minStrength);
        Panel efficiencyPanel = new Panel(mc, this).setLayout(new HorizontalLayout())
                .setDesiredHeight(16)
                .addChild(new Label(mc, this).setHorizontalAlignment(HorizontalAlignment.ALIGH_LEFT).setText("Efficiency").setDesiredWidth(50))
                .addChild(minEfficiency);

        return new Panel(mc, this).setLayout(new VerticalLayout().setSpacing(1).setVerticalMargin(3))
                .setFilledRectThickness(-2)
                .setFilledBackground(StyleConfig.colorListBackground)
                .addChild(purityPanel)
                .addChild(strengthPanel)
                .addChild(efficiencyPanel)
                .setLayoutHint(new PositionalLayout.PositionalHint(9, 6, 100, 58));
    }

    private void updateSettings() {
        int purity = 0;
        try {
            purity = Integer.parseInt(minPurity.getText());
        } catch (NumberFormatException e) {
        }
        int strength = 0;
        try {
            strength = Integer.parseInt(minStrength.getText());
        } catch (NumberFormatException e) {
        }
        int efficiency = 0;
        try {
            efficiency = Integer.parseInt(minEfficiency.getText());
        } catch (NumberFormatException e) {
        }
        int maxMb = 0;
        try {
            maxMb = Integer.parseInt(maxAmount.getText());
        } catch (NumberFormatException e) {
        }
        tileEntity.setMinPurity(purity / 100.0f);
        tileEntity.setMinStrength(strength / 100.0f);
        tileEntity.setMinEfficiency(efficiency / 100.0f);
        tileEntity.setMaxMb(maxMb);
        sendServerCommand(DeepResonance.networkHandler.getNetworkWrapper(), ValveTileEntity.CMD_SETTINGS,
                new Argument("purity", purity / 100.0f),
                new Argument("strength", strength / 100.0f),
                new Argument("efficiency", efficiency / 100.0f),
                new Argument("maxMb", maxMb));
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float v, int i, int i2) {
        drawWindow();
    }
}
