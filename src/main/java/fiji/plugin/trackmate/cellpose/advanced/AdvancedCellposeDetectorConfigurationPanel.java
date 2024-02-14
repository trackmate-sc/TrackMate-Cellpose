package fiji.plugin.trackmate.cellpose.advanced;

import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorFactory.KEY_CELL_PROB_THRESHOLD;
import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorFactory.KEY_FLOW_THRESHOLD;
import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorFactory.KEY_RESAMPLE;
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.cellpose.AbstractCellposeSettings;
import fiji.plugin.trackmate.cellpose.CellposeDetectorConfigurationPanel;
import fiji.plugin.trackmate.cellpose.CellposeDetectorFactory;
import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorFactory.KEY_DO2DZ;
import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorFactory.KEY_IOU_THRESHOLD;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;
import fiji.plugin.trackmate.gui.displaysettings.SliderPanelDouble;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements;
import java.awt.event.ItemListener;
import javax.swing.JCheckBox;
import javax.swing.SwingConstants;

public class AdvancedCellposeDetectorConfigurationPanel extends CellposeDetectorConfigurationPanel
{

	private static final long serialVersionUID = 1L;

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

	private final StyleElements.BoundedDoubleElement cellProbThresholdEl = new StyleElements.BoundedDoubleElement("Cell prob", -6.0, 6.0)
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

        private final StyleElements.BoundedDoubleElement iouThresholdEl = new StyleElements.BoundedDoubleElement( "IOU threshold", 0.0, 1.0 )
	{

		private double iouThreshold = 0.25;

		@Override
		public double get()
		{
			return iouThreshold;
		}

		@Override
		public void set( final double v )
		{
			iouThreshold = v;
		}
	};
        
        protected final JCheckBox chckbxDo2DZ;
        
	protected final JCheckBox chckbxResample;

        
	public AdvancedCellposeDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		super( settings, model );

		/*
		 * Add flow threshold.
		 */

		int gridy = 12;

		final JLabel lblFlowThreshold = new JLabel( "Flow threshold:" );
		lblFlowThreshold.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblFlowThreshold = new GridBagConstraints();
		gbcLblFlowThreshold.anchor = GridBagConstraints.EAST;
		gbcLblFlowThreshold.insets = new Insets( 0, 5, 5, 5 );
		gbcLblFlowThreshold.gridx = 0;
		gbcLblFlowThreshold.gridy = gridy;
		add( lblFlowThreshold, gbcLblFlowThreshold );

		final SliderPanelDouble sliderPanelFlowThreshold = StyleElements.linkedSliderPanel( flowThresholdEl, 3, 0.1 );
		setFont( sliderPanelFlowThreshold, SMALL_FONT );
		final GridBagConstraints gbcFlowThresholdSlider = new GridBagConstraints();
		gbcFlowThresholdSlider.anchor = GridBagConstraints.EAST;
		gbcFlowThresholdSlider.insets = new Insets( 0, 5, 5, 5 );
		gbcFlowThresholdSlider.fill = GridBagConstraints.HORIZONTAL;
		gbcFlowThresholdSlider.gridx = 1;
		gbcFlowThresholdSlider.gridwidth = 2;
		gbcFlowThresholdSlider.gridy = gridy;
		add( sliderPanelFlowThreshold, gbcFlowThresholdSlider );

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
		add( lblCellProb, gbcLblCellProb );

		final SliderPanelDouble sliderPanelCellProbThreshold = StyleElements.linkedSliderPanel( cellProbThresholdEl, 3, 0.4 );
		setFont( sliderPanelCellProbThreshold, SMALL_FONT );
		final GridBagConstraints gbcCellProbThresholdSlider = new GridBagConstraints();
		gbcCellProbThresholdSlider.anchor = GridBagConstraints.EAST;
		gbcCellProbThresholdSlider.insets = new Insets( 0, 5, 5, 5 );
		gbcCellProbThresholdSlider.fill = GridBagConstraints.HORIZONTAL;
		gbcCellProbThresholdSlider.gridx = 1;
		gbcCellProbThresholdSlider.gridwidth = 2;
		gbcCellProbThresholdSlider.gridy = gridy;
		add( sliderPanelCellProbThreshold, gbcCellProbThresholdSlider );
                
