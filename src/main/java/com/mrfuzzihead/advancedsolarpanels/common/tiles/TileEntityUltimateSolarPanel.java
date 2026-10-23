package com.mrfuzzihead.advancedsolarpanels.common.tiles;

import com.mrfuzzihead.advancedsolarpanels.AdvancedSolarPanel;

public class TileEntityUltimateSolarPanel extends TileEntitySolarPanel {

    public TileEntityUltimateSolarPanel() {
        super(
            "blockUltimateSolarPanel.name",
            3,
            AdvancedSolarPanel.uhGenDay,
            AdvancedSolarPanel.uhOutput,
            AdvancedSolarPanel.uhStorage,
            3);
    }
}
