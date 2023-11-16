package fiji.plugin.trackmate.cellpose.zmerge;

import static fiji.plugin.trackmate.tracking.overlap.OverlapTrackerFactory.DEFAULT_MIN_IOU;
import static fiji.plugin.trackmate.tracking.overlap.OverlapTrackerFactory.DEFAULT_SCALE_FACTOR;
import static fiji.plugin.trackmate.tracking.overlap.OverlapTrackerFactory.KEY_MIN_IOU;
import static fiji.plugin.trackmate.tracking.overlap.OverlapTrackerFactory.KEY_SCALE_FACTOR;

import java.util.Map;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.cellpose.CellposeDetectorConfigurationPanel;
import fiji.plugin.trackmate.cellpose.CellposeSettings.PretrainedModelCellpose;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;

public class Cellpose2DZDetectorConfigurationPanel extends CellposeDetectorConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	public Cellpose2DZDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		super( settings, model, Cellpose2DZDetectorFactory.NAME, ICON, DOC1_URL, "cellpose", PretrainedModelCellpose.values() );
	}

	@Override
	protected SpotDetectorFactoryBase< ? > getDetectorFactory()
	{
		return new Cellpose2DZDetectorFactory<>();
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
