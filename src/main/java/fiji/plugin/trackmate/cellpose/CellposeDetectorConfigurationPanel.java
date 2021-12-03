/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2021 The Institut Pasteur.
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

import static fiji.plugin.trackmate.cellpose.CellposeDetectorFactory.KEY_CELLPOSE_MODEL;
import static fiji.plugin.trackmate.cellpose.CellposeDetectorFactory.KEY_CELLPOSE_PYTHON_FILEPATH;
import static fiji.plugin.trackmate.cellpose.CellposeDetectorFactory.KEY_CELL_DIAMETER;
import static fiji.plugin.trackmate.cellpose.CellposeDetectorFactory.KEY_LOGGER;
import static fiji.plugin.trackmate.cellpose.CellposeDetectorFactory.KEY_OPTIONAL_CHANNEL_2;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.ThresholdDetectorFactory.KEY_SIMPLIFY_CONTOURS;
import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;
import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;
import static fiji.plugin.trackmate.gui.Icons.PREVIEW_ICON;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.scijava.prefs.PrefService;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.cellpose.CellposeSettings.PretrainedModel;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.util.FileChooser;
import fiji.plugin.trackmate.util.FileChooser.DialogType;
import fiji.plugin.trackmate.util.JLabelLogger;
import fiji.plugin.trackmate.util.TMUtils;

