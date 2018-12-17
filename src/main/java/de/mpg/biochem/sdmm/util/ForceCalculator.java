/*******************************************************************************
 * MARS - MoleculeArchive Suite - A collection of ImageJ2 commands for single-molecule analysis.
 * 
 * Copyright (C) 2018 - 2019 Karl Duderstadt
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.mpg.biochem.sdmm.util;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.AllowedSolution;
import org.apache.commons.math3.analysis.solvers.BracketingNthOrderBrentSolver;

//From http://commons.apache.org/proper/commons-math/userguide/analysis.html
public class ForceCalculator implements UnivariateFunction {
	  final double relativeAccuracy = 1.0e-12;
	  final double absoluteAccuracy = 1.0e-9;
	  final int    maxOrder         = 5;
	  final double kB = 1.380648528*Math.pow(10,-23);
	  double temperature = 296.15;
	  
	  BracketingNthOrderBrentSolver solver;
	  
	  double persistenceLength, L0, msd;
	
	  public ForceCalculator(double persistenceLength, double L0) {
		  this.persistenceLength = persistenceLength;
		  this.L0 = L0;
		  
		  solver = new BracketingNthOrderBrentSolver(relativeAccuracy, absoluteAccuracy, maxOrder);
	  }
	  
	  public ForceCalculator(double persistenceLength, double L0, double temperature) {
		  this.temperature = temperature;
		  this.persistenceLength = persistenceLength;
		  this.L0 = L0;
		  
		  solver = new BracketingNthOrderBrentSolver(relativeAccuracy, absoluteAccuracy, maxOrder);
	  }
	  
	  public double[] calculate(double msd) {
		    this.msd = msd;
			double length = Double.NaN;
			try {
				//the length must be longer than 0.1 nm and shorter than 1/10000 th of full length
			    length = solver.solve(100, this, Math.pow(10, -10), L0 - L0/10000, AllowedSolution.ANY_SIDE);
			} catch (LocalException le) {
				length = Double.NaN;
			}
		  
			double[] output = new double[2];
			output[0] = getWLCForce(length);
			output[1] = length;
			
			return output;
	  }
	
	   public double value(double length) {
	     return getEquipartitionForce(length) - getWLCForce(length);
	   }
	   
	   public double getWLCForce(double length) {
		   double a = kB*temperature/persistenceLength;
		   return a*(0.25*(Math.pow(1-length/L0,-2)) - 0.25 + length/L0);
	   }
	   
	   public double getEquipartitionForce(double length) {
		   return (kB*temperature*length)/msd;
	   }
	   
	   private static class LocalException extends RuntimeException {

		   // the x value that caused the problem
		   private final double x;

		   public LocalException(double x) {
		     this.x = x;
		   }

		   public double getX() {
		     return x;
		   }

		 }
}
