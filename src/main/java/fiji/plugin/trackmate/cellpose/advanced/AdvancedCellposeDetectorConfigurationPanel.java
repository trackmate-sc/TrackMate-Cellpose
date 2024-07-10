package fiji.plugin.trackmate.cellpose.advanced;

import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorFactory.DOC_ADV_CELLPOSE_URL;
import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorFactory.KEY_CELL_MIN_SIZE;
import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorFactory.KEY_CELL_PROB_THRESHOLD;
import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorFactory.KEY_FLOW_THRESHOLD;
import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorFactory.KEY_RESAMPLE;
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.cellpose.CellposeDetectorConfigurationPanel;
import fiji.plugin.trackmate.cellpose.CellposeSettings.PretrainedModelCellpose;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.displaysettings.SliderPanelDouble;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements;

public class AdvancedCellposeDetectorConfigurationPanel extends CellposeDetectorConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	private static final String TITLE = AdvancedCellposeDetectorFactory.NAME;

	private final StyleElements.BoundedDoubleElement flowThresholdEl = new StyleElements.BoundedDoubleElement( "Flow threshold", 0.0, 3.0 )
	{

		private double flowThreshold = 0.;

		@Override
		public double get()
		{
			return flowThreshold;
		}

		@Override
		public void set( final double v )
		{
			flowThreshold = v;
		}
	};

	private final StyleElements.BoundedDoubleElement cellProbThresholdEl = new StyleElements.BoundedDoubleElement( "Cell prob", -6.0, 6.0 )
	{

		private double cellProbThreshold = 0.;

		@Override
		public double get()
		{
			return cellProbThreshold;
		}

		@Override
		public void set( final double v )
		{
			cellProbThreshold = v;
		}
	};

	private static final NumberFormat CELLMINSIZE_FORMAT = new DecimalFormat( "#.#" );

	protected final JFormattedTextField ftfCellMinSize;

	protected final JCheckBox chckbxResample;

	public AdvancedCellposeDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		super( settings, model, TITLE, ICON, DOC_ADV_CELLPOSE_URL, "cellpose", PretrainedModelCellpose.values() );

		/** Show 3D parameter only if image has z slices */
		boolean is3D = false;
		if ( null != settings.imp && !DetectionUtils.is2D( settings.imp ) )
			is3D = true;

		/*
		 * Add flow threshold.
		 */

		int gridy = 13;

		final JLabel lblFlowThreshold = new JLabel( "Flow threshold:" );
		lblFlowThreshold.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblFlowThreshold = new GridBagConstraints();
		gbcLblFlowThreshold.anchor = GridBagConstraints.EAST;
		gbcLblFlowThreshold.insets = new Insets( 0, 5, 5, 5 );
		gbcLblFlowThreshold.gridx = 0;
		gbcLblFlowThreshold.gridy = gridy;
		mainPanel.add( lblFlowThreshold, gbcLblFlowThreshold );

		final SliderPanelDouble sliderPanelFlowThreshold = StyleElements.linkedSliderPanel( flowThresholdEl, 3, 0.1 );
		GuiUtils.setFont( sliderPanelFlowThreshold, SMALL_FONT );
		final GridBagConstraints gbcFlowThresholdSlider = new GridBagConstraints();
		gbcFlowThresholdSlider.anchor = GridBagConstraints.EAST;
		gbcFlowThresholdSlider.insets = new Insets( 0, 5, 5, 5 );
		gbcFlowThresholdSlider.fill = GridBagConstraints.HORIZONTAL;
		gbcFlowThresholdSlider.gridx = 1;
		gbcFlowThresholdSlider.gridwidth = 2;
		gbcFlowThresholdSlider.gridy = gridy;
		mainPanel.add( sliderPanelFlowThreshold, gbcFlowThresholdSlider );

		/*
		 * Add cell probability threshold.
		 */

		gridy++;

		final JLabel lblCellProb = new JLabel( "Cell probability threshold:" );
		lblCellProb.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblCellProb = new GridBagConstraints();
		gbcLblCellProb.anchor = GridBagConstraints.EAST;
		gbcLblCellProb.insets = new Insets( 0, 5, 5, 5 );
		gbcLblCellProb.gridx = 0;
		gbcLblCellProb.gridy = gridy;
		mainPanel.add( lblCellProb, gbcLblCellProb );

		final SliderPanelDouble sliderPanelCellProbThreshold = StyleElements.linkedSliderPanel( cellProbThresholdEl, 3, 0.4 );
		GuiUtils.setFont( sliderPanelCellProbThreshold, SMALL_FONT );
		final GridBagConstraints gbcCellProbThresholdSlider = new GridBagConstraints();
		gbcCellProbThresholdSlider.anchor = GridBagConstraints.EAST;
		gbcCellProbThresholdSlider.insets = new Insets( 0, 5, 5, 5 );
		gbcCellProbThresholdSlider.fill = GridBagConstraints.HORIZONTAL;
		gbcCellProbThresholdSlider.gridx = 1;
		gbcCellProbThresholdSlider.gridwidth = 2;
		gbcCellProbThresholdSlider.gridy = gridy;
		mainPanel.add( sliderPanelCellProbThreshold, gbcCellProbThresholdSlider );

		/*
		 * Cell minimum size
		 */
		gridy++;

		final JLabel lblCellMinSize = new JLabel( "Remove cells below:" );
		lblCellMinSize.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblCellMinSize = new GridBagConstraints();
		gbcLblCellMinSize.anchor = GridBagConstraints.EAST;
		gbcLblCellMinSize.insets = new Insets( 0, 5, 5, 5 );
		gbcLblCellMinSize.gridx = 0;
		gbcLblCellMinSize.gridy = gridy;
		mainPanel.add( lblCellMinSize, gbcLblCellMinSize );
		lblCellMinSize.setVisible( is3D );

		ftfCellMinSize = new JFormattedTextField( CELLMINSIZE_FORMAT );
		ftfCellMinSize.setHorizontalAlignment( SwingConstants.CENTER );
		ftfCellMinSize.setFont( SMALL_FONT );
		final GridBagConstraints gbcFtfCellMinSize = new GridBagConstraints();
		gbcFtfCellMinSize.insets = new Insets( 0, 5, 5, 5 );
		gbcFtfCellMinSize.fill = GridBagConstraints.HORIZONTAL;
		gbcFtfCellMinSize.gridx = 1;
		gbcFtfCellMinSize.gridy = gridy;
		mainPanel.add( ftfCellMinSize, gbcFtfCellMinSize );
		ftfCellMinSize.setVisible( is3D );

		final JLabel lblSpaceUnits = new JLabel( "pixels" );
		lblSpaceUnits.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblSpaceUnits = new GridBagConstraints();
		gbcLblSpaceUnits.insets = new Insets( 0, 5, 5, 5 );
		gbcLblSpaceUnits.gridx = 2;
		gbcLblSpaceUnits.gridy = gridy;
		lblSpaceUnits.setVisible( is3D );
		mainPanel.add( lblSpaceUnits, gbcLblSpaceUnits );

		/*
		 * 3D mode option.
		 */

		gridy++;