                 /*
		 * 3D mode option.
		 */
                boolean is3D = false;
		if ( null != settings.imp && !DetectionUtils.is2D( settings.imp ) )
			is3D = true;
		gridy++;
		chckbxDo2DZ = new JCheckBox( "Do 2D + Z stitch segmentation:" );
		chckbxDo2DZ.setHorizontalTextPosition( SwingConstants.LEFT );
		chckbxDo2DZ.setFont( SMALL_FONT );
		final GridBagConstraints gbcChckbxDo2DZ = new GridBagConstraints();
		gbcChckbxDo2DZ.anchor = GridBagConstraints.EAST;
		gbcChckbxDo2DZ.insets = new Insets( 0, 0, 0, 5 );
		gbcChckbxDo2DZ.gridx = 0;
		gbcChckbxDo2DZ.gridy = gridy;
		add( chckbxDo2DZ, gbcChckbxDo2DZ );
                chckbxDo2DZ.setVisible(is3D);
                
                /*
		 * Add iou threshold.
		 */
                gridy ++;
		final JLabel lblIouThreshold = new JLabel( "IOU threshold:" );
		lblIouThreshold.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblIouThreshold = new GridBagConstraints();
		gbcLblIouThreshold.anchor = GridBagConstraints.EAST;
		gbcLblIouThreshold.insets = new Insets( 0, 5, 5, 5 );
		gbcLblIouThreshold.gridx = 0;
		gbcLblIouThreshold.gridy = gridy;
		add( lblIouThreshold, gbcLblIouThreshold );
                lblIouThreshold.setVisible(is3D);

		final SliderPanelDouble sliderPanelIouThreshold = StyleElements.linkedSliderPanel( iouThresholdEl, 3, 0.1 );
		setFont( sliderPanelIouThreshold, SMALL_FONT );
		final GridBagConstraints gbcIouThresholdSlider = new GridBagConstraints();
		gbcIouThresholdSlider.anchor = GridBagConstraints.EAST;
		gbcIouThresholdSlider.insets = new Insets( 0, 5, 5, 5 );
		gbcIouThresholdSlider.fill = GridBagConstraints.HORIZONTAL;
		gbcIouThresholdSlider.gridx = 1;
		gbcIouThresholdSlider.gridwidth = 2;
		gbcIouThresholdSlider.gridy = gridy;
		add( sliderPanelIouThreshold, gbcIouThresholdSlider );
                sliderPanelIouThreshold.setVisible(is3D);
                
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
		gbcChckbxResample.gridx = 0;
		gbcChckbxResample.gridy = gridy;
		add( chckbxResample, gbcChckbxResample );
                
                /*
		 * Listeners and specificities.
		 */
                if (is3D )
                {
                    final ItemListener lmode = e -> {
			final boolean do2dz =  ( Boolean ) chckbxDo2DZ.isSelected() ;
			lblIouThreshold.setVisible( do2dz );
			sliderPanelIouThreshold.setVisible( do2dz ); 
                        sliderPanelFlowThreshold.setEnabled( do2dz ); // flow threshold is not used in 3D mode
                    };
                    chckbxDo2DZ.addItemListener( lmode );
                    lmode.itemStateChanged( null );
                }
                
                
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		super.setSettings( settings );
		flowThresholdEl.set( ( double ) settings.get( KEY_FLOW_THRESHOLD ) );
		flowThresholdEl.update();
		cellProbThresholdEl.set( ( double ) settings.get( KEY_CELL_PROB_THRESHOLD ) );
		cellProbThresholdEl.update();
                chckbxDo2DZ.setSelected( ( boolean ) settings.get( KEY_DO2DZ ) );
                iouThresholdEl.set( ( double ) settings.get( KEY_IOU_THRESHOLD ) );
		iouThresholdEl.update();
                chckbxResample.setSelected( ( boolean ) settings.get( KEY_RESAMPLE ) );
	}

	@Override
	public Map< String, Object > getSettings()
	{
            	final Map< String, Object > settings = super.getSettings();
		settings.put( KEY_FLOW_THRESHOLD, flowThresholdEl.get() );
		settings.put( KEY_CELL_PROB_THRESHOLD, cellProbThresholdEl.get() );
                settings.put( KEY_DO2DZ, chckbxDo2DZ.isSelected() );
                settings.put( KEY_IOU_THRESHOLD, iouThresholdEl.get() );
                settings.put( KEY_RESAMPLE, chckbxResample.isSelected() );
		return settings;
	}
        
        @Override
        protected SpotDetectorFactoryBase< ? > getDetectorFactory()
	{
		return new AdvancedCellposeDetectorFactory<>();
	}

	public static final void setFont( final JComponent panel, final Font font )
	{
		for ( final Component c : panel.getComponents() )
			c.setFont( font );
	}
}
