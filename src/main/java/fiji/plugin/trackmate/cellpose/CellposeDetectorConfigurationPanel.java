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

import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.util.DetectionPreview;
import fiji.plugin.trackmate.util.DetectionPreviewPanel;
import fiji.plugin.trackmate.util.cli.CliGuiBuilder;
import fiji.plugin.trackmate.util.cli.CliGuiBuilder.CliConfigPanel;
import fiji.plugin.trackmate.util.cli.TrackMateSettingsBuilder;

public class CellposeDetectorConfigurationPanel extends ConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	private static final String PREF_KEY_CELLPOSE_PYTHON_FILEPATH = "trackmate.cellpose.python.path";

	private static final String PREF_KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH = "trackmate.cellpose.custommodel.path";

	protected final CliConfigPanel mainPanel;

	private final CellposeCLI cli;

	public CellposeDetectorConfigurationPanel(
			final Settings settings,
			final Model model,
			final CellposeCLI cli,
			final String title,
			final Icon icon,
			final String docURL )
	{
		this.cli = cli;

		final BorderLayout borderLayout = new BorderLayout();
		setLayout( borderLayout );

		/*
		 * HEADER
		 */

		final JPanel header = new JPanel();
		header.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
		header.setLayout( new BoxLayout( header, BoxLayout.Y_AXIS ) );

		final JLabel lblDetector = new JLabel( title, icon, JLabel.RIGHT );
		lblDetector.setFont( BIG_FONT );
		lblDetector.setHorizontalAlignment( SwingConstants.CENTER );
		lblDetector.setAlignmentX( JLabel.CENTER_ALIGNMENT );
		header.add( lblDetector );
		header.add( Box.createVerticalStrut( 5 ) );
		final JEditorPane infoDisplay = GuiUtils.infoDisplay( "<html>" + "Documentation for this module "
				+ "<a href=\"" + docURL + "\">on the ImageJ Wiki</a>."
				+ "</html>", false );
		infoDisplay.setMaximumSize( new Dimension( 100_000, 40 ) );
		header.add( infoDisplay );
		add( header, BorderLayout.NORTH );

		/*
		 * CONFIG
		 */

		this.mainPanel = CliGuiBuilder.build( cli );
		final JScrollPane scrollPane = new JScrollPane( mainPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		scrollPane.setBorder( null );
		scrollPane.getVerticalScrollBar().setUnitIncrement( 16 );
		add( scrollPane, BorderLayout.CENTER );

		/*
		 * PREVIEW
		 */

		final DetectionPreview detectionPreview = DetectionPreview.create()
				.model( model )
				.settings( settings )
				.detectorFactory( getDetectorFactory() )
				.detectionSettingsSupplier( () -> getSettings() )
				.get();
		final DetectionPreviewPanel p = detectionPreview.getPanel();
		p.setPreferredSize( new Dimension( 200, 200 ) );

		add( p, BorderLayout.SOUTH );
	}

	protected SpotDetectorFactoryBase< ? > getDetectorFactory()
	{
		return new CellposeDetectorFactory<>();
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		try
		{
			TrackMateSettingsBuilder.fromTrackMateSettings( settings, cli );
			mainPanel.refresh();
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final Map< String, Object > map = new HashMap<>();
		TrackMateSettingsBuilder.toTrackMateSettings( map, cli );
		return map;
	}

	@Override
	public void clean()
	{}
}
