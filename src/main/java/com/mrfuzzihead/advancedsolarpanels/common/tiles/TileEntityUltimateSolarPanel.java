package advsolar.common.tiles;

import advsolar.common.AdvancedSolarPanel;

public class TileEntityUltimateSolarPanel extends TileEntitySolarPanel {
   public TileEntityUltimateSolarPanel() {
      super(
         "blockUltimateSolarPanel.name",
         3,
         AdvancedSolarPanel.uhGenDay,
         AdvancedSolarPanel.uhGenNight,
         AdvancedSolarPanel.uhOutput,
         AdvancedSolarPanel.uhStorage
      );
   }

   @Override
   public String getInvName() {
      return "Ult Solar Panel";
   }
}
