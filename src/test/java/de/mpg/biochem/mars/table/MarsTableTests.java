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

package de.mpg.biochem.mars.table;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.scijava.table.DoubleColumn;
import org.scijava.table.GenericColumn;

public class MarsTableTests {

	public static final double[] testArray = { 721.4053492, -2340.864487,
		-2694.189541, 4995.646673, 1643.211239, 3860.546144, 815.7884318,
		-1007.139018, -1404.076795, 195.7610837, -1785.972797, 190.3236654,
		-4411.370252, -4743.992124, -1015.504958, -2371.327814, -1253.05302,
		1880.42598, 2403.053577, 3225.70651, 3197.5338, 1430.499206, -1569.147553,
		-833.1068533, 4869.277227, -401.6722113, -263.3618484, 3597.692509,
		-478.9343676, 2446.011308, -2355.304877, -3960.300897, -2194.073013,
		-538.2221627, 1589.697328, -591.7635183, -3285.584816, 3609.117657,
		-4393.357614, -742.773464, -1938.162097, -932.6219098, -1763.380138,
		-4545.133429, -4441.702275, -3118.083929, -1619.414716, -3854.88565,
		2525.164109, -1827.672271, -434.4189716, 3742.582315, -4434.650675,
		2300.292346, 618.3404286, 2054.073997, 1650.418286, 4640.574408,
		-437.0977777, 353.1586727, -1850.484199, -2027.520054, -3559.296065,
		2620.35719, 1800.380686, -112.9199202, -1755.952663, 3584.271061,
		4995.583324, -1773.42819, 1769.444419, -3867.960143, -577.9060965,
		-4377.209655, 4941.997092, 4578.256938, -1646.14796, -2699.880246,
		1246.094114, -4005.604619, 1967.859112, 2195.915889, -2539.674577,
		-2181.147607, -849.8377472, -4810.946999, 2239.147404, -4658.640019,
		2253.321032, -2392.739537, -2466.804294, -4724.617614, -4700.818179,
		-829.9974383, 299.5609289, -4882.151576, 3537.51919, 832.2358674,
		-3007.967915, -2885.338512 };

	public static final double[] testArrayOdd = { 721.4053492, -2340.864487,
		-2694.189541, 4995.646673, 1643.211239, 3860.546144, 815.7884318,
		-1007.139018, -1404.076795, 195.7610837, -1785.972797, 190.3236654,
		-4411.370252, -4743.992124, -1015.504958, -2371.327814, -1253.05302,
		1880.42598, 2403.053577, 3225.70651, 3197.5338, 1430.499206, -1569.147553,
		-833.1068533, 4869.277227, -401.6722113, -263.3618484, 3597.692509,
		-478.9343676, 2446.011308, -2355.304877, -3960.300897, -2194.073013,
		-538.2221627, 1589.697328, -591.7635183, -3285.584816, 3609.117657,
		-4393.357614, -742.773464, -1938.162097, -932.6219098, -1763.380138,
		-4545.133429, -4441.702275, -3118.083929, -1619.414716, -3854.88565,
		2525.164109, -1827.672271, -434.4189716, 3742.582315, -4434.650675,
		2300.292346, 618.3404286, 2054.073997, 1650.418286, 4640.574408,
		-437.0977777, 353.1586727, -1850.484199, -2027.520054, -3559.296065,
		2620.35719, 1800.380686, -112.9199202, -1755.952663, 3584.271061,
		4995.583324, -1773.42819, 1769.444419, -3867.960143, -577.9060965,
		-4377.209655, 4941.997092, 4578.256938, -1646.14796, -2699.880246,
		1246.094114, -4005.604619, 1967.859112, 2195.915889, -2539.674577,
		-2181.147607, -849.8377472, -4810.946999, 2239.147404, -4658.640019,
		2253.321032, -2392.739537, -2466.804294, -4724.617614, -4700.818179,
		-829.9974383, 299.5609289, -4882.151576, 3537.51919, 832.2358674,
		-3007.967915 };

	public static final double[] testArrayNaNs = { 721.4053492, -2340.864487,
		-2694.189541, 4995.646673, 1643.211239, 3860.546144, 815.7884318,
		-1007.139018, -1404.076795, 195.7610837, -1785.972797, 190.3236654,
		-4411.370252, -4743.992124, -1015.504958, -2371.327814, -1253.05302,
		1880.42598, 2403.053577, 3225.70651, 3197.5338, 1430.499206, -1569.147553,
		-833.1068533, 4869.277227, -401.6722113, -263.3618484, 3597.692509,
		-478.9343676, 2446.011308, -2355.304877, -3960.300897, -2194.073013,
		-538.2221627, 1589.697328, -591.7635183, -3285.584816, 3609.117657,
		-4393.357614, -742.773464, Double.NaN, -1938.162097, -932.6219098,
		Double.NaN, -1763.380138, -4545.133429, -4441.702275, -3118.083929,
		-1619.414716, -3854.88565, 2525.164109, -1827.672271, -434.4189716,
		3742.582315, -4434.650675, 2300.292346, 618.3404286, 2054.073997,
		1650.418286, 4640.574408, -437.0977777, 353.1586727, -1850.484199,
		-2027.520054, -3559.296065, 2620.35719, 1800.380686, -112.9199202,
		-1755.952663, 3584.271061, 4995.583324, -1773.42819, 1769.444419,
		-3867.960143, -577.9060965, -4377.209655, 4941.997092, 4578.256938,
		-1646.14796, -2699.880246, 1246.094114, -4005.604619, 1967.859112,
		2195.915889, -2539.674577, -2181.147607, -849.8377472, -4810.946999,
		2239.147404, -4658.640019, 2253.321032, -2392.739537, -2466.804294,
		-4724.617614, -4700.818179, -829.9974383, 299.5609289, -4882.151576,
		3537.51919, 832.2358674, -3007.967915, -2885.338512, Double.NaN };

	public static final double[] testArrayAllNaNs = { Double.NaN, Double.NaN,
		Double.NaN, Double.NaN, Double.NaN, Double.NaN };

