package de.mpg.biochem.mars.kcp.commands;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.decimal4j.util.DoubleRounder;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.table.DoubleColumn;
import org.scijava.ui.UIService;
import org.scijava.widget.ChoiceWidget;

import de.mpg.biochem.mars.kcp.KCP;
import de.mpg.biochem.mars.kcp.Segment;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.MarsRecord;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.molecule.MoleculeArchiveService;
import de.mpg.biochem.mars.table.MarsTable;
import de.mpg.biochem.mars.util.LogBuilder;
import de.mpg.biochem.mars.util.MarsPosition;
import net.imagej.ops.Initializable;

@Plugin(type = Command.class, headless = true, label = "Single Change Point Finder", menu = {
		@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT,
				mnemonic = MenuConstants.PLUGINS_MNEMONIC),
		@Menu(label = "MoleculeArchive Suite", weight = MenuConstants.PLUGINS_WEIGHT,
			mnemonic = 's'),
		@Menu(label = "KCP", weight = 40,
			mnemonic = 'k'),
		@Menu(label = "Single Change Point Finder", weight = 1, mnemonic = 's')})
public class SingleChangePointFinder extends DynamicCommand implements Command, Initializable {
	//GENERAL SERVICES NEEDED	
		@Parameter
		private LogService logService;
		
	    @Parameter
	    private StatusService statusService;
		
		@Parameter
	    private MoleculeArchiveService moleculeArchiveService;
		
		@Parameter
	    private UIService uiService;
	    
	    @Parameter(label="MoleculeArchive")
	  	private MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive;
	    
	    @Parameter(label="X Column", choices = {"a", "b", "c"})
		private String Xcolumn;
	    
	    @Parameter(label="Y Column", choices = {"a", "b", "c"})
		private String Ycolumn;
	    
	    //@Parameter(label="Confidence value")
		private double confidenceLevel = 0.99;
	    
	    //@Parameter(label="Global sigma")
		private double global_sigma = 1;
	    
	    //@Parameter(label = "Region source:",
		//		style = ChoiceWidget.RADIO_BUTTON_HORIZONTAL_STYLE, choices = { "Molecules",
		//				"Metadata" })
		private String regionSource;
	    
	    //@Parameter(label="Calculate from background")
	    //private boolean calcBackgroundSigma = true;
	    
	    //@Parameter(label="Background region", required=false)
		//private String backgroundRegion;
	    
	    //@Parameter(label="Analyze region")
	    private boolean region = false;
	    
	    //@Parameter(label="Region", required=false)
		private String regionName;
	    
	    //@Parameter(label="Fit steps (zero slope)")
		private boolean step_analysis = true;
	    
	    @Parameter(label="Position Name")
		private String positionName = "Change Here";
	    
		@Parameter(label = "Include:",
				style = ChoiceWidget.RADIO_BUTTON_HORIZONTAL_STYLE, choices = { "All",
						"Tagged with", "Untagged" })
		private String include;
		
		@Parameter(label="Tags (comma separated list)")
		private String tags = "";
	    
	    //Global variables
	    //For the progress thread
	  	private final AtomicBoolean progressUpdating = new AtomicBoolean(true);
	  	private final AtomicInteger numFinished = new AtomicInteger(0);
		
