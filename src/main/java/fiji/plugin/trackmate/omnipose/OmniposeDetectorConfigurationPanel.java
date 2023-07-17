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
package fiji.plugin.trackmate.omnipose;

import static fiji.plugin.trackmate.cellpose.CellposeDetectorFactory.KEY_CELL_DIAMETER;
import static fiji.plugin.trackmate.cellpose.CellposeDetectorFactory.KEY_LOGGER;
import static fiji.plugin.trackmate.cellpose.CellposeDetectorFactory.KEY_OPTIONAL_CHANNEL_2;
import static fiji.plugin.trackmate.cellpose.CellposeDetectorFactory.KEY_USE_GPU;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.ThresholdDetectorFactory.KEY_SIMPLIFY_CONTOURS;
import static fiji.plugin.trackmate.omnipose.OmniposeDetectorFactory.KEY_OMNIPOSE_CUSTOM_MODEL_FILEPATH;
import static fiji.plugin.trackmate.omnipose.OmniposeDetectorFactory.KEY_OMNIPOSE_MODEL;
import static fiji.plugin.trackmate.omnipose.OmniposeDetectorFactory.KEY_OMNIPOSE_PYTHON_FILEPATH;

import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.cellpose.CellposeDetectorConfigurationPanel;
import fiji.plugin.trackmate.cellpose.CellposeUtils;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import fiji.plugin.trackmate.omnipose.OmniposeSettings.PretrainedModelOmnipose;

public class OmniposeDetectorConfigurationPanel extends CellposeDetectorConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	private static final String TITLE = OmniposeDetectorFactory.NAME;

	private static final ImageIcon ICON = CellposeUtils.omniposeLogo64();

	protected static final String DOC1_URL = "";

	public OmniposeDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		super( settings, model, TITLE, ICON, DOC1_URL, "omnipose", PretrainedModelOmnipose.values() );
	}

	@Override
	protected SpotDetectorFactoryBase< ? > getDetectorFactory()
	{
		return new OmniposeDetectorFactory<>();
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		tfCellposeExecutable.setText( ( String ) settings.get( KEY_OMNIPOSE_PYTHON_FILEPATH ) );
		tfCustomPath.setText( ( String ) settings.get( KEY_OMNIPOSE_CUSTOM_MODEL_FILEPATH ) );
		cmbboxPretrainedModel.setSelectedItem( settings.get( KEY_OMNIPOSE_MODEL ) );
		cmbboxCh1.setSelectedIndex( ( int ) settings.get( KEY_TARGET_CHANNEL ) );
		cmbboxCh2.setSelectedIndex( ( int ) settings.get( KEY_OPTIONAL_CHANNEL_2 ) );
		ftfDiameter.setValue( settings.get( KEY_CELL_DIAMETER ) );
		chckbxUseGPU.setSelected( ( boolean ) settings.get( KEY_USE_GPU ) );
		chckbxSimplify.setSelected( ( boolean ) settings.get( KEY_SIMPLIFY_CONTOURS ) );
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final HashMap< String, Object > settings = new HashMap<>( 9 );

		settings.put( KEY_OMNIPOSE_PYTHON_FILEPATH, tfCellposeExecutable.getText() );
		settings.put( KEY_OMNIPOSE_CUSTOM_MODEL_FILEPATH, tfCustomPath.getText() );
		settings.put( KEY_OMNIPOSE_MODEL, cmbboxPretrainedModel.getSelectedItem() );

		settings.put( KEY_TARGET_CHANNEL, cmbboxCh1.getSelectedIndex() );
		settings.put( KEY_OPTIONAL_CHANNEL_2, cmbboxCh2.getSelectedIndex() );

		final double diameter = ( ( Number ) ftfDiameter.getValue() ).doubleValue();
		settings.put( KEY_CELL_DIAMETER, diameter );
		settings.put( KEY_SIMPLIFY_CONTOURS, chckbxSimplify.isSelected() );
		settings.put( KEY_USE_GPU, chckbxUseGPU.isSelected() );

		settings.put( KEY_LOGGER, logger );

		return settings;
	}
}
