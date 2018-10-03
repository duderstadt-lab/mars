package de.mpg.biochem.sdmm.kcp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

import org.scijava.log.LogService;

import de.mpg.biochem.sdmm.ImageProcessing.Peak;
import de.mpg.biochem.sdmm.molecule.Molecule;
import de.mpg.biochem.sdmm.molecule.MoleculeArchive;
import de.mpg.biochem.sdmm.table.SDMMResultsTable;
import net.imagej.table.DoubleColumn;

public class SegmentDistributionBuilder {
	//Here we use a bunch of global variables for everything
	//Would be better no to do this and instead make all the 
	//methods self contained and static
	//However, this is kind of how it was previously in the old version
	//Also the advantage now is that you load the data once
	// and can then make as many distributinos as you want on it
	
	private Random ran;
	
	private double Dstart = 0;
	private double Dend = 0;
	private int bins = 20;
	private double binWidth;
	
	private boolean filter = false;
	private double filter_region_start = 0;
	private double filter_region_stop = 100;
	
	private int bootstrap_cycles = 100;
	private boolean bootstrap_Segments = false;
	private boolean bootstrap_Molecules = false;
	
	private MoleculeArchive archive;
	private ArrayList<String> UIDs;
	private String SegmentsTableName;
	
	private LogService logService;
	
	private ForkJoinPool forkJoinPool;
	//Need to determine the number of threads
	final int PARALLELISM_LEVEL = Runtime.getRuntime().availableProcessors();
	
	public SegmentDistributionBuilder(MoleculeArchive archive, ArrayList<String> UIDs, String SegmentsTableName, double Dstart, double Dend, int bins, LogService logService) {
		this.archive = archive;
		this.UIDs = UIDs;
		this.SegmentsTableName = SegmentsTableName;
		
		this.Dstart = Dstart;
		this.Dend = Dend;
		this.bins = bins;
		
		binWidth = (Dend - Dstart)/bins;
		
		this.logService = logService;
		
		ran = new Random();
	}
	
	//METHODS FOR SETTING OR UNSETTING A RATE FILTER REGION
	public void setFilter(double filter_region_start, double filter_region_stop) {
		filter = true;
		this.filter_region_start = filter_region_start;
		this.filter_region_stop = filter_region_stop;
	}
	
	public void unsetFilter() {
		filter = false;
	}
	
	//METHODS TO ACTIVE BOOTSTRAPPING
	public void bootstrapMolecules(int bootstrap_cycles) {
		bootstrap_Segments = false;
		bootstrap_Molecules = true;
		this.bootstrap_cycles = bootstrap_cycles;
	}
	
	public void bootstrapSegments(int bootstrap_cycles) {
		bootstrap_Segments = true;
		bootstrap_Molecules = false;
		this.bootstrap_cycles = bootstrap_cycles;
	}
	
	public void noBootstrapping() {
		bootstrap_Segments = false;
		bootstrap_Molecules = false;
	}
	