	public static final double[][] XY = { { 1, 721.4053492 }, { 1.1,
		-2340.864487 }, { 1.2, -2694.189541 }, { 1.3, 4995.646673 }, { 1.4,
			1643.211239 }, { 1.5, 3860.546144 }, { 1.6, 815.7884318 }, { 1.7,
				-1007.139018 }, { 1.8, -1404.076795 }, { 1.9, 195.7610837 }, { 2,
					-1785.972797 }, { 2.1, 190.3236654 }, { 2.2, -4411.370252 }, { 2.3,
						-4743.992124 }, { 2.4, -1015.504958 }, { 2.5, -2371.327814 }, { 2.6,
							-1253.05302 }, { 2.7, 1880.42598 }, { 2.8, 2403.053577 }, { 2.9,
								3225.70651 }, { 3, 3197.5338 }, { 3.1, 1430.499206 }, { 3.2,
									-1569.147553 }, { 3.3, -833.1068533 }, { 3.4, 4869.277227 }, {
										3.5, -401.6722113 }, { 3.6, -263.3618484 }, { 3.7,
											3597.692509 }, { 3.8, -478.9343676 }, { 3.9,
												2446.011308 }, { 4, -2355.304877 }, { 4.1,
													-3960.300897 }, { 4.2, -2194.073013 }, { 4.3,
														-538.2221627 }, { 4.4, 1589.697328 }, { 4.5,
															-591.7635183 }, { 4.6, -3285.584816 }, { 4.7,
																3609.117657 }, { 4.8, -4393.357614 }, { 4.9,
																	-742.773464 }, { 5, -1938.162097 }, { 5.1,
																		-932.6219098 }, { 5.2, -1763.380138 }, {
																			5.3, -4545.133429 }, { 5.4,
																				-4441.702275 }, { 5.5, -3118.083929 }, {
																					5.6, -1619.414716 }, { 5.7,
																						-3854.88565 }, { 5.8, 2525.164109 },
		{ 5.9, -1827.672271 }, { 6, -434.4189716 }, { 6.1, 3742.582315 }, { 6.2,
			-4434.650675 }, { 6.3, 2300.292346 }, { 6.4, 618.3404286 }, { 6.5,
				2054.073997 }, { 6.6, 1650.418286 }, { 6.7, 4640.574408 }, { 6.8,
					-437.0977777 }, { 6.9, 353.1586727 }, { 7, -1850.484199 }, { 7.1,
						-2027.520054 }, { 7.2, -3559.296065 }, { 7.3, 2620.35719 }, { 7.4,
							1800.380686 }, { 7.5, -112.9199202 }, { 7.6, -1755.952663 }, {
								7.7, 3584.271061 }, { 7.8, 4995.583324 }, { 7.9, -1773.42819 },
		{ 8, 1769.444419 }, { 8.1, -3867.960143 }, { 8.2, -577.9060965 }, { 8.3,
			-4377.209655 }, { 8.4, 4941.997092 }, { 8.5, 4578.256938 }, { 8.6,
				-1646.14796 }, { 8.7, -2699.880246 }, { 8.8, 1246.094114 }, { 8.9,
					-4005.604619 }, { 9, 1967.859112 }, { 9.1, 2195.915889 }, { 9.2,
						-2539.674577 }, { 9.3, -2181.147607 }, { 9.4, -849.8377472 }, { 9.5,
							-4810.946999 }, { 9.6, 2239.147404 }, { 9.7, -4658.640019 }, {
								9.8, 2253.321032 }, { 9.9, -2392.739537 }, { 10, -2466.804294 },
		{ 10.1, -4724.617614 }, { 10.2, -4700.818179 }, { 10.3, -829.9974383 }, {
			10.4, 299.5609289 }, { 10.5, -4882.151576 }, { 10.6, 3537.51919 }, { 10.7,
				832.2358674 }, { 10.8, -3007.967915 }, { 10.9, -2885.338512 } };

	public static final double[][] XYNaNs = { { 1, 721.4053492 }, { 1.1,
		-2340.864487 }, { 1.2, -2694.189541 }, { 1.3, 4995.646673 }, { 1.4,
			1643.211239 }, { 1.5, 3860.546144 }, { 1.6, 815.7884318 }, { 1.7,
				-1007.139018 }, { 1.8, -1404.076795 }, { 1.9, 195.7610837 }, { 2,
					-1785.972797 }, { 2.1, 190.3236654 }, { 2.2, -4411.370252 }, { 2.3,
						-4743.992124 }, { 2.4, -1015.504958 }, { 2.45, Double.NaN }, { 2.5,
							-2371.327814 }, { 2.6, -1253.05302 }, { 2.7, 1880.42598 }, { 2.8,
								2403.053577 }, { 2.9, 3225.70651 }, { 3, 3197.5338 }, { 3.1,
									1430.499206 }, { 3.2, -1569.147553 }, { 3.3, -833.1068533 }, {
										3.4, 4869.277227 }, { 3.5, -401.6722113 }, { 3.6,
											-263.3618484 }, { 3.7, 3597.692509 }, { 3.8,
												-478.9343676 }, { 3.9, 2446.011308 }, { 4,
													-2355.304877 }, { 4.1, -3960.300897 }, { 4.2,
														-2194.073013 }, { 4.3, -538.2221627 }, { 4.4,
															1589.697328 }, { 4.5, -591.7635183 }, { 4.6,
																-3285.584816 }, { 4.7, 3609.117657 }, { 4.8,
																	-4393.357614 }, { 4.9, -742.773464 }, { 4.95,
																		Double.NaN }, { 5, -1938.162097 }, { 5.1,
																			-932.6219098 }, { Double.NaN, 1453.6758 },
		{ 5.2, -1763.380138 }, { 5.3, -4545.133429 }, { 5.4, -4441.702275 }, { 5.5,
			-3118.083929 }, { 5.6, -1619.414716 }, { 5.7, -3854.88565 }, { 5.8,
				2525.164109 }, { 5.9, -1827.672271 }, { 6, -434.4189716 }, { 6.1,
					3742.582315 }, { 6.2, -4434.650675 }, { 6.3, 2300.292346 }, { 6.4,
						618.3404286 }, { 6.5, 2054.073997 }, { 6.6, 1650.418286 }, { 6.7,
							4640.574408 }, { 6.8, -437.0977777 }, { 6.9, 353.1586727 }, { 7,
								-1850.484199 }, { 7.1, -2027.520054 }, { 7.2, -3559.296065 }, {
									7.3, 2620.35719 }, { 7.4, 1800.380686 }, { 7.5,
										-112.9199202 }, { 7.6, -1755.952663 }, { 7.7, 3584.271061 },
		{ 7.8, 4995.583324 }, { 7.9, -1773.42819 }, { 8, 1769.444419 }, { 8.1,
			-3867.960143 }, { 8.2, -577.9060965 }, { 8.3, -4377.209655 }, { 8.4,
				4941.997092 }, { 8.5, 4578.256938 }, { 8.6, -1646.14796 }, { 8.7,
					-2699.880246 }, { 8.8, 1246.094114 }, { 8.9, -4005.604619 }, { 9,
						1967.859112 }, { 9.1, 2195.915889 }, { 9.2, -2539.674577 }, { 9.3,
							-2181.147607 }, { 9.4, -849.8377472 }, { 9.5, -4810.946999 }, {
								9.6, 2239.147404 }, { 9.7, -4658.640019 }, { 9.8, 2253.321032 },
		{ 9.9, -2392.739537 }, { 10, -2466.804294 }, { 10.1, -4724.617614 }, { 10.2,
			-4700.818179 }, { 10.3, -829.9974383 }, { 10.4, 299.5609289 }, { 10.5,
				-4882.151576 }, { 10.6, 3537.51919 }, { 10.7, 832.2358674 }, { 10.8,
					-3007.967915 }, { 10.9, -2885.338512 }, { Double.NaN, Double.NaN } };

	public static final double[][] XYAllNaNs = { { Double.NaN, Double.NaN }, {
		Double.NaN, Double.NaN }, { Double.NaN, Double.NaN }, { Double.NaN,
			Double.NaN }, { Double.NaN, Double.NaN }, { Double.NaN, Double.NaN }, {
				Double.NaN, Double.NaN } };

