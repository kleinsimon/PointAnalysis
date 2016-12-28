import java.awt.Color;
import java.awt.Composite;
import java.awt.Point;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;

import ij.ImagePlus;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.ImageRoi;
import ij.gui.Overlay;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.process.Blitter;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

public class PointAnalysisInteractiveHandler {
	private Integer px, py, nd, sx, sy, markLengthPx;
	private Double dx, dy, ox, oy;
	private Hashtable<Point, Integer> markList = new Hashtable<Point, Integer>();
	private ImagePlus iplus = null;
	private ImageCanvas icanv = null;
	private Integer numMarks = 0;
	private ArrayList<Point> points;
	private PointAnalysisInteractiveMenuStrip menuStrip;
	ImageProcessor ip = null;
	public PointAnalysisInteractiveMouseHandler mouseActionListener;
	private ResultsTable rt;
	private Boolean randomize;
	Overlay ovl;
	private Color ovlColor;

	public PointAnalysisInteractiveHandler(int pointsX, int pointsY, String overlayColor, int numDom, Boolean randomizePoints,
			int markLenPx, ImagePlus image, ResultsTable restable,
			PointAnalysisInteractiveMenuStrip parentStrip) {
		px = pointsX;
		py = pointsY;
		
		nd = numDom;
		markLengthPx = markLenPx;
		iplus = image;
		ip = iplus.getProcessor();
		icanv = iplus.getCanvas();
		rt = restable;
		menuStrip = parentStrip;
		randomize = randomizePoints;
		setColor(overlayColor);
		
		sx = iplus.getWidth();
		sy = iplus.getHeight();
				
		dx = ((double) sx / ((double) px + 1.0));
		dy = ((double) sy / ((double) py + 1.0));
		
		ox = dx;
		oy = dy;
		
		mouseActionListener = new PointAnalysisInteractiveMouseHandler(this);

		ImageCanvas icanv = iplus.getCanvas();
		icanv.addMouseMotionListener(mouseActionListener);
		icanv.addMouseListener(mouseActionListener);

		initPoints();
		
		ij.IJ.setTool(12);
		icanv.disablePopupMenu(true);
		drawOverlay();
		iplus.draw();
		menuStrip.updateCounter(getMarkCounts());
	}

	public void setColor(String overlayColor) {
		try {
		    Field field = Color.class.getField(overlayColor.toUpperCase());
		    ovlColor = (Color)field.get(null);
		} catch (Exception e) {
			ovlColor = Color.RED;
		}
	}

	public void updateSize() {
		iplus.getWindow().validate();
		icanv.zoomIn(0, 0);
		icanv.zoomOut(0, 0);
	}
	
	public void initPoints() {
		int n;
		if (randomize){
			n = px.intValue();
			points = new ArrayList<Point>(n);
			
			for (int i=0; i<n; i++){
				Point p = new Point(rndInt(ox, sx-ox), rndInt(oy,sy-oy));
				points.add(p);
				markList.put(p, 0);
			}
		} else {
			n = px.intValue()  * py.intValue();
			points = new ArrayList<Point>(n);
			
			for (double ix=ox; ix <= sx-ox; ix+=dx) {
				for (double iy=oy; iy <= sy-oy; iy+=dy) {
					Point p = new Point(Tools.getRoundedInt(ix),Tools.getRoundedInt(iy));
					points.add(p);
					markList.put(p, 0);
				}
			}
		}		
	}
	
	public int rndInt(int min, int max){
		double r = Math.random();
		double s = max - min;
		return Tools.getRoundedInt(min + r * s);
	}
	
	public int rndInt(Double min, Double max){
		double r = Math.random();
		double s = max - min;
		return Tools.getRoundedInt(min + r * s);
	}

	public Point getRealPos() {
		return icanv.getCursorLoc();
	}

	public void remove() {
		iplus.setOverlay(null);
		iplus.updateAndDraw();
		icanv.removeMouseListener(mouseActionListener);
		icanv.removeMouseMotionListener(mouseActionListener);
		icanv.disablePopupMenu(false);
	}

	public void analyze() {

		Integer[] domains = getMarkCounts();

		rt.incrementCounter();
		int row = rt.getCounter() - 1;

		rt.setValue("Image", row, iplus.getTitle());

		for (int ri = 0; ri < nd; ri++) {
			String cName = String.format("Domain %n", ri);
			rt.setValue(cName, row, domains[ri]);
		}

		rt.show("Point Analysis Results");
	}
	
	public Integer[] getMarkCounts() {
		Integer domains[] = new Integer[nd];
		
		for (int i=0; i<domains.length; i++)
			domains[i]=0;
		
		for (Integer p : markList.values()) {
			domains[p]++;
		}
		return domains;
	}

	public void clear() {
		initPoints();
		iplus.updateAndDraw();
	}

	public void togglePoint(Boolean rev) {
		Point cursorPos = getRealPos();
		Point p = getNextPoint(cursorPos);

		if (markList.get(p) == null) {
			markList.put(p, 1);
		} 
		else {
			int olddom = markList.get(p);
			if (olddom == nd-1)
				markList.put(p, 0);
			else
				markList.put(p, olddom+1);
		}
		drawOverlay();
		numMarks++;
		menuStrip.updateCounter(getMarkCounts());
	}

	public void drawOverlay() {
		ImageProcessor overlay = new ColorProcessor(ip.getWidth(), ip.getHeight());
		
		overlay.setColor(ovlColor);

		//Point cursorPos = getRealPos();
		

		int l = markLengthPx / 2;

		Enumeration<Point> e = markList.keys();
		while (e.hasMoreElements()) {
			Point p = e.nextElement();
			Integer dom = markList.get(p)+1;
			overlay.drawLine(p.x, p.y-l, p.x, p.y+l);
			overlay.drawLine(p.x-l, p.y, p.x+l, p.y);	
			overlay.drawString(dom.toString(), p.x+l, p.y, Color.BLACK);
		}
		
		
		ImageRoi roi = new ImageRoi(0, 0, overlay);
		//roi.setName(iplus.getShortTitle() + " measured stripes");
		//roi.setOpacity(1d);
		roi.setZeroTransparent(true);

		//ovl = new Overlay(roi);
		iplus.setOverlay(roi, Color.red, 0, Color.red);
		// iplus.setRoi(roi);
		// icanv.setCursor(Cursor.CURSOR_NONE);
		iplus.draw();
	}

	public Point getNextPoint(Point Cursor) {
		if (Cursor == null)
			throw new NullPointerException("No Point given");
		

		double dst = Double.POSITIVE_INFINITY;
		Point nearest = null;
		
		Enumeration<Point> e = markList.keys();
		while (e.hasMoreElements()) {
			Point p = e.nextElement();
			double d = Point.distance(p.x, p.y, Cursor.x, Cursor.y);
			if (d<dst){
				dst = d;
				nearest = p;
			}
		}

		return nearest;
	}
}
