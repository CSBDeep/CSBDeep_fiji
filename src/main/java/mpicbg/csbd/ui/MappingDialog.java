/*-
 * #%L
 * CSBDeep: CNNs for image restoration of fluorescence microscopy.
 * %%
 * Copyright (C) 2017 - 2018 Deborah Schmidt, Florian Jug, Benjamin Wilhelm
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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

import mpicbg.csbd.network.ImageTensor;

public class MappingDialog {

	public static void create( final ImageTensor inputNode ) {

		if ( inputNode.isMappingInitialized() ) {
			final List< JComboBox< String > > drops = new ArrayList<>();

			final JPanel dialogPanel = new JPanel();
			final JPanel inputDimPanel = new JPanel();
			final JPanel imgDimPanel = new JPanel();
			final JPanel mappingPanel = new JPanel();

			imgDimPanel.setBorder( BorderFactory.createTitledBorder( "Image" ) );
			mappingPanel.setBorder( BorderFactory.createTitledBorder( "Mapping" ) );
			inputDimPanel.setBorder( BorderFactory.createTitledBorder( "Model input" ) );

			final List< String > dimStringsSize = new ArrayList<>();
			for ( int i = 0; i < inputNode.numDimensions(); i++ ) {

				final String dimName = inputNode.getDatasetDimName( i );
				final long dimSize = inputNode.getDatasetDimSize( i );

				final JTextField field = new JTextField();
				field.setText( String.valueOf( dimSize ) );
				field.setEditable( false );
				imgDimPanel.add( new JLabel( dimName + ":", SwingConstants.RIGHT ) );
				imgDimPanel.add( field );
				dimStringsSize.add( MappingDialog.dimString( dimName, dimSize ) );
			}

			int tfDimCount = 0;
			for ( int i = 0; i < inputNode.numDimensions(); i++ ) {
				if ( inputNode.getDatasetDimIndexByTFIndex( i ) != null ) {

					final String dimName = inputNode.getDatasetDimNameByTFIndex( i );
					final long dimSize = inputNode.getDatasetDimSizeFromNodeDim( i );
					final String tfDimSize = String.valueOf(
							inputNode.getNodeShape()[ tfDimCount ] );

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
				inputNode.printMapping();
				for ( int i = 0; i < drops.size(); i++ ) {
					System.out.println(
							"selected index for tf index " + i + ": " + drops.get(
									i ).getSelectedIndex() );
					inputNode.setNodeAxisByKnownAxesIndex( i, drops.get( i ).getSelectedIndex() );
				}
				inputNode.printMapping();
			}
		} else {
			System.out.println(
					"Model and/or input image not initialized. call updateModel(); and updateImage() before opening mapping dialog" );
		}

	}

	public static String dimString( final String dimName, final long dimSize ) {
		return dimName + " [" + dimSize + "]";
	}

}