	public static final double[][] XYZ = { { 1, 721.4053492, 1 }, { 1.1,
		-2340.864487, 2 }, { 1.2, -2694.189541, 3 }, { 1.3, 4995.646673, 1 }, { 1.4,
			1643.211239, 4 }, { 1.5, 3860.546144, 5 }, { 1.6, 815.7884318, 6 }, { 1.7,
				-1007.139018, 5 }, { 1.8, -1404.076795, 4 }, { 1.9, 195.7610837, 3 }, {
					2, -1785.972797, 2 }, { 2.1, 190.3236654, 3 }, { 2.2, -4411.370252,
						4 }, { 2.3, -4743.992124, 5 }, { 2.4, -1015.504958, 4 }, { 2.5,
							-2371.327814, 5 }, { 2.6, -1253.05302, 6 }, { 2.7, 1880.42598,
								5 }, { 2.8, 2403.053577, 6 }, { 2.9, 3225.70651, 5 }, { 3,
									3197.5338, 4 }, { 3.1, 1430.499206, 3 }, { 3.2, -1569.147553,
										2 }, { 3.3, -833.1068533, 1 }, { 3.4, 4869.277227, 2 }, {
											3.5, -401.6722113, 3 }, { 3.6, -263.3618484, 4 }, { 3.7,
												3597.692509, 5 }, { 3.8, -478.9343676, 6 }, { 3.9,
													2446.011308, 6 }, { 4, -2355.304877, 6 }, { 4.1,
														-3960.300897, 6 }, { 4.2, -2194.073013, 6 }, { 4.3,
															-538.2221627, 5 }, { 4.4, 1589.697328, 5 }, { 4.5,
																-591.7635183, 5 }, { 4.6, -3285.584816, 5 }, {
																	4.7, 3609.117657, 4 }, { 4.8, -4393.357614,
																		4 }, { 4.9, -742.773464, 3 }, { 5,
																			-1938.162097, 3 }, { 5.1, -932.6219098,
																				3 }, { 5.2, -1763.380138, 3 }, { 5.3,
																					-4545.133429, 2 }, { 5.4,
																						-4441.702275, 2 }, { 5.5,
																							-3118.083929, 2 }, { 5.6,
																								-1619.414716, 2 }, { 5.7,
																									-3854.88565, 2 }, { 5.8,
																										2525.164109, 3 }, { 5.9,
																											-1827.672271, 3 }, { 6,
																												-434.4189716, 3 }, {
																													6.1, 3742.582315, 2 },
		{ 6.2, -4434.650675, 3 }, { 6.3, 2300.292346, 1 }, { 6.4, 618.3404286, 2 },
		{ 6.5, 2054.073997, 1 }, { 6.6, 1650.418286, 1 }, { 6.7, 4640.574408, 1 }, {
			6.8, -437.0977777, 1 }, { 6.9, 353.1586727, 1 }, { 7, -1850.484199, 1 }, {
				7.1, -2027.520054, 2 }, { 7.2, -3559.296065, 3 }, { 7.3, 2620.35719,
					4 }, { 7.4, 1800.380686, 5 }, { 7.5, -112.9199202, 6 }, { 7.6,
						-1755.952663, 6 }, { 7.7, 3584.271061, 5 }, { 7.8, 4995.583324, 4 },
		{ 7.9, -1773.42819, 4 }, { 8, 1769.444419, 3 }, { 8.1, -3867.960143, 3 }, {
			8.2, -577.9060965, 2 }, { 8.3, -4377.209655, 2 }, { 8.4, 4941.997092, 2 },
		{ 8.5, 4578.256938, 1 }, { 8.6, -1646.14796, 1 }, { 8.7, -2699.880246, 1 },
		{ 8.8, 1246.094114, 1 }, { 8.9, -4005.604619, 1 }, { 9, 1967.859112, 1 }, {
			9.1, 2195.915889, 3 }, { 9.2, -2539.674577, 2 }, { 9.3, -2181.147607, 3 },
		{ 9.4, -849.8377472, 4 }, { 9.5, -4810.946999, 5 }, { 9.6, 2239.147404, 4 },
		{ 9.7, -4658.640019, 5 }, { 9.8, 2253.321032, 6 }, { 9.9, -2392.739537, 5 },
		{ 10, -2466.804294, 4 }, { 10.1, -4724.617614, 3 }, { 10.2, -4700.818179,
			2 }, { 10.3, -829.9974383, 1 }, { 10.4, 299.5609289, 2 }, { 10.5,
				-4882.151576, 1 }, { 10.6, 3537.51919, 2 }, { 10.7, 832.2358674, 3 }, {
					10.8, -3007.967915, 4 }, { 10.9, -2885.338512, 5 } };

	public static final double[][] XYZSortY = { { 10.5, -4882.151576, 1 }, { 9.5,
		-4810.946999, 5 }, { 2.3, -4743.992124, 5 }, { 10.1, -4724.617614, 3 }, {
			10.2, -4700.818179, 2 }, { 9.7, -4658.640019, 5 }, { 5.3, -4545.133429,
				2 }, { 5.4, -4441.702275, 2 }, { 6.2, -4434.650675, 3 }, { 2.2,
					-4411.370252, 4 }, { 4.8, -4393.357614, 4 }, { 8.3, -4377.209655, 2 },
		{ 8.9, -4005.604619, 1 }, { 4.1, -3960.300897, 6 }, { 8.1, -3867.960143,
			3 }, { 5.7, -3854.88565, 2 }, { 7.2, -3559.296065, 3 }, { 4.6,
				-3285.584816, 5 }, { 5.5, -3118.083929, 2 }, { 10.8, -3007.967915, 4 },
		{ 10.9, -2885.338512, 5 }, { 8.7, -2699.880246, 1 }, { 1.2, -2694.189541,
			3 }, { 9.2, -2539.674577, 2 }, { 10, -2466.804294, 4 }, { 9.9,
				-2392.739537, 5 }, { 2.5, -2371.327814, 5 }, { 4, -2355.304877, 6 }, {
					1.1, -2340.864487, 2 }, { 4.2, -2194.073013, 6 }, { 9.3, -2181.147607,
						3 }, { 7.1, -2027.520054, 2 }, { 5, -1938.162097, 3 }, { 7,
							-1850.484199, 1 }, { 5.9, -1827.672271, 3 }, { 2, -1785.972797,
								2 }, { 7.9, -1773.42819, 4 }, { 5.2, -1763.380138, 3 }, { 7.6,
									-1755.952663, 6 }, { 8.6, -1646.14796, 1 }, { 5.6,
										-1619.414716, 2 }, { 3.2, -1569.147553, 2 }, { 1.8,
											-1404.076795, 4 }, { 2.6, -1253.05302, 6 }, { 2.4,
												-1015.504958, 4 }, { 1.7, -1007.139018, 5 }, { 5.1,
													-932.6219098, 3 }, { 9.4, -849.8377472, 4 }, { 3.3,
														-833.1068533, 1 }, { 10.3, -829.9974383, 1 }, { 4.9,
															-742.773464, 3 }, { 4.5, -591.7635183, 5 }, { 8.2,
																-577.9060965, 2 }, { 4.3, -538.2221627, 5 }, {
																	3.8, -478.9343676, 6 }, { 6.8, -437.0977777,
																		1 }, { 6, -434.4189716, 3 }, { 3.5,
																			-401.6722113, 3 }, { 3.6, -263.3618484,
																				4 }, { 7.5, -112.9199202, 6 }, { 2.1,
																					190.3236654, 3 }, { 1.9, 195.7610837,
																						3 }, { 10.4, 299.5609289, 2 }, {
																							6.9, 353.1586727, 1 }, { 6.4,
																								618.3404286, 2 }, { 1,
																									721.4053492, 1 }, { 1.6,
																										815.7884318, 6 }, { 10.7,
																											832.2358674, 3 }, { 8.8,
																												1246.094114, 1 }, { 3.1,
																													1430.499206, 3 }, {
																														4.4, 1589.697328,
																														5 }, { 1.4,
																															1643.211239, 4 },
		{ 6.6, 1650.418286, 1 }, { 8, 1769.444419, 3 }, { 7.4, 1800.380686, 5 }, {
			2.7, 1880.42598, 5 }, { 9, 1967.859112, 1 }, { 6.5, 2054.073997, 1 }, {
				9.1, 2195.915889, 3 }, { 9.6, 2239.147404, 4 }, { 9.8, 2253.321032, 6 },
		{ 6.3, 2300.292346, 1 }, { 2.8, 2403.053577, 6 }, { 3.9, 2446.011308, 6 }, {
			5.8, 2525.164109, 3 }, { 7.3, 2620.35719, 4 }, { 3, 3197.5338, 4 }, { 2.9,
				3225.70651, 5 }, { 10.6, 3537.51919, 2 }, { 7.7, 3584.271061, 5 }, {
					3.7, 3597.692509, 5 }, { 4.7, 3609.117657, 4 }, { 6.1, 3742.582315,
						2 }, { 1.5, 3860.546144, 5 }, { 8.5, 4578.256938, 1 }, { 6.7,
							4640.574408, 1 }, { 3.4, 4869.277227, 2 }, { 8.4, 4941.997092,
								2 }, { 7.8, 4995.583324, 4 }, { 1.3, 4995.646673, 1 } };

