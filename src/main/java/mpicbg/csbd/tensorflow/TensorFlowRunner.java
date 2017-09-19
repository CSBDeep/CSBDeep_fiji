package mpicbg.csbd.tensorflow;

import javax.swing.JOptionPane;

import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

public class TensorFlowRunner {

	/*
	 * runs graph on input tensor
	 * converts result tensor to dataset
	 */
	public static Tensor executeGraph( final Graph g, final Tensor image, String inputNodeName, String outputNodeName ) {

		System.out.println( "executeInceptionGraph" );

		try (
				Session s = new Session( g );) {

//			int size = s.runner().feed(inputNodeName, image).fetch(outputNodeName).run().size();
//			System.out.println("output array size: " + size);

			Tensor output_t = null;

			/*
			 * check if keras_learning_phase node has to be set
			 */
			if ( g.operation( "dropout_1/keras_learning_phase" ) != null ) {
				final Tensor learning_phase = Tensor.create( false );
				try {
					/*
					 * execute graph
					 */
					final Tensor output_t2 = s.runner().feed( inputNodeName, image ).feed(
							"dropout_1/keras_learning_phase",
							learning_phase ).fetch( outputNodeName ).run().get( 0 );
					output_t = output_t2;
				} catch ( final Exception e ) {
					e.printStackTrace();
				}
			} else {
				try {
					/*
					 * execute graph
					 */
					final Tensor output_t2 = s.runner().feed( inputNodeName, image ).fetch(
							outputNodeName ).run().get( 0 );
					output_t = output_t2;
				} catch ( final Exception e ) {
					e.printStackTrace();
				}
			}

			if ( output_t != null ) {
				System.out.println(
						"Output tensor with " + output_t.numDimensions() + " dimensions" );

				if ( output_t.numDimensions() == 0 ) {
					showError( "Output tensor has no dimensions" );
					return null;
				}

				return output_t;
			}
			return null;

		} catch ( final Exception e ) {
			System.out.println( "could not create output dataset" );
			e.printStackTrace();
		}
		return null;
	}
	
	public static void showError( final String errorMsg ) {
		JOptionPane.showMessageDialog(
				null,
				errorMsg,
				"Error",
				JOptionPane.ERROR_MESSAGE );
	}

	
}