	    @Override
		public void initialize() {
			final MutableModuleItem<String> XcolumnItems = getInfo().getMutableInput("Xcolumn", String.class);
			XcolumnItems.setChoices(moleculeArchiveService.getColumnNames());
			
			final MutableModuleItem<String> YcolumnItems = getInfo().getMutableInput("Ycolumn", String.class);
			YcolumnItems.setChoices(moleculeArchiveService.getColumnNames());
		}
		@Override
		public void run() {		
			//Lock the window so it can't be changed while processing
			if (!uiService.isHeadless())
				archive.getWindow().lock();
			
			//Build log message
			LogBuilder builder = new LogBuilder();
			
			String log = LogBuilder.buildTitleBlock("Single Change Point Finder");
			
			addInputParameterLog(builder);
			log += builder.buildParameterList();
			archive.addLogMessage(log);
			
			//Build Collection of UIDs based on tags if they exist...
	        ArrayList<String> UIDs;
			if (include.equals("Tagged with")) {
				//First we parse tags to make a list...
		        String[] tagList = tags.split(",");
		        for (int i=0; i<tagList.length; i++) {
		        	tagList[i] = tagList[i].trim();
		        }
				
				UIDs = (ArrayList<String>)archive.getMoleculeUIDs().stream().filter(UID -> {
					boolean hasTags = true;
					for (int i=0; i<tagList.length; i++) {
						if (!archive.moleculeHasTag(UID, tagList[i])) {
							hasTags = false;
							break;
						}
					}
					return hasTags;
				}).collect(toList());
			} else if (include.equals("Untagged")) {
				UIDs = (ArrayList<String>)archive.getMoleculeUIDs().stream().filter(UID -> archive.get(UID).hasNoTags()).collect(toList());
			} else {
				//  we include All molecules...
				UIDs = archive.getMoleculeUIDs();
			}
			
			//Let's build a thread pool and in a multithreaded manner perform changepoint analysis on all molecules
			//Need to determine the number of threads
			final int PARALLELISM_LEVEL = Runtime.getRuntime().availableProcessors();
			
			ForkJoinPool forkJoinPool = new ForkJoinPool(PARALLELISM_LEVEL);
			
			//Output first part of log message...
			logService.info(log);
			
			double starttime = System.currentTimeMillis();
			logService.info("Finding Single Change Points...");
			archive.getWindow().updateLockMessage("Finding Single Change Points...");
		    try {
		    	//Start a thread to keep track of the progress of the number of frames that have been processed.
		    	//Waiting call back to update the progress bar!!
		    	Thread progressThread = new Thread() {
		            public synchronized void run() {
	                    try {
	        		        while(progressUpdating.get()) {
	        		        	Thread.sleep(100);
	        		        	statusService.showStatus(numFinished.intValue(), UIDs.size(), "Finding Single Change Points for " + archive.getName());
	        		        }
	                    } catch (Exception e) {
	                        e.printStackTrace();
	                    }
		            }
		        };

		        progressThread.start();
		    	
		        //This will spawn a bunch of threads that will analyze molecules individually in parallel 
		        //and put the changepoint tables back into the same molecule record
		        
		        forkJoinPool.submit(() -> UIDs.parallelStream().forEach(i -> {
		        		Molecule molecule = archive.get(i);
		        		
		        		if (molecule.getDataTable().hasColumn(Xcolumn) && molecule.getDataTable().hasColumn(Ycolumn)) {
		        			findChangePoints(molecule);
		        			archive.put(molecule);
		        		}
		        })).get();
		        
		        progressUpdating.set(false);
		        
		        statusService.showStatus(1, 1, "Change point search for archive " + archive.getName() + " - Done!");
		        
		    } catch (InterruptedException | ExecutionException e) {
		        // handle exceptions
		    	logService.error(e.getMessage());
		    	e.printStackTrace();
				logService.info(LogBuilder.endBlock(false));
				forkJoinPool.shutdown();
				return;
		    } finally {
		        forkJoinPool.shutdown();
		    }
			
		    logService.info("Time: " + DoubleRounder.round((System.currentTimeMillis() - starttime)/60000, 2) + " minutes.");
		    logService.info(LogBuilder.endBlock(true));
		    archive.addLogMessage(LogBuilder.endBlock(true));
		    
			//Unlock the window so it can be changed
		    if (!uiService.isHeadless())
		    	archive.unlock();

		}
		