	//METHODS FOR EACH DISTRIBUTION TYPE
	public SDMMResultsTable buildRateGaussian() {
		SDMMResultsTable table;
		if (bootstrap_Segments || bootstrap_Molecules) {
			 table = new SDMMResultsTable(7, bins);
		} else {
			table = new SDMMResultsTable(3, bins);
		}
		
		table.setColumnHeader(0,"Rate");
		table.setColumnHeader(1,"Probability");
		table.setColumnHeader(2,"Probability Density");
		
		ArrayList<Gaussian> gaussians = generateGaussians(UIDs);
		
		double[] distribution = generate_Gaussian_Distribution(gaussians);

		double binWidth = (Dend - Dstart)/bins;
		
		// Now lets determine the normalization constant...
		double normalization = 0;
		for (int a = 0 ; a < bins ; a++) {
			normalization += distribution[a];
		}
		double prob_den_norm = normalization*binWidth;
		
		// Now lets renormalize the distribution and go ahead and generate the table...
		for (int a = 0 ; a < bins ; a++) {
			table.setValue("Rate", a, Dstart + (a + 0.5)*binWidth);
			table.setValue("Probability", a, distribution[a]/normalization);
			table.setValue("Probability Density", a, distribution[a]/prob_den_norm);
		}
		
		//Now if we are bootstrapping we generate a series of resampled distributions and output the mean and std.
		if (bootstrap_Segments || bootstrap_Molecules) {
			ConcurrentMap<Integer, double[]> boot_distributions = new ConcurrentHashMap<>(bootstrap_cycles);
			
			forkJoinPool = new ForkJoinPool(PARALLELISM_LEVEL);
			
			try {
		        //This will spawn a bunch of threads that will generate distributions individually in parallel 
				//and put the results into the boot_distributions map
		        //keys will just be numbered from 1 to bootstrap_cycles ...
		        forkJoinPool.submit(() -> IntStream.range(0,bootstrap_cycles).parallel().forEach(q -> { 
		        	double[] bootDistribution;
					
					if (bootstrap_Molecules) {		
						ArrayList<String> resampledUIDs = new ArrayList<String>();
						for (int i=0;i<UIDs.size();i++) {
							resampledUIDs.add(UIDs.get(ran.nextInt(UIDs.size())));
						}
						//build distribution.
						bootDistribution = generate_Gaussian_Distribution(generateGaussians(resampledUIDs));
					} else {
						// bootstrap_Segments must be true...
						ArrayList<Gaussian> resampledGaussians = new ArrayList<Gaussian>();
						for (int i=0;i<gaussians.size();i++) {
							resampledGaussians.add(gaussians.get(ran.nextInt(gaussians.size())));
						}
						//build distribution.
						bootDistribution = generate_Gaussian_Distribution(resampledGaussians);
					}
					
					// Now lets determine the normalization constant...
					double norm = 0;
					for (int a = 0 ; a < bins ; a++) {
						norm += bootDistribution[a];
					}
					
					double[] new_dist = new double[bins];
					for (int a = 0 ; a < bins ; a++) {
						new_dist[a] = bootDistribution[a]/norm;
					}
					boot_distributions.put(q, new_dist);
		        })).get();

		    } catch (InterruptedException | ExecutionException e) {
		        // handle exceptions
		    	//TODO
				//logService.info(builder.endBlock(true));
		    } finally {
		        forkJoinPool.shutdown();
		    }
			
			buildBootstrapRateColumns(table, boot_distributions);
		}
		
		return table;
	}

	public SDMMResultsTable buildRateHistogram() {
		SDMMResultsTable table;
		if (bootstrap_Segments || bootstrap_Molecules) {
			 table = new SDMMResultsTable(7, bins);
		} else {
			table = new SDMMResultsTable(3, bins);
		}
		
		table.setColumnHeader(0,"Rate");
		table.setColumnHeader(1,"Probability");
		table.setColumnHeader(2,"Probability Density");
		
		ArrayList<Segment> allSegments = collectSegments(UIDs);
		
		double[] distribution = generate_Histogram_Distribution(allSegments);
		
		// Now lets determine the normalization constant...
		double normalization = 0;
		for (int a = 0 ; a < bins ; a++) {
			normalization += distribution[a];
		}
		double prob_den_norm = normalization*binWidth;
		
		// Now lets renormalize the distribution and go ahead and generate the table...
		for (int a = 0 ; a < bins ; a++) {
			table.setValue("Rate", a, Dstart + (a + 0.5)*binWidth);
			table.setValue("Probability", a, distribution[a]/normalization);
			table.setValue("Probability Density", a, distribution[a]/prob_den_norm);
		}
		
		//Now if we are bootstrapping we generate a series of resampled distributions and output the mean and std.
		if (bootstrap_Segments || bootstrap_Molecules) {
			ConcurrentMap<Integer, double[]> boot_distributions = new ConcurrentHashMap<>(bootstrap_cycles);
			
			forkJoinPool = new ForkJoinPool(PARALLELISM_LEVEL);
			
			try {
		        //This will spawn a bunch of threads that will generate distributions individually in parallel 
				//and put the results into the boot_distributions map
		        //keys will just be numbered from 1 to bootstrap_cycles ...
		        forkJoinPool.submit(() -> IntStream.range(0,bootstrap_cycles).parallel().forEach(q -> { 
		        	double[] bootDistribution;
					
					if (bootstrap_Molecules) {		
						ArrayList<String> resampledUIDs = new ArrayList<String>();
						for (int i=0;i<UIDs.size();i++) {
							resampledUIDs.add(UIDs.get(ran.nextInt(UIDs.size())));
						}
						//build distribution.
						bootDistribution = generate_Histogram_Distribution(collectSegments(resampledUIDs));
					} else {
						// bootstrap_Segments must be true...
						ArrayList<Segment> resampledSegments = new ArrayList<Segment>();
						for (int i=0;i<allSegments.size();i++) {
							resampledSegments.add(allSegments.get(ran.nextInt(allSegments.size())));
						}
						//build distribution.
						bootDistribution = generate_Histogram_Distribution(resampledSegments);
					}
					
					// Now lets determine the normalization constant...
					double norm = 0;
					for (int a = 0 ; a < bins ; a++) {
						norm += bootDistribution[a];
					}
					
					double[] new_dist = new double[bins];
					for (int a = 0 ; a < bins ; a++) {
						new_dist[a] = bootDistribution[a]/norm;
					}
					boot_distributions.put(q, new_dist);
		        })).get();

		    } catch (InterruptedException | ExecutionException e) {
		        // handle exceptions
		    	//TODO
				//logService.info(builder.endBlock(true));
		    } finally {
		        forkJoinPool.shutdown();
		    }
			
			buildBootstrapRateColumns(table, boot_distributions);
		}
		
		return table;
	}
	
