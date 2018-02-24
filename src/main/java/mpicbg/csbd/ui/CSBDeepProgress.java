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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

	private class Step {

		public JLabel status;
		public JLabel title;
		public int numIterations;
		public int iteration;
		public boolean stepDone;
	}

	final JButton okButton, cancelButton;
	private final JProgressBar progressBar;
	private final Component progressBarSpace;
	private final JTextPane taskOutput;

	public static final int STATUS_IDLE = -1;
	public static final int STATUS_RUNNING = 0;
	public static final int STATUS_DONE = 1;
	public static final int STATUS_FAIL = 2;

	List< Step > steps = new ArrayList<>();
	int currentStep;
	boolean currentStepFailing;
	int currentRound = 1;
	int numRounds = 1;

	final JPanel stepContainer;

	JLabel noTensorFlow =
			new JLabel( "<html>Couldn't load tensorflow from library<br />path and will therefore use CPU<br />instead of GPU version.<br />This will affect performance.<br />See wiki for further details.</html>", SwingConstants.RIGHT );

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

		stepContainer = new JPanel();
		stepContainer.setLayout( new BoxLayout( stepContainer, BoxLayout.Y_AXIS ) );

		stepContainer.setBorder( new EmptyBorder( 0, 0, 0, 123 ) );

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
		final TitledBorder warningborder =
				BorderFactory.createTitledBorder( borderline, "Warning" );
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
		topPanel.add( stepContainer, BorderLayout.WEST );
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

	public void addStep( final String title ) {
		final JPanel steprow = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
		final JLabel statusLabel = new JLabel( "\u2013", SwingConstants.CENTER );
		final Font font = statusLabel.getFont();
		statusLabel.setFont( new Font( font.getName(), Font.BOLD, font.getSize() * 2 ) );
		statusLabel.setPreferredSize( new Dimension( 50, 30 ) );
		statusLabel.setMinimumSize( new Dimension( 50, 30 ) );
		statusLabel.setMaximumSize( new Dimension( 50, 30 ) );
		final Step step = new Step();
		step.status = statusLabel;
		step.title = new JLabel( title );
		step.stepDone = false;
		steps.add( step );
		steprow.add( step.status );
		steprow.add( step.title );
		stepContainer.add( steprow );
	}

	private void resetProgress() {
		for ( final Step step : steps ) {
			step.stepDone = false;
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

		// TODO update progressbar
//		if ( currentStep != STEP_RUNMODEL ) progressBar.setValue( 0 );
//		progressBar.setVisible( currentStep == STEP_RUNMODEL );
//		progressBarSpace.setVisible( currentStep != STEP_RUNMODEL );

		//update OK and CANCEL buttons
		boolean alldone = true;
		for ( final Step step : steps ) {
			if ( !step.stepDone ) alldone = false;
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
			steps.get( currentStep ).stepDone = true;
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

	public void setStepDone( final int step ) {
		steps.get( currentStep ).stepDone = true;
		setStepStatus( step, STATUS_DONE );
		updateGUI();
	}

	public void setStepFail( final int step ) {
		currentStepFailing = true;
		setStepStatus( step, STATUS_FAIL );
		updateGUI();
	}

	private void setCurrentStepStatus( final int status ) {
		setStepStatus( currentStep, status );
	}

	private void setStepStatus( final int step, final int status ) {

		if ( status < steps.size() && step >= 0 ) {
			final JLabel statuslabel = steps.get( step ).status;
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

	public static CSBDeepProgress create() {
		return create( true );
	}

	public static CSBDeepProgress createWithGPUWarning() {
		return create( false );
	}

	private static CSBDeepProgress create( final boolean usesTF ) {
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

}
