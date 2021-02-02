/*-
 * #%L
 * Molecule Archive Suite (Mars) - core data storage and processing algorithms.
 * %%
 * Copyright (C) 2018 - 2021 Karl Duderstadt
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

package de.mpg.biochem.mars.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.ops.OpService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.scijava.Context;
import org.scijava.app.StatusService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

import de.mpg.biochem.mars.image.commands.PeakFinderCommand;
import de.mpg.biochem.mars.table.MarsTable;
import de.mpg.biochem.mars.table.MarsTableService;
import de.mpg.biochem.mars.util.Gaussian2D;

public class PeakFinderCommandTest {

	private static final double TOLERANCE = 0.01;

	@Parameter
	protected Context context;

	@Parameter
	protected DatasetService datasetService;

	@Parameter
	protected LogService logService;

	protected Context createContext() {
		return new Context(DatasetService.class, StatusService.class,
			OpService.class, MarsTableService.class);
	}

	@BeforeEach
	public void setUp() {
		createContext().inject(this);
	}

	@AfterEach
	public synchronized void cleanUp() {
		if (context != null) {
			context.dispose();
			context = null;
			datasetService = null;
			logService = null;
		}
	}

	@Test
	void findPeaksCommand() {
		final PeakFinderCommand peakFinder = new PeakFinderCommand();

		peakFinder.setContext(context);

		peakFinder.setDataset(simulateDataset());
		peakFinder.setUseRoi(false);
		peakFinder.setChannel(0);
		peakFinder.setT(0);
		peakFinder.setUseDogFiler(true);
		peakFinder.setDogFilterRadius(1.8d);
		peakFinder.setThreshold(50);
		peakFinder.setMinimumDistance(4);
		peakFinder.setFindNegativePeaks(false);
		peakFinder.setGeneratePeakCountTable(false);
		peakFinder.setGeneratePeakTable(true);
		peakFinder.setMinimumRsquared(0);
		peakFinder.setAddToRoiManager(false);
		peakFinder.setProcessAllFrames(true);
		peakFinder.setFitPeaks(true);
		peakFinder.setFitRadius(4);
		peakFinder.setIntegrate(true);
		peakFinder.setIntegrationInnerRadius(1);
		peakFinder.setIntegrationOuterRadius(3);

		// Run the Command
		peakFinder.run();

		// Retrieve output from the command
		// peakCountTable = peakFinder.getPeakCountTable()

		MarsTable peakTable = peakFinder.getPeakTable();

		assertEquals(peakTable.getValue("T", 0), 0);
		assertTrue(Math.abs(10 - peakTable.getValue("x", 0)) < TOLERANCE,
			"Peak x position is off by more than the tolerance. Should be 10 was " +
				peakTable.getValue("x", 0));
		assertTrue(Math.abs(10 - peakTable.getValue("y", 0)) < TOLERANCE,
			"Peak y position is off by more than the tolerance. Should be 10 was " +
				peakTable.getValue("y", 0));
		assertTrue(Math.abs(15789.0 - peakTable.getValue("intensity",
			0)) < TOLERANCE,
			"Peak intensity is off by more than the tolerance. Should be 15789 was " +
				peakTable.getValue("intensity", 0));
		assertTrue(Math.abs(3500.0 - peakTable.getValue("baseline", 0)) < 1,
			"Peak baseline is off by more than the 1. Should be 3500 was " + peakTable
				.getValue("baseline", 0));
		assertTrue(Math.abs(3000.0 - peakTable.getValue("height", 0)) < 1,
			"Peak height is off by more than the 1. Should be 3000 was " + peakTable
				.getValue("height", 0));
		assertTrue(Math.abs(1.2 - peakTable.getValue("sigma", 0)) < TOLERANCE,
			"Peak sigma is off by more than the 10. Should be 3000 was " + peakTable
				.getValue("sigma", 0));
		assertTrue(Math.abs(1 - peakTable.getValue("R2", 0)) < TOLERANCE,
			"Peak R2 is off by more than the tolerance. Should be 1 was " + peakTable
				.getValue("sigma", 0));

		assertEquals(peakTable.getValue("T", 1), 0);
		assertTrue(Math.abs(43.7 - peakTable.getValue("x", 1)) < TOLERANCE,
			"Peak x position is off by more than the tolerance. Should be 43.7 was " +
				peakTable.getValue("x", 1));
		assertTrue(Math.abs(26.7 - peakTable.getValue("y", 1)) < TOLERANCE,
			"Peak y position is off by more than the tolerance. Should be 26.7 was " +
				peakTable.getValue("y", 1));
		assertTrue(Math.abs(14756.0 - peakTable.getValue("intensity",
			1)) < TOLERANCE,
			"Peak intensity is off by more than the tolerance. Should be 14756 was " +
				peakTable.getValue("intensity", 1));
		assertTrue(Math.abs(3500.0 - peakTable.getValue("baseline", 1)) < 1,
			"Peak baseline is off by more than the 1. Should be 3500 was " + peakTable
				.getValue("baseline", 1));
		assertTrue(Math.abs(3000.0 - peakTable.getValue("height", 1)) < 1,
			"Peak height is off by more than the 1. Should be 3000 was " + peakTable
				.getValue("height", 1));
		assertTrue(Math.abs(1.2 - peakTable.getValue("sigma", 1)) < TOLERANCE,
			"Peak sigma is off by more than the 10. Should be 3000 was " + peakTable
				.getValue("sigma", 1));
		assertTrue(Math.abs(1 - peakTable.getValue("R2", 1)) < TOLERANCE,
			"Peak R2 is off by more than the tolerance. Should be 1 was " + peakTable
				.getValue("sigma", 1));

		assertEquals(peakTable.getValue("T", 2), 0);
		assertTrue(Math.abs(32.5 - peakTable.getValue("x", 2)) < TOLERANCE,
			"Peak x position is off by more than the tolerance. Should be 43.7 was " +
				peakTable.getValue("x", 2));
		assertTrue(Math.abs(40 - peakTable.getValue("y", 2)) < TOLERANCE,
			"Peak y position is off by more than the tolerance. Should be 26.7 was " +
				peakTable.getValue("y", 2));
		assertTrue(Math.abs(14416.0 - peakTable.getValue("intensity",
			2)) < TOLERANCE,
			"Peak intensity is off by more than the tolerance. Should be 14756 was " +
				peakTable.getValue("intensity", 2));
		assertTrue(Math.abs(3500.0 - peakTable.getValue("baseline", 2)) < 1,
			"Peak baseline is off by more than the 1. Should be 3500 was " + peakTable
				.getValue("baseline", 2));
		assertTrue(Math.abs(3000.0 - peakTable.getValue("height", 2)) < 1,
			"Peak height is off by more than the 1. Should be 3000 was " + peakTable
				.getValue("height", 2));
		assertTrue(Math.abs(1.2 - peakTable.getValue("sigma", 2)) < TOLERANCE,
			"Peak sigma is off by more than the 10. Should be 3000 was " + peakTable
				.getValue("sigma", 2));
		assertTrue(Math.abs(1 - peakTable.getValue("R2", 2)) < TOLERANCE,
			"Peak R2 is off by more than the tolerance. Should be 1 was " + peakTable
				.getValue("sigma", 2));

		assertEquals(peakTable.getValue("T", 3), 1);
		assertTrue(Math.abs(10 - peakTable.getValue("x", 3)) < TOLERANCE,
			"Peak x position is off by more than the tolerance. Should be 10 was " +
				peakTable.getValue("x", 3));
		assertTrue(Math.abs(10 - peakTable.getValue("y", 3)) < TOLERANCE,
			"Peak y position is off by more than the tolerance. Should be 10 was " +
				peakTable.getValue("y", 3));
		assertTrue(Math.abs(15789.0 - peakTable.getValue("intensity",
			3)) < TOLERANCE,
			"Peak intensity is off by more than the tolerance. Should be 15789 was " +
				peakTable.getValue("intensity", 3));
		assertTrue(Math.abs(3500.0 - peakTable.getValue("baseline", 3)) < 1,
			"Peak baseline is off by more than the 1. Should be 3500 was " + peakTable
				.getValue("baseline", 3));
		assertTrue(Math.abs(3000.0 - peakTable.getValue("height", 3)) < 1,
			"Peak height is off by more than the 1. Should be 3000 was " + peakTable
				.getValue("height", 3));
		assertTrue(Math.abs(1.2 - peakTable.getValue("sigma", 3)) < TOLERANCE,
			"Peak sigma is off by more than the 10. Should be 3000 was " + peakTable
				.getValue("sigma", 3));
		assertTrue(Math.abs(1 - peakTable.getValue("R2", 3)) < TOLERANCE,
			"Peak R2 is off by more than the tolerance. Should be 1 was " + peakTable
				.getValue("sigma", 3));

		assertEquals(peakTable.getValue("T", 4), 1);
		assertTrue(Math.abs(43.7 - peakTable.getValue("x", 4)) < TOLERANCE,
			"Peak x position is off by more than the tolerance. Should be 43.7 was " +
				peakTable.getValue("x", 4));
		assertTrue(Math.abs(26.7 - peakTable.getValue("y", 4)) < TOLERANCE,
			"Peak y position is off by more than the tolerance. Should be 26.7 was " +
				peakTable.getValue("y", 4));
		assertTrue(Math.abs(14756.0 - peakTable.getValue("intensity",
			4)) < TOLERANCE,
			"Peak intensity is off by more than the tolerance. Should be 14756 was " +
				peakTable.getValue("intensity", 4));
		assertTrue(Math.abs(3500.0 - peakTable.getValue("baseline", 4)) < 1,
			"Peak baseline is off by more than the 1. Should be 3500 was " + peakTable
				.getValue("baseline", 4));
		assertTrue(Math.abs(3000.0 - peakTable.getValue("height", 4)) < 1,
			"Peak height is off by more than the 1. Should be 3000 was " + peakTable
				.getValue("height", 4));
		assertTrue(Math.abs(1.2 - peakTable.getValue("sigma", 4)) < TOLERANCE,
			"Peak sigma is off by more than the 10. Should be 3000 was " + peakTable
				.getValue("sigma", 4));
		assertTrue(Math.abs(1 - peakTable.getValue("R2", 4)) < TOLERANCE,
			"Peak R2 is off by more than the tolerance. Should be 1 was " + peakTable
				.getValue("sigma", 4));

		assertEquals(peakTable.getValue("T", 5), 1);
		assertTrue(Math.abs(32.5 - peakTable.getValue("x", 5)) < TOLERANCE,
			"Peak x position is off by more than the tolerance. Should be 43.7 was " +
				peakTable.getValue("x", 5));
		assertTrue(Math.abs(40 - peakTable.getValue("y", 5)) < TOLERANCE,
			"Peak y position is off by more than the tolerance. Should be 26.7 was " +
				peakTable.getValue("y", 5));
		assertTrue(Math.abs(14416.0 - peakTable.getValue("intensity",
			5)) < TOLERANCE,
			"Peak intensity is off by more than the tolerance. Should be 14756 was " +
				peakTable.getValue("intensity", 5));
		assertTrue(Math.abs(3500.0 - peakTable.getValue("baseline", 5)) < 1,
			"Peak baseline is off by more than the 1. Should be 3500 was " + peakTable
				.getValue("baseline", 5));
		assertTrue(Math.abs(3000.0 - peakTable.getValue("height", 5)) < 1,
			"Peak height is off by more than the 1. Should be 3000 was " + peakTable
				.getValue("height", 5));
		assertTrue(Math.abs(1.2 - peakTable.getValue("sigma", 5)) < TOLERANCE,
			"Peak sigma is off by more than the 10. Should be 3000 was " + peakTable
				.getValue("sigma", 5));
		assertTrue(Math.abs(1 - peakTable.getValue("R2", 5)) < TOLERANCE,
			"Peak R2 is off by more than the tolerance. Should be 1 was " + peakTable
				.getValue("sigma", 5));

	}

	public Dataset simulateDataset() {
		long[] dim = { 50, 50, 2 };
		AxisType[] axes = { Axes.X, Axes.Y, Axes.TIME };
		Dataset dataset = datasetService.create(dim, "Simulated Image", axes, 16,
			false, false);

		for (int t = 0; t < dim[2]; t++)
			for (int x = 0; x < dim[0]; x++)
				for (int y = 0; y < dim[1]; y++) {
					Gaussian2D peak1 = new Gaussian2D(1000d, 3000d, 10d, 10d, 1.2d);
					Gaussian2D peak2 = new Gaussian2D(1000d, 3000d, 32.5d, 40d, 1.2d);
					Gaussian2D peak3 = new Gaussian2D(1000d, 3000d, 43.7d, 26.7d, 1.2d);

					dataset.getImgPlus().randomAccess().setPositionAndGet(x, y, t)
						.setReal(500 + peak1.getValue(x, y) + peak2.getValue(x, y) + peak3
							.getValue(x, y));
				}

		return dataset;
	}
}