	public SDMMResultsTable buildDurationHistogram() {
		return buildDurationHistogram(false, false);
	}
	
	public SDMMResultsTable buildProcessivityByMoleculeHistogram() {
		return buildDurationHistogram(false, true);
	}
	
	public SDMMResultsTable buildProcessivityByRegionHistogram() {	
		return buildDurationHistogram(true, false);
	}
	
	private SDMMResultsTable buildDurationHistogram(boolean processivityPerRegion, boolean processivityPerMolecule) {
		SDMMResultsTable table;
		if (bootstrap_Segments || bootstrap_Molecules) {
			table = new SDMMResultsTable(4, bins);
		} else {
			table = new SDMMResultsTable(2, bins);
		}
		
		table.setColumnHeader(0,"Duration");
		table.setColumnHeader(1,"Occurences");
		
		ArrayList<Segment> allSegments = collectSegments(UIDs);
		
		double[] distribution = generate_Duration_Distribution(allSegments, processivityPerRegion, processivityPerMolecule);
		
		for (int a = 0 ; a < bins ; a++) {
			table.setValue("Duration", a, Dstart + (a + 0.5)*binWidth);
			table.setValue("Occurences", a, distribution[a]);
		}
		
		//Now if we are bootstrapping we generate a series of resampled distributions and output the mean and std.
		if (bootstrap_Segments || bootstrap_Molecules) {
			ConcurrentMap<Integer, double[]> boot_distributions = new ConcurrentHashMap<>(bootstrap_cycles);
			
			forkJoinPool = new ForkJoinPool(PARALLELISM_LEVEL);
			
			try {
		        //This will spawn a bunch of threads that will generate distributions individually in parallel 
				//and put the results into the boot_distributions map
		        //keys will just be numbered from 1 to bootstrap_cycles ...
		        forkJoinPool.submit(() -> IntStream.range(0,bootstrap_cycles).parallel().forEach(q -> { 
		        	double[] bootDistribution;
					
					if (bootstrap_Molecules) {		
						ArrayList<String> resampledUIDs = new ArrayList<String>();
						for (int i=0;i<UIDs.size();i++) {
							resampledUIDs.add(UIDs.get(ran.nextInt(UIDs.size())));
						}
						//build distribution.
						bootDistribution = generate_Duration_Distribution(collectSegments(resampledUIDs), processivityPerRegion, processivityPerMolecule);
					} else {
						// bootstrap_Segments must be true...
						ArrayList<Segment> resampledSegments = new ArrayList<Segment>();
						for (int i=0;i<allSegments.size();i++) {
							resampledSegments.add(allSegments.get(ran.nextInt(allSegments.size())));
						}
						//build distribution.
						bootDistribution = generate_Duration_Distribution(resampledSegments, processivityPerRegion, processivityPerMolecule);
					}
					
					// Now lets determine the normalization constant...
					double norm = 0;
					for (int a = 0 ; a < bins ; a++) {
						norm += bootDistribution[a];
						logService.info("norm " + norm);
					}
					
					double[] new_dist = new double[bins];
					for (int a = 0 ; a < bins ; a++) {
						new_dist[a] = bootDistribution[a]/norm;
						logService.info("new_dist" + bootDistribution[a]/norm);
					}
					boot_distributions.put(q, new_dist);
		        })).get();

		    } catch (InterruptedException | ExecutionException e) {
		        // handle exceptions
		    	//TODO
				//logService.info(builder.endBlock(true));
		    } finally {
		        forkJoinPool.shutdown();
		    }
			buildBootstrapDurationColumns(table, boot_distributions);
		}
		
		return table;
	}
	
