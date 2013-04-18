package org.gudy.azureus2.countrylocator;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;

/* TODO (overview)
 * 1. the ScrolledComposite should resize according to window size
 * 2. (partly done)layout of different components is not perfect yet
 * 3. optional: country dot scales with changing zoomfactor
 * 4. optional: countries are selectable by mouseclick on the map
 */
/**
 * A composite which displays a zoomable & scrollable map with statistics.
 * <em>Important:</em> The map has to be set via {@link #setImage(Image)}.
 * 
 * @author free_lancer
 * @author gooogelybear
 */
public class ZoomedScrolledMap extends Composite {
	/* contains informations needed for drawing countries */
	private final MapView mapView;
	
	/* SWT Widgets */
	private final Label zoomLabel;
	private final ScrolledComposite scroll;
	private final Canvas canvas;
	private final Scale scale;
	private Image mapImage;
	
	/* App data */
	/**
	 * The size of image when it gets initialized. Per definition this is the
	 * size for zoomfactor 1
	 */
	private int imgWidth, imgHeight;
	
	/**
	 * Needed for tooltip support: Indicates the location where the cursor was
	 * hovering when a tooltip has been requested. A value of <code>null</code>
	 * indicates that no tooltip is required.
	 */
	private Point toolTipLocation;
	
	private float oldZoom = 1f;
	
	/* color mapping stuff */
	/** the min value of the currently displayed stats */
	private float colorMapMinValue;
	
	/** the max value of the currently displayed stats */
	private float colorMapMaxValue;
	
	/* color preferences */
	// TODO we could directly read this from the config file
	/** the current min color for the country dots */
	static Color countryMinColor;
	
	/** the current max color for the country dots */
	static Color countryMaxColor;
	
	/** the current color for highlighting countries */
	static Color countryHighlightColor;
	
