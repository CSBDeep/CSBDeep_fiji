package mpicbg.csbd;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class MappingDialog {

	public static void create(final DatasetTensorBridge bridge) {
		
		if(bridge.complete()){
			List<JComboBox<String>> drops = new ArrayList<>();
			
			JPanel dialogPanel = new JPanel();
			JPanel modelDimPanel = new JPanel();
			JPanel imgDimPanel = new JPanel();
			JPanel mappingPanel = new JPanel();
			
			imgDimPanel.setBorder(BorderFactory.createTitledBorder("Image"));
			modelDimPanel.setBorder(BorderFactory.createTitledBorder("Model input"));
			mappingPanel.setBorder(BorderFactory.createTitledBorder("Mapping"));
			
			List<String> dimStringsLength = new ArrayList<>();
			for(int i = 0; i < bridge.numDimensions(); i++){
				JTextField field = new JTextField();
		        field.setText(String.valueOf(bridge.getDatasetDimLength(i)));
		        field.setEditable(false);
		        imgDimPanel.add(new JLabel(bridge.getDatasetDimName(i) + ":", SwingConstants.RIGHT));
		        imgDimPanel.add(field);
		        dimStringsLength.add(bridge.getDatasetDimName(i) + " [" + bridge.getDatasetDimLength(i) + "]");
			}
			
			int dimCount = 0;
			for(int i = 0; i < bridge.numDimensions(); i++){
				if(bridge.getMapping(i) >= 0){
					JTextField field = new JTextField();
			        field.setText(String.valueOf(bridge.getTensorShape().size(dimCount)));
			        field.setEditable(false);
			        modelDimPanel.add(new JLabel(dimCount + ":", SwingConstants.RIGHT));
			        modelDimPanel.add(field);
			        modelDimPanel.add(field);
					
					JComboBox<String> dimDrop = new JComboBox(dimStringsLength.toArray());
					dimDrop.setSelectedIndex(bridge.getMapping(i));
					mappingPanel.add(new JLabel(dimCount + " [" + bridge.getTensorShape().size(dimCount) + "] :", SwingConstants.RIGHT));
					mappingPanel.add(dimDrop);
					drops.add(dimDrop);
					
					dimCount++;				
				}
			}
			
			GridLayout col1Layout = new GridLayout(0,1);
			GridLayout col5Layout = new GridLayout(0,10);
			col5Layout.setHgap(15);
			col1Layout.setVgap(15);
			imgDimPanel.setLayout(col5Layout);
			modelDimPanel.setLayout(col5Layout);
			mappingPanel.setLayout(col5Layout);
			dialogPanel.setLayout(col1Layout);
			dialogPanel.add(imgDimPanel);
			dialogPanel.add(modelDimPanel);
			dialogPanel.add(mappingPanel);
			
			int result = JOptionPane.showConfirmDialog(null, dialogPanel, 
					"Please match image and tensorflow model dimensions", JOptionPane.OK_CANCEL_OPTION);
			if (result == JOptionPane.OK_OPTION) {
				bridge.printMapping();
				for(int i = 0; i < drops.size(); i++){
					System.out.println("selected index for tf index " + i + ": " + drops.get(i).getSelectedIndex());
					bridge.setMappingRealTFIndex(i, drops.get(i).getSelectedIndex());
				}
				bridge.printMapping();
			}	
		}else{
			System.out.println("Model and/or input image not initialized. call updateModel(); and updateImage() before opening mapping dialog");
		}
		
	}

}
