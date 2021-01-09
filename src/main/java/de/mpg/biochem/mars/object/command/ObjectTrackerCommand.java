/*-
 * #%L
 * Molecule Archive Suite (Mars) - core data storage and processing algorithms.
 * %%
 * Copyright (C) 2018 - 2020 Karl Duderstadt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package de.mpg.biochem.mars.object.command;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.display.ImageDisplay;
import net.imagej.ops.Initializable;
import net.imagej.ops.OpService;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.IterableRegion;
import net.imglib2.roi.RealMask;
import net.imglib2.roi.Regions;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import org.decimal4j.util.DoubleRounder;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.convert.ConvertService;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.NumberWidget;

import de.mpg.biochem.mars.image.MarsImageUtils;
import de.mpg.biochem.mars.image.Peak;
import de.mpg.biochem.mars.image.PeakShape;
import de.mpg.biochem.mars.image.PeakTracker;
import de.mpg.biochem.mars.metadata.MarsOMEMetadata;
import de.mpg.biochem.mars.metadata.MarsOMEUtils;
import de.mpg.biochem.mars.molecule.*;
import de.mpg.biochem.mars.object.ObjectArchive;
import de.mpg.biochem.mars.table.MarsTableService;
import de.mpg.biochem.mars.util.LogBuilder;
import de.mpg.biochem.mars.util.MarsMath;
import de.mpg.biochem.mars.util.MarsUtil;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import io.scif.Metadata;
import io.scif.img.SCIFIOImgPlus;
import io.scif.ome.OMEMetadata;
import io.scif.ome.services.OMEXMLService;
import io.scif.services.FormatService;
import io.scif.services.TranslatorService;
import loci.common.services.ServiceException;
import ome.units.quantity.Length;
import ome.xml.meta.OMEXMLMetadata;
import ome.xml.model.enums.DimensionOrder;
import ome.xml.model.enums.EnumerationException;
import ome.xml.model.enums.UnitsLength;
import ome.xml.model.enums.handlers.UnitsLengthEnumHandler;
import ome.xml.model.primitives.PositiveInteger;

import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.algorithm.labeling.ConnectedComponents.StructuringElement;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.numeric.IntegerType;

import java.util.Iterator;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.geom.real.Polygon2D;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;

import net.imglib2.algorithm.neighborhood.HyperSphereShape;
import net.imglib2.type.logic.BitType;

@Plugin(type = Command.class, label = "Object Tracker", menu = { @Menu(
	label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT,
	mnemonic = MenuConstants.PLUGINS_MNEMONIC), @Menu(label = "Mars",
		weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = 's'), @Menu(
			label = "Image", weight = 20, mnemonic = 'm'), @Menu(
				label = "Object Tracker", weight = 10, mnemonic = 'p') })
public class ObjectTrackerCommand extends DynamicCommand implements Command,
	Initializable
{

	/**
	 * SERVICES
	 */
	@Parameter
	private LogService logService;

	@Parameter
	private StatusService statusService;

	@Parameter
	private TranslatorService translatorService;

	@Parameter
	private OMEXMLService omexmlService;

	@Parameter
	private FormatService formatService;

	@Parameter
	private MarsTableService resultsTableService;

	@Parameter
	private ConvertService convertService;

	@Parameter
	private OpService opService;

	@Parameter
	private MoleculeArchiveService moleculeArchiveService;

	/**
	 * IMAGE
	 */
	@Parameter(label = "Image to search for Objects")
	private ImageDisplay imageDisplay;

	/**
	 * ROI
	 */
	@Parameter(required = false)
	private RoiManager roiManager;

	@Parameter(label = "Use ROI", persist = false)
	private boolean useROI = true;
	
	/**
	 * FINDER SETTINGS
	 */
	@Parameter(label = "Channel", choices = { "a", "b", "c" }, persist = false)
	private String channel = "0";

	@Parameter(label = "Local ostu radius")
	private long ostuRadius = 50;

	@Parameter(label = "Minimum distance between object centers")
	private int minimumDistance = 4;
	
	@Parameter(visibility = ItemVisibility.INVISIBLE, persist = false,
		callback = "previewChanged")
	private boolean preview = false;

	@Parameter(label = "Segmentation preview:",
		style = ChoiceWidget.RADIO_BUTTON_HORIZONTAL_STYLE, choices = { "contour",
			"area" })
	private String previewRoiType;

	@Parameter(visibility = ItemVisibility.MESSAGE)
	private String tObjectCount = "count: 0";

	@Parameter(label = "T", min = "0", style = NumberWidget.SCROLL_BAR_STYLE,
		persist = false)
	private int previewT;

	/**
	 * FITTER SETTINGS
	 */
	@Parameter(visibility = ItemVisibility.MESSAGE)
	private final String fitterTitle = "Contour Settings:";

	@Parameter(label = "linear interpolation factor")
	private int interpolationFactor = 4;

	/**
	 * TRACKER SETTINGS
	 */
	@Parameter(visibility = ItemVisibility.MESSAGE)
	private final String trackerTitle = "Object tracker settings:";

	@Parameter(label = "Max difference x")
	private double maxDifferenceX = 1;

	@Parameter(label = "Max difference y")
	private double maxDifferenceY = 1;

	@Parameter(label = "Max difference T")
	private int maxDifferenceT = 1;

	@Parameter(label = "Minimum track length")
	private int minTrajectoryLength = 100;

	/**
	 * VERBOSE
	 */
	@Parameter(label = "Verbose output")
	private boolean verbose = false;

	@Parameter(label = "Microscope")
	private String microscope;

	@Parameter(label = "Pixel length")
	private double pixelLength = 1;

	@Parameter(label = "Pixel units", choices = { "pixel", "µm", "nm" })
	private String pixelUnits = "pixel";
	
	@Parameter(visibility = ItemVisibility.MESSAGE)
	private final String excludeTitle = "List of time points to exclude (T0, T1-T2, etc...)";
	
	@Parameter(label = "Exclude", required = false)
	private String excludeTimePointList = "";

	@Parameter
	private UIService uiService;

	/**
	 * OUTPUTS
	 */
	@Parameter(label = "Molecule Archive", type = ItemIO.OUTPUT)
	private ObjectArchive archive;

	/**
	 * Map from T to peak list
	 */
	private ConcurrentMap<Integer, List<Peak>> objectStack;

	private PeakTracker tracker;

	private Roi roi;

	private Dataset dataset;
	private ImagePlus image;
	private boolean swapZandT = false;

	@Override
	public void initialize() {
		if (imageDisplay != null) {
			dataset = (Dataset) imageDisplay.getActiveView().getData();
			image = convertService.convert(imageDisplay, ImagePlus.class);
		}
		else return;

		if (image.getRoi() == null) {
			final MutableModuleItem<Boolean> useRoifield = getInfo().getMutableInput(
				"useROI", Boolean.class);
			useRoifield.setValue(this, false);
		}
		else roi = image.getRoi();

		final MutableModuleItem<String> channelItems = getInfo().getMutableInput(
			"channel", String.class);
		long channelCount = dataset.getChannels();
		ArrayList<String> channels = new ArrayList<String>();
		for (int ch = 0; ch < channelCount; ch++)
			channels.add(String.valueOf(ch));
		channelItems.setChoices(channels);
		channelItems.setValue(this, String.valueOf(image.getChannel() - 1));

		final MutableModuleItem<Integer> preFrame = getInfo().getMutableInput(
			"previewT", Integer.class);
		if (image.getNFrames() < 2) {
			preFrame.setValue(this, image.getSlice() - 1);
			preFrame.setMaximumValue(image.getStackSize() - 1);
			swapZandT = true;
		}
		else {
			preFrame.setValue(this, image.getFrame() - 1);
			preFrame.setMaximumValue(image.getNFrames() - 1);
		}
	}

	@Override
	public void run() {
		if (dataset == null && image != null)
			dataset = convertService.convert(image, Dataset.class);
		
		if (dataset.dimension(dataset.dimensionIndex(Axes.TIME)) < 2) swapZandT = true;

		if (image != null) {
			image.deleteRoi();
			image.setOverlay(null);
		}

		// Build log
		LogBuilder builder = new LogBuilder();
		String log = LogBuilder.buildTitleBlock("Object Tracker");
		addInputParameterLog(builder);
		log += builder.buildParameterList();
		logService.info(log);

		objectStack = new ConcurrentHashMap<>();

		double starttime = System.currentTimeMillis();
		logService.info("Finding and Fitting Peaks...");

		final int PARALLELISM_LEVEL = Runtime.getRuntime().availableProcessors();

		int zDim = dataset.getImgPlus().dimensionIndex(Axes.Z);
		int zSize = (int) dataset.getImgPlus().dimension(zDim);

		int tDim = dataset.getImgPlus().dimensionIndex(Axes.TIME);
		int tSize = (int) dataset.getImgPlus().dimension(tDim);

		final int frameCount = (swapZandT) ? zSize : tSize;
		
		//build list of timepoints to process...
		
		List<int[]> excludeTimePoints = new ArrayList<int[]>();
		if (excludeTimePointList.length() > 0) {
			try {
				final String[] excludeArray = excludeTimePointList.split(",");
				for (int i=0; i<excludeArray.length; i++) {
					String[] endPoints = excludeArray[i].split("-");
					int start = Integer.valueOf(endPoints[0].trim());
					int end = (endPoints.length > 1) ? Integer.valueOf(endPoints[1].trim()) : start;
		
					excludeTimePoints.add(new int[] {start, end});
				}
			} catch (NumberFormatException e) {
				logService.info("NumberFormatException encountered when parsing exclude list. Tracking all time points.");
				excludeTimePoints = new ArrayList<int[]>();
			}
		}
		
		List<Integer> processTimePoints = new ArrayList<Integer>();
		for (int t=0; t<frameCount; t++) {
			boolean processedTimePoint = true;
			for (int index=0; index<excludeTimePoints.size(); index++)
				if (excludeTimePoints.get(index)[0] <= t && t <= excludeTimePoints.get(index)[1]) {
					processedTimePoint = false;
					break;
				}
			
			if (processedTimePoint)
				processTimePoints.add(t);
		}

		MarsUtil.forkJoinPoolBuilder(statusService, logService, () -> statusService
			.showStatus(objectStack.size(), frameCount, "Finding objects for " + dataset
				.getName()), () -> processTimePoints.parallelStream().forEach(
					t -> {
						List<Peak> tobjects = findObjectsInT(Integer.valueOf(channel), t);

						if (tobjects.size() > 0) objectStack.put(t, tobjects);
					}), PARALLELISM_LEVEL);

		logService.info("Time: " + DoubleRounder.round((System.currentTimeMillis() -
			starttime) / 60000, 2) + " minutes.");

		tracker = new PeakTracker(maxDifferenceX, maxDifferenceY, maxDifferenceT,
			minimumDistance, minTrajectoryLength, false, verbose, logService,
			pixelLength);

		archive = new ObjectArchive("archive.yama");

		MarsOMEMetadata marsOMEMetadata = buildOMEMetadata();

		try {
			UnitsLengthEnumHandler unitshandler = new UnitsLengthEnumHandler();
			Length pixelSize = new Length(pixelLength, UnitsLengthEnumHandler
				.getBaseUnit((UnitsLength) unitshandler.getEnumeration(pixelUnits)));

			marsOMEMetadata.getImage(0).setPixelsPhysicalSizeX(pixelSize);
			marsOMEMetadata.getImage(0).setPixelsPhysicalSizeY(pixelSize);
		}
		catch (EnumerationException e1) {
			e1.printStackTrace();
		}

		archive.putMetadata(marsOMEMetadata);

		tracker.track(objectStack, archive, Integer.valueOf(channel), processTimePoints);

		archive.naturalOrderSortMoleculeIndex();

		// Make sure the output archive has the correct name
		getInfo().getMutableOutput("archive", SingleMoleculeArchive.class).setLabel(
			archive.getName());

		if (image != null) image.setRoi(roi);

		try {
			Thread.sleep(100);
		}
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		statusService.showProgress(1, 1);

		logService.info("Finished in " + DoubleRounder.round((System
			.currentTimeMillis() - starttime) / 60000, 2) + " minutes.");
		if (archive.getNumberOfMolecules() == 0) {
			logService.info(
				"No molecules found. Maybe there is a problem with your settings");
			archive = null;
			logService.info(LogBuilder.endBlock(false));
		}
		else {
			logService.info(LogBuilder.endBlock(true));

			log += "\n" + LogBuilder.endBlock(true);
			archive.logln(log);
			archive.logln("   ");
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends RealType<T> & NativeType<T>> List<Peak> findObjectsInT(
		int channel, int t)
	{
		RandomAccessibleInterval<T> img = (swapZandT) ? MarsImageUtils
			.get2DHyperSlice((ImgPlus<T>) dataset.getImgPlus(), t, -1, -1)
			: MarsImageUtils.get2DHyperSlice((ImgPlus<T>) dataset.getImgPlus(), 0,
				channel, t);

		List<Peak> objects = new ArrayList<Peak>();
		
		Roi processingRoi = (useROI && roi != null) ? roi : new Roi(new Rectangle(0, 0, (int)dataset.dimension(0), (int)dataset.dimension(1)));

		final long[] dims = new long[img.numDimensions()];
        img.dimensions(dims);
        
        /*
         * scaleFactors = [0.5, 0.5, 1] // Reduce X and Y to 50%; leave C dimension alone.
interpolationStrategy = new NLinearInterpolatorFactory()

// crop to only one channel left
image = ij.op().run("scaleView", clown, scaleFactors, interpolationStrategy)
         */
		
		final RandomAccessibleInterval<BitType> binaryImg = (RandomAccessibleInterval<BitType>) opService.run("create.img", img, new BitType());
		
		opService.run("threshold.otsu", binaryImg, img, new HyperSphereShape(this.ostuRadius));
		
		final RandomAccessibleInterval<UnsignedShortType> indexImg = ArrayImgs.unsignedShorts(dims);
        final ImgLabeling<Integer, UnsignedShortType> labeling = new ImgLabeling<>(indexImg);

        opService.run("labeling.cca", labeling, binaryImg, StructuringElement.FOUR_CONNECTED);
		
        LabelRegions< Integer > regions = new LabelRegions< Integer >(labeling);
        Iterator< LabelRegion< Integer > > iterator = regions.iterator();
        while ( iterator.hasNext() ) {
        	LabelRegion< Integer > region = iterator.next();
        	Polygon2D poly = opService.geom().contour( region, true );
        	float[] xPoints = new float[poly.numVertices()];
        	float[] yPoints = new float[poly.numVertices()];
        	for (int i=0; i<poly.numVertices(); i++) {
        		RealLocalizable p = poly.vertex(i);
        		xPoints[i] = p.getFloatPosition(0);
        		yPoints[i] = p.getFloatPosition(1);
        	}
        	PolygonRoi r = new PolygonRoi( xPoints, yPoints, Roi.POLYGON);
        	r = new PolygonRoi(r.getInterpolatedPolygon(1, false), Roi.POLYGON);
        	r = smoothPolygonRoi(r);
        	r = new PolygonRoi(r.getInterpolatedPolygon(Math.min(2, r.getNCoordinates() * 0.1), false), Roi.POLYGON);
        	
        	double[] xs = new double[r.getFloatPolygon().xpoints.length];
        	double[] ys = new double[r.getFloatPolygon().ypoints.length];
        	for (int i=0; i< xs.length; i++) {
        		xs[i] = r.getFloatPolygon().xpoints[i];
        		ys[i] = r.getFloatPolygon().ypoints[i];
        	}
        	objects.add(PeakShape.createPeak(xs, ys));
        }

		objects = MarsImageUtils.removeNearestNeighbors(objects, minimumDistance);

		return objects;
	}
	
	private PolygonRoi smoothPolygonRoi(PolygonRoi r) {
	    FloatPolygon poly = r.getFloatPolygon();
	    FloatPolygon poly2 = new FloatPolygon();
	    int nPoints = poly.npoints;
	    for (int i = 0; i < nPoints; i += 2) {
	        int iMinus = (i + nPoints - 1) % nPoints;
	        int iPlus = (i + 1) % nPoints;
	        poly2.addPoint((poly.xpoints[iMinus] + poly.xpoints[iPlus] + poly.xpoints[i])/3,
	                (poly.ypoints[iMinus] + poly.ypoints[iPlus] + poly.ypoints[i])/3);
	    }
//				return new PolygonRoi(poly2, r.getType());
	    return new PolygonRoi(poly2, Roi.POLYGON);
	}

	private MarsOMEMetadata buildOMEMetadata() {
		ImgPlus<?> imp = dataset.getImgPlus();

		OMEXMLMetadata omexmlMetadata = null;
		if (!(imp instanceof SCIFIOImgPlus)) {
			logService.info("This image has not been opened with SCIFIO. Creating OME Metadata...");
			try {
				omexmlMetadata = MarsOMEUtils.createOMEXMLMetadata(omexmlService,
					dataset);
			} catch (ServiceException e) {
				e.printStackTrace();
			}
		}
		else {
			Metadata metadata = (Metadata) dataset.getProperties().get(
				"scifio.metadata.global");
			
			OMEMetadata omeMeta = new OMEMetadata(getContext());
			if (!translatorService.translate(metadata, omeMeta, true)) {
				logService.info(
					"Unable to extract OME Metadata. Creating...");
				try {
					omexmlMetadata = MarsOMEUtils.createOMEXMLMetadata(omexmlService,
							dataset);
				} catch (ServiceException e) {
					e.printStackTrace();
				}
			}
			else {
				omexmlMetadata = omeMeta.getRoot();
			}
		}
		
		if (swapZandT) {
			int sizeT = omexmlMetadata.getPixelsSizeT(0).getNumberValue().intValue();
			int sizeZ = omexmlMetadata.getPixelsSizeZ(0).getNumberValue().intValue();
			
			omexmlMetadata.setPixelsSizeT(new PositiveInteger(sizeZ), 0);
			omexmlMetadata.setPixelsSizeZ(new PositiveInteger(sizeT), 0);
		}

		// Ensures that MarsMicromangerFormat correctly sets the ImageID based on
		// the position.
		try {
			if (omexmlMetadata.getDoubleAnnotationCount() > 0 && omexmlMetadata
				.getDoubleAnnotationID(0).equals("ImageID"))
			{
				omexmlMetadata.setImageID("Image:" + omexmlMetadata
					.getDoubleAnnotationValue(0).intValue(), 0);
			}
		}
		catch (NullPointerException e) {
			// Do nothing. Many of the omexmlmetadata methods give
			// NullPointerExceptions
			// if fields are not set.
		}

		String metaUID;
		if (omexmlMetadata.getUUID() != null) metaUID = MarsMath.getUUID58(
			omexmlMetadata.getUUID()).substring(0, 10);
		else metaUID = MarsMath.getUUID58().substring(0, 10);

		return new MarsOMEMetadata(metaUID, omexmlMetadata);
	}

	@Override
	public void preview() {
		if (preview) {
			if (image != null) {
				image.deleteRoi();
				image.setOverlay(null);
			}

			if (swapZandT) image.setSlice(previewT + 1);
			else image.setPosition(Integer.valueOf(channel) + 1, 1, previewT + 1);

			List<Peak> peaks = findObjectsInT(Integer.valueOf(channel), previewT);

			final MutableModuleItem<String> preFrameCount = getInfo().getMutableInput(
				"tObjectCount", String.class);

			if (!peaks.isEmpty()) {

				if (previewRoiType.equals("contour")) {
					Overlay overlay = new Overlay();

					for (Peak p : peaks) {
						float[] xs = new float[p.getShape().x.length];
						float[] ys = new float[p.getShape().y.length];
			        	for (int i=0; i< xs.length; i++) {
			        		xs[i] = (float) p.getShape().x[i];
			        		ys[i] = (float) p.getShape().y[i];
			        	}
						
						PolygonRoi r = new PolygonRoi(xs, ys, Roi.POLYGON);
						overlay.add(r);
					}

					image.setOverlay(overlay);
				}
				else {
					//Overlay overlay = new Overlay();
					
					//FIX ME add area to image..
					
					//for (Peak p : peaks) {
						
						
						// The pixel origin for OvalRois is at the upper left corner !!!!
						// The pixel origin for PointRois is at the center !!!
						//final OvalRoi areaRoi = new OvalRoi(p.getDoublePosition(0) + 0.5 -
						//	fitRadius, p.getDoublePosition(1) + 0.5 - fitRadius, fitRadius *
						//		2, fitRadius * 2);
						//areaRoi.setStrokeColor(Color.CYAN.darker());

						//overlay.add(areaRoi);
					//}
					//image.setOverlay(overlay);
				}

				preFrameCount.setValue(this, "count: " + peaks.size());
			}
			else {
				preFrameCount.setValue(this, "count: 0");
			}
		}
	}

	@Override
	public void cancel() {
		if (image != null) {
			image.setOverlay(null);
			image.setRoi(roi);
		}
	}

	/** Called when the {@link #preview} parameter value changes. */
	protected void previewChanged() {
		if (!preview) cancel();
	}

	private void addInputParameterLog(LogBuilder builder) {
		if (image != null) {
			builder.addParameter("Image Title", image.getTitle());
			if (image.getOriginalFileInfo() != null && image
				.getOriginalFileInfo().directory != null)
			{
				builder.addParameter("Image Directory", image
					.getOriginalFileInfo().directory);
			}
		}
		else builder.addParameter("Dataset Name", dataset.getName());
		
		// FIX ME add missing fields and add missing getters and setters below...

		builder.addParameter("useROI", String.valueOf(useROI));
		builder.addParameter("Channel", channel);
		builder.addParameter("Local ostu radius", String.valueOf(ostuRadius));
		builder.addParameter("Minimum Distance", String.valueOf(minimumDistance));
		builder.addParameter("Verbose output", String.valueOf(verbose));
		builder.addParameter("Max difference x", String.valueOf(maxDifferenceX));
		builder.addParameter("Max difference y", String.valueOf(maxDifferenceY));
		builder.addParameter("Max difference T", String.valueOf(maxDifferenceT));
		builder.addParameter("Minimum track length", String.valueOf(
			minTrajectoryLength));
		builder.addParameter("Microscope", microscope);
		builder.addParameter("Pixel Length", String.valueOf(this.pixelLength));
		builder.addParameter("Pixel Units", this.pixelUnits);
		builder.addParameter("Exclude time points", excludeTimePointList);
		builder.addParameter("Swap Z and T", swapZandT);
	}

	// Getters and Setters
	public ObjectArchive getArchive() {
		return archive;
	}

	public void setDataset(Dataset dataset) {
		this.dataset = dataset;
	}

	public Dataset getDataset() {
		return dataset;
	}

	public void setImagePlus(ImagePlus image) {
		this.image = image;
	}

	public ImagePlus getImagePlus() {
		return image;
	}

	public void setUseROI(boolean useROI) {
		this.useROI = useROI;
	}

	public boolean getUseROI() {
		return useROI;
	}
	
	public void setRoi(Roi roi) {
		this.roi = roi;
	}
	
	public Roi getROI() {
		return this.roi;
	}

	public void setChannel(int channel) {
		this.channel = String.valueOf(channel);
	}

	public int getChannel() {
		return Integer.valueOf(channel);
	}

	public void setLocalOstuRadius(int ostuRadius) {
		this.ostuRadius = ostuRadius;
	}

	public double getLocalOstuRadius() {
		return ostuRadius;
	}

	public void setMinimumDistance(int minimumDistance) {
		this.minimumDistance = minimumDistance;
	}

	public int getMinimumDistance() {
		return minimumDistance;
	}

	public void setVerboseOutput(boolean verbose) {
		this.verbose = verbose;
	}

	public boolean getVerboseOutput() {
		return verbose;
	}

	public void setMaxDifferenceX(double PeakTracker_maxDifferenceX) {
		this.maxDifferenceX = PeakTracker_maxDifferenceX;
	}

	public double getMaxDifferenceX() {
		return maxDifferenceX;
	}

	public void setMaxDifferenceY(double maxDifferenceY) {
		this.maxDifferenceY = maxDifferenceY;
	}

	public double getMaxDifferenceY() {
		return maxDifferenceY;
	}

	public void setMaxDifferenceT(int maxDifferenceT) {
		this.maxDifferenceT = maxDifferenceT;
	}

	public int getMaxDifferenceT() {
		return maxDifferenceT;
	}

	public void setMinimumTrackLength(int minTrajectoryLength) {
		this.minTrajectoryLength = minTrajectoryLength;
	}

	public int getMinimumTrackLength() {
		return minTrajectoryLength;
	}

	public void setMicroscope(String microscope) {
		this.microscope = microscope;
	}

	public String getMicroscope() {
		return microscope;
	}

	public void setPixelLength(double pixelLength) {
		this.pixelLength = pixelLength;
	}

	public double getPixelLength() {
		return this.pixelLength;
	}

	public void setPixelUnits(String pixelUnits) {
		this.pixelUnits = pixelUnits;
	}

	public String getPixelUnits() {
		return this.pixelUnits;
	}
	
	public void setExcludedTimePointsList(String excludeTimePointList) {
		this.excludeTimePointList = excludeTimePointList;
	}
	
	public String getExcludedTimePointsList() {
		return this.excludeTimePointList;
	}
}
