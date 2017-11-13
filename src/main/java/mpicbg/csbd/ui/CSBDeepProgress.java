/*-
 * #%L
 * CSBDeep Fiji Plugin: Use deep neural networks for image restoration for fluorescence microscopy.
 * %%
 * Copyright (C) 2017 Deborah Schmidt, Florian Jug, Benjamin Wilhelm
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public class CSBDeepProgress extends JPanel
		implements
		PropertyChangeListener {

	final JButton okButton, cancelButton;
	private final JProgressBar progressBar;
	private final Component progressBarSpace;
	private final JTextPane taskOutput;

	public static final int STATUS_IDLE = -1;
	public static final int STATUS_RUNNING = 0;
	public static final int STATUS_DONE = 1;
	public static final int STATUS_FAIL = 2;

	public static final int STEP_LOADMODEL = 0;
	public static final int STEP_PREPROCRESSING = 1;
	public static final int STEP_RUNMODEL = 2;
	public static final int STEP_POSTPROCESSING = 3;

	JLabel[] stepTitle = { new JLabel( "Load model" ),
						   new JLabel( "Preprocessing" ),
						   new JLabel( "Run model" ),
						   new JLabel( "Postprocessing         " ) };
	JLabel[] stepStatus = new JLabel[ stepTitle.length ];
	boolean[] stepDone = new boolean[ stepTitle.length ];
	int currentStep;
	boolean currentStepFailing;
	int currentRound = 1;
	int numRounds = 1;

	JLabel noTensorFlow =
			new JLabel( "<html>Couldn't load tensorflow from library<br />path and will therefore use CPU<br />instead of GPU version.<br />This will affect performance.</html>", SwingConstants.RIGHT );

	private final SimpleAttributeSet red = new SimpleAttributeSet();

	public JButton getCancelBtn() {
		return cancelButton;
	}

	public JButton getOkBtn() {
		return okButton;
	}

	public CSBDeepProgress( final boolean usesTF ) {

		super( new BorderLayout() );

		StyleConstants.setForeground( red, Color.red );

		final JPanel steps = new JPanel();
		steps.setLayout( new BoxLayout( steps, BoxLayout.Y_AXIS ) );

		for ( int i = 0; i < stepTitle.length; i++ ) {
			final JPanel steprow = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
			final JLabel statusLabel = new JLabel( "\u2013", SwingConstants.CENTER );
			final Font font = statusLabel.getFont();
			statusLabel.setFont( new Font( font.getName(), Font.BOLD, font.getSize() * 2 ) );
			statusLabel.setPreferredSize( new Dimension( 50, 30 ) );
			statusLabel.setMinimumSize( new Dimension( 50, 30 ) );
			statusLabel.setMaximumSize( new Dimension( 50, 30 ) );
			stepStatus[ i ] = statusLabel;
			steprow.add( stepStatus[ i ] );
			steprow.add( stepTitle[ i ] );
			steps.add( steprow );
		}
		steps.setBorder( new EmptyBorder( 0, 0, 0, 123 ) );

		progressBar = new JProgressBar( 0, 100 );
		progressBar.setStringPainted( true );

		taskOutput = new JTextPane();
		taskOutput.setAutoscrolls( true );
		taskOutput.setMinimumSize( new Dimension( 0, 80 ) );
		taskOutput.setPreferredSize( new Dimension( 0, 80 ) );
		taskOutput.setMargin( new Insets( 5, 5, 5, 5 ) );
		taskOutput.setEditable( false );

		//WARNINGS
		final JPanel notePanel = new JPanel();
		notePanel.setLayout( new BoxLayout( notePanel, BoxLayout.Y_AXIS ) );
		notePanel.setMinimumSize( new Dimension( 280, 0 ) );
		notePanel.setPreferredSize( new Dimension( 280, 0 ) );
		final Border borderline = BorderFactory.createLineBorder( Color.red );
		final TitledBorder warningborder = BorderFactory.createTitledBorder( borderline, "Warning" );
		warningborder.setTitleColor( Color.red );
		if ( !usesTF ) {
			final JPanel note1 = new JPanel();
			note1.setBorder( warningborder );
			noTensorFlow.setBorder( new EmptyBorder( 2, 5, 5, 5 ) );
			note1.add( noTensorFlow );
			note1.setMinimumSize( new Dimension( 280, 100 ) );
			note1.setMaximumSize(
					new Dimension( 100000, ( int ) note1.getPreferredSize().getHeight() ) );
			notePanel.add( note1 );
		}
		notePanel.add( Box.createVerticalGlue() );

		final JPanel topPanel = new JPanel( new BorderLayout() );
		topPanel.add( steps, BorderLayout.WEST );
		topPanel.add( notePanel, BorderLayout.EAST );

		add( topPanel, BorderLayout.PAGE_START );

		final JPanel centerPanel = new JPanel();
		centerPanel.setLayout( new BoxLayout( centerPanel, BoxLayout.Y_AXIS ) );
		centerPanel.add( Box.createRigidArea( new Dimension( 0, 10 ) ) );
		progressBarSpace = Box.createRigidArea( new Dimension( 0, 20 ) );
		centerPanel.add( progressBarSpace );
		centerPanel.add( progressBar );
		centerPanel.add( new JScrollPane( taskOutput ) );
		add( centerPanel, BorderLayout.CENTER );
		setBorder( BorderFactory.createEmptyBorder( 20, 20, 20, 20 ) );

		okButton = new JButton( "Ok" );
		okButton.setEnabled( false );
		cancelButton = new JButton( "Cancel" );
		cancelButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {}
		} );

		resetProgress();

		final JPanel footer = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
		footer.setBorder( BorderFactory.createEmptyBorder( 15, 0, -5, -3 ) );
		footer.setAlignmentX( Component.RIGHT_ALIGNMENT );
		footer.add( cancelButton );
		footer.add( okButton );

		add( footer, BorderLayout.SOUTH );

	}

	private void resetProgress() {
		for ( int i = 0; i < stepDone.length; i++ ) {
			stepDone[ i ] = false;
		}
		currentStep = -1;
		currentStepFailing = false;
		progressBar.setValue( 0 );
	}

	public void setProgressBarMax( final int value ) {
		progressBar.setMaximum( value );
	}

	public void setProgressBarValue( final int value ) {
		progressBar.setValue( value );
	}

	private void updateGUI() {

		//update progressbar
		if ( currentStep != STEP_RUNMODEL ) progressBar.setValue( 0 );
		progressBar.setVisible( currentStep == STEP_RUNMODEL );
		progressBarSpace.setVisible( currentStep != STEP_RUNMODEL );

		//update OK and CANCEL buttons
		boolean alldone = true;
		for ( int i = 0; i < stepDone.length; i++ ) {
			if ( !stepDone[ i ] ) alldone = false;
		}
		if ( currentRound < numRounds ) alldone = false;
		okButton.setEnabled( alldone || currentStepFailing );
		cancelButton.setEnabled( !alldone && !currentStepFailing );

	}

	public void setStepStart( final int step ) {
		currentStep = step;
		currentStepFailing = false;
		setCurrentStepStatus( STATUS_RUNNING );
		updateGUI();
	}

	public void setCurrentStepDone() {
		if ( currentStep >= 0 ) {
			stepDone[ currentStep ] = true;
			setCurrentStepStatus( STATUS_DONE );
		}
		currentStepFailing = false;
		currentStep = -1;
		updateGUI();
	}

	public void setCurrentStepFail() {
		currentStepFailing = true;
		setCurrentStepStatus( STATUS_FAIL );
		updateGUI();
	}

	private void setCurrentStepStatus( final int status ) {
		setStepStatus( currentStep, status );
	}

	private void setStepStatus( final int step, final int status ) {

		if ( status < stepStatus.length && step >= 0 ) {
			final JLabel statuslabel = stepStatus[ step ];
			switch ( status ) {
			case STATUS_IDLE:
				statuslabel.setText( "\u2013" );
				statuslabel.setForeground( Color.getHSBColor( 0.6f, 0.f, 0.3f ) );
				break;
			case STATUS_RUNNING:
				statuslabel.setText( "\u2794" );
				statuslabel.setForeground( Color.getHSBColor( 0.6f, 0.f, 0.3f ) );
				break;
			case STATUS_DONE:
				statuslabel.setText( "\u2713" );
				statuslabel.setForeground( Color.getHSBColor( 0.3f, 1, 0.6f ) );
				break;
			case STATUS_FAIL:
				statuslabel.setText( "\u2013" );
				statuslabel.setForeground( Color.red );
				break;
			}
		}
	}

	public void addLog( final String data ) {
		addLog( data, null );
	}

	public void addLog( final String data, final SimpleAttributeSet style ) {
		final Date date = new Date();
		final DateFormat df = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
		try {
			taskOutput.getDocument().insertString(
					taskOutput.getDocument().getLength(),
					df.format( date ) + " | " + data + "\n",
					style );
			taskOutput.setCaretPosition( taskOutput.getDocument().getLength() );
		} catch ( final BadLocationException exc ) {
			// TODO Auto-generated catch block
			exc.printStackTrace();
		}
	}

	public void addError( final String data ) {
		addLog( data, red );
	}

	@Override
	public void propertyChange( final PropertyChangeEvent evt ) {
		// TODO Auto-generated method stub

	}

	public static CSBDeepProgress create( final boolean usesTF ) {
		//Create and set up the window.
		final JFrame frame = new JFrame( "CSBDeep progress" );
//		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		//Create and set up the content pane.
		final CSBDeepProgress newContentPane = new CSBDeepProgress( usesTF );
		newContentPane.getOkBtn().addActionListener( e -> frame.dispose() );
		newContentPane.setOpaque( true ); //content panes must be opaque
		frame.setContentPane( newContentPane );

		//Display the window.
		frame.pack();
		frame.setVisible( true );

		return newContentPane;
	}

	public boolean getProgressBarDone() {
		return progressBar.getValue() == progressBar.getMaximum();
	}

	public boolean getProgressBarStarting() {
		return progressBar.getValue() == 0;
	}

	public void setNextRound() {
		currentRound++;
		numRounds = Math.max( numRounds, currentRound );
		stepDone[ 1 ] = false;
		stepDone[ 2 ] = false;
		stepDone[ 3 ] = false;
		setStepStatus( 1, STATUS_IDLE );
		setStepStatus( 2, STATUS_IDLE );
		setStepStatus( 3, STATUS_IDLE );
		currentStep = 1;
		stepTitle[ 1 ].setText( "(" + currentRound + "/" + numRounds + ") Preprocessing" );
		stepTitle[ 2 ].setText( "(" + currentRound + "/" + numRounds + ") Run model" );
		stepTitle[ 3 ].setText( "(" + currentRound + "/" + numRounds + ") Postprocessing" );
	}

	public void setNumRounds( final int numRounds ) {
		this.numRounds = numRounds;
		if ( numRounds > 1 ) {
			stepTitle[ 1 ].setText( "(1/" + numRounds + ") Preprocessing" );
			stepTitle[ 2 ].setText( "(1/" + numRounds + ") Run model " );
			stepTitle[ 3 ].setText( "(1/" + numRounds + ") Postprocessing" );
		}
	}

	public void addRounds( final int rounds ) {
		setNumRounds( numRounds + rounds );
	}

}