//		chckbxDo2DZ = new JCheckBox( "Do 2D + Z stitch segmentation:" );
//		chckbxDo2DZ.setHorizontalTextPosition( SwingConstants.LEFT );
//		chckbxDo2DZ.setFont( SMALL_FONT );
//		final GridBagConstraints gbcChckbxDo2DZ = new GridBagConstraints();
//		gbcChckbxDo2DZ.anchor = GridBagConstraints.EAST;
//		gbcChckbxDo2DZ.insets = new Insets( 0, 0, 0, 5 );
//		gbcChckbxDo2DZ.gridx = 0;
//		gbcChckbxDo2DZ.gridy = gridy;
//		mainPanel.add( chckbxDo2DZ, gbcChckbxDo2DZ );
//		chckbxDo2DZ.setVisible( is3D );

		/*
		 * Add resample option.
		 */

		gridy++;
		chckbxResample = new JCheckBox( "Resample:" );
		chckbxResample.setHorizontalTextPosition( SwingConstants.LEFT );
		chckbxResample.setFont( SMALL_FONT );
		final GridBagConstraints gbcChckbxResample = new GridBagConstraints();
		gbcChckbxResample.anchor = GridBagConstraints.EAST;
		gbcChckbxResample.insets = new Insets( 0, 0, 0, 5 );
		gbcChckbxResample.gridx = 2;
		gbcChckbxResample.gridy = gridy;
		mainPanel.add( chckbxResample, gbcChckbxResample );

		/*
		 * Add iou threshold.
		 */
		
		gridy++;

