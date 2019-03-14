/*******************************************************************************
 * Copyright (C) 2019, Karl Duderstadt
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package de.mpg.biochem.mars.RoiTools;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import de.mpg.biochem.mars.table.*;
import de.mpg.biochem.mars.util.LevenbergMarquardt;
import de.mpg.biochem.mars.util.LogBuilder;

@Plugin(type = Command.class, label = "Transformation Parameter Calculator", menu = {
		@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT,
				mnemonic = MenuConstants.PLUGINS_MNEMONIC),
		@Menu(label = "MoleculeArchive Suite", weight = MenuConstants.PLUGINS_WEIGHT,
			mnemonic = 's'),
		@Menu(label = "ROI Tools", weight = 30,
			mnemonic = 'r'),
		@Menu(label = "Transformation Parameter Calculator", weight = 20, mnemonic = 't')})
public class TransformationParameterCalculatorCommand extends DynamicCommand implements Command {
	@Parameter
	private LogService logService;
	
	@Parameter(label="Coordinate map table")
	private MARSResultsTable data_table;
	
	@Parameter(label="initial_x_translation")
	private double initial_x_translation = 1;
	
	@Parameter(label="initial_y_translation")
	private double initial_y_translation = 1;
	
	@Parameter(label="initial_x_scaling")
	private double initial_x_scaling = 1;
	
	@Parameter(label="initial_y_scaling")
	private double initial_y_scaling = 1;
	
	@Parameter(label="initial_rotation_angle")
	private double initial_rotation_angle = 0;
	
	@Parameter(label="vary_x_translation")
	private boolean vary_x_translation = true;
	
	@Parameter(label="vary_y_translation")
	private boolean vary_y_translation = true;
	
	@Parameter(label="vary_x_scaling")
	private boolean vary_x_scaling = true;
	
	@Parameter(label="vary_y_scaling")
	private boolean vary_y_scaling = true;
	
	@Parameter(label="vary_rotation_angle")
	private boolean vary_rotation_angle = true;
	
	//OUTPUT
	@Parameter(label="Coordinate Alignment Parameters", type = ItemIO.OUTPUT)
	private MARSResultsTable alignment_parameters_table;
	
	private double precision = 1e-6;
	
	private LevenbergMarquardt lm = new LevenbergMarquardt() {
		
		@Override
		public double getValue(double[] x, double[] p, double[] dyda) {
			if (x[2] == 0) {
				//Means that the x' equation should be returned...
				dyda[0] = 1;
				dyda[1] = 0;
				dyda[2] = Math.cos(p[4])*x[0];
				dyda[3] = -Math.sin(p[4])*x[1];
				dyda[4] = - p[2]*Math.sin(p[4])*x[0] - p[3]*Math.cos(p[4])*x[1];
				
				return p[2]*Math.cos(p[4])*x[0] - p[3]*Math.sin(p[4])*x[1] + p[0];
			} else {
				//Means that the y' equation should be returned...
				dyda[0] = 0;
				dyda[1] = 1;
				dyda[2] = Math.sin(p[4])*x[0];
				dyda[3] = Math.cos(p[4])*x[1];
				dyda[4] = p[2]*Math.cos(p[4])*x[0] - p[3]*Math.sin(p[4])*x[1];
				
				return p[2]*Math.sin(p[4])*x[0] + p[3]*Math.cos(p[4])*x[1] + p[1];
			}
		}
	};
	
	@Override
	public void run() {
		
		//Build log message
		LogBuilder builder = new LogBuilder();
		
		String log = builder.buildTitleBlock("Transformation Parameter Calculator");
		
		addInputParameterLog(builder);
		log += builder.buildParameterList();
		
		//Output first part of log message...
		logService.info(log);
		
		//Code for image alignment...first we just put some random arrays...later we build from a table.
		//See laue wiki entry to more information about the configuration of the values and inputs/outputs..
		double[][] xs = new double[data_table.getRowCount()*2][3];
		double[] ys = new double[xs.length];
		
		int cur_row = 0;
		
		//We will need to duplicate all the points for everything to work nicely with the LM code (see wiki entry)
		for (int i = 0; i < data_table.getRowCount()*2; i+=2 ) {
			xs[i][0] = data_table.getValue("x1", cur_row);
			xs[i][1] = data_table.getValue("y1", cur_row);
			xs[i][2] = 0;
			
			ys[i] = data_table.getValue("x2", cur_row);
			
			xs[i+1][0] = data_table.getValue("x1", cur_row);
			xs[i+1][1] = data_table.getValue("y1", cur_row);
			xs[i+1][2] = 1;
			
			ys[i+1] = data_table.getValue("y2", cur_row);
			
			cur_row++;
		}
		
		double[] p = new double[5];
		p[0] = initial_x_translation;
		p[1] = initial_y_translation;
		p[2] = initial_x_scaling;
		p[3] = initial_y_scaling;
		p[4] = initial_rotation_angle;
		
		boolean[] vary = new boolean[5];
		vary[0] = vary_x_translation;
		vary[1] = vary_y_translation;
		vary[2] = vary_x_scaling;
		vary[3] = vary_y_scaling;
		vary[4] = vary_rotation_angle;
		
		lm.precision = precision;
		
		int n = xs.length;
		double[] e = new double[p.length];
		
		lm.solve(xs, ys, null, n, p, vary, e, 0.000001);
		
		alignment_parameters_table = new MARSResultsTable(10, 1);
		alignment_parameters_table.setName("Coordinate Alignment Parameters");
		alignment_parameters_table.setColumnHeader(0, "x_translation");
		alignment_parameters_table.setColumnHeader(1, "x_translation_error");
		alignment_parameters_table.setColumnHeader(2, "y_translation");
		alignment_parameters_table.setColumnHeader(3, "y_translation_error");
		alignment_parameters_table.setColumnHeader(4, "x_scaling");
		alignment_parameters_table.setColumnHeader(5, "x_scaling_error");
		alignment_parameters_table.setColumnHeader(6, "y_scaling");
		alignment_parameters_table.setColumnHeader(7, "y_scaling_error");
		alignment_parameters_table.setColumnHeader(8, "rotation_angle");
		alignment_parameters_table.setColumnHeader(9, "rotation_angle_error");
		
		alignment_parameters_table.setValue("x_translation", 0, p[0]);
		alignment_parameters_table.setValue("x_translation_error", 0, e[0]);
		alignment_parameters_table.setValue("y_translation", 0, p[1]);
		alignment_parameters_table.setValue("y_translation_error", 0, e[1]);
		alignment_parameters_table.setValue("x_scaling", 0, p[2]);
		alignment_parameters_table.setValue("x_scaling_error", 0, e[2]);
		alignment_parameters_table.setValue("y_scaling", 0, p[3]);
		alignment_parameters_table.setValue("y_scaling_error", 0, e[3]);
		alignment_parameters_table.setValue("rotation_angle", 0, p[4]);
		alignment_parameters_table.setValue("rotation_angle_error", 0, e[4]);
		
		logService.info(LogBuilder.endBlock(true));
		logService.info(" ");
	}
	
	private void addInputParameterLog(LogBuilder builder) {
		builder.addParameter("Coordinate map table", data_table.getName());
		
		builder.addParameter("initial_x_translation", String.valueOf(initial_x_translation));
		builder.addParameter("initial_y_translation", String.valueOf(initial_y_translation));
		builder.addParameter("initial_x_scaling", String.valueOf(initial_x_scaling));
		builder.addParameter("initial_y_scaling", String.valueOf(initial_y_scaling));
		builder.addParameter("initial_rotation_angle", String.valueOf(initial_rotation_angle));
		
		builder.addParameter("vary_x_translation", String.valueOf(vary_x_translation));
		builder.addParameter("vary_y_translation", String.valueOf(vary_y_translation));
		builder.addParameter("vary_x_scaling", String.valueOf(vary_x_scaling));
		builder.addParameter("vary_y_scaling", String.valueOf(vary_y_scaling));
		builder.addParameter("vary_rotation_angle", String.valueOf(vary_rotation_angle));
	}
}
