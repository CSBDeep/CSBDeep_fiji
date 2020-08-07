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
package de.csbdresden.csbdeep.network.model.tensorflow;

import java.io.FileNotFoundException;
import java.sql.Timestamp;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

import de.csbdresden.csbdeep.network.DefaultModelLoader;
import de.csbdresden.csbdeep.network.ModelLoader;
import de.csbdresden.csbdeep.network.model.Network;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.tensorflow.TensorFlowService;
import net.imglib2.type.numeric.real.FloatType;

@Plugin(type = Command.class)
public class TensorflowCommand implements Command {

    @Parameter
    Dataset dataset;

    @Parameter
    private TensorFlowService tensorFlowService;

    @Parameter
    private DatasetService datasetService;

    @Parameter(label="load network 3d", callback = "loadNetwork3d")
    private Button button3d;

    @Parameter(label="load network 2d", callback = "loadNetwork2d")
    private Button button2d;


    Network network = null;
    ModelLoader modelLoader = new DefaultModelLoader();

    private void initNetwork() {
        if(network == null)
            network = new TensorFlowNetwork(null);
    }

    private void loadNetwork2d() throws FileNotFoundException {
        initNetwork();
        network.dispose();
        modelLoader.run("model2d-" + new Timestamp(System.currentTimeMillis()).getTime(), network, "/home/random/Development/imagej/project/CSBDeep/tests/generic_test2/denoise2D/model.zip", dataset);
    }

    private void loadNetwork3d() throws FileNotFoundException {
        initNetwork();
        network.dispose();
        modelLoader.run("model3d-" + new Timestamp(System.currentTimeMillis()).getTime(), network, "/home/random/Development/imagej/project/CSBDeep/tests/generic_test2/denoise3D/model.zip", dataset);
    }

    @Override
    public void run() {

        initNetwork();

        try {
            modelLoader.run("model2d-" + new Timestamp(System.currentTimeMillis()).getTime(), network, "/home/random/Development/imagej/project/CSBDeep/tests/generic_test2/denoise2D/model.zip", dataset);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        network.dispose();
        try {
            modelLoader.run("model3d-" + new Timestamp(System.currentTimeMillis()).getTime(), network, "/home/random/Development/imagej/project/CSBDeep/tests/generic_test2/denoise3D/model.zip", dataset);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void main(final String... args) throws Exception {

        final ImageJ ij = new ImageJ();

        ij.launch(args);

        final Dataset dataset = ij.dataset().create(new FloatType(), new long[] { 30, 80, 2,
                5 }, "", new AxisType[] { Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z });

        ij.ui().show(dataset);

        ij.command().run(TensorflowCommand.class, true, "dataset", dataset);

    }

}
