/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2021 - 2023 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.cellpose;

import static fiji.plugin.trackmate.cellpose.CellposeDetectorFactory.KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH;
import static fiji.plugin.trackmate.cellpose.CellposeDetectorFactory.KEY_CELLPOSE_MODEL;
import static fiji.plugin.trackmate.cellpose.CellposeDetectorFactory.KEY_CELLPOSE_PYTHON_FILEPATH;
import static fiji.plugin.trackmate.cellpose.CellposeDetectorFactory.KEY_CELL_DIAMETER;
import static fiji.plugin.trackmate.cellpose.CellposeDetectorFactory.KEY_LOGGER;
import static fiji.plugin.trackmate.cellpose.CellposeDetectorFactory.KEY_OPTIONAL_CHANNEL_2;
import static fiji.plugin.trackmate.cellpose.CellposeDetectorFactory.KEY_USE_GPU;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.ThresholdDetectorFactory.KEY_SIMPLIFY_CONTOURS;
import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;
import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;
import static java.lang.Integer.min;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.cellpose.AbstractCellposeSettings.PretrainedModel;
import fiji.plugin.trackmate.cellpose.CellposeSettings.PretrainedModelCellpose;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.util.DetectionPreview;
import fiji.plugin.trackmate.util.FileChooser;
import fiji.plugin.trackmate.util.FileChooser.DialogType;
import fiji.plugin.trackmate.util.FileChooser.SelectionMode;

