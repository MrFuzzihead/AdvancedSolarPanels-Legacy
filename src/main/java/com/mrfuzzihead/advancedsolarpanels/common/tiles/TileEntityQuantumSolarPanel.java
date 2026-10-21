package com.mrfuzzihead.advancedsolarpanels.common.tiles;

import com.mrfuzzihead.advancedsolarpanels.AdvancedSolarPanel;

public class TileEntityQuantumSolarPanel extends TileEntitySolarPanel {

    public TileEntityQuantumSolarPanel() {
        super(
            "blockQuantumSolarPanel.name",
            4,
            AdvancedSolarPanel.qpGenDay,
            AdvancedSolarPanel.qpGenNight,
            AdvancedSolarPanel.qpOutput,
            AdvancedSolarPanel.qpStorage);
    }

    @Override
    public String getInvName() {
        return "Quantum Solar Panel";
    }
}
