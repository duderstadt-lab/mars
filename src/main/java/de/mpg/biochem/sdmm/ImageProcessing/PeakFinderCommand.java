package de.mpg.biochem.sdmm.ImageProcessing;

import ij.ImagePlus;
import ij.Prefs;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij.gui.PointRoi;

import org.scijava.module.MutableModuleItem;

import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.command.Previewable;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.util.RealRect;

import de.mpg.biochem.sdmm.molecule.ImageMetaData;
import de.mpg.biochem.sdmm.table.ResultsTableService;
import de.mpg.biochem.sdmm.table.SDMMResultsTable;
import io.scif.config.SCIFIOConfig;
import io.scif.config.SCIFIOConfig.ImgMode;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;
import io.scif.img.SCIFIOImgPlus;
import net.imagej.display.ImageDisplay;
import net.imagej.display.OverlayService;
import net.imglib2.Cursor;
import net.imglib2.RealPoint;

import java.util.ArrayList;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import net.imagej.ops.Initializable;
import net.imagej.table.DoubleColumn;
import io.scif.img.IO;
import io.scif.img.ImgIOException;

import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import net.imglib2.img.ImagePlusAdapter;

@Plugin(type = Command.class, label = "Peak Finder", menu = {
		@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT,
				mnemonic = MenuConstants.PLUGINS_MNEMONIC),
		@Menu(label = "SDMM Plugins", weight = MenuConstants.PLUGINS_WEIGHT,
			mnemonic = 's'),
		@Menu(label = "Image Processing", weight = 20,
			mnemonic = 'm'),
		@Menu(label = "Peak Finder", weight = 1, mnemonic = 'd')})
public class PeakFinderCommand<T extends RealType< T >> extends DynamicCommand implements Command, Initializable, Previewable {
	
	//GENERAL SERVICES NEEDED
	@Parameter(required=false)
	private RoiManager roiManager;
	
	@Parameter
	private LogService logService;
	
    @Parameter
    private StatusService statusService;
    
	@Parameter
    private ResultsTableService resultsTableService;
	
	//INPUT IMAGE
	@Parameter(label = "Image to search for Peaks")
	private ImagePlus image; 
	
	//ROI SETTINGS
	@Parameter(label="use ROI", persist=false)
	private boolean useROI = true;
	
	@Parameter(label="ROI x0", persist=false)
	private int x0;
	
	@Parameter(label="ROI y0", persist=false)
	private int y0;
	
	@Parameter(label="ROI width", persist=false)
	private int w;
	
	@Parameter(label="ROI height", persist=false)
	private int h;
	
	//PEAK FINDER SETTINGS
	@Parameter(label="Use Discoidal Averaging Filter")
	private boolean useDiscoidalAveragingFilter;
	
	@Parameter(label="Inner radius")
	private int DS_innerRadius;
	
	@Parameter(label="Outer radius")
	private int DS_outerRadius;
	
	@Parameter(label="Detection threshold (mean + N * STD)")
	private int threshold;
	
	@Parameter(label="Minimum distance between peaks (in pixels)")
	private int minimumDistance;
	
	@Parameter(label="Generate peak count table")
	private boolean generatePeakCountTable;
	
	@Parameter(label="Generate peaks table")
	private boolean generatePeakTable;
	
	@Parameter(label="Add to RoiManger")
	private boolean addToRoiManger;
	
	@Parameter(label="Process all slices")
	private boolean allSlices;

	@Parameter(visibility = ItemVisibility.INVISIBLE, persist = false, callback = "previewChanged")
	private boolean preview = false;
	
	//PEAK FITTER
	@Parameter(visibility = ItemVisibility.MESSAGE)
	private final String header =
		"Peak fitter settings:";
	
	@Parameter(label="Fit peaks")
	private boolean fitPeaks = false;
	
	@Parameter(label="Fit Radius")
	private int fitRadius = 2;
	
	//Initial guesses for fitting
	@Parameter(label="Initial Baseline")
	private double PeakFitter_initialBaseline = Double.NaN;
	
	@Parameter(label="Initial Height")
	private double PeakFitter_initialHeight = Double.NaN;
	
	@Parameter(label="Initial Sigma")
	private double PeakFitter_initialSigma = Double.NaN;
	
