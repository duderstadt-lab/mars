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
package de.mpg.biochem.mars.table;

import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;

import org.scijava.display.Display;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.ui.UserInterface;
import org.scijava.ui.viewer.AbstractDisplayViewer;
import org.scijava.ui.viewer.DisplayViewer;

import net.imagej.display.WindowService;

@Plugin(type = DisplayViewer.class)
public class MARSResultsTableView extends AbstractDisplayViewer<MARSResultsTable> implements DisplayViewer<MARSResultsTable> {
	
	@Parameter
    private ResultsTableService resultsTableService;
	
	//This method is called to create and display a window
	//here we override it to make sure that calls like uiService.show( .. for SDMMResultsTable 
	//will use this method automatically..
	@Override
	public void view(final UserInterface ui, final Display<?> d) {
		MARSResultsTable results = (MARSResultsTable)d.get(0);
		results.setName(d.getName());

		resultsTableService.addResultsTable(results);
		d.setName(results.getName());
		
		//We also create a new window since we assume it is a new table...
		new MARSResultsTableWindow(results.getName(), results, resultsTableService);
	}

	@Override
	public boolean canView(final Display<?> d) {
		if (d instanceof MARSResultsTableDisplay) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public MARSResultsTableDisplay getDisplay() {
		return (MARSResultsTableDisplay) super.getDisplay();
	}

	@Override
	public boolean isCompatible(UserInterface arg0) {
		//Needs to be updated if all contexts are to be enabled beyond ImageJ
		return true;
	}
}
