/*-
 * #%L
 * CSBDeep: CNNs for image restoration of fluorescence microscopy.
 * %%
 * Copyright (C) 2017 - 2020 Deborah Schmidt, Florian Jug, Benjamin Wilhelm
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

package de.csbdresden.csbdeep.ui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import de.csbdresden.csbdeep.network.model.ImageTensor;

public class MappingDialog {

	private static String NOT_USED_LABEL = "-";

	public static void create(final ImageTensor inputNode,
		final ImageTensor outputNode)
	{

		if(inputNode == null) return;

		final List<JComboBox<String>> inputDrops = new ArrayList<>();
		// final List< JComboBox< String > > outputDrops = new ArrayList<>();

		final JPanel dialogPanel = new JPanel();
		final JPanel inputDimPanel = new JPanel();
		final JPanel imgDimPanel = new JPanel();
		final JPanel inputMappingPanel = new JPanel();
		// final JPanel outputMappingPanel = new JPanel();

		imgDimPanel.setBorder(BorderFactory.createTitledBorder("Image"));
		inputMappingPanel.setBorder(BorderFactory.createTitledBorder(
			"Mapping input"));
		// outputMappingPanel.setBorder( BorderFactory.createTitledBorder(
		// "Mapping output" ) );
		inputDimPanel.setBorder(BorderFactory.createTitledBorder("Model input"));

		final List<String> dimStringsSize = new ArrayList<>();
		for (int i = 0; i < inputNode.numDimensions(); i++) {

			final String dimName = inputNode.getDatasetDimName(i);
			final long dimSize = inputNode.getImageDimensions().get(i);

			final JTextField field = new JTextField();
			field.setText(String.valueOf(dimSize));
			field.setEditable(false);
			imgDimPanel.add(new JLabel(dimName + ":", SwingConstants.RIGHT));
			imgDimPanel.add(field);
			dimStringsSize.add(MappingDialog.dimString(dimName, dimSize));
		}
		dimStringsSize.add(NOT_USED_LABEL);

		int tfDimCount = 0;
		for (int i = 0; i < inputNode.getNodeShape().length; i++) {

			final String dimName = inputNode.getDatasetDimNameByNodeDim(i);
			final long dimSize = inputNode.getDatasetDimSizeByNodeDim(i);
			final String tfDimSize = String.valueOf(inputNode
				.getNodeShape()[tfDimCount]);
			final JTextField field = new JTextField();
			field.setText(tfDimSize);
			field.setEditable(false);
			inputDimPanel.add(new JLabel(tfDimCount + ":", SwingConstants.RIGHT));
			inputDimPanel.add(field);
			final List<String> dimStringsLengthArr = new ArrayList<>();
			for (int j = 0; j < dimStringsSize.size(); j++) {
				dimStringsLengthArr.add(dimStringsSize.get(j));
			}
			final JComboBox dimDrop = new JComboBox<>(dimStringsLengthArr
				.toArray());
			if (dimStringsLengthArr.contains(MappingDialog.dimString(dimName,
				dimSize)))
			{
				dimDrop.setSelectedItem(MappingDialog.dimString(dimName, dimSize));
			}
			else {
				dimDrop.setSelectedItem(NOT_USED_LABEL);
			}

			inputMappingPanel.add(new JLabel(tfDimCount + " [" + tfDimSize + "] :",
				SwingConstants.RIGHT));
			inputMappingPanel.add(dimDrop);
			inputDrops.add(dimDrop);

			tfDimCount++;
		}

		tfDimCount = 0;

		// for ( int i = 0; i < outputNode.getNodeShape().length; i++ ) {
		//
		// final String dimName = outputNode.getDatasetDimNameByNodeDim( i );
		// final long dimSize = outputNode.getDatasetDimSizeByNodeDim( i );
		// final String tfDimSize = String.valueOf(
		// outputNode.getNodeShape()[ tfDimCount ] );
		//
		// String[] availableAxes = new
		// String[outputNode.getAvailableAxes().length];
		// for(int j = 0; j < availableAxes.length; j++) {
		// availableAxes[j] = outputNode.getAvailableAxes()[j].getLabel();
		// }
		// final JComboBox< String > dimDrop = new JComboBox<>(availableAxes);
		// dimDrop.setSelectedItem( MappingDialog.dimString( dimName, dimSize ) );
		// outputMappingPanel.add(
		// new JLabel( tfDimCount + " [" + tfDimSize + "] :", SwingConstants.RIGHT
		// ) );
		// outputMappingPanel.add( dimDrop );
		// outputDrops.add( dimDrop );
		//
		// tfDimCount++;
		// }

		final GridLayout col1Layout = new GridLayout(0, 1);
		final GridLayout col5Layout = new GridLayout(0, 10);
		col5Layout.setHgap(15);
		col1Layout.setVgap(15);

		imgDimPanel.setLayout(col5Layout);
		inputDimPanel.setLayout(col5Layout);
		inputMappingPanel.setLayout(col5Layout);
		dialogPanel.setLayout(col1Layout);

		dialogPanel.add(imgDimPanel);
		dialogPanel.add(inputDimPanel);
		dialogPanel.add(inputMappingPanel);
		// dialogPanel.add( outputMappingPanel );

		final int result = JOptionPane.showConfirmDialog(null, dialogPanel,
			"Please match image and tensorflow model dimensions",
			JOptionPane.OK_CANCEL_OPTION);

		if (result == JOptionPane.OK_OPTION) {
			for (int i = 0; i < inputDrops.size(); i++) {
//				 System.out.println(
//				 "selected index for tf index " + i + ": " + inputDrops.get(
//				 i ).getSelectedIndex() );
				inputNode.setNodeAxisByImageAxisIndex(i, inputDrops.get(i)
					.getSelectedIndex());
			}
			inputNode.generateMapping();
			inputNode.printMapping();
			// for ( int i = 0; i < outputDrops.size(); i++ ) {
			// outputNode.setNodeAxisByKnownAxesIndex( i, outputDrops.get( i
			// ).getSelectedIndex() );
			// }
		}

	}

	public static String dimString(final String dimName, final long dimSize) {
		return dimName + " [" + dimSize + "]";
	}

}
