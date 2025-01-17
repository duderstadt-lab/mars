/*-
 * #%L
 * Molecule Archive Suite (Mars) - core data storage and processing algorithms.
 * %%
 * Copyright (C) 2018 - 2025 Karl Duderstadt
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

package de.mpg.biochem.mars.kcp.commands;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.scijava.Initializable;

import org.decimal4j.util.DoubleRounder;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.table.DoubleColumn;
import org.scijava.widget.ChoiceWidget;

import de.mpg.biochem.mars.kcp.KCP;
import de.mpg.biochem.mars.kcp.KCPSegment;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.MarsRecord;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.molecule.MoleculeArchiveService;
import de.mpg.biochem.mars.table.MarsTable;
import de.mpg.biochem.mars.util.LogBuilder;
import de.mpg.biochem.mars.util.MarsPosition;

@Plugin(type = Command.class, headless = true,
	label = "Single Change Point Finder", menu = { @Menu(
		label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT,
		mnemonic = MenuConstants.PLUGINS_MNEMONIC), @Menu(label = "Mars",
			weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = 'm'), @Menu(
				label = "KCP", weight = 4, mnemonic = 'k'), @Menu(
					label = "Single Change Point Finder", weight = 2, mnemonic = 's') })
public class SingleChangePointFinder extends DynamicCommand implements Command,
	Initializable
{

	// GENERAL SERVICES NEEDED
	@Parameter
	private LogService logService;

	@Parameter
	private MoleculeArchiveService moleculeArchiveService;

	@Parameter(callback = "archiveSelectionChanged", label = "MoleculeArchive")
	private MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;

	@Parameter(label = "X column", choices = { "a", "b", "c" })
	private String xColumn;

	@Parameter(label = "Y column", choices = { "a", "b", "c" })
	private String yColumn;

	@Parameter(label = "Analyze region")
	private boolean analyseRegion = false;

	@Parameter(label = "Region source:",
		style = ChoiceWidget.RADIO_BUTTON_HORIZONTAL_STYLE, choices = { "Molecules",
			"Metadata" })
	private String regionSource;

	@Parameter(label = "Region", required = false)
	private String regionName;

	@Parameter(label = "Fit steps (zero slope)")
	private boolean step_analysis = true;

	@Parameter(label = "Add segments table")
	private boolean addSegmentsTable = true;

	@Parameter(label = "Add position")
	private boolean addPosition = true;

	@Parameter(label = "Position")
	private String positionName = "Change Here";

	@Parameter(label = "Include:",
		style = ChoiceWidget.RADIO_BUTTON_HORIZONTAL_STYLE, choices = { "All",
			"Tagged with", "Untagged" })
	private String include;

	@Parameter(label = "Tags (comma separated list (AND))")
	private String tags = "";

	@Parameter(label = "Thread count", required = false, min = "1", max = "120")
	private int nThreads = Runtime.getRuntime().availableProcessors();

	// Global variables
	// For the progress thread
	private final AtomicBoolean progressUpdating = new AtomicBoolean(true);
	private final AtomicInteger numFinished = new AtomicInteger(0);

	// -- Callback methods --
	@SuppressWarnings("unused")
	private void archiveSelectionChanged() {
		ArrayList<String> columns = new ArrayList<>(archive.properties().getColumnSet());
		columns.sort(String::compareToIgnoreCase);

		final MutableModuleItem<String> xColumnItems = getInfo().getMutableInput(
			"xColumn", String.class);
		xColumnItems.setChoices(columns);

		final MutableModuleItem<String> yColumnItems = getInfo().getMutableInput(
			"yColumn", String.class);
		yColumnItems.setChoices(columns);
	}

	@Override
	public void initialize() {
		ArrayList<String> columns = new ArrayList<>(moleculeArchiveService.getArchives().get(0).properties()
				.getColumnSet());
		columns.sort(String::compareToIgnoreCase);

		final MutableModuleItem<String> xColumnItems = getInfo().getMutableInput(
			"xColumn", String.class);
		xColumnItems.setChoices(columns);

		final MutableModuleItem<String> yColumnItems = getInfo().getMutableInput(
			"yColumn", String.class);
		yColumnItems.setChoices(columns);
	}

	@Override
	public void run() {
		progressUpdating.set(true);
		numFinished.set(0);

		// Build log message
		LogBuilder builder = new LogBuilder();

		String log = LogBuilder.buildTitleBlock("Single Change Point Finder");

		addInputParameterLog(builder);
		log += builder.buildParameterList();
		archive.logln(log);

		// Build Collection of UIDs based on tags if they exist...
		List<String> UIDs;
		if (include.equals("Tagged with")) {
			// First we parse tags to make a list...
			String[] tagList = tags.split(",");
			for (int i = 0; i < tagList.length; i++) {
				tagList[i] = tagList[i].trim();
			}

			UIDs = archive.getMoleculeUIDs().stream().filter(UID -> {
				boolean hasTags = true;
				for (String s : tagList) {
					if (!archive.moleculeHasTag(UID, s)) {
						hasTags = false;
						break;
					}
				}
				return hasTags;
			}).collect(toList());
		}
		else if (include.equals("Untagged")) {
			UIDs = archive.getMoleculeUIDs().stream().filter(
				UID -> archive.get(UID).hasNoTags()).collect(toList());
		}
		else {
			// we include All molecules...
			UIDs = archive.getMoleculeUIDs();
		}

		ForkJoinPool forkJoinPool = new ForkJoinPool(nThreads);

		// Output first part of log message...
		logService.info(log);

		double startTime = System.currentTimeMillis();
		logService.info("Finding Single Change Points...");
		archive.getWindow().updateLockMessage("Finding Single Change Points...");
		try {
			// Start a thread to keep track of the progress of the number of frames
			// that have been processed.
			// Waiting call back to update the progress bar!!
			Thread progressThread = new Thread() {

				public synchronized void run() {
					try {
						while (progressUpdating.get()) {
							Thread.sleep(100);
							archive.getWindow().setProgress(numFinished.doubleValue() / UIDs
								.size());
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			};

			progressThread.start();

			// This will spawn a bunch of threads that will analyze molecules
			// individually in parallel and put the change point tables back
			// into the same molecule record.
			forkJoinPool.submit(() -> UIDs.parallelStream().forEach(i -> {
				Molecule molecule = archive.get(i);

				if (molecule.getTable().hasColumn(xColumn) && molecule.getTable()
					.hasColumn(yColumn))
				{
					findChangePoints(molecule);
					archive.put(molecule);
				}
			})).get();

			progressUpdating.set(false);

			archive.getWindow().setProgress(1);

		}
		catch (InterruptedException | ExecutionException e) {
			// handle exceptions
			logService.error(e.getMessage());
			e.printStackTrace();
			logService.info(LogBuilder.endBlock(false));
			forkJoinPool.shutdown();
			return;
		}
		finally {
			forkJoinPool.shutdown();
		}

		logService.info("Time: " + DoubleRounder.round((System.currentTimeMillis() -
			startTime) / 60000, 2) + " minutes.");
		logService.info(LogBuilder.endBlock(true));
		archive.logln(LogBuilder.endBlock(true));
	}

	private void findChangePoints(Molecule molecule) {
		MarsTable table = molecule.getTable();
		MarsRecord regionRecord = (regionSource.equals("Molecules")) ? molecule : archive.getMetadata(molecule.getMetadataUID());

		// START NaN FIX
		ArrayList<Double> xDataSafe = new ArrayList<>();
		ArrayList<Double> yDataSafe = new ArrayList<>();
		for (int i = 0; i < table.getRowCount(); i++) {
			if (!Double.isNaN(table.getValue(xColumn, i)) && !Double.isNaN(
				table.getValue(yColumn, i)))
			{
				xDataSafe.add(table.getValue(xColumn, i));
				yDataSafe.add(table.getValue(yColumn, i));
			}
		}

		int rowCount = xDataSafe.size();

		int offset = 0;
		int length = rowCount;

		double[] xData = new double[rowCount];
		double[] yData = new double[rowCount];
		for (int i = 0; i < rowCount; i++) {
			xData[i] = xDataSafe.get(i);
			yData[i] = yDataSafe.get(i);
		}
		// END FIX

		for (int j = 0; j < rowCount; j++) {
			if (analyseRegion) {
				if (regionRecord.hasRegion(regionName) && xData[j] <= regionRecord
					.getRegion(regionName).getStart())
				{
					offset = j;
				}
				else if (regionRecord.hasRegion(regionName) && xData[j] <= regionRecord
					.getRegion(regionName).getEnd())
				{
					length = j - offset;
				}
			}
		}

		if (length == 0) {
			// When length is zero we add a single dummy row with all NaN values.
			ArrayList<KCPSegment> segments = new ArrayList<>();
			KCPSegment segment = new KCPSegment(Double.NaN, Double.NaN, Double.NaN,
				Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
			segments.add(segment);
			molecule.putSegmentsTable(xColumn, yColumn, buildSegmentTable(segments));
			numFinished.incrementAndGet();
			return;
		}

		double[] xRegion = Arrays.copyOfRange(xData, offset, offset + length);
		double[] yRegion = Arrays.copyOfRange(yData, offset, offset + length);

		int llr_max_row = changePoint(xRegion, yRegion);

		if (llr_max_row == -1) return;

		if (addPosition) {
			double stroke = 1;
			MarsPosition position = new MarsPosition(positionName, xColumn,
				xRegion[llr_max_row], "black", stroke);
			molecule.putPosition(position);
		}

		if (addSegmentsTable) {
			ArrayList<Integer> cp_positions = new ArrayList<>();
			cp_positions.add(0);
			cp_positions.add(llr_max_row);
			cp_positions.add(xRegion.length - 1);
			MarsTable segmentsTable = KCP.generate_segments(xRegion, yRegion,
				cp_positions, step_analysis);
			if (analyseRegion) molecule.putSegmentsTable(xColumn, yColumn, regionName,
				segmentsTable);
			else molecule.putSegmentsTable(xColumn, yColumn, segmentsTable);
		}

		archive.put(molecule);

		numFinished.incrementAndGet();
	}

	// returns the change-point position if one is found. Otherwise, returns -1;
	private int changePoint(double[] xData, double[] yData)
	{
		// First we determine the fit for the null hypothesis.
		double[] null_line = linearRegression(xData, yData, 0, xData.length);
		double null_ll = log_likelihood(xData, yData, 0, xData.length, null_line[0],
			null_line[2]);

		// current max log-likelihood ratio value and position.
		double llr_max = 0;
		int llr_max_position = -1;

		// Next we determine the fit for each pair of lines and store the
		// log-likelihood
		for (int w = 2; w < xData.length - 2; w++) {
			// linear fit for first half
			double[] segA_line = linearRegression(xData, yData, 0, w);
			// linear fit for second half
			double[] segB_line = linearRegression(xData, yData, w, xData.length -
				w);

			double ll_ratio = log_likelihood(xData, yData, 0, w, segA_line[0],
				segA_line[2]) + log_likelihood(xData, yData, w, xData.length - w,
					segB_line[0], segB_line[2]) - null_ll;

			if (ll_ratio > llr_max) {
				llr_max = ll_ratio;
				llr_max_position = w;
			}
		}

		return llr_max_position;
	}

	// Equations and notation taken directly from "An Introduction to Error
	// Analysis" by Taylor 2nd edition
	// y = A + Bx
	// A = output[0] +/- output[1]
	// B = output[2] +/- output[3]
	// error is the STD here.
	private double[] linearRegression(double[] xData, double[] yData, int offset,
		int length)
	{

		double[] output = new double[4];

		if (step_analysis) {
			double yAverage = 0;
			for (int i = offset; i < offset + length; i++) {
				yAverage += yData[i];
			}
			yAverage = yAverage / length;
			double yDiffSquares = 0;
			for (int i = offset; i < offset + length; i++) {
				yDiffSquares += (yAverage - yData[i]) * (yAverage - yData[i]);
			}

			output[0] = yAverage;
			output[1] = Math.sqrt(yDiffSquares / (length - 1));
			output[2] = 0;
			output[3] = 0;
		}
		else {
			// First we determine delta (Taylor's notation)
			double xSumSquares = 0;
			double xSum = 0;
			double ySum = 0;
			double xySum = 0;
			for (int i = offset; i < offset + length; i++) {
				xSumSquares += xData[i] * xData[i];
				xSum += xData[i];
				ySum += yData[i];
				xySum += xData[i] * yData[i];
			}
			double Delta = length * xSumSquares - xSum * xSum;
			double A = (xSumSquares * ySum - xSum * xySum) / Delta;
			double B = (length * xySum - xSum * ySum) / Delta;

			double ymAmBxSquare = 0;
			for (int i = offset; i < offset + length; i++) {
				ymAmBxSquare += (yData[i] - A - B * xData[i]) * (yData[i] - A - B *
					xData[i]);
			}
			double sigmaY = Math.sqrt(ymAmBxSquare / (length - 2));

			output[0] = A;
			output[1] = sigmaY * Math.sqrt(xSumSquares / Delta);
			output[2] = B;
			output[3] = sigmaY * Math.sqrt(length / Delta);
		}

		return output;
	}

	private double log_likelihood(double[] xData, double[] yData, int offset,
		int length, double A, double B)
	{
		// B is the slope and A is the intercept of the line.
		double lineSum = 0;
		for (int i = offset; i < offset + length; i++) {
			lineSum += (yData[i] - B * xData[i] - A) * (yData[i] - B * xData[i] - A);
		}

		return length * Math.log(1 / Math.sqrt(2 * Math.PI)) - lineSum / (2.0);
	}

	private MarsTable buildSegmentTable(ArrayList<KCPSegment> segments) {
		MarsTable output = new MarsTable();

		output.add(new DoubleColumn(KCPSegment.X1));
		output.add(new DoubleColumn(KCPSegment.Y1));
		output.add(new DoubleColumn(KCPSegment.X2));
		output.add(new DoubleColumn(KCPSegment.Y2));
		output.add(new DoubleColumn(KCPSegment.A));
		output.add(new DoubleColumn(KCPSegment.SIGMA_A));
		output.add(new DoubleColumn(KCPSegment.B));
		output.add(new DoubleColumn(KCPSegment.SIGMA_B));

		int row = 0;
		for (KCPSegment seg : segments) {
			output.appendRow();
			output.setValue(KCPSegment.X1, row, seg.x1);
			output.setValue(KCPSegment.Y1, row, seg.y1);
			output.setValue(KCPSegment.X2, row, seg.x2);
			output.setValue(KCPSegment.Y2, row, seg.y2);
			output.setValue(KCPSegment.A, row, seg.a);
			output.setValue(KCPSegment.SIGMA_A, row, seg.sigma_a);
			output.setValue(KCPSegment.B, row, seg.b);
			output.setValue(KCPSegment.SIGMA_B, row, seg.sigma_b);
			row++;
		}
		return output;
	}

	private void addInputParameterLog(LogBuilder builder) {
		builder.addParameter("MoleculeArchive", archive.getName());
		builder.addParameter("X column", xColumn);
		builder.addParameter("Y column", yColumn);
		builder.addParameter("Analyze region", String.valueOf(analyseRegion));
		builder.addParameter("Region source", regionSource);
		builder.addParameter("Region", regionName);
		builder.addParameter("Fit steps (zero slope)", String.valueOf(
			step_analysis));
		builder.addParameter("Add segments table", String.valueOf(
			addSegmentsTable));
		builder.addParameter("Add position", String.valueOf(addPosition));
		builder.addParameter("Position", positionName);
		builder.addParameter("Include tags", include);
		builder.addParameter("Tags", tags);
	}

	public void setArchive(
		MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive)
	{
		this.archive = archive;
	}

	public
		MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>>
		getArchive()
	{
		return archive;
	}

	public void setXColumn(String xColumn) {
		this.xColumn = xColumn;
	}

	public String getXColumn() {
		return xColumn;
	}

	public void setYColumn(String yColumn) {
		this.yColumn = yColumn;
	}

	public String getYColumn() {
		return yColumn;
	}

	public void setAnalyzeRegion(boolean analyseRegion) {
		this.analyseRegion = analyseRegion;
	}

	public boolean getAnalyzeRegion() {
		return analyseRegion;
	}

	public void setRegionSource(String regionSource) {
		this.regionSource = regionSource;
	}

	public String getRegionSource() {
		return regionSource;
	}

	public void setRegion(String regionName) {
		this.regionName = regionName;
	}

	public String getRegion() {
		return regionName;
	}

	public void setAddSegmentsTable(boolean addSegmentsTable) {
		this.addSegmentsTable = addSegmentsTable;
	}

	public boolean getAddSegmentsTable() {
		return addSegmentsTable;
	}

	public void setAddPosition(boolean addPosition) {
		this.addPosition = addPosition;
	}

	public boolean getAddPosition() {
		return addPosition;
	}

	public void setPosition(String positionName) {
		this.positionName = positionName;
	}

	public String getPosition() {
		return positionName;
	}

	public void setIncludeTags(String include) {
		this.include = include;
	}

	public String getIncludeTags() {
		return include;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public String getTags() {
		return tags;
	}

	public void setFitSteps(boolean step_analysis) {
		this.step_analysis = step_analysis;
	}

	public boolean getFitSteps() {
		return step_analysis;
	}

	public void setThreads(int nThreads) {
		this.nThreads = nThreads;
	}

	public int getThreads() {
		return this.nThreads;
	}
}
