package mpicbg.csbd.tensorflow;

import java.util.Arrays;

import javax.swing.JOptionPane;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;
import org.tensorflow.framework.SignatureDef;
import org.tensorflow.framework.TensorInfo;

public class TensorFlowRunner {

	/*
	 * runs graph on input tensor
	 * converts result tensor to dataset
	 */
	public static Tensor executeGraph(
			final SavedModelBundle model,
			final SignatureDef sig,
			final Tensor image,
			final String inputNodeName,
			final String outputNodeName ) {

		System.out.println(
				"executeInceptionGraph with input shape " + Arrays.toString( image.shape() ) );

		Tensor output_t = null;
		try {
			/*
			 * execute graph
			 */
			output_t = model.session().runner() //
					.feed( opName( sig.getInputsOrThrow( inputNodeName ) ), image ) //
					.fetch( opName( sig.getOutputsOrThrow( outputNodeName ) ) ) //
					.run().get( 0 );
		} catch ( final Exception e ) {
			e.printStackTrace();
		}

		if ( output_t != null ) {

			if ( output_t.numDimensions() == 0 ) {
				showError( "Output tensor has no dimensions" );
				return null;
			}

			return output_t;
		}
		return null;
	}

	/**
	 * The SignatureDef inputs and outputs contain names of the form
	 * {@code <operation_name>:<output_index>}, where for this model,
	 * {@code <output_index>} is always 0. This function trims the {@code :0}
	 * suffix to get the operation name.
	 */
	private static String opName( final TensorInfo t ) {
		final String n = t.getName();
		if ( n.endsWith( ":0" ) ) { return n.substring( 0, n.lastIndexOf( ":0" ) ); }
		return n;
	}

	public static void showError( final String errorMsg ) {
		JOptionPane.showMessageDialog(
				null,
				errorMsg,
				"Error",
				JOptionPane.ERROR_MESSAGE );
	}

}
