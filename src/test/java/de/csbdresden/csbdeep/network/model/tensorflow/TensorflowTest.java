package de.csbdresden.csbdeep.network.model.tensorflow;

import org.junit.Ignore;
import org.junit.Test;

import net.imagej.ImageJ;

public class TensorflowTest {

    @Test
    @Ignore
    public void testTensorflowService() {
        ImageJ ij = new ImageJ();
        ij.ui().setHeadless(true);
        ij.command().run(TensorflowCommand.class, false);
    }

}