	//INTERNAL METHODS FOR BUILDING DISTRIBUTIONS
	private double[] generate_Gaussian_Distribution(ArrayList<Gaussian> Gaussians) {
		double integration_resolution = 1000;
		double[] distribution = new double[bins];
		for (int a = 0 ; a < bins ; a++) {
			//Here we should integrate to get the mean value in the bin instead of just taking the value at the center 
			for (double q = Dstart + a*binWidth; q < (Dstart + (a+1)*binWidth); q+=binWidth/integration_resolution) {
				for (int i = 0; i < Gaussians.size(); i++) {
					distribution[a] += Gaussians.get(i).getValue(q)*Gaussians.get(i).getDuration()*(binWidth/integration_resolution);
				}
			}
		}
		
		return distribution;
	}
	
	private double[] generate_Histogram_Distribution(ArrayList<Segment> allSegments) {
		double[] distribution = new double[bins];
		for (int j=0;j<distribution.length;j++)
			distribution[j] = 0;
		
		for (int a = 0 ; a < bins ; a++) {
			for (int i = 0; i < allSegments.size(); i++) {
				if (Double.isNaN(allSegments.get(i).B))
					continue;
				if (!filter || (allSegments.get(i).B > filter_region_start && allSegments.get(i).B < filter_region_stop)) {
					//We test to see if the current slope is in the current bin, which is centered at the positon on the x-axis.
					if (((Dstart + a*binWidth) < allSegments.get(i).B) && (allSegments.get(i).B <= (Dstart + (a+1)*binWidth))) {
						//If it is inside we add the number of observations of that slope minus 1 since it takes at least two frames to find the slope.
						distribution[a] += (allSegments.get(i).x2-allSegments.get(i).x1);
					}
				}
			}
		}
		return distribution;
	}
	
	private double[] generate_Duration_Distribution(ArrayList<Segment> allSegments, boolean processivityPerRegion, boolean processivityPerMolecule) {
		double[] distribution = new double[bins];
		for (int j=0;j<distribution.length;j++)
			distribution[j] = 0;
		
		ArrayList<Double> durations = new ArrayList<Double>();
		boolean wasInsideRegion = false;
		double duration = 0;
		String curUID = allSegments.get(0).getUID();
		
		if (processivityPerMolecule) {
			for (int i = 0; i < allSegments.size(); i++) {
				if (!filter || (allSegments.get(i).B > filter_region_start && allSegments.get(i).B < filter_region_stop)) {
					if (allSegments.get(i).getUID().equals(curUID)) {
						durations.add(duration);
						duration = 0;
					}
					duration += allSegments.get(i).B*(allSegments.get(i).x2 - allSegments.get(i).x1);
					curUID = allSegments.get(i).getUID();
				}
			}
			if (duration != 0)
				durations.add(duration);
		} else {
			for (int i = 0; i < allSegments.size(); i++) {
				if (!filter || (allSegments.get(i).B > filter_region_start && allSegments.get(i).B < filter_region_stop)) {
					if (allSegments.get(i).getUID().equals(curUID)) {
						if (wasInsideRegion) {
							durations.add(duration);
							duration = 0;
						}
					}
					if (processivityPerRegion)
						duration += allSegments.get(i).B*(allSegments.get(i).x2 - allSegments.get(i).x1);
					else 
						duration += allSegments.get(i).x2 - allSegments.get(i).x1;
					wasInsideRegion = true;
				} else if (wasInsideRegion) {
					durations.add(duration);
					duration = 0;
					wasInsideRegion = false;
				}
				curUID = allSegments.get(i).getUID();
			}
			if (duration != 0)
				durations.add(duration);
		}
		
		for (int a = 0 ; a < bins ; a++) {
			for (int i = 0; i < durations.size(); i++) {
				if ((Dstart + a*binWidth) <= durations.get(i) && durations.get(i) < (Dstart + (a+1)*binWidth)) {
					distribution[a]++;
				}
			}
		}
		return distribution;
	}
	
	private ArrayList<Segment> collectSegments(Collection<String> UIDset) {
		ArrayList<Segment> allSegments = new ArrayList<Segment>();
		
		//Can't do this multithreaded at the moment since we are using an arraylist
		UIDset.stream().forEach(UID -> {
			if (archive.get(UID).getSegmentsTable(SegmentsTableName) != null) {
				//Get the segments table for the current molecule
				SDMMResultsTable segments = archive.get(UID).getSegmentsTable(SegmentsTableName);
				
				for (int row = 0; row < segments.getRowCount(); row++) {
					if (Double.isNaN(segments.getValue("B", row)))
						continue;
					if (!filter || segments.getValue("B", row) > filter_region_start && segments.getValue("B", row) < filter_region_stop)
						allSegments.add(new Segment(segments, row, UID));
				}
			}
		});
		
		return allSegments;
	}
	
