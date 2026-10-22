package com.mrfuzzihead.advancedsolarpanels.common.tiles;

import com.mrfuzzihead.advancedsolarpanels.AdvancedSolarPanel;

public class TileEntityHybridSolarPanel extends TileEntitySolarPanel {

    public TileEntityHybridSolarPanel() {
        super(
            "blockHybridSolarPanel.name",
            3,
            AdvancedSolarPanel.hGenDay,
            AdvancedSolarPanel.hGenNight,
            AdvancedSolarPanel.hOutput,
            AdvancedSolarPanel.hStorage,
            2);
    }

    @Override
    public String getInvName() {
        return "Hyb Solar Panel";
    }
}