public class CellposeDetectorConfigurationPanel extends ConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	private static final String TITLE = CellposeDetectorFactory.NAME;

	protected static final ImageIcon ICON = CellposeUtils.cellposeLogo64();

	private static final NumberFormat DIAMETER_FORMAT = new DecimalFormat( "#.#" );

	protected static final String DOC1_URL = "https://imagej.net/plugins/trackmate/trackmate-cellpose";

	private final JButton btnBrowseCellposePath;

	protected final JTextField tfCellposeExecutable;

	protected final JComboBox< PretrainedModel > cmbboxPretrainedModel;

	protected final JComboBox< String > cmbboxCh1;
        
        protected int nbChannels;

	protected final JComboBox< String > cmbboxCh2;

	protected final JFormattedTextField ftfDiameter;

	protected final JCheckBox chckbxSimplify;

	protected final Logger logger;

	protected final JCheckBox chckbxUseGPU;

	protected final JTextField tfCustomPath;

	private final JButton btnBrowseCustomModel;

	private final String executableName;

	public CellposeDetectorConfigurationPanel(
			final Settings settings,
			final Model model )
	{
		this( settings, model, TITLE, ICON, DOC1_URL, "cellpose", PretrainedModelCellpose.values() );
	}

	protected CellposeDetectorConfigurationPanel(
			final Settings settings,
			final Model model,
			final String title,
			final Icon icon,
			final String docURL,
			final String executableName,
			final PretrainedModel[] pretrainedModels )
	{
		this.executableName = executableName;
		this.logger = model.getLogger();

		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.rowWeights = new double[] { 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., .1 };
		gridBagLayout.columnWidths = new int[] { 144, 0, 32 };
		gridBagLayout.columnWeights = new double[] { 1.0, 1.0, 0.0 };
		setLayout( gridBagLayout );

		int gridy = 0;

		final JLabel lblDetector = new JLabel( title, icon, JLabel.RIGHT );
		lblDetector.setFont( BIG_FONT );
		lblDetector.setHorizontalAlignment( SwingConstants.CENTER );
		final GridBagConstraints gbcLblDetector = new GridBagConstraints();
		gbcLblDetector.gridwidth = 3;
		gbcLblDetector.insets = new Insets( 0, 5, 5, 0 );
		gbcLblDetector.fill = GridBagConstraints.HORIZONTAL;
		gbcLblDetector.gridx = 0;
		gbcLblDetector.gridy = gridy;
		add( lblDetector, gbcLblDetector );

		gridy++;

		final String text = "Click here for the documentation";
		final JLabel lblUrl = new JLabel( text );
		lblUrl.setHorizontalAlignment( SwingConstants.CENTER );
		lblUrl.setForeground( Color.BLUE.darker() );
		lblUrl.setFont( FONT.deriveFont( Font.ITALIC ) );
		lblUrl.setCursor( new Cursor( Cursor.HAND_CURSOR ) );
		lblUrl.addMouseListener( new MyMouseAdapter( lblUrl, docURL, text ) );
		final GridBagConstraints gbcLblUrl = new GridBagConstraints();
		gbcLblUrl.fill = GridBagConstraints.HORIZONTAL;
		gbcLblUrl.gridwidth = 3;
		gbcLblUrl.insets = new Insets( 0, 10, 5, 15 );
		gbcLblUrl.gridx = 0;
		gbcLblUrl.gridy = gridy;
		add( lblUrl, gbcLblUrl );

		/*
		 * Path to Python or Cellpose.
		 */

		gridy++;

		final JLabel lblCusstomModelFile = new JLabel( "Path to " + executableName + " / python executable:" );
		lblCusstomModelFile.setFont( FONT );
		final GridBagConstraints gbcLblCusstomModelFile = new GridBagConstraints();
		gbcLblCusstomModelFile.gridwidth = 2;
		gbcLblCusstomModelFile.anchor = GridBagConstraints.SOUTHWEST;
		gbcLblCusstomModelFile.insets = new Insets( 0, 5, 5, 5 );
		gbcLblCusstomModelFile.gridx = 0;
		gbcLblCusstomModelFile.gridy = gridy;
		add( lblCusstomModelFile, gbcLblCusstomModelFile );

		btnBrowseCellposePath = new JButton( "Browse" );
		btnBrowseCellposePath.setFont( FONT );
		final GridBagConstraints gbcBtnBrowseCellposePath = new GridBagConstraints();
		gbcBtnBrowseCellposePath.insets = new Insets( 0, 0, 5, 5 );
		gbcBtnBrowseCellposePath.anchor = GridBagConstraints.SOUTHEAST;
		gbcBtnBrowseCellposePath.gridx = 2;
		gbcBtnBrowseCellposePath.gridy = gridy;
		add( btnBrowseCellposePath, gbcBtnBrowseCellposePath );

		gridy++;

		tfCellposeExecutable = new JTextField( "" );
		tfCellposeExecutable.setFont( SMALL_FONT );
		final GridBagConstraints gbcTfCellpose = new GridBagConstraints();
		gbcTfCellpose.gridwidth = 3;
		gbcTfCellpose.insets = new Insets( 0, 5, 5, 5 );
		gbcTfCellpose.fill = GridBagConstraints.BOTH;
		gbcTfCellpose.gridx = 0;
		gbcTfCellpose.gridy = gridy;
		add( tfCellposeExecutable, gbcTfCellpose );
		tfCellposeExecutable.setColumns( 15 );

		/*
		 * Custom model.
		 */

		gridy++;

		final JLabel lblPathToCustomModel = new JLabel( "Path to custom model:" );
		lblPathToCustomModel.setFont( new Font( "Arial", Font.PLAIN, 10 ) );
		final GridBagConstraints gbcLblPathToCustomModel = new GridBagConstraints();
		gbcLblPathToCustomModel.anchor = GridBagConstraints.SOUTHWEST;
		gbcLblPathToCustomModel.gridwidth = 2;
		gbcLblPathToCustomModel.insets = new Insets( 0, 5, 5, 5 );
		gbcLblPathToCustomModel.gridx = 0;
		gbcLblPathToCustomModel.gridy = gridy;
		add( lblPathToCustomModel, gbcLblPathToCustomModel );

		btnBrowseCustomModel = new JButton( "Browse" );
		btnBrowseCustomModel.setFont( new Font( "Arial", Font.PLAIN, 10 ) );
		final GridBagConstraints gbcBtnBrowseCustomModel = new GridBagConstraints();
		gbcBtnBrowseCustomModel.insets = new Insets( 0, 0, 5, 5 );
		gbcBtnBrowseCustomModel.anchor = GridBagConstraints.SOUTHEAST;
		gbcBtnBrowseCustomModel.gridx = 2;
		gbcBtnBrowseCustomModel.gridy = gridy;
		add( btnBrowseCustomModel, gbcBtnBrowseCustomModel );

		gridy++;

		tfCustomPath = new JTextField( " " );
		tfCustomPath.setFont( new Font( "Arial", Font.PLAIN, 10 ) );
		tfCustomPath.setColumns( 15 );
		final GridBagConstraints gbcTfCustomPath = new GridBagConstraints();
		gbcTfCustomPath.gridwidth = 3;
		gbcTfCustomPath.insets = new Insets( 0, 5, 5, 5 );
		gbcTfCustomPath.fill = GridBagConstraints.BOTH;
		gbcTfCustomPath.gridx = 0;
		gbcTfCustomPath.gridy = gridy;
		add( tfCustomPath, gbcTfCustomPath );

		/*
		 * Pretrained model.
		 */

		gridy++;

		final JLabel lblPretrainedModel = new JLabel( "Pretrained model:" );
		lblPretrainedModel.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblPretrainedModel = new GridBagConstraints();
		gbcLblPretrainedModel.anchor = GridBagConstraints.EAST;
		gbcLblPretrainedModel.insets = new Insets( 0, 5, 5, 5 );
		gbcLblPretrainedModel.gridx = 0;
		gbcLblPretrainedModel.gridy = gridy;
		add( lblPretrainedModel, gbcLblPretrainedModel );

		cmbboxPretrainedModel = new JComboBox<>( new Vector<>( Arrays.asList( pretrainedModels ) ) );
		cmbboxPretrainedModel.setFont( SMALL_FONT );
		final GridBagConstraints gbcCmbboxPretrainedModel = new GridBagConstraints();
		gbcCmbboxPretrainedModel.gridwidth = 2;
		gbcCmbboxPretrainedModel.insets = new Insets( 0, 5, 5, 5 );
		gbcCmbboxPretrainedModel.fill = GridBagConstraints.HORIZONTAL;
		gbcCmbboxPretrainedModel.gridx = 1;
		gbcCmbboxPretrainedModel.gridy = gridy;
		add( cmbboxPretrainedModel, gbcCmbboxPretrainedModel );

		/*
		 * Channel 1
		 */

		gridy++;

		final JLabel lblSegmentInChannel = new JLabel( "Channel to segment:" );
		lblSegmentInChannel.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblSegmentInChannel = new GridBagConstraints();
		gbcLblSegmentInChannel.anchor = GridBagConstraints.EAST;
		gbcLblSegmentInChannel.insets = new Insets( 0, 5, 5, 5 );
		gbcLblSegmentInChannel.gridx = 0;
		gbcLblSegmentInChannel.gridy = gridy;
		add( lblSegmentInChannel, gbcLblSegmentInChannel );

                nbChannels = min(settings.imp.getNChannels(),3); // CellPose cannot segment in chanels > 3 (setup for R, G, B)
                final List<String> l1 = new ArrayList<String>();
                for (int c=1; c<=nbChannels; c++ )
                {
                    l1.add(""+c);
                }
		cmbboxCh1 = new JComboBox<>( new Vector<>( l1 ) );
		cmbboxCh1.setFont( SMALL_FONT );
		final GridBagConstraints gbcSpinner = new GridBagConstraints();
		gbcSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbcSpinner.gridwidth = 2;
		gbcSpinner.insets = new Insets( 0, 5, 5, 5 );
		gbcSpinner.gridx = 1;
		gbcSpinner.gridy = gridy;
		add( cmbboxCh1, gbcSpinner );
                
                
		/*
		 * Channel 2.
		 */

		gridy++;

		final JLabel lblSegmentInChannelOptional = new JLabel( "Optional second channel:" );
		lblSegmentInChannelOptional.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblSegmentInChannelOptional = new GridBagConstraints();
		gbcLblSegmentInChannelOptional.anchor = GridBagConstraints.EAST;
		gbcLblSegmentInChannelOptional.insets = new Insets( 0, 5, 5, 5 );
		gbcLblSegmentInChannelOptional.gridx = 0;
		gbcLblSegmentInChannelOptional.gridy = gridy;
		add( lblSegmentInChannelOptional, gbcLblSegmentInChannelOptional );

                final List<String> l2 = new ArrayList<String>();
                l2.add("0: None");
                for (int c=1; c<=nbChannels; c++ )
                {
                    l2.add(""+c);
                }
		cmbboxCh2 = new JComboBox<>( new Vector<>( l2 ) );
		cmbboxCh2.setFont( SMALL_FONT );
		final GridBagConstraints gbcSpinnerCh2 = new GridBagConstraints();
		gbcSpinnerCh2.fill = GridBagConstraints.HORIZONTAL;
		gbcSpinnerCh2.gridwidth = 2;
		gbcSpinnerCh2.insets = new Insets( 0, 5, 5, 5 );
		gbcSpinnerCh2.gridx = 1;
		gbcSpinnerCh2.gridy = gridy;
		add( cmbboxCh2, gbcSpinnerCh2 );

		/*
		 * Diameter.
		 */

		gridy++;

		final JLabel lblDiameter = new JLabel( "Cell diameter:" );
		lblDiameter.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblDiameter = new GridBagConstraints();
		gbcLblDiameter.anchor = GridBagConstraints.EAST;
		gbcLblDiameter.insets = new Insets( 0, 5, 5, 5 );
		gbcLblDiameter.gridx = 0;
		gbcLblDiameter.gridy = gridy;
		add( lblDiameter, gbcLblDiameter );

		ftfDiameter = new JFormattedTextField( DIAMETER_FORMAT );
		ftfDiameter.setHorizontalAlignment( SwingConstants.CENTER );
		ftfDiameter.setFont( SMALL_FONT );
		final GridBagConstraints gbcFtfDiameter = new GridBagConstraints();
		gbcFtfDiameter.insets = new Insets( 0, 5, 5, 5 );
		gbcFtfDiameter.fill = GridBagConstraints.HORIZONTAL;
		gbcFtfDiameter.gridx = 1;
		gbcFtfDiameter.gridy = gridy;
		add( ftfDiameter, gbcFtfDiameter );

		final JLabel lblSpaceUnits = new JLabel( model.getSpaceUnits() );
		lblSpaceUnits.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblSpaceUnits = new GridBagConstraints();
		gbcLblSpaceUnits.insets = new Insets( 0, 5, 5, 5 );
		gbcLblSpaceUnits.gridx = 2;
		gbcLblSpaceUnits.gridy = gridy;
		add( lblSpaceUnits, gbcLblSpaceUnits );

		/*
		 * Use GPU.
		 */

		gridy++;

		chckbxUseGPU = new JCheckBox( "Use GPU:" );
		chckbxUseGPU.setHorizontalTextPosition( SwingConstants.LEFT );
		chckbxUseGPU.setFont( SMALL_FONT );
		final GridBagConstraints gbcChckbxUseGPU = new GridBagConstraints();
		gbcChckbxUseGPU.anchor = GridBagConstraints.EAST;
		gbcChckbxUseGPU.insets = new Insets( 0, 0, 0, 5 );
		gbcChckbxUseGPU.gridx = 0;
		gbcChckbxUseGPU.gridy = gridy;
		add( chckbxUseGPU, gbcChckbxUseGPU );

		/*
		 * Simplify contours.
		 */

		chckbxSimplify = new JCheckBox( "Simplify contours:" );
		chckbxSimplify.setHorizontalTextPosition( SwingConstants.LEFT );
		chckbxSimplify.setFont( SMALL_FONT );
		final GridBagConstraints gbcChckbxSimplify = new GridBagConstraints();
		gbcChckbxSimplify.anchor = GridBagConstraints.EAST;
		gbcChckbxSimplify.gridwidth = 2;
		gbcChckbxSimplify.insets = new Insets( 0, 5, 0, 5 );
		gbcChckbxSimplify.gridx = 1;
		gbcChckbxSimplify.gridy = gridy;
		add( chckbxSimplify, gbcChckbxSimplify );

		/*
		 * Preview.
		 */

		gridy = 18;

		final GridBagConstraints gbcBtnPreview = new GridBagConstraints();
		gbcBtnPreview.gridwidth = 3;
		gbcBtnPreview.fill = GridBagConstraints.BOTH;
		gbcBtnPreview.insets = new Insets( 0, 5, 5, 5 );
		gbcBtnPreview.gridx = 0;
		gbcBtnPreview.gridy = gridy;

		final DetectionPreview detectionPreview = DetectionPreview.create()
				.model( model )
				.settings( settings )
				.detectorFactory( getDetectorFactory() )
				.detectionSettingsSupplier( () -> getSettings() )
				.axisLabel( "Area histogram" )
				.get();
		add( detectionPreview.getPanel(), gbcBtnPreview );


		/*
		 * Listeners and specificities.
		 */

		final ItemListener l3 = e -> {
			final boolean isCustom = ( ( PretrainedModel ) cmbboxPretrainedModel.getSelectedItem() ).isCustom();
			tfCustomPath.setVisible( isCustom );
			lblPathToCustomModel.setVisible( isCustom );
			btnBrowseCustomModel.setVisible( isCustom );
		};
		cmbboxPretrainedModel.addItemListener( l3 );
		l3.itemStateChanged( null );

		btnBrowseCellposePath.addActionListener( l -> browseCellposePath() );
		btnBrowseCustomModel.addActionListener( l -> browseCustomModelPath() );
	}

	protected SpotDetectorFactoryBase< ? > getDetectorFactory()
	{
		return new CellposeDetectorFactory<>();
	}

	private void browseCustomModelPath()
	{
		btnBrowseCustomModel.setEnabled( false );
		try
		{
			final File file = FileChooser.chooseFile( this, tfCustomPath.getText(), null,
					"Browse to a " + executableName + " custom model", DialogType.LOAD, SelectionMode.FILES_ONLY );
			if ( file != null )
				tfCustomPath.setText( file.getAbsolutePath() );
		}
		finally
		{
			btnBrowseCustomModel.setEnabled( true );
		}
	}

	private void browseCellposePath()
	{
		btnBrowseCellposePath.setEnabled( false );
		try
		{
			final File file = FileChooser.chooseFile( this, tfCellposeExecutable.getText(), null,
					"Browse to the " + executableName + " Python executable", DialogType.LOAD, SelectionMode.FILES_ONLY );
			if ( file != null )
				tfCellposeExecutable.setText( file.getAbsolutePath() );
		}
		finally
		{
			btnBrowseCellposePath.setEnabled( true );
		}
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		tfCellposeExecutable.setText( ( String ) settings.get( KEY_CELLPOSE_PYTHON_FILEPATH ) );
		tfCustomPath.setText( ( String ) settings.get( KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH ) );
		cmbboxPretrainedModel.setSelectedItem( settings.get( KEY_CELLPOSE_MODEL ) );
                int key_target = (int) settings.get(KEY_TARGET_CHANNEL)-1;
                // to ensure that the default channel to segment parameter is compatible with number of channels in the image
                if ( key_target >= nbChannels )
                {
                    key_target = nbChannels-1;
                }
                if ( key_target < 0 ) 
                { 
                    key_target = 0; 
                }
		cmbboxCh1.setSelectedIndex( key_target );
		cmbboxCh2.setSelectedIndex( ( int ) settings.get( KEY_OPTIONAL_CHANNEL_2 ) );
		ftfDiameter.setValue( settings.get( KEY_CELL_DIAMETER ) );
		chckbxUseGPU.setSelected( ( boolean ) settings.get( KEY_USE_GPU ) );
		chckbxSimplify.setSelected( ( boolean ) settings.get( KEY_SIMPLIFY_CONTOURS ) );
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final HashMap< String, Object > settings = new HashMap<>( 9 );

		settings.put( KEY_CELLPOSE_PYTHON_FILEPATH, tfCellposeExecutable.getText() );
		settings.put( KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH, tfCustomPath.getText() );
		settings.put( KEY_CELLPOSE_MODEL, cmbboxPretrainedModel.getSelectedItem() );
		settings.put( KEY_TARGET_CHANNEL, cmbboxCh1.getSelectedIndex()+1 );
		settings.put( KEY_OPTIONAL_CHANNEL_2, cmbboxCh2.getSelectedIndex() );

		final double diameter = ( ( Number ) ftfDiameter.getValue() ).doubleValue();
		settings.put( KEY_CELL_DIAMETER, diameter );
		settings.put( KEY_SIMPLIFY_CONTOURS, chckbxSimplify.isSelected() );
		settings.put( KEY_USE_GPU, chckbxUseGPU.isSelected() );

		settings.put( KEY_LOGGER, logger );

		return settings;
	}

	@Override
	public void clean()
	{}

	private class MyMouseAdapter extends MouseAdapter
	{

		private final JLabel lblUrl;

		private final String docURL;

		private final String text;

		public MyMouseAdapter( final JLabel lblUrl, final String docURL, final String text )
		{
			this.lblUrl = lblUrl;
			this.docURL = docURL;
			this.text = text;
		}

		@Override
		public void mouseClicked( final java.awt.event.MouseEvent e )
		{
			try
			{
				Desktop.getDesktop().browse( new URI( docURL ) );
			}
			catch ( URISyntaxException | IOException ex )
			{
				ex.printStackTrace();
			}
		}

		@Override
		public void mouseExited( final java.awt.event.MouseEvent e )
		{
			lblUrl.setText( text );
		}

		@Override
		public void mouseEntered( final java.awt.event.MouseEvent e )
		{
			lblUrl.setText( "<html><a href=''>" + docURL + "</a></html>" );
		}
	}
}
