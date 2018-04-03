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
package mpicbg.csbd.commands;

import mpicbg.csbd.ui.MappingDialog;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.numeric.RealType;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.widget.Button;

import java.io.File;

/**
 */
@Plugin( type = Command.class, menuPath = "Plugins>CSBDeep>Generic network", headless = true )
public class GenericNetwork extends CSBDeepCommand implements Command {

	@Parameter( visibility = ItemVisibility.MESSAGE )
	protected String normtext = "Normalization";
	@Parameter
	protected boolean _normalizeInput = true;
	@Parameter
	protected float _percentileBottom = 0.03f;
	@Parameter
	protected float _percentileTop = 0.998f;

	protected float _min = 0;
	protected float _max = 1;

	@Parameter( label = "Clamp normalization" )
	protected boolean _clamp = false;

	@Parameter( label = "Import model (.zip)", callback = "modelChanged", initializer = "modelInitialized", persist = false )
	private File modelFile;
	private final String modelFileKey = "modelfile-anynetwork";
	@Parameter( label = "Model name")
	private String _modelName;

	@Parameter( label = "Adjust image <-> tensorflow mapping", callback = "openTFMappingDialog" )
	private Button changeTFMapping;

	@Parameter
	private PrefService prefService;

	/** Executed whenever the {@link #modelFile} parameter is initialized. */
	protected void modelInitialized() {
		final String p_modelfile = prefService.get( modelFileKey, "" );
		if ( p_modelfile != "" ) {
			modelFile = new File( p_modelfile );
		}
	}

	/** Executed whenever the {@link #modelFile} parameter changes. */
	protected void modelChanged() {

		if ( modelFile != null ) {
			savePreferences();
		}

	}

	protected void openTFMappingDialog() {

		prepareInputAndNetwork();

		MappingDialog.create( network.getInputNode(), network.getOutputNode() );
	}

	@Override
	protected void prepareInputAndNetwork() {
		modelFileUrl = modelFile.getAbsolutePath();
		modelName = _modelName;
		super.prepareInputAndNetwork();
		checkAndResolveDimensionReduction();
	}

	private void checkAndResolveDimensionReduction() {
		for(AxisType axis : network.getInputNode().getNodeAxes()) {
			if(!network.getOutputNode().getNodeAxes().contains(axis)){
//				log("Network input node axis " + axis.getLabel() + " not present in output node, will be reduced");
				network.setDoDimensionReduction( true, axis );
			}
		}
		network.doDimensionReduction();
	}

	@Override
	public void run() {

		normalizeInput = _normalizeInput;
		percentileBottom = _percentileBottom;
		percentileTop = _percentileTop;
		min = _min;
		max = _max;
		clamp = _clamp;

		prepareInputAndNetwork();
		checkAndResolveDimensionReduction();

//		try {
			// TODO is validation input needed?
//			validateInput(
//					getInput(),
//					"3D grayscale image with dimension order X-Y-Z",
//					OptionalLong.empty(),
//					OptionalLong.empty(),
//					OptionalLong.empty());
			super.run();
//		} catch (final IOException e) {
//			showError(e.getMessage());
//		}
	}

	private void savePreferences() {
		prefService.put( modelFileKey, modelFile.getAbsolutePath() );
	}

	/**
	 * This main function serves for development purposes.
	 * It allows you to run the plugin immediately out of
	 * your integrated development environment (IDE).
	 *
	 * @param args
	 *            whatever, it's ignored
	 * @throws Exception
	 */
	public static void main( final String... args ) throws Exception {

		final ImageJ ij = new ImageJ();

		ij.launch( args );

		// ask the user for a file to open
//		final File file = ij.ui().chooseFile( null, "open" );
		final File file =
				new File( "/home/random/Development/imagej/plugins/CSBDeep-data/net_project/input-1.tif" );

		if ( file != null && file.exists() ) {
			// load the dataset
			final Dataset dataset = ij.scifio().datasetIO().open( file.getAbsolutePath() );

			// show the image
			ij.ui().show( dataset );

			// invoke the plugin
			ij.command().run( GenericNetwork.class, true );
		}

	}

}