		private void findChangePoints(Molecule molecule) {
			MarsTable datatable = molecule.getDataTable();
			
			MarsRecord regionRecord = null;
			if (region) {
				if (regionSource.equals("Molecules")) {
					regionRecord = molecule;
				} else {
					regionRecord = archive.getImageMetadata(molecule.getImageMetadataUID());
				}
				
				if (!regionRecord.hasRegion(regionName))
					return;
			}
			
			//START NaN FIX
			ArrayList<Double> xDataSafe = new ArrayList<Double>();
			ArrayList<Double> yDataSafe = new ArrayList<Double>();
			for (int i=0;i<datatable.getRowCount();i++) {
				if (!Double.isNaN(datatable.getValue(Xcolumn, i)) && !Double.isNaN(datatable.getValue(Ycolumn, i))) {
					xDataSafe.add(datatable.getValue(Xcolumn, i));
					yDataSafe.add(datatable.getValue(Ycolumn, i));
				}
			}
			
			int rowCount = xDataSafe.size();
			
			int offset = 0;
			int length = rowCount;
			
			double[] xData = new double[rowCount];
			double[] yData = new double[rowCount];
			for (int i=0;i<rowCount;i++) {
				xData[i] = xDataSafe.get(i);
				yData[i] = yDataSafe.get(i);
			}
			//END FIX
			
			for (int j=0;j<rowCount;j++) {
				if (region) {
					if (regionRecord.hasRegion(regionName) && xData[j] <= regionRecord.getRegion(regionName).getStart()) {
						offset = j;
					} else if (regionRecord.hasRegion(regionName) && xData[j] <= regionRecord.getRegion(regionName).getEnd()) {
						length = j - offset;
					}
				}
				/*	
				if (calcBackgroundSigma) {
					if (regionRecord.hasRegion(backgroundRegion) && xData[j] <= regionRecord.getRegion(backgroundRegion).getStart()) {
						SigXstart = j;
					} else if (regionRecord.hasRegion(backgroundRegion) && xData[j] <= regionRecord.getRegion(backgroundRegion).getEnd()) {
						SigXend = j;
					}
				}
				*/
			}
			
			if (length == 0) {
				return;
				
				//This means the region probably doesn't exist...
				//So we just add a single dummy row with All NaN values...
				//Then we return...
				//ArrayList<Segment> segs = new ArrayList<Segment>();
				//Segment segment = new Segment(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
				//segs.add(segment);
				//molecule.putSegmentsTable(Xcolumn, Ycolumn, buildSegmentTable(segs));
				//numFinished.incrementAndGet();
				//return;
			}
			
			//Use global sigma or use local sigma or calculate sigma (in this order of priority)
			//double sigma = global_sigma;
			/*
			if (molecule.hasParameter(Ycolumn + "_sigma")) {
				sigma = molecule.getParameter(Ycolumn + "_sigma");
			} else if (molecule.hasRegion(backgroundRegion)) {
				if (calcBackgroundSigma)
					sigma = KCP.calc_sigma(yData, SigXstart, SigXend);
			}
			*/
			
			//double[] xRegion = Arrays.copyOfRange(xData, offset, offset + length);
			//double[] yRegion = Arrays.copyOfRange(yData, offset, offset + length);
	
			int llr_max_row = changePoint(xData, yData, 0, xData.length);
			
			if (llr_max_row == -1)
				return;
			
			//MarsPosition(String name, String column, double position, String color, double stroke)
			double stroke = 1;
			MarsPosition position = new MarsPosition(positionName, Xcolumn, xData[llr_max_row], "black", stroke);
			molecule.putPosition(position);
			/*
			KCP change = new KCP(sigma, confidenceLevel, xRegion, yRegion, step_analysis);
			try {
				MarsTable segmentsTable = buildSegmentTable(change.generate_segments());
				if (region)
					molecule.putSegmentsTable(Xcolumn, Ycolumn, regionName, segmentsTable);
				else
					molecule.putSegmentsTable(Xcolumn, Ycolumn, segmentsTable);
			} catch (ArrayIndexOutOfBoundsException e) {
				logService.error("Out of Bounds Exception");
				logService.error("UID " + molecule.getUID() + " gave an error ");
				logService.error("sigma " + sigma);
				logService.error("confidenceLevel " + confidenceLevel);
				logService.error("offset " + offset);
				logService.error("length " + length);
				e.printStackTrace();
			}
			*/
			archive.put(molecule);
			
			numFinished.incrementAndGet();
		}
		
		// returns the change-point position if one is found.  Otherwise returns -1;
		private int changePoint(double[] xData, double[] yData, int offset, int length) {
			//Here we store the llr curve for the movie
			ArrayList<Double> cur_Y_llr = new ArrayList<Double>();
			ArrayList<Double> cur_X_llr = new ArrayList<Double>();
			
			//First we determine the fit for the null hypothesis...
			double[] null_line = linearRegression(xData, yData, offset, length);
			double null_ll = log_likelihood(xData, yData, offset, length, null_line[0], null_line[2]);
			
			//current max log-likelihood ratio value and position.
			double llr_max = 0;
			int llr_max_position = -1; 
			
			// Next we determine the fit for each pair of lines and store the log-likelihood
			for (int w=2; w < length - 2 ; w++) {
				//linear fit for first half
				double[] segA_line = linearRegression(xData, yData, offset, w);
				//linear fit for second half
				double[] segB_line = linearRegression(xData, yData, offset + w, length - w);
				
				double ll_ratio = log_likelihood(xData, yData, offset, w, segA_line[0], segA_line[2]) + log_likelihood(xData, yData, offset + w, length - w, segB_line[0], segB_line[2]) - null_ll;
				
				cur_Y_llr.add(ll_ratio);
				cur_X_llr.add(xData[offset + w]);
				
				if (ll_ratio > llr_max) {
					llr_max = ll_ratio;
					llr_max_position = offset + w;
				}
			}
			
			return llr_max_position;
		}
		
