package mpicbg.csbd.network;

import java.sql.Timestamp;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

import mpicbg.csbd.network.task.DefaultModelLoader;
import mpicbg.csbd.network.task.ModelLoader;
import mpicbg.csbd.network.tensorflow.TensorFlowNetwork;
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
            network = new TensorFlowNetwork(tensorFlowService, datasetService,
                null);
    }

    private void loadNetwork2d() {
        initNetwork();
        network.dispose();
        modelLoader.run("model2d-" + new Timestamp(System.currentTimeMillis()).getTime(), network, "/home/random/Development/imagej/project/CSBDeep/tests/generic_test2/denoise2D/model.zip", dataset);
    }

    private void loadNetwork3d() {
        initNetwork();
        network.dispose();
        modelLoader.run("model3d-" + new Timestamp(System.currentTimeMillis()).getTime(), network, "/home/random/Development/imagej/project/CSBDeep/tests/generic_test2/denoise3D/model.zip", dataset);
    }

    @Override
    public void run() {
        System.out.println("jo");
//        Location model3d = null, model2d = null;
//        try {
//            model3d = IOHelper.loadFileOrURL("/home/random/Development/imagej/project/CSBDeep/tests/generic_test2/denoise3D/model.zip");
//            model2d = IOHelper.loadFileOrURL("/home/random/Development/imagej/project/CSBDeep/tests/generic_test2/denoise2D/model.zip");
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        try {
//            System.out.println("blub");
//            tensorFlowService.loadModel(model3d, "tmp_3dmodel", "serve");
//            tensorFlowService.loadModel(model2d, "tmp_2dmodel", "serve");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        initNetwork();

        modelLoader.run("model2d-" + new Timestamp(System.currentTimeMillis()).getTime(), network, "/home/random/Development/imagej/project/CSBDeep/tests/generic_test2/denoise2D/model.zip", dataset);
        network.dispose();
        modelLoader.run("model3d-" + new Timestamp(System.currentTimeMillis()).getTime(), network, "/home/random/Development/imagej/project/CSBDeep/tests/generic_test2/denoise3D/model.zip", dataset);
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
