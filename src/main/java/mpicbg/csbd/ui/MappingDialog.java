package mpicbg.csbd.ui;

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

import mpicbg.csbd.tensorflow.DatasetTensorBridge;

public class MappingDialog {

	public static void create( final DatasetTensorBridge bridge ) {

		if ( bridge.complete() ) {
			final List< JComboBox< String > > drops = new ArrayList<>();

			final JPanel dialogPanel = new JPanel();
			final JPanel inputDimPanel = new JPanel();
			final JPanel imgDimPanel = new JPanel();
			final JPanel mappingPanel = new JPanel();

			imgDimPanel.setBorder( BorderFactory.createTitledBorder( "Image" ) );
			mappingPanel.setBorder( BorderFactory.createTitledBorder( "Mapping" ) );
			inputDimPanel.setBorder( BorderFactory.createTitledBorder( "Model input" ) );

			final List< String > dimStringsSize = new ArrayList<>();
			for ( int i = 0; i < bridge.numDimensions(); i++ ) {

				String dimName = bridge.getDatasetDimName( i );
				long dimSize = bridge.getDatasetDimSize( i );

				final JTextField field = new JTextField();
				field.setText( String.valueOf( dimSize ) );
				field.setEditable( false );
				imgDimPanel.add( new JLabel( dimName + ":", SwingConstants.RIGHT ) );
				imgDimPanel.add( field );
				dimStringsSize.add( MappingDialog.dimString( dimName, dimSize ) );
			}

			int tfDimCount = 0;
			for ( int i = 0; i < bridge.numDimensions(); i++ ) {
				if ( bridge.getDatasetDimIndexByTFIndex( i ) != null ) {

					String dimName = bridge.getDatasetDimNameByTFIndex( i );
					long dimSize = bridge.getDatasetDimSizeFromTFIndex( i );
					String tfDimSize = String.valueOf(
							bridge.getInputTensorInfo().getTensorShape().getDim(
									tfDimCount ).getSize() );

					final JTextField field = new JTextField();
					field.setText( tfDimSize );
					field.setEditable( false );
					inputDimPanel.add( new JLabel( tfDimCount + ":", SwingConstants.RIGHT ) );
					inputDimPanel.add( field );
					inputDimPanel.add( field );

					final String[] dimStringsLengthArr = new String[ dimStringsSize.size() ];
					for ( int j = 0; j < dimStringsSize.size(); j++ ) {
						dimStringsLengthArr[ j ] = dimStringsSize.get( j );
					}
					final JComboBox< String > dimDrop = new JComboBox<>( dimStringsLengthArr );
					dimDrop.setSelectedItem( MappingDialog.dimString( dimName, dimSize ) );
					mappingPanel.add(
							new JLabel( tfDimCount + " [" + tfDimSize + "] :", SwingConstants.RIGHT ) );
					mappingPanel.add( dimDrop );
					drops.add( dimDrop );

					tfDimCount++;
				}
			}

			final GridLayout col1Layout = new GridLayout( 0, 1 );
			final GridLayout col5Layout = new GridLayout( 0, 10 );
			col5Layout.setHgap( 15 );
			col1Layout.setVgap( 15 );

			imgDimPanel.setLayout( col5Layout );
			inputDimPanel.setLayout( col5Layout );
			mappingPanel.setLayout( col5Layout );
			dialogPanel.setLayout( col1Layout );

			dialogPanel.add( imgDimPanel );
			dialogPanel.add( inputDimPanel );
			dialogPanel.add( mappingPanel );

			final int result = JOptionPane.showConfirmDialog(
					null,
					dialogPanel,
					"Please match image and tensorflow model dimensions",
					JOptionPane.OK_CANCEL_OPTION );

			if ( result == JOptionPane.OK_OPTION ) {
				bridge.printMapping();
				for ( int i = 0; i < drops.size(); i++ ) {
					System.out.println(
							"selected index for tf index " + i + ": " + drops.get(
									i ).getSelectedIndex() );
					bridge.setTFMappingByKnownAxesIndex( i, drops.get( i ).getSelectedIndex() );
				}
				bridge.printMapping();
			}
		} else {
			System.out.println(
					"Model and/or input image not initialized. call updateModel(); and updateImage() before opening mapping dialog" );
		}

	}

	public static String dimString( String dimName, long dimSize ) {
		return dimName + " [" + dimSize + "]";
	}

}