	//parameters to vary during fit
	
	@Parameter(label="Vary Baseline")
	private boolean PeakFitter_varyBaseline = true;
	
	@Parameter(label="Vary Height")
	private boolean PeakFitter_varyHeight = true;
	
	@Parameter(label="Vary Sigma")
	private boolean PeakFitter_varySigma = true;
	
	//Maximum allow error for the fitting process
	@Parameter(label="Max Error Baseline")
	private double PeakFitter_maxErrorBaseline = 5000;
	
	@Parameter(label="Max Error Height")
	private double PeakFitter_maxErrorHeight = 5000;
	
	@Parameter(label="Max Error X")
	private double PeakFitter_maxErrorX = 1;
	
	@Parameter(label="Max Error Y")
	private double PeakFitter_maxErrorY = 1;
	
	@Parameter(label="Max Error Sigma")
	private double PeakFitter_maxErrorSigma = 1;
	
	//Which columns to write in peak table
	@Parameter(label="Verbose table fit output")
	private boolean PeakFitter_writeEverything = true;
	
	//OUTPUT PARAMETERS
	@Parameter(label="Peak Count", type = ItemIO.OUTPUT)
	private SDMMResultsTable peakCount;
	
	@Parameter(label="Peaks", type = ItemIO.OUTPUT)
	private SDMMResultsTable peakTable;
	
	//instance of a PeakFinder to use for all the peak finding operations by passing an image and getting back a peak list.
	private PeakFinder finder;
	
	//instance of a PeakFitter to use for all the peak fitting operations by passing an image and pixel index list and getting back subpixel fits..
	private PeakFitter fitter;
	
	//A map with peak lists for each slice for an image stack
	private ConcurrentMap<Integer, ArrayList<Peak>> PeakStack;
	
	//box region for analysis added to the image.
	private Rectangle rect;
	private Roi startingRoi;
	
	//For the progress thread
	private final AtomicBoolean progressUpdating = new AtomicBoolean(true);
	
	//array for max error margins.
	private double[] maxError;
	private boolean[] vary;
	