	public static final double[][] XYZSortZY = { { 10.5, -4882.151576, 1 }, { 8.9,
		-4005.604619, 1 }, { 8.7, -2699.880246, 1 }, { 7, -1850.484199, 1 }, { 8.6,
			-1646.14796, 1 }, { 3.3, -833.1068533, 1 }, { 10.3, -829.9974383, 1 }, {
				6.8, -437.0977777, 1 }, { 6.9, 353.1586727, 1 }, { 1, 721.4053492, 1 },
		{ 8.8, 1246.094114, 1 }, { 6.6, 1650.418286, 1 }, { 9, 1967.859112, 1 }, {
			6.5, 2054.073997, 1 }, { 6.3, 2300.292346, 1 }, { 8.5, 4578.256938, 1 }, {
				6.7, 4640.574408, 1 }, { 1.3, 4995.646673, 1 }, { 10.2, -4700.818179,
					2 }, { 5.3, -4545.133429, 2 }, { 5.4, -4441.702275, 2 }, { 8.3,
						-4377.209655, 2 }, { 5.7, -3854.88565, 2 }, { 5.5, -3118.083929,
							2 }, { 9.2, -2539.674577, 2 }, { 1.1, -2340.864487, 2 }, { 7.1,
								-2027.520054, 2 }, { 2, -1785.972797, 2 }, { 5.6, -1619.414716,
									2 }, { 3.2, -1569.147553, 2 }, { 8.2, -577.9060965, 2 }, {
										10.4, 299.5609289, 2 }, { 6.4, 618.3404286, 2 }, { 10.6,
											3537.51919, 2 }, { 6.1, 3742.582315, 2 }, { 3.4,
												4869.277227, 2 }, { 8.4, 4941.997092, 2 }, { 10.1,
													-4724.617614, 3 }, { 6.2, -4434.650675, 3 }, { 8.1,
														-3867.960143, 3 }, { 7.2, -3559.296065, 3 }, { 1.2,
															-2694.189541, 3 }, { 9.3, -2181.147607, 3 }, { 5,
																-1938.162097, 3 }, { 5.9, -1827.672271, 3 }, {
																	5.2, -1763.380138, 3 }, { 5.1, -932.6219098,
																		3 }, { 4.9, -742.773464, 3 }, { 6,
																			-434.4189716, 3 }, { 3.5, -401.6722113,
																				3 }, { 2.1, 190.3236654, 3 }, { 1.9,
																					195.7610837, 3 }, { 10.7, 832.2358674,
																						3 }, { 3.1, 1430.499206, 3 }, { 8,
																							1769.444419, 3 }, { 9.1,
																								2195.915889, 3 }, { 5.8,
																									2525.164109, 3 }, { 2.2,
																										-4411.370252, 4 }, { 4.8,
																											-4393.357614, 4 }, { 10.8,
																												-3007.967915, 4 }, { 10,
																													-2466.804294, 4 }, {
																														7.9, -1773.42819,
																														4 }, { 1.8,
																															-1404.076795, 4 },
		{ 2.4, -1015.504958, 4 }, { 9.4, -849.8377472, 4 }, { 3.6, -263.3618484,
			4 }, { 1.4, 1643.211239, 4 }, { 9.6, 2239.147404, 4 }, { 7.3, 2620.35719,
				4 }, { 3, 3197.5338, 4 }, { 4.7, 3609.117657, 4 }, { 7.8, 4995.583324,
					4 }, { 9.5, -4810.946999, 5 }, { 2.3, -4743.992124, 5 }, { 9.7,
						-4658.640019, 5 }, { 4.6, -3285.584816, 5 }, { 10.9, -2885.338512,
							5 }, { 9.9, -2392.739537, 5 }, { 2.5, -2371.327814, 5 }, { 1.7,
								-1007.139018, 5 }, { 4.5, -591.7635183, 5 }, { 4.3,
									-538.2221627, 5 }, { 4.4, 1589.697328, 5 }, { 7.4,
										1800.380686, 5 }, { 2.7, 1880.42598, 5 }, { 2.9, 3225.70651,
											5 }, { 7.7, 3584.271061, 5 }, { 3.7, 3597.692509, 5 }, {
												1.5, 3860.546144, 5 }, { 4.1, -3960.300897, 6 }, { 4,
													-2355.304877, 6 }, { 4.2, -2194.073013, 6 }, { 7.6,
														-1755.952663, 6 }, { 2.6, -1253.05302, 6 }, { 3.8,
															-478.9343676, 6 }, { 7.5, -112.9199202, 6 }, {
																1.6, 815.7884318, 6 }, { 9.8, 2253.321032, 6 },
		{ 2.8, 2403.053577, 6 }, { 3.9, 2446.011308, 6 } };

	public static final double[][] XYZRowsDeleted = { { 1, 721.4053492, 1 }, {
		1.1, -2340.864487, 2 }, { 1.2, -2694.189541, 3 }, { 1.3, 4995.646673, 1 }, {
			1.4, 1643.211239, 4 }, { 1.5, 3860.546144, 5 }, { 1.6, 815.7884318, 6 }, {
				1.7, -1007.139018, 5 }, { 1.8, -1404.076795, 4 }, { 2.3, -4743.992124,
					5 }, { 2.4, -1015.504958, 4 }, { 2.5, -2371.327814, 5 }, { 2.6,
						-1253.05302, 6 }, { 2.7, 1880.42598, 5 }, { 2.8, 2403.053577, 6 }, {
							2.9, 3225.70651, 5 }, { 3, 3197.5338, 4 }, { 3.1, 1430.499206,
								3 }, { 3.2, -1569.147553, 2 }, { 3.3, -833.1068533, 1 }, { 3.5,
									-401.6722113, 3 }, { 3.6, -263.3618484, 4 }, { 3.7,
										3597.692509, 5 }, { 3.8, -478.9343676, 6 }, { 3.9,
											2446.011308, 6 }, { 4, -2355.304877, 6 }, { 4.1,
												-3960.300897, 6 }, { 4.2, -2194.073013, 6 }, { 4.3,
													-538.2221627, 5 }, { 4.4, 1589.697328, 5 }, { 4.5,
														-591.7635183, 5 }, { 4.6, -3285.584816, 5 }, { 4.7,
															3609.117657, 4 }, { 4.8, -4393.357614, 4 }, { 4.9,
																-742.773464, 3 }, { 5, -1938.162097, 3 }, { 5.1,
																	-932.6219098, 3 }, { 5.2, -1763.380138, 3 }, {
																		5.3, -4545.133429, 2 }, { 5.4, -4441.702275,
																			2 }, { 5.5, -3118.083929, 2 }, { 5.6,
																				-1619.414716, 2 }, { 5.7, -3854.88565,
																					2 }, { 5.8, 2525.164109, 3 }, { 5.9,
																						-1827.672271, 3 }, { 6,
																							-434.4189716, 3 }, { 6.1,
																								3742.582315, 2 }, { 6.2,
																									-4434.650675, 3 }, { 6.3,
																										2300.292346, 1 }, { 6.4,
																											618.3404286, 2 }, { 6.5,
																												2054.073997, 1 }, { 6.6,
																													1650.418286, 1 }, {
																														6.7, 4640.574408,
																														1 }, { 6.8,
																															-437.0977777, 1 },
		{ 6.9, 353.1586727, 1 }, { 7, -1850.484199, 1 }, { 7.1, -2027.520054, 2 }, {
			7.2, -3559.296065, 3 }, { 7.3, 2620.35719, 4 }, { 7.4, 1800.380686, 5 }, {
				7.5, -112.9199202, 6 }, { 7.6, -1755.952663, 6 }, { 7.7, 3584.271061,
					5 }, { 7.8, 4995.583324, 4 }, { 7.9, -1773.42819, 4 }, { 8,
						1769.444419, 3 }, { 8.1, -3867.960143, 3 }, { 8.2, -577.9060965,
							2 }, { 8.3, -4377.209655, 2 }, { 8.4, 4941.997092, 2 }, { 8.5,
								4578.256938, 1 }, { 8.6, -1646.14796, 1 }, { 8.7, -2699.880246,
									1 }, { 8.8, 1246.094114, 1 }, { 8.9, -4005.604619, 1 }, { 9,
										1967.859112, 1 }, { 9.1, 2195.915889, 3 }, { 9.2,
											-2539.674577, 2 }, { 9.3, -2181.147607, 3 }, { 9.4,
												-849.8377472, 4 }, { 9.5, -4810.946999, 5 }, { 9.6,
													2239.147404, 4 }, { 9.7, -4658.640019, 5 }, { 9.8,
														2253.321032, 6 }, { 9.9, -2392.739537, 5 }, { 10,
															-2466.804294, 4 }, { 10.1, -4724.617614, 3 }, {
																10.2, -4700.818179, 2 }, { 10.3, -829.9974383,
																	1 }, { 10.4, 299.5609289, 2 }, { 10.5,
																		-4882.151576, 1 }, { 10.6, 3537.51919, 2 },
		{ 10.7, 832.2358674, 3 }, { 10.8, -3007.967915, 4 }, { 10.9, -2885.338512,
			5 } };

	public static final double[][] XYZKeepRows = { { 1.9, 195.7610837, 3 }, { 2,
		-1785.972797, 2 }, { 2.1, 190.3236654, 3 }, { 2.2, -4411.370252, 4 }, { 3.4,
			4869.277227, 2 } };

	public static final String[] stringColumn = { "zero", "one", "two", "three",
		"four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve",
		"thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen",
		"nineteen", "twenty", "twenty one" };

	public static final double[][] XYString = { { 1, 721.4053492 }, { 1.1,
		-2340.864487 }, { 1.2, -2694.189541 }, { 1.3, 4995.646673 }, { 1.4,
			1643.211239 }, { 1.5, 3860.546144 }, { 1.6, 815.7884318 }, { 1.7,
				-1007.139018 }, { 1.8, -1404.076795 }, { 1.9, 195.7610837 }, { 2,
					-1785.972797 }, { 2.1, 190.3236654 }, { 2.2, -4411.370252 }, { 2.3,
						-4743.992124 }, { 2.4, -1015.504958 }, { 2.5, -2371.327814 }, { 2.6,
							-1253.05302 }, { 2.7, 1880.42598 }, { 2.8, 2403.053577 }, { 2.9,
								3225.70651 }, { 3, 3197.5338 }, { 3.1, 1430.499206 } };

	public static final String[] stringColumnSorted = { "eight", "eighteen",
		"eleven", "fifteen", "five", "four", "fourteen", "nine", "nineteen", "one",
		"seven", "seventeen", "six", "sixteen", "ten", "thirteen", "three",
		"twelve", "twenty", "twenty one", "two", "zero" };

	public static final double[][] XYStringColumnSorted = { { 1.8, -1404.076795 },
		{ 2.8, 2403.053577 }, { 2.1, 190.3236654 }, { 2.5, -2371.327814 }, { 1.5,
			3860.546144 }, { 1.4, 1643.211239 }, { 2.4, -1015.504958 }, { 1.9,
				195.7610837 }, { 2.9, 3225.70651 }, { 1.1, -2340.864487 }, { 1.7,
					-1007.139018 }, { 2.7, 1880.42598 }, { 1.6, 815.7884318 }, { 2.6,
						-1253.05302 }, { 2, -1785.972797 }, { 2.3, -4743.992124 }, { 1.3,
							4995.646673 }, { 2.2, -4411.370252 }, { 3, 3197.5338 }, { 3.1,
								1430.499206 }, { 1.2, -2694.189541 }, { 1, 721.4053492 } };

	/*
	 * TEST max()
	 */

	@Test
	void max() {
		MarsTable table = buildTestArrayTable();
		assertEquals(4995.646673, table.max("col0"));
	}

	@Test
	void maxNaNs() {
		MarsTable table = buildTestArrayNaNsTable();
		assertEquals(4995.646673, table.max("col0"));
	}

	@Test
	void maxAllNaNs() {
		MarsTable table = buildTestArrayAllNaNsTable();
		assertEquals(Double.NaN, table.max("col0"));
	}

	@Test
	void maxNoColumn() {
		MarsTable table = buildTestArrayTable();
		assertEquals(Double.NaN, table.max("not here"));
	}

	/*
	 * TEST max() for selected rows
	 */

	@Test
	void maxSelectedRows() {
		MarsTable table = buildTestXYTable();
		assertEquals(4869.277227, table.max("col1", "col0", 3, 3.5));
	}

	@Test
	void maxSelectedRowsNaNs() {
		MarsTable table = buildTestXYNaNsTable();
		assertEquals(4869.277227, table.max("col1", "col0", 3, 3.5));
	}

	@Test
	void maxSelectedRowsAllNaNs() {
		MarsTable table = buildTestXYAllNaNsTable();
		assertEquals(Double.NaN, table.max("col1", "col0", 3, 3.5));
	}

	@Test
	void maxSelectedRowsNoColumn() {
		MarsTable table = buildTestXYTable();
		assertEquals(Double.NaN, table.max("Not a Column", "col0", 3, 3.5));
	}

	/*
	 * TEST min()
	 */

	@Test
	void min() {
		MarsTable table = buildTestArrayTable();
		assertEquals(-4882.151576, table.min("col0"));
	}

	@Test
	void minNaNs() {
		MarsTable table = buildTestArrayNaNsTable();
		assertEquals(-4882.151576, table.min("col0"));
	}

	@Test
	void minAllNaNs() {
		MarsTable table = buildTestArrayAllNaNsTable();
		assertEquals(Double.NaN, table.min("col0"));
	}

	@Test
	void minNoColumn() {
		MarsTable table = buildTestArrayTable();
		assertEquals(Double.NaN, table.min("not here"));
	}

	/*
	 * TEST min() for selected rows
	 */

	@Test
	void minSelectedRows() {
		MarsTable table = buildTestXYTable();
		assertEquals(-1569.147553, table.min("col1", "col0", 3, 3.5));
	}

	@Test
	void minSelectedRowsNaNs() {
		MarsTable table = buildTestXYNaNsTable();
		assertEquals(-1569.147553, table.min("col1", "col0", 3, 3.5));
	}

	@Test
	void minSelectedRowsAllNaNs() {
		MarsTable table = buildTestXYAllNaNsTable();
		assertEquals(Double.NaN, table.max("col1", "col0", 3, 3.5));
	}

	@Test
	void minSelectedRowsNoColumn() {
		MarsTable table = buildTestXYTable();
		assertEquals(Double.NaN, table.min("Not a Column", "col0", 3, 3.5));
	}

	/*
	 * TEST mean()
	 */

	@Test
	void mean() {
		MarsTable table = buildTestArrayTable();
		assertEquals(-457.49063168200007, table.mean("col0"));
	}

	@Test
	void meanNaNs() {
		MarsTable table = buildTestArrayNaNsTable();
		assertEquals(-457.49063168200007, table.mean("col0"));
	}

	@Test
	void meanAllNaNs() {
		MarsTable table = buildTestArrayAllNaNsTable();
		assertEquals(Double.NaN, table.mean("col0"));
	}

	@Test
	void meanNoColumn() {
		MarsTable table = buildTestArrayTable();
		assertEquals(Double.NaN, table.mean("not here"));
	}

	/*
	 * TEST mean() for selected rows
	 */

	@Test
	void meanSelectedRows() {
		MarsTable table = buildTestXYTable();
		assertEquals(1115.5639359, table.mean("col1", "col0", 3, 3.5));
	}

	@Test
	void meanSelectedRowsNaNs() {
		MarsTable table = buildTestXYNaNsTable();
		assertEquals(1115.5639359, table.mean("col1", "col0", 3, 3.5));
	}

	@Test
	void meanSelectedRowsAllNaNs() {
		MarsTable table = buildTestXYAllNaNsTable();
		assertEquals(Double.NaN, table.mean("col1", "col0", 3, 3.5));
	}

	@Test
	void meanSelectedRowsNoColumn() {
		MarsTable table = buildTestXYTable();
		assertEquals(Double.NaN, table.mean("Not a Column", "col0", 3, 3.5));
	}

	/*
	 * TEST median()
	 */

	@Test
	void median() {
		MarsTable table = buildTestArrayTable();
		assertEquals(-786.38545115, table.median("col0"));
	}

	@Test
	void medianOdd() {
		MarsTable table = buildTestArrayOddTable();
		assertEquals(-742.773464, table.median("col0"));
	}

	@Test
	void medianNaNs() {
		MarsTable table = buildTestArrayNaNsTable();
		assertEquals(-786.38545115, table.median("col0"));
	}

	@Test
	void medianAllNaNs() {
		MarsTable table = buildTestArrayAllNaNsTable();
		assertEquals(Double.NaN, table.median("col0"));
	}

	@Test
	void medianNoColumn() {
		MarsTable table = buildTestArrayTable();
		assertEquals(Double.NaN, table.median("not here"));
	}

	/*
	 * TEST median() for selected rows
	 */

	@Test
	void medianSelectedRows() {
		MarsTable table = buildTestXYTable();
		assertEquals(-401.6722113, table.median("col1", "col0", 2, 4));
	}

	@Test
	void medianSelectedRowsNaNs() {
		MarsTable table = buildTestXYNaNsTable();
		assertEquals(-401.6722113, table.median("col1", "col0", 2, 4));
	}

	@Test
	void medianSelectedRowsAllNaNs() {
		MarsTable table = buildTestXYAllNaNsTable();
		assertEquals(Double.NaN, table.median("col1", "col0", 2, 4));
	}

	@Test
	void medianSelectedRowsNoColumn() {
		MarsTable table = buildTestXYTable();
		assertEquals(Double.NaN, table.median("Not a Column", "col0", 2, 4));
	}

	/*
	 * TEST mad()
	 */

	@Test
	void mad() {
		MarsTable table = buildTestArrayTable();
		assertEquals(2219.2335605, table.mad("col0"));
	}

	@Test
	void madOdd() {
		MarsTable table = buildTestArrayOddTable();
		assertEquals(2265.1944510000003, table.mad("col0"));
	}

	@Test
	void madNaNs() {
		MarsTable table = buildTestArrayNaNsTable();
		assertEquals(2219.2335605, table.mad("col0"));
	}

	@Test
	void madAllNaNs() {
		MarsTable table = buildTestArrayAllNaNsTable();
		assertEquals(Double.NaN, table.mad("col0"));
	}

	@Test
	void madNoColumn() {
		MarsTable table = buildTestArrayTable();
		assertEquals(Double.NaN, table.mad("not here"));
	}

	/*
	 * TEST mad() for selected rows.
	 */

	@Test
	void madSelectedRows() {
		MarsTable table = buildTestXYTable();
		assertEquals(1953.6326657, table.mad("col1", "col0", 2, 4));
	}

	@Test
	void madSelectedRowsNaNs() {
		MarsTable table = buildTestXYNaNsTable();
		assertEquals(1953.6326657, table.mad("col1", "col0", 2, 4));
	}

	@Test
	void madSelectedRowsAllNaNs() {
		MarsTable table = buildTestXYAllNaNsTable();
		assertEquals(Double.NaN, table.mad("col1", "col0", 2, 4));
	}

	@Test
	void madSelectedRowsNoColumn() {
		MarsTable table = buildTestXYTable();
		assertEquals(Double.NaN, table.mad("Not a Column", "col0", 2, 4));
	}

	/*
	 * TEST std()
	 */

	@Test
	void std() {
		MarsTable table = buildTestArrayTable();
		assertEquals(2785.9745028072207, table.std("col0"));
	}

	@Test
	void stdNaNs() {
		MarsTable table = buildTestArrayNaNsTable();
		assertEquals(2785.9745028072207, table.std("col0"));
	}

	@Test
	void stdAllNaNs() {
		MarsTable table = buildTestArrayAllNaNsTable();
		assertEquals(Double.NaN, table.std("col0"));
	}

	@Test
	void stdNoColumn() {
		MarsTable table = buildTestArrayTable();
		assertEquals(Double.NaN, table.std("not here"));
	}

	/*
	 * TEST std() for selected rows
	 */

	@Test
	void stdSelectedRows() {
		MarsTable table = buildTestXYTable();
		assertEquals(2617.7803706062314, table.std("col1", "col0", 2, 4));
	}

	@Test
	void stdSelectedRowsNaNs() {
		MarsTable table = buildTestXYNaNsTable();
		assertEquals(2617.7803706062314, table.std("col1", "col0", 2, 4));
	}

	@Test
	void stdSelectedRowsAllNaNs() {
		MarsTable table = buildTestXYAllNaNsTable();
		assertEquals(Double.NaN, table.std("col1", "col0", 2, 4));
	}

	@Test
	void stdSelectedRowsNoColumn() {
		MarsTable table = buildTestXYTable();
		assertEquals(Double.NaN, table.std("Not a column", "col0", 2, 4));
	}

	/*
	 * TEST sem()
	 */

	@Test
	void sem() {
		MarsTable table = buildTestArrayTable();
		assertEquals(278.59745028072206, table.sem("col0"));
	}

	@Test
	void semNaNs() {
		MarsTable table = buildTestArrayNaNsTable();
		assertEquals(278.59745028072206, table.sem("col0"));
	}

	@Test
	void semAllNaNs() {
		MarsTable table = buildTestArrayAllNaNsTable();
		assertEquals(Double.NaN, table.sem("col0"));
	}

	@Test
	void semNoColumn() {
		MarsTable table = buildTestArrayTable();
		assertEquals(Double.NaN, table.sem("not here"));
	}

	/*
	 * TEST sem() for selected rows
	 */

	@Test
	void semSelectedRows() {
		MarsTable table = buildTestXYTable();
		assertEquals(571.2465095748861, table.sem("col1", "col0", 2, 4));
	}

	@Test
	void semSelectedRowsNaNs() {
		MarsTable table = buildTestXYNaNsTable();
		assertEquals(571.2465095748861, table.sem("col1", "col0", 2, 4));
	}

	@Test
	void semSelectedRowsAllNaNs() {
		MarsTable table = buildTestXYAllNaNsTable();
		assertEquals(Double.NaN, table.sem("col1", "col0", 2, 4));
	}

	@Test
	void semSelectedRowsNoColumn() {
		MarsTable table = buildTestXYTable();
		assertEquals(Double.NaN, table.sem("Not a column", "col0", 2, 4));
	}
	/*
	 * TEST linearRegression()
	 */

	@Test
	void linearRegression() {
		MarsTable table = buildTestXYTable();
		assertArrayEquals(new double[] { 265.2601469487532, 636.3646939229644,
			-121.47071909760561, 96.22579573697894 }, table.linearRegression("col0",
				"col1"));
	}

	@Test
	void linearRegressionNaNs() {
		MarsTable table = buildTestXYNaNsTable();
		assertArrayEquals(new double[] { 265.2601469487532, 636.3646939229644,
			-121.47071909760561, 96.22579573697894 }, table.linearRegression("col0",
				"col1"));
	}

	@Test
	void linearRegressionAllNaNs() {
		MarsTable table = buildTestXYAllNaNsTable();
		assertArrayEquals(new double[] { Double.NaN, Double.NaN, Double.NaN,
			Double.NaN }, table.linearRegression("col0", "col1"));
	}

	@Test
	void linearRegressionNoColumn() {
		MarsTable table = buildTestXYTable();
		assertArrayEquals(new double[] { Double.NaN, Double.NaN, Double.NaN,
			Double.NaN }, table.linearRegression("Not a column", "col1"));
	}

	/*
	 * TEST linearRegression() for selected rows
	 */

	@Test
	void linearRegressionSelectedRows() {
		MarsTable table = buildTestXYTable();
		assertArrayEquals(new double[] { -4803.156050166231, 2732.521749986391,
			1628.9532088935057, 892.8347701436365 }, table.linearRegression("col0",
				"col1", 2, 4));
	}

	@Test
	void linearRegressionSelectedRowsNaNs() {
		MarsTable table = buildTestXYNaNsTable();
		assertArrayEquals(new double[] { -4803.156050166231, 2732.521749986391,
			1628.9532088935057, 892.8347701436365 }, table.linearRegression("col0",
				"col1", 2, 4));
	}

	@Test
	void linearRegressionSelectedRowsAllNaNs() {
		MarsTable table = buildTestXYAllNaNsTable();
		assertArrayEquals(new double[] { Double.NaN, Double.NaN, Double.NaN,
			Double.NaN }, table.linearRegression("col0", "col1", 2, 4));
	}

	@Test
	void linearRegressionSelectedRowsNoColumn() {
		MarsTable table = buildTestXYTable();
		assertArrayEquals(new double[] { Double.NaN, Double.NaN, Double.NaN,
			Double.NaN }, table.linearRegression("Not a column", "col1", 2, 4));
	}

	/*
	 * TEST variance()
	 */

	@Test
	void variance() {
		MarsTable table = buildTestArrayTable();
		assertEquals(7684037.390989021, table.variance("col0"));
	}

	@Test
	void varianceNaNs() {
		MarsTable table = buildTestArrayNaNsTable();
		assertEquals(7684037.390989021, table.variance("col0"));
	}

	@Test
	void varianceAllNaNs() {
		MarsTable table = buildTestArrayAllNaNsTable();
		assertEquals(Double.NaN, table.variance("col0"));
	}

	@Test
	void varianceNoColumn() {
		MarsTable table = buildTestArrayTable();
		assertEquals(Double.NaN, table.variance("not here"));
	}

	/*
	 * TEST variance() for selected rows
	 */

	@Test
	void varianceSelectedRows() {
		MarsTable table = buildTestXYTable();
		assertEquals(6526451.494029807, table.variance("col1", "col0", 2, 4));
	}

	@Test
	void varianceSelectedRowsNaNs() {
		MarsTable table = buildTestXYNaNsTable();
		assertEquals(6526451.494029807, table.variance("col1", "col0", 2, 4));
	}

	@Test
	void varianceSelectedRowsAllNaNs() {
		MarsTable table = buildTestXYAllNaNsTable();
		assertEquals(Double.NaN, table.variance("col1", "col0", 2, 4));
	}

	@Test
	void varianceSelectedRowsNoColumn() {
		MarsTable table = buildTestXYTable();
		assertEquals(Double.NaN, table.variance("Not a column", "col0", 2, 4));
	}

	/*
	 * TEST sort()
	 */

	@Test
	void sortOneColumn() {
		MarsTable table = buildTestXYZTable();
		table.sort(true, "col1");
		double[][] result = new double[table.getRowCount()][3];
		for (int row = 0; row < table.getRowCount(); row++) {
			result[row][0] = table.getValue("col0", row);
			result[row][1] = table.getValue("col1", row);
			result[row][2] = table.getValue("col2", row);
		}

		assertArrayEquals(XYZSortY, result);
	}

	@Test
	void sortTwoColumns() {
		MarsTable table = buildTestXYZTable();
		table.sort(true, "col2", "col1");
		double[][] result = new double[table.getRowCount()][3];
		for (int row = 0; row < table.getRowCount(); row++) {
			result[row][0] = table.getValue("col0", row);
			result[row][1] = table.getValue("col1", row);
			result[row][2] = table.getValue("col2", row);
		}

		assertArrayEquals(XYZSortZY, result);
	}

	@Test
	void sortStringColumn() {
		MarsTable table = buildTestXYStringTable();
		table.sort(true, "col2");
		double[][] xyResult = new double[table.getRowCount()][2];
		for (int row = 0; row < table.getRowCount(); row++) {
			xyResult[row][0] = table.getValue("col0", row);
			xyResult[row][1] = table.getValue("col1", row);
		}
		assertArrayEquals(XYStringColumnSorted, xyResult);

		String[] stringResult = new String[table.getRowCount()];
		for (int row = 0; row < table.getRowCount(); row++) {
			stringResult[row] = table.getStringValue("col2", row);
		}

		assertArrayEquals(stringColumnSorted, stringResult);
	}

	/*
	 * TEST deleteRows()
	 */

	@Test
	void deleteRows() {
		MarsTable table = buildTestXYZTable();
		int[] rows = new int[] { 9, 10, 11, 12, 24 };
		table.deleteRows(rows);
		double[][] result = new double[table.getRowCount()][3];
		for (int row = 0; row < table.getRowCount(); row++) {
			result[row][0] = table.getValue("col0", row);
			result[row][1] = table.getValue("col1", row);
			result[row][2] = table.getValue("col2", row);
		}

		assertArrayEquals(XYZRowsDeleted, result);
	}

	@Test
	void deleteRowsArrayList() {
		MarsTable table = buildTestXYZTable();
		ArrayList<Integer> rows = new ArrayList<>();
		rows.add(9);
		rows.add(10);
		rows.add(11);
		rows.add(12);
		rows.add(24);
		table.deleteRows(rows);
		double[][] result = new double[table.getRowCount()][3];
		for (int row = 0; row < table.getRowCount(); row++) {
			result[row][0] = table.getValue("col0", row);
			result[row][1] = table.getValue("col1", row);
			result[row][2] = table.getValue("col2", row);
		}

		assertArrayEquals(XYZRowsDeleted, result);
	}

	/*
	 * TEST keepRows()
	 */

	@Test
	void keepRows() {
		MarsTable table = buildTestXYZTable();
		int[] rows = new int[] { 9, 10, 11, 12, 24 };
		table.keepRows(rows);
		double[][] result = new double[table.getRowCount()][3];
		for (int row = 0; row < table.getRowCount(); row++) {
			result[row][0] = table.getValue("col0", row);
			result[row][1] = table.getValue("col1", row);
			result[row][2] = table.getValue("col2", row);
		}

		assertArrayEquals(XYZKeepRows, result);
	}

	@Test
	void keepRowsArrayList() {
		MarsTable table = buildTestXYZTable();
		ArrayList<Integer> rows = new ArrayList<>();
		rows.add(9);
		rows.add(10);
		rows.add(11);
		rows.add(12);
		rows.add(24);
		table.keepRows(rows);
		double[][] result = new double[table.getRowCount()][3];
		for (int row = 0; row < table.getRowCount(); row++) {
			result[row][0] = table.getValue("col0", row);
			result[row][1] = table.getValue("col1", row);
			result[row][2] = table.getValue("col2", row);
		}

		assertArrayEquals(XYZKeepRows, result);
	}

	/*
	 * TEST clone()
	 */

	@Test
	void cloneTest() {
		MarsTable table = buildTestXYStringTable();
		MarsTable copy = table.clone();

		assert (table.equals(copy));
	}

	/*
	 * UTILITY METHODS
	 */

	public static MarsTable buildTestArrayTable() {
		MarsTable table = new MarsTable();
		DoubleColumn col0 = new DoubleColumn("col0");
		for (double value : testArray)
			col0.add(value);

		table.add(col0);

		return table;
	}

	public static MarsTable buildTestArrayOddTable() {
		MarsTable table = new MarsTable();
		DoubleColumn col0 = new DoubleColumn("col0");
		for (double value : testArrayOdd)
			col0.add(value);

		table.add(col0);

		return table;
	}

	public static MarsTable buildTestArrayNaNsTable() {
		MarsTable table = new MarsTable();
		DoubleColumn col0 = new DoubleColumn("col0");
		for (double value : testArrayNaNs)
			col0.add(value);

		table.add(col0);

		return table;
	}

	public static MarsTable buildTestArrayAllNaNsTable() {
		MarsTable table = new MarsTable();
		DoubleColumn col0 = new DoubleColumn("col0");
		for (double value : testArrayAllNaNs)
			col0.add(value);

		table.add(col0);

		return table;
	}

	public static MarsTable buildTestXYTable() {
		MarsTable table = new MarsTable();
		DoubleColumn col0 = new DoubleColumn("col0");
		DoubleColumn col1 = new DoubleColumn("col1");
		for (double[] value : XY) {
			col0.add(value[0]);
			col1.add(value[1]);
		}

		table.add(col0);
		table.add(col1);

		return table;
	}

	public static MarsTable buildTestXYNaNsTable() {
		MarsTable table = new MarsTable();
		DoubleColumn col0 = new DoubleColumn("col0");
		DoubleColumn col1 = new DoubleColumn("col1");
		for (double[] value : XYNaNs) {
			col0.add(value[0]);
			col1.add(value[1]);
		}

		table.add(col0);
		table.add(col1);

		return table;
	}

	public static MarsTable buildTestXYAllNaNsTable() {
		MarsTable table = new MarsTable();
		DoubleColumn col0 = new DoubleColumn("col0");
		DoubleColumn col1 = new DoubleColumn("col1");
		for (double[] value : XYAllNaNs) {
			col0.add(value[0]);
			col1.add(value[1]);
		}

		table.add(col0);
		table.add(col1);

		return table;
	}

	public static MarsTable buildTestXYZTable() {
		MarsTable table = new MarsTable();
		DoubleColumn col0 = new DoubleColumn("col0");
		DoubleColumn col1 = new DoubleColumn("col1");
		DoubleColumn col2 = new DoubleColumn("col2");
		for (double[] value : XYZ) {
			col0.add(value[0]);
			col1.add(value[1]);
			col2.add(value[2]);
		}

		table.add(col0);
		table.add(col1);
		table.add(col2);

		return table;
	}

	public static MarsTable buildTestXYStringTable() {
		MarsTable table = new MarsTable();
		DoubleColumn col0 = new DoubleColumn("col0");
		DoubleColumn col1 = new DoubleColumn("col1");
		GenericColumn col2 = new GenericColumn("col2");
		for (double[] value : XYString) {
			col0.add(value[0]);
			col1.add(value[1]);
		}

		Collections.addAll(col2, stringColumn);

		table.add(col0);
		table.add(col1);
		table.add(col2);

		return table;
	}
}
