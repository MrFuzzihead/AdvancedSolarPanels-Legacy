package com.mrfuzzihead.advancedsolarpanels.common.tiles;

import com.mrfuzzihead.advancedsolarpanels.AdvancedSolarPanel;

public class TileEntityQuantumSolarPanel extends TileEntitySolarPanel {

    public TileEntityQuantumSolarPanel() {
        super(
            "blockQuantumSolarPanel.name",
            4,
            AdvancedSolarPanel.qpGenDay,
            AdvancedSolarPanel.qpOutput,
            AdvancedSolarPanel.qpStorage,
            5);
    }
}
