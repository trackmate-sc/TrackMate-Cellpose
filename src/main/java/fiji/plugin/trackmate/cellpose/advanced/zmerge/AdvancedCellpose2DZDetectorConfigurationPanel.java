package fiji.plugin.trackmate.cellpose.advanced.zmerge;

import static fiji.plugin.trackmate.tracking.overlap.OverlapTrackerFactory.DEFAULT_MIN_IOU;
import static fiji.plugin.trackmate.tracking.overlap.OverlapTrackerFactory.DEFAULT_SCALE_FACTOR;
import static fiji.plugin.trackmate.tracking.overlap.OverlapTrackerFactory.KEY_MIN_IOU;
import static fiji.plugin.trackmate.tracking.overlap.OverlapTrackerFactory.KEY_SCALE_FACTOR;

import java.util.Map;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorConfigurationPanel;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;

public class AdvancedCellpose2DZDetectorConfigurationPanel extends AdvancedCellposeDetectorConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	public AdvancedCellpose2DZDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		super( settings, model );
	}

	@Override
	protected SpotDetectorFactory< ? > getDetectorFactory()
	{
		return new AdvancedCellpose2DZDetectorFactory<>();
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final Map< String, Object > s = super.getSettings();
		s.put( KEY_MIN_IOU, DEFAULT_MIN_IOU );
		s.put( KEY_SCALE_FACTOR, DEFAULT_SCALE_FACTOR );
		return s;
	}
}