		// Equations and notation taken directly from "An Introduction to Error Analysis" by Taylor 2nd edition
		// y = A + Bx
		// A = output[0] +/- output[1]
		// B = output[2] +/- output[3]
		// error is the STD here.
		private double[] linearRegression(double[] xData, double[] yData, int offset, int length) {
			
			double[] output = new double[4];

			if (step_analysis) {
				double Yaverage = 0;
				for (int i = offset; i< offset + length; i++) {
					Yaverage += yData[i];
				}
				Yaverage = Yaverage / length;
				double Ydiffsquares = 0;
				for (int i = offset; i< offset + length; i++) {
					Ydiffsquares += (Yaverage - yData[i])*(Yaverage - yData[i]);
				}
				
				output[0] = Yaverage;
				output[1] = Math.sqrt(Ydiffsquares/(length-1));		
				output[2] = 0;
				output[3] = 0;
			} else {
				//First we determine delta (Taylor's notation)
				double XsumSquares = 0;
				double Xsum = 0;
				double Ysum = 0;
				double XYsum = 0;
				for (int i = offset; i< offset + length; i++) {
					XsumSquares += xData[i]*xData[i];
					Xsum += xData[i];
					Ysum += yData[i];
					XYsum += xData[i]*yData[i];
				}
				double Delta = length*XsumSquares-Xsum*Xsum;
				double A = (XsumSquares*Ysum-Xsum*XYsum)/Delta;
				double B = (length*XYsum-Xsum*Ysum)/Delta;
				
				double ymAmBxSquare = 0;
				for (int i = offset; i < offset + length; i++) {
					ymAmBxSquare += (yData[i]-A-B*xData[i])*(yData[i]-A-B*xData[i]);
				}
				double sigmaY = Math.sqrt(ymAmBxSquare/(length-2));
				
				output[0] = A;
				output[1] = sigmaY*Math.sqrt(XsumSquares/Delta);		
				output[2] = B;
				output[3] = sigmaY*Math.sqrt(length/Delta);
			}
			
			return output;
		}
		
		private double log_likelihood(double[] xData, double[] yData, int offset, int length, double A, double B) {
			// B is the slope and A is the intercept of the line.
			double lineSum = 0;
			for (int i = offset; i < offset + length ; i++ ) {
				lineSum += (yData[i]-B*xData[i]-A)*(yData[i]-B*xData[i]-A);
			}
			
			//I guess these pi terms cancel below so I could remove them but I will leave it for now...
			return length*Math.log(1/global_sigma*Math.sqrt(2*Math.PI))-lineSum/(2*global_sigma*global_sigma);
		}
		
		private MarsTable buildSegmentTable(ArrayList<Segment> segments) {
			MarsTable output = new MarsTable();
			
			//Do i need to add these columns first? I can't remember...
			output.add(new DoubleColumn("x1"));
			output.add(new DoubleColumn("y1"));
			output.add(new DoubleColumn("x2"));
			output.add(new DoubleColumn("y2"));
			output.add(new DoubleColumn("A"));
			output.add(new DoubleColumn("sigma_A"));
			output.add(new DoubleColumn("B"));
			output.add(new DoubleColumn("sigma_B"));
			
			int row = 0;
			for (Segment seg : segments) {
				output.appendRow();
				output.setValue("x1", row, seg.x1);
				output.setValue("y1", row, seg.y1);
				output.setValue("x2", row, seg.x2);
				output.setValue("y2", row, seg.y2);
				output.setValue("A", row, seg.A);
				output.setValue("sigma_A", row, seg.A_sigma);
				output.setValue("B", row, seg.B);
				output.setValue("sigma_B", row, seg.B_sigma);
				row++;
			}
			return output;
		}
		
		private void addInputParameterLog(LogBuilder builder) {
			builder.addParameter("MoleculeArchive", archive.getName());
			builder.addParameter("X Column", Xcolumn);
			builder.addParameter("Y Column", Ycolumn);
			//builder.addParameter("Confidence value", String.valueOf(confidenceLevel));
			//builder.addParameter("Global sigma", String.valueOf(global_sigma));
			//builder.addParameter("Fit steps (zero slope)", String.valueOf(step_analysis));
		}
		
		public void setArchive(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive) {
			this.archive = archive;
		}
		
		public MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> getArchive() {
			return archive;
		}
		
		public void setXcolumn(String Xcolumn) {
			this.Xcolumn = Xcolumn;
		}
		
		public String getXcolumn() {
			return Xcolumn;
		}
	    
		public void setYcolumn(String Ycolumn) {
			this.Ycolumn = Ycolumn;
		}
		
		public String getYcolumn() {
			return Ycolumn;
		}
		
		public void setConfidenceLevel(double confidenceLevel) {
			this.confidenceLevel = confidenceLevel;
		}
		
		public double getConfidenceLevel() {
			return confidenceLevel;
		}
		    
		public void setGlobalSigma(double global_sigma) {
			this.global_sigma = global_sigma;
		}
		
		public double getGlobalSigma() {
			return global_sigma;
		}
		
		public void setFitSteps(boolean step_analysis) {
			this.step_analysis = step_analysis;
		}
		    
		public boolean getFitSteps() {
			return step_analysis;
		}
}