	private ArrayList<Gaussian> generateGaussians(Collection<String> UIDset) {
		ArrayList<Gaussian> Gaussians = new ArrayList<Gaussian>(); 
		
		//Can't do this multithreaded at the moment since we are using an arraylist
		UIDset.stream().forEach(UID -> {
			if (archive.get(UID).getSegmentsTable(SegmentsTableName) != null) {
				//Get the segments table for the current molecule
				SDMMResultsTable segments = archive.get(UID).getSegmentsTable(SegmentsTableName);
				
				for (int row = 0; row < segments.getRowCount(); row++) {
					if (Double.isNaN(segments.getValue("B", row)) || Double.isNaN(segments.getValue("sigma_B", row)))
						continue;
					if (!filter || segments.getValue("B", row) > filter_region_start && segments.getValue("B", row) < filter_region_stop)
						Gaussians.add(new Gaussian(segments.getValue("B", row), segments.getValue("sigma_B", row), segments.getValue("x2", row)-segments.getValue("x1", row)));
				}
			}
		});
		
		return Gaussians;
	}
	
	public void buildBootstrapRateColumns(SDMMResultsTable table, ConcurrentMap<Integer, double[]> boot_distributions) {
		//Now we build and add additional columns to the table.
		double[] mean_distribution = new double[bins];
		double[] std_distribution = new double[bins];
		
		for (int a=0;a<bins;a++) {
			mean_distribution[a] = 0;
		}
		
		for (int a=0;a<bins;a++) {
			for (int q=0;q<bootstrap_cycles;q++) {
				mean_distribution[a] += boot_distributions.get(q)[a];
			}
			mean_distribution[a] /= bootstrap_cycles;
		}
		
		for (int a=0;a<bins;a++) {
			double sumSquareDiffs = 0;
			for (int q=0;q<bootstrap_cycles;q++) {
				sumSquareDiffs += (boot_distributions.get(q)[a] - mean_distribution[a])*(boot_distributions.get(q)[a] - mean_distribution[a]);
			}
			std_distribution[a] = Math.sqrt(sumSquareDiffs/(bootstrap_cycles-1));
		}
		
		table.setColumnHeader(3,"Bootstrap Probability");
		table.setColumnHeader(4,"Bootstrap Probability STD");
		table.setColumnHeader(5,"Bootstrap Probability Density");
		table.setColumnHeader(6,"Bootstrap Probability Density STD");
		
		for (int a = 0 ; a < bins ; a++) {
			table.setValue("Bootstrap Probability", a, mean_distribution[a]);
			table.setValue("Bootstrap Probability STD", a, std_distribution[a]);
			table.setValue("Bootstrap Probability Density", a, mean_distribution[a]/binWidth);
			table.setValue("Bootstrap Probability Density STD", a, std_distribution[a]/binWidth);
		}
	}
	
	//Kind of duplicated from above...
	//should simplify
	public void buildBootstrapDurationColumns(SDMMResultsTable table, ConcurrentMap<Integer, double[]> boot_distributions) {
		//Now we build and add additional columns to the table.
		double[] mean_distribution = new double[bins];
		double[] std_distribution = new double[bins];
		
		for (int a=0;a<bins;a++) {
			mean_distribution[a] = 0;
		}
		
		for (int a=0;a<bins;a++) {
			for (int q=0;q<bootstrap_cycles;q++) {
				mean_distribution[a] += boot_distributions.get(q)[a];
			}
			mean_distribution[a] /= bootstrap_cycles;
		}
		
		for (int a=0;a<bins;a++) {
			double sumSquareDiffs = 0;
			for (int q=0;q<bootstrap_cycles;q++) {
				sumSquareDiffs += (boot_distributions.get(q)[a] - mean_distribution[a])*(boot_distributions.get(q)[a] - mean_distribution[a]);
			}
			std_distribution[a] = Math.sqrt(sumSquareDiffs/(bootstrap_cycles-1));
		}
		
		table.setColumnHeader(2, "Bootstrap Probability");
		table.setColumnHeader(3, "Bootstrap Probability STD");
		
		for (int a = 0 ; a < bins ; a++) {
			table.setValue("Bootstrap Probability", a, mean_distribution[a]);
			table.setValue("Bootstrap Probability STD", a, std_distribution[a]);
		}
	}
	
	
}