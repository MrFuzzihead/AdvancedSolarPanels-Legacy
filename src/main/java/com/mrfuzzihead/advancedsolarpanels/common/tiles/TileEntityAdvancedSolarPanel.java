package com.mrfuzzihead.advancedsolarpanels.common.tiles;

import com.mrfuzzihead.advancedsolarpanels.AdvancedSolarPanel;

public class TileEntityAdvancedSolarPanel extends TileEntitySolarPanel {

    public TileEntityAdvancedSolarPanel() {
        super(
            "blockAdvancedSolarPanel.name",
            2,
            AdvancedSolarPanel.advGenDay,
            AdvancedSolarPanel.advGenNight,
            AdvancedSolarPanel.advOutput,
            AdvancedSolarPanel.advStorage,
            1);
    }

    @Override
    public String getInvName() {
        return "Adv Solar Panel";
    }
}