//		final JLabel lblIouThreshold = new JLabel( "IoU threshold:" );
//		lblIouThreshold.setFont( SMALL_FONT );
//		final GridBagConstraints gbcLblIouThreshold = new GridBagConstraints();
//		gbcLblIouThreshold.anchor = GridBagConstraints.EAST;
//		gbcLblIouThreshold.insets = new Insets( 5, 5, 5, 5 );
//		gbcLblIouThreshold.gridx = 0;
//		gbcLblIouThreshold.gridy = gridy;
//		mainPanel.add( lblIouThreshold, gbcLblIouThreshold );
//		lblIouThreshold.setVisible( is3D );

//		final SliderPanelDouble sliderPanelIouThreshold = StyleElements.linkedSliderPanel( iouThresholdEl, 3, 0.1 );
//		setFont( sliderPanelIouThreshold, SMALL_FONT );
//		final GridBagConstraints gbcIouThresholdSlider = new GridBagConstraints();
//		gbcIouThresholdSlider.anchor = GridBagConstraints.EAST;
//		gbcIouThresholdSlider.insets = new Insets( 5, 5, 5, 5 );
//		gbcIouThresholdSlider.fill = GridBagConstraints.HORIZONTAL;
//		gbcIouThresholdSlider.gridx = 1;
//		gbcIouThresholdSlider.gridwidth = 2;
//		gbcIouThresholdSlider.gridy = gridy;
//		mainPanel.add( sliderPanelIouThreshold, gbcIouThresholdSlider );
//		sliderPanelIouThreshold.setVisible( is3D );

		/*
		 * Listeners and specificities.
		 */

//		if ( is3D )
//		{
//			final ItemListener lmode = e -> {
//				final boolean do2dz = chckbxDo2DZ.isSelected();
//				lblIouThreshold.setVisible( do2dz );
//				sliderPanelIouThreshold.setVisible( do2dz );
//				sliderPanelFlowThreshold.setEnabled( do2dz );
//				// flow threshold is not used in 3D mode
//			};
//			chckbxDo2DZ.addItemListener( lmode );
//			lmode.itemStateChanged( null );
//		}
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		super.setSettings( settings );
		flowThresholdEl.set( ( double ) settings.get( KEY_FLOW_THRESHOLD ) );
		flowThresholdEl.update();
		cellProbThresholdEl.set( ( double ) settings.get( KEY_CELL_PROB_THRESHOLD ) );
		cellProbThresholdEl.update();
		ftfCellMinSize.setValue( ( double ) settings.get( KEY_CELL_MIN_SIZE ) );
//		chckbxDo2DZ.setSelected( ( boolean ) settings.get( KEY_DO2DZ ) );
//		iouThresholdEl.set( ( double ) settings.get( KEY_IOU_THRESHOLD ) );
//		iouThresholdEl.update();
		chckbxResample.setSelected( ( boolean ) settings.get( KEY_RESAMPLE ) );
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final Map< String, Object > settings = super.getSettings();
		settings.put( KEY_FLOW_THRESHOLD, flowThresholdEl.get() );
		settings.put( KEY_CELL_PROB_THRESHOLD, cellProbThresholdEl.get() );
		final double cellMinSize = ( ( Number ) ftfCellMinSize.getValue() ).doubleValue();
		settings.put( KEY_CELL_MIN_SIZE, cellMinSize );
//		settings.put( KEY_DO2DZ, chckbxDo2DZ.isSelected() );
//		settings.put( KEY_IOU_THRESHOLD, iouThresholdEl.get() );
		settings.put( KEY_RESAMPLE, chckbxResample.isSelected() );
		return settings;
	}

	@Override
	protected SpotDetectorFactoryBase< ? > getDetectorFactory()
	{
		return new AdvancedCellposeDetectorFactory<>();
	}
}
