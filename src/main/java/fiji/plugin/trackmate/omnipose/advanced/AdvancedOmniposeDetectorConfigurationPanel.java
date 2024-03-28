package fiji.plugin.trackmate.omnipose.advanced;

import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorFactory.KEY_CELL_PROB_THRESHOLD;
import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorFactory.KEY_FLOW_THRESHOLD;
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.Map;

import javax.swing.JLabel;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.displaysettings.SliderPanelDouble;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements;
import fiji.plugin.trackmate.omnipose.OmniposeDetectorConfigurationPanel;
import fiji.plugin.trackmate.omnipose.OmniposeSettings.PretrainedModelOmnipose;

public class AdvancedOmniposeDetectorConfigurationPanel extends OmniposeDetectorConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	private static final String TITLE = AdvancedOmniposeDetectorFactory.NAME;;

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

	public AdvancedOmniposeDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		super( settings, model, TITLE, ICON, DOC1_URL, "omnipose", PretrainedModelOmnipose.values() );

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

		final JLabel lblCellProb = new JLabel( "Mask threshold:" );
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
	}

	@Override
	protected SpotDetectorFactoryBase< ? > getDetectorFactory()
	{
		return new AdvancedOmniposeDetectorFactory<>();
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		super.setSettings( settings );
		flowThresholdEl.set( ( double ) settings.get( KEY_FLOW_THRESHOLD ) );
		flowThresholdEl.update();
		cellProbThresholdEl.set( ( double ) settings.get( KEY_CELL_PROB_THRESHOLD ) );
		cellProbThresholdEl.update();
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final Map< String, Object > settings = super.getSettings();
		settings.put( KEY_FLOW_THRESHOLD, flowThresholdEl.get() );
		settings.put( KEY_CELL_PROB_THRESHOLD, cellProbThresholdEl.get() );
		return settings;
	}
}