public class CellposeDetectorConfigurationPanel extends ConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	private static final String TITLE = CellposeDetectorFactory.NAME;

	private static final ImageIcon ICON = CellposeUtils.logo64();

	private static final NumberFormat DIAMETER_FORMAT = new DecimalFormat( "#.#" );

	private final JButton btnBrowse;

	private final JTextField tfCellposeExecutable;

	private final PrefService prefService;

	private final JComboBox< PretrainedModel > cmbboxPretrainedModel;

	private final JComboBox< String > cmbboxCh1;

	private final JComboBox< String > cmbboxCh2;

	private final JFormattedTextField ftfDiameter;

	private final JCheckBox chckbxSimplify;

	private final Logger logger;

	public CellposeDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		this.prefService = TMUtils.getContext().getService( PrefService.class );
		this.logger = model.getLogger();

		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 144, 0, 32 };
		gridBagLayout.columnWeights = new double[] { 0.0, 1.0, 0.0 };
		setLayout( gridBagLayout );

		final JLabel lblSettingsForDetector = new JLabel( "Settings for detector:" );
		lblSettingsForDetector.setFont( FONT );
		final GridBagConstraints gbc_lblSettingsForDetector = new GridBagConstraints();
		gbc_lblSettingsForDetector.gridwidth = 3;
		gbc_lblSettingsForDetector.insets = new Insets( 0, 5, 0, 5 );
		gbc_lblSettingsForDetector.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblSettingsForDetector.gridx = 0;
		gbc_lblSettingsForDetector.gridy = 0;
		add( lblSettingsForDetector, gbc_lblSettingsForDetector );

		final JLabel lblDetector = new JLabel( TITLE, ICON, JLabel.RIGHT );
		lblDetector.setFont( BIG_FONT );
		lblDetector.setHorizontalAlignment( SwingConstants.CENTER );
		final GridBagConstraints gbc_lblDetector = new GridBagConstraints();
		gbc_lblDetector.gridwidth = 3;
		gbc_lblDetector.insets = new Insets( 0, 5, 0, 5 );
		gbc_lblDetector.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblDetector.gridx = 0;
		gbc_lblDetector.gridy = 1;
		add( lblDetector, gbc_lblDetector );

		/*
		 * Help text.
		 */
		final JLabel lblHelptext = new JLabel( CellposeDetectorFactory.INFO_TEXT
				.replace( "<br>", "" )
				.replace( "<p>", "<p align=\"justify\">" )
				.replace( "<html>", "<html><p align=\"justify\">" ) );
		lblHelptext.setFont( FONT.deriveFont( Font.ITALIC ) );
		final GridBagConstraints gbc_lblHelptext = new GridBagConstraints();
		gbc_lblHelptext.anchor = GridBagConstraints.NORTH;
		gbc_lblHelptext.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblHelptext.gridwidth = 3;
		gbc_lblHelptext.insets = new Insets( 5, 10, 5, 10 );
		gbc_lblHelptext.gridx = 0;
		gbc_lblHelptext.gridy = 2;
		add( lblHelptext, gbc_lblHelptext );

		/*
		 * Path to Python.
		 */

		final JLabel lblCusstomModelFile = new JLabel( "Path to Cellpose Python executable:" );
		lblCusstomModelFile.setFont( FONT );
		final GridBagConstraints gbcLblCusstomModelFile = new GridBagConstraints();
		gbcLblCusstomModelFile.anchor = GridBagConstraints.SOUTHWEST;
		gbcLblCusstomModelFile.insets = new Insets( 0, 5, 0, 5 );
		gbcLblCusstomModelFile.gridx = 0;
		gbcLblCusstomModelFile.gridy = 3;
		add( lblCusstomModelFile, gbcLblCusstomModelFile );

		btnBrowse = new JButton( "Browse" );
		btnBrowse.setFont( FONT );
		final GridBagConstraints gbcBtnBrowse = new GridBagConstraints();
		gbcBtnBrowse.insets = new Insets( 0, 5, 0, 5 );
		gbcBtnBrowse.anchor = GridBagConstraints.SOUTHEAST;
		gbcBtnBrowse.gridwidth = 2;
		gbcBtnBrowse.gridx = 1;
		gbcBtnBrowse.gridy = 3;
		add( btnBrowse, gbcBtnBrowse );

		btnBrowse.addActionListener( l -> browse() );

		tfCellposeExecutable = new JTextField( "" );
		tfCellposeExecutable.setFont( SMALL_FONT );
		final GridBagConstraints gbcTfCellpose = new GridBagConstraints();
		gbcTfCellpose.gridwidth = 3;
		gbcTfCellpose.insets = new Insets( 0, 5, 0, 5 );
		gbcTfCellpose.fill = GridBagConstraints.BOTH;
		gbcTfCellpose.gridx = 0;
		gbcTfCellpose.gridy = 4;
		add( tfCellposeExecutable, gbcTfCellpose );
		tfCellposeExecutable.setColumns( 10 );

		/*
		 * Pretrained model.
		 */

		final JLabel lblPretrainedModel = new JLabel( "Pretrained model:" );
		lblPretrainedModel.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblPretrainedModel = new GridBagConstraints();
		gbcLblPretrainedModel.anchor = GridBagConstraints.EAST;
		gbcLblPretrainedModel.insets = new Insets( 0, 5, 0, 5 );
		gbcLblPretrainedModel.gridx = 0;
		gbcLblPretrainedModel.gridy = 6;
		add( lblPretrainedModel, gbcLblPretrainedModel );

		cmbboxPretrainedModel = new JComboBox<>( new Vector<>( Arrays.asList( PretrainedModel.values() ) ) );
		cmbboxPretrainedModel.setFont( SMALL_FONT );
		final GridBagConstraints gbcCmbboxPretrainedModel = new GridBagConstraints();
		gbcCmbboxPretrainedModel.gridwidth = 2;
		gbcCmbboxPretrainedModel.insets = new Insets( 0, 5, 0, 5 );
		gbcCmbboxPretrainedModel.fill = GridBagConstraints.HORIZONTAL;
		gbcCmbboxPretrainedModel.gridx = 1;
		gbcCmbboxPretrainedModel.gridy = 6;
		add( cmbboxPretrainedModel, gbcCmbboxPretrainedModel );

		/*
		 * Channel 1
		 */

		final JLabel lblSegmentInChannel = new JLabel( "Channel to segment:" );
		lblSegmentInChannel.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblSegmentInChannel = new GridBagConstraints();
		gbcLblSegmentInChannel.anchor = GridBagConstraints.EAST;
		gbcLblSegmentInChannel.insets = new Insets( 0, 5, 0, 5 );
		gbcLblSegmentInChannel.gridx = 0;
		gbcLblSegmentInChannel.gridy = 7;
		add( lblSegmentInChannel, gbcLblSegmentInChannel );

		final List< String > l1 = Arrays.asList(
				"0: grayscale",
				"1: red",
				"2: green",
				"3: blue" );
		cmbboxCh1 = new JComboBox<>( new Vector<>( l1 ) );
		cmbboxCh1.setFont( SMALL_FONT );
		final GridBagConstraints gbcSpinner = new GridBagConstraints();
		gbcSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbcSpinner.gridwidth = 2;
		gbcSpinner.insets = new Insets( 0, 5, 0, 5 );
		gbcSpinner.gridx = 1;
		gbcSpinner.gridy = 7;
		add( cmbboxCh1, gbcSpinner );

		/*
		 * Channel 2.
		 */

		final JLabel lblSegmentInChannelOptional = new JLabel( "Optional second channel:" );
		lblSegmentInChannelOptional.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblSegmentInChannelOptional = new GridBagConstraints();
		gbcLblSegmentInChannelOptional.anchor = GridBagConstraints.EAST;
		gbcLblSegmentInChannelOptional.insets = new Insets( 0, 5, 0, 5 );
		gbcLblSegmentInChannelOptional.gridx = 0;
		gbcLblSegmentInChannelOptional.gridy = 8;
		add( lblSegmentInChannelOptional, gbcLblSegmentInChannelOptional );

		final List< String > l2 = Arrays.asList(
				"0: none",
				"1: red",
				"2: green",
				"3: blue" );
		cmbboxCh2 = new JComboBox<>( new Vector<>( l2 ) );
		cmbboxCh2.setFont( SMALL_FONT );
		final GridBagConstraints gbcSpinnerCh2 = new GridBagConstraints();
		gbcSpinnerCh2.fill = GridBagConstraints.HORIZONTAL;
		gbcSpinnerCh2.gridwidth = 2;
		gbcSpinnerCh2.insets = new Insets( 0, 5, 0, 5 );
		gbcSpinnerCh2.gridx = 1;
		gbcSpinnerCh2.gridy = 8;
		add( cmbboxCh2, gbcSpinnerCh2 );

		/*
		 * Diameter.
		 */

		final JLabel lblDiameter = new JLabel( "Cell diameter:" );
		lblDiameter.setFont( SMALL_FONT );
		final GridBagConstraints gbc_lblDiameter = new GridBagConstraints();
		gbc_lblDiameter.anchor = GridBagConstraints.EAST;
		gbc_lblDiameter.insets = new Insets( 0, 5, 0, 5 );
		gbc_lblDiameter.gridx = 0;
		gbc_lblDiameter.gridy = 9;
		add( lblDiameter, gbc_lblDiameter );

		ftfDiameter = new JFormattedTextField( DIAMETER_FORMAT );
		ftfDiameter.setHorizontalAlignment( SwingConstants.CENTER );
		ftfDiameter.setFont( SMALL_FONT );
		final GridBagConstraints gbcFtfDiameter = new GridBagConstraints();
		gbcFtfDiameter.insets = new Insets( 0, 5, 0, 5 );
		gbcFtfDiameter.fill = GridBagConstraints.HORIZONTAL;
		gbcFtfDiameter.gridx = 1;
		gbcFtfDiameter.gridy = 9;
		add( ftfDiameter, gbcFtfDiameter );

		final JLabel lblSpaceUnits = new JLabel( model.getSpaceUnits() );
		lblSpaceUnits.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblSpaceUnits = new GridBagConstraints();
		gbcLblSpaceUnits.insets = new Insets( 0, 5, 0, 5 );
		gbcLblSpaceUnits.gridx = 2;
		gbcLblSpaceUnits.gridy = 9;
		add( lblSpaceUnits, gbcLblSpaceUnits );

		chckbxSimplify = new JCheckBox( "Simplify contours:" );
		chckbxSimplify.setHorizontalTextPosition( SwingConstants.LEFT );
		chckbxSimplify.setFont( SMALL_FONT );
		final GridBagConstraints gbcChckbxSimplify = new GridBagConstraints();
		gbcChckbxSimplify.anchor = GridBagConstraints.EAST;
		gbcChckbxSimplify.gridwidth = 2;
		gbcChckbxSimplify.insets = new Insets( 0, 5, 0, 5 );
		gbcChckbxSimplify.gridx = 0;
		gbcChckbxSimplify.gridy = 10;
		add( chckbxSimplify, gbcChckbxSimplify );

		final JLabelLogger labelLogger = new JLabelLogger();
		final GridBagConstraints gbc_labelLogger = new GridBagConstraints();
		gbc_labelLogger.gridwidth = 3;
		gbc_labelLogger.gridx = 0;
		gbc_labelLogger.gridy = 14;
		add( labelLogger, gbc_labelLogger );
		final Logger localLogger = labelLogger.getLogger();

		/*
		 * Preview.
		 */

		final JButton btnPreview = new JButton( "Preview", PREVIEW_ICON );
		btnPreview.setFont( FONT );
		final GridBagConstraints gbc_btnPreview = new GridBagConstraints();
		gbc_btnPreview.gridwidth = 2;
		gbc_btnPreview.anchor = GridBagConstraints.SOUTHEAST;
		gbc_btnPreview.insets = new Insets( 0, 5, 0, 5 );
		gbc_btnPreview.gridx = 1;
		gbc_btnPreview.gridy = 13;
		add( btnPreview, gbc_btnPreview );

		/*
		 * Listeners and specificities.
		 */

		btnPreview.addActionListener( e -> DetectionUtils.preview(
				model,
				settings,
				new CellposeDetectorFactory<>(),
				getSettings(),
				settings.imp.getFrame() - 1,
				localLogger,
				b -> btnPreview.setEnabled( b ) ) );

		final PropertyChangeListener l = e -> prefService.put(
				CellposeDetectorConfigurationPanel.class,
				KEY_CELLPOSE_PYTHON_FILEPATH, tfCellposeExecutable.getText() );
		tfCellposeExecutable.addPropertyChangeListener( "value", l );
	}

	protected void browse()
	{
		btnBrowse.setEnabled( false );
		try
		{
			final File file = FileChooser.chooseFile( this, tfCellposeExecutable.getText(), null, "Browse to the Cellpose Python executable", DialogType.LOAD );
			if ( file != null )
			{
				tfCellposeExecutable.setText( file.getAbsolutePath() );
				prefService.put( CellposeDetectorConfigurationPanel.class,
						KEY_CELLPOSE_PYTHON_FILEPATH, file.getAbsolutePath() );
			}
		}
		finally
		{
			btnBrowse.setEnabled( true );
		}
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		tfCellposeExecutable.setText( ( String ) settings.get( KEY_CELLPOSE_PYTHON_FILEPATH ) );
		cmbboxPretrainedModel.setSelectedItem( settings.get( KEY_CELLPOSE_MODEL ) );
		cmbboxCh1.setSelectedIndex( ( int ) settings.get( KEY_TARGET_CHANNEL ) );
		cmbboxCh2.setSelectedIndex( ( int ) settings.get( KEY_OPTIONAL_CHANNEL_2 ) );
		ftfDiameter.setValue( settings.get( KEY_CELL_DIAMETER ) );
		chckbxSimplify.setSelected( ( boolean ) settings.get( KEY_SIMPLIFY_CONTOURS ) );
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final HashMap< String, Object > settings = new HashMap<>( 6 );

		settings.put( KEY_CELLPOSE_PYTHON_FILEPATH, tfCellposeExecutable.getText() );
		settings.put( KEY_CELLPOSE_MODEL, cmbboxPretrainedModel.getSelectedItem() );

		settings.put( KEY_TARGET_CHANNEL, cmbboxCh1.getSelectedIndex() );
		settings.put( KEY_OPTIONAL_CHANNEL_2, cmbboxCh2.getSelectedIndex() );

		final double diameter = ( ( Number ) ftfDiameter.getValue() ).doubleValue();
		settings.put( KEY_CELL_DIAMETER, diameter );
		settings.put( KEY_SIMPLIFY_CONTOURS, chckbxSimplify.isSelected() );

		settings.put( KEY_LOGGER, logger );

		return settings;
	}

	@Override
	public void clean()
	{}
}