	@Override
	public void initialize() {
		if (image.getRoi() == null) {
			rect = new Rectangle(0,0,image.getWidth()-1,image.getHeight()-1);
			final MutableModuleItem<Boolean> useRoifield = getInfo().getMutableInput("useROI", Boolean.class);
			useRoifield.setValue(this, false);
		} else {
			rect = image.getRoi().getBounds();
			startingRoi = image.getRoi();
		}
		
		final MutableModuleItem<Integer> imgX0 = getInfo().getMutableInput("x0", Integer.class);
		imgX0.setValue(this, rect.x);
		
		final MutableModuleItem<Integer> imgY0 = getInfo().getMutableInput("y0", Integer.class);
		imgY0.setValue(this, rect.y);
		
		final MutableModuleItem<Integer> imgWidth = getInfo().getMutableInput("w", Integer.class);
		imgWidth.setValue(this, rect.width);
		
		final MutableModuleItem<Integer> imgHeight = getInfo().getMutableInput("h", Integer.class);
		imgHeight.setValue(this, rect.height);

	}
	@Override
	public void run() {				
		image.deleteRoi();
		
		//Used to store peak list for single frame operations
		ArrayList<Peak> peaks = new ArrayList<Peak>();
		
		if (fitPeaks) {
			vary = new boolean[5];
			vary[0] = PeakFitter_varyBaseline;
			vary[1] = PeakFitter_varyHeight;
			vary[2] = true;
			vary[3] = true;
			vary[4] = PeakFitter_varySigma;
			
			fitter = new PeakFitter(vary);
			
			maxError = new double[5];
			maxError[0] = PeakFitter_maxErrorBaseline;
			maxError[1] = PeakFitter_maxErrorHeight;
			maxError[2] = PeakFitter_maxErrorX;
			maxError[3] = PeakFitter_maxErrorY;
			maxError[4] = PeakFitter_maxErrorSigma;

		}
		
		if (allSlices) {
			PeakStack = new ConcurrentHashMap<>();
			
			//Need to determine the number of threads
			final int PARALLELISM_LEVEL = Runtime.getRuntime().availableProcessors();
			
			ForkJoinPool forkJoinPool = new ForkJoinPool(PARALLELISM_LEVEL);
		    try {
		    	//Start a thread to keep track of the progress of the number of frames that have been processed.
		    	//Waiting call back to update the progress bar!!
		    	Thread progressThread = new Thread() {
		            public synchronized void run() {
	                    try {
	        		        while(progressUpdating.get()) {
	        		        	Thread.sleep(100);
	        		        	statusService.showStatus(PeakStack.size(), image.getStackSize(), "Finding Peaks for " + image.getTitle());
	        		        }
	                    } catch (Exception e) {
	                        e.printStackTrace();
	                    }
		            }
		        };

		        progressThread.start();
		        
		        //This will spawn a bunch of threads that will analyze frames individually in parallel and put the results into the PeakStack map as lists of
		        //peaks with the slice number as a key in the map for each list...
		        forkJoinPool.submit(() -> IntStream.rangeClosed(1, image.getStackSize()).parallel().forEach(i -> PeakStack.put(i, findPeaksInSlice(i)))).get();
		        
		        progressUpdating.set(false);
		        
		        statusService.showProgress(100, 100);
		        statusService.showStatus("Peak search for " + image.getTitle() + " - Done!");
		        
		    } catch (InterruptedException | ExecutionException e) {
		        // handle exceptions
		    } finally {
		        forkJoinPool.shutdown();
		    }
			
		} else {
			ImagePlus selectedImage = new ImagePlus("current slice", image.getImageStack().getProcessor(image.getCurrentSlice()));
			peaks = findPeaks(selectedImage);
			if (fitPeaks) 
				fitPeaks(selectedImage.getProcessor(), peaks);
		}
		
		if (generatePeakCountTable) {
			peakCount = new SDMMResultsTable("Peak Count - " + image.getTitle());
			DoubleColumn sliceColumn = new DoubleColumn("slice");
			DoubleColumn countColumn = new DoubleColumn("peaks");
			
			if (allSlices) {
				for (int i=1;i <= PeakStack.size() ; i++) {
					sliceColumn.addValue(i);
					countColumn.addValue(PeakStack.get(i).size());
				}
			} else {
				sliceColumn.addValue(1);
				countColumn.addValue(peaks.size());
			}
			peakCount.add(countColumn);
			peakCount.add(sliceColumn);
			
			//Make sure the output table has the correct name
			getInfo().getMutableOutput("peakCount", SDMMResultsTable.class).setLabel(peakCount.getName());
		}
		
		if (generatePeakTable) {
			//build a table with all peaks
			peakTable = new SDMMResultsTable("Peaks - " + image.getTitle());
			
			DoubleColumn[] columns;
			
			if (allSlices) {
				columns = new DoubleColumn[3];
				columns[0] = new DoubleColumn("x");
				columns[1] = new DoubleColumn("y");
				columns[2] = new DoubleColumn("slice");
				for (int i=1;i<=PeakStack.size() ; i++) {
					ArrayList<Peak> slicePeaks = PeakStack.get(i);
					for (int j=0;j<slicePeaks.size();j++) {
						columns[0].addValue(slicePeaks.get(j).getX());
						columns[1].addValue(slicePeaks.get(j).getY());
						columns[2].addValue(i);
					}
				}
			} else {
				columns = new DoubleColumn[2];
				columns[0] = new DoubleColumn("x");
				columns[1] = new DoubleColumn("y");
				for (int i=0;i < peaks.size();i++) {
					columns[0].addValue(peaks.get(i).getX());
					columns[1].addValue(peaks.get(i).getY());
				}
			}
			for(int i=0;i<columns.length;i++)
				peakTable.add(columns[i]);	
			
			//Make sure the output table has the correct name
			getInfo().getMutableOutput("peakTable", SDMMResultsTable.class).setLabel(peakTable.getName());
		}
		
		if (addToRoiManger) {
			if (allSlices) {
				//loop through map and slices and add to Manager
				//This is slow probably because of the continuous GUI updating, but I am not sure a solution
				//There is only one add method for the RoiManager and you can only add one Roi at a time.
				int peakNumber = 1;
				for (int i=1;i <= PeakStack.size() ; i++) {
					peakNumber = AddToManger(PeakStack.get(i),i, peakNumber);
				}
			} else {
				AddToManger(peaks,0);
			}
			statusService.showStatus("Done adding ROIs to Manger");
		}
		image.setRoi(startingRoi);
	}
	