	public ZoomedScrolledMap(Composite parent, int style, MapView mapView) {
		super(parent, style);
		this.mapView = mapView;
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		setLayout(gridLayout);
				
	 	scroll = new ScrolledComposite(this, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
	 	canvas = new Canvas(scroll, SWT.DOUBLE_BUFFERED);		
	 	scale = new Scale(this, SWT.VERTICAL);
	 	zoomLabel = new Label(this, SWT.NONE);
	 		 	
		setupScroll();
		setupZoomLabel();
		setupScale();
	}
	
	private void setupScroll() {		
		scroll.setAlwaysShowScrollBars(true);
		scroll.setContent(canvas);
		
	 	GridData gridData = new GridData(SWT.CENTER, SWT.FILL, false, false);
		gridData.widthHint = 800; // TODO Should this be dynamic?
		gridData.heightHint = 400;
		scroll.setLayoutData(gridData);

		layout(true);
	}
	
	private void setupZoomLabel() {	
		zoomLabel.setText("1.0x zoom");
		zoomLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		layout(true);	
	}
	
	private void setupScale() {
		scale.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		scale.setMinimum(0);
		scale.setMaximum(100);
		scale.setIncrement(1);
		scale.setSelection(0);
		scale.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				zoomLabel.setText(getZoom() + "x zoom");
				// move origin of scroll s.t. image zooms towards center of image 
				// TODO it would work, but its very jerky
//				float newZoom = getZoom();
//				int dx = (int)Math.round((newZoom - oldZoom)*imgWidth/2f);
//				int dy = (int)Math.round((newZoom - oldZoom)*imgHeight/2f);
//				Point p = scroll.getOrigin();
//				scroll.setOrigin(p.x + dx, p.y + dy);
//				oldZoom = newZoom;
//				this causes canvas to redraw
//				canvas.setSize((int)(imgWidth*newZoom), (int)(imgHeight*newZoom));
				canvas.setSize((int)(imgWidth*getZoom()), (int)(imgHeight*getZoom()));
			}
		});
		layout(true);
	}
	
	/** sets the map image */
	public void setImage(Image argImg) {
		mapImage = argImg;
		
		/* initialize map size */
		float ratio = (float)mapImage.getBounds().height/mapImage.getBounds().width;
		//System.out.println("scroll size:"+scroll.getSize()); // 819x419
		
		/* Initially the image should fill the entire scrolledComposite */
		/* Note: clientArea = The area without trimmings (scrollbars etc) */
		imgWidth = scroll.getClientArea().width;
		imgHeight = (int)(ratio*imgWidth);
		canvas.setLocation(0,0);
		canvas.setSize(imgWidth, imgHeight);		
		GeoUtils.setMapSize(imgWidth, imgHeight);
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				redrawMap(e.gc);
			}
		});

		/* reroute redraw() calls from this to canvas */
		this.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				canvas.redraw();
			}
		});
		
		/* mouseListeners needed for tooltips */
		canvas.addMouseTrackListener(new MouseTrackAdapter() {
			public void mouseExit(MouseEvent e) {
				toolTipLocation = null;
				canvas.redraw();
			}
			public void mouseHover(MouseEvent e) {
				/* cursor is standing still -> draw tooltip */
				toolTipLocation = new Point(e.x, e.y);
				canvas.redraw();
			}	
		});
		canvas.addMouseMoveListener(new MouseMoveListener() {
			public void mouseMove(MouseEvent e) {
				/* as long as the cursor is moving no tooltip should be displayed*/
				toolTipLocation = null;
			}	
		});
		
		redraw();
	}
	
	private void redrawMap(GC gc) {
		/* [1] Draw map: 
		 * To realise zooming the map is scaled to the size of the canvas
		 */
		// TODO Use canvas.setBackgroundImage(image) when SWT 3.2 is available
		gc.drawImage(mapImage, 0, 0, mapImage.getBounds().width, mapImage.getBounds().height, 
				0, 0, canvas.getSize().x, canvas.getSize().y);
		
		gc.setAdvanced( true );
		gc.setAntialias( SWT.ON );
		/* get country informations */
		Map selectedStats = mapView.getSelectedStats();
		if (selectedStats == null) return;
		Set countryCodes = selectedStats.keySet();
		Collection values = selectedStats.values();
		
		/* get name of currently selected country */		
		String selectedCountry = mapView.getSelectedCountryName();
		
		/* define color map values for countries based on the min & max values of the
		 * displayed stats */
		setColorMapValues(((Long)Collections.min(values)).floatValue(), ((Long)Collections.max(values)).floatValue());
		
		/* needed for tooltips */
		String toolTipLabel = null;
		
		/* main loop for map redraw */
		for (Iterator it = countryCodes.iterator(); it.hasNext();) {
			String cc = (String)it.next();
			Point unscaledPoint = GeoUtils.mapCountryCodeToXYCoord(cc);
			if (unscaledPoint != null) {
				// rescale point according to current zoom factor
				Point p = new Point((int)(getZoom()*unscaledPoint.x), (int)(getZoom()*unscaledPoint.y));
				// TODO reuse methods from country locator (cc -> name)
				String currentCountry = new Locale("", cc).getDisplayCountry(
						Locale.getDefault());
				
				/* [2] Highlight country which is currently selected in the table  */
				if (currentCountry.equals(selectedCountry)) {
					gc.setBackground(countryHighlightColor);
					gc.fillOval(p.x-UIConstants.COUNTRY_RADIUS-2, p.y-UIConstants.COUNTRY_RADIUS-2,
							UIConstants.COUNTRY_SIZE+4, UIConstants.COUNTRY_SIZE+4);
				}
				
				/* [3] Draw country */
				Color color = mapFloatToColor(((Long)selectedStats.get(cc)).floatValue());
				gc.setBackground(color);
				gc.fillOval(p.x-UIConstants.COUNTRY_RADIUS, p.y-UIConstants.COUNTRY_RADIUS,
						UIConstants.COUNTRY_SIZE, UIConstants.COUNTRY_SIZE);
				color.dispose();
				
				/* [4] setup tooltip if cursor hovers over a country */
				if ((toolTipLocation != null) && (toolTipLabel == null)) {
					Rectangle region = new Rectangle(toolTipLocation.x-UIConstants.COUNTRY_RADIUS, toolTipLocation.y-UIConstants.COUNTRY_RADIUS,
							UIConstants.COUNTRY_SIZE, UIConstants.COUNTRY_SIZE);
					if (region.contains(p)) {
						toolTipLabel = currentCountry + " (" + selectedStats.get(cc) + ")";
					}
				}
			}
		}
		
		/* [5] do the actual drawing of the tooltip (on top of all countries)
		 * the tooltip will only be displayed if there was a match in [4] */
		if (toolTipLabel != null) {
			int toolTipYOffset = 15;
			gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
			gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
			gc.drawString(toolTipLabel,toolTipLocation.x,toolTipLocation.y-toolTipYOffset);
		}
	}
	
	/** 
	 * Calculates zoomfactor from scale slider
	 * 
	 * @return value between 1 and UIConstants.MAX_ZOOM */
	private float getZoom() {
		return (UIConstants.MAX_ZOOM - 1f)*scale.getSelection()/100f + 1f;
	}
	
	/* color mapping */
	/**
	 * Set value interval for colorMap() 
	 */
	private void setColorMapValues(float minValue, float maxValue) {
		colorMapMinValue = minValue;
		colorMapMaxValue = maxValue;
	}
		
	/**
	 * Maps scalar values between colorMapMinValue and colorMapMaxValue linearly to
	 * the colorspace  between <code>countryMinColor</code> and <code>countryMaxColor</code>.
	 * Values exceeding the min and max values are truncated. This is used to generate the
	 * colors for the statistics values.
	 */
	private Color mapFloatToColor(float val) {
		//catch invalid value interval bounds
		if (colorMapMinValue >= colorMapMaxValue) 
			return new Color(Display.getCurrent(), countryMinColor.getRGB());
		
		//truncate value if necessary
		if (val > colorMapMaxValue) {
			val = colorMapMaxValue;
		} else if (val < colorMapMinValue) {
			val = colorMapMinValue;
		}
		//interpolate colors
		float a = (val - colorMapMinValue)/(colorMapMaxValue - colorMapMinValue);
		int r = (int)((1f - a)*countryMinColor.getRed() + a*countryMaxColor.getRed());
		int g = (int)((1f - a)*countryMinColor.getGreen() + a*countryMaxColor.getGreen());
		int b = (int)((1f - a)*countryMinColor.getBlue() + a*countryMaxColor.getBlue());
		return new Color(Display.getCurrent(), r, g, b);
	}
}