	private ArrayList<Peak> findPeaksInSlice(int slice) {
		ArrayList<Peak> peaks = findPeaks(new ImagePlus("slice " + slice, image.getImageStack().getProcessor(slice)));
		if (fitPeaks)
			fitPeaks(image.getImageStack().getProcessor(slice), peaks);
		return peaks;
	}
	
	private void AddToManger(ArrayList<Peak> peaks, int slice) {
		AddToManger(peaks, slice, 1);
	}
	
	private int AddToManger(ArrayList<Peak> peaks, int slice, int startingPeakNum) {
		if (roiManager == null)
			roiManager = new RoiManager();
		int pCount = startingPeakNum;
		if (!peaks.isEmpty()) {
			for (Peak peak : peaks) {
				PointRoi peakRoi = new PointRoi(peak.getDoublePosition(0) + 0.5, peak.getDoublePosition(1) + 0.5);
				if (slice == 0) {
					peakRoi.setName("Peak_"+pCount);
				} else {
					peakRoi.setName("Peak_"+slice+"_"+pCount);
				}
				peakRoi.setPosition(slice);
				roiManager.addRoi(peakRoi);
				pCount++;
			}
		}
		return pCount;
	}
	
	public ArrayList<Peak> findPeaks(ImagePlus imp) {
		ArrayList<Peak> peaks;
		
		if (useDiscoidalAveragingFilter) {
	    	finder = new PeakFinder< T >(threshold, minimumDistance, DS_innerRadius, DS_outerRadius);
	    } else {
	    	finder = new PeakFinder< T >(threshold, minimumDistance);
	    }
		
		if (useROI) {
	    	peaks = finder.findPeaks(imp, new Roi(x0, y0, w, h));
		} else {
			peaks = finder.findPeaks(imp);
		}
		
		if (peaks == null)
			peaks = new ArrayList<Peak>();
		
		return peaks;
	}
	
	public void fitPeaks(ImageProcessor imp, ArrayList<Peak> positionList) {
		
		int fitWidth = fitRadius * 2 + 1;
		
		//Need to read in initial guess values
		//Also, need to read out fit results back into peak
		
		for (Peak peak: positionList) {
			
			imp.setRoi(new Roi(peak.getX() - fitRadius, peak.getY() - fitRadius, fitWidth, fitWidth));
			
			double[] p = new double[5];
			p[0] = PeakFitter_initialBaseline;
			p[1] = PeakFitter_initialHeight;
			p[2] = peak.getX();
			p[3] = peak.getY();
			p[4] = PeakFitter_initialSigma;
			double[] e = new double[5];
			
			fitter.fitPeak(imp, p, e);
			
			// First we reset valid since it was set to false for all peaks
			// during the finding step to avoid finding the same peak twice.
			peak.setValid();
			
			for (int i = 0; i < p.length && peak.isValid(); i++) {
				if (Double.isNaN(p[i]) || Double.isNaN(e[i]) || Math.abs(e[i]) > maxError[i])
					peak.setNotValid();
			}
			
			peak.setValues(p);
			peak.setErrorValues(e);
		}
	}
	
	@Override
	public void preview() {
		if (preview) {
			image.deleteRoi();
			ImagePlus selectedImage = new ImagePlus("current slice", image.getImageStack().getProcessor(image.getCurrentSlice()));
			ArrayList<Peak> peaks = findPeaks(selectedImage);
			
			if (!peaks.isEmpty()) {
				Polygon poly = new Polygon();
				
				for (Peak p: peaks) {
					int x = (int)p.getDoublePosition(0);
					int y = (int)p.getDoublePosition(1);
					poly.addPoint(x, y);
				}
				
				PointRoi peakRoi = new PointRoi(poly);
				image.setRoi(peakRoi);
			}
		}
	}
	
	@Override
	public void cancel() {
		image.setRoi(startingRoi);
	}
	
	/** Called when the {@link #preview} parameter value changes. */
	protected void previewChanged() {
		// When preview box is unchecked, reset the Roi back to how it was before...
		if (!preview) cancel();
	}
}
