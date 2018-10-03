package de.mpg.biochem.sdmm.molecule;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.DialogPrompt.MessageType;
import org.scijava.ui.DialogPrompt.OptionType;
import org.scijava.ui.UIService;
import org.scijava.widget.FileWidget;

import com.fasterxml.jackson.core.JsonParseException;

import de.mpg.biochem.sdmm.table.ResultsTableService;
import de.mpg.biochem.sdmm.table.SDMMResultsTable;
import de.mpg.biochem.sdmm.util.LogBuilder;
import net.imagej.ops.Initializable;

import javax.swing.filechooser.FileSystemView;

@Plugin(type = Command.class, label = "Build archive from table", menu = {
		@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT,
				mnemonic = MenuConstants.PLUGINS_MNEMONIC),
		@Menu(label = "SDMM Plugins", weight = MenuConstants.PLUGINS_WEIGHT,
			mnemonic = 's'),
		@Menu(label = "Molecule Utils", weight = 1,
			mnemonic = 'm'),
		@Menu(label = "Build archive from table", weight = 10, mnemonic = 'b')})
public class BuildArchiveFromTableCommand extends DynamicCommand {
	@Parameter
    private MoleculeArchiveService moleculeArchiveService;
	
	@Parameter
	private ResultsTableService resultsTableService;
	
    @Parameter
    private UIService uiService;
    
    @Parameter
    private StatusService statusService;
    
    @Parameter
    private LogService logService;
    
    @Parameter(label="Table with molecule column")
	private SDMMResultsTable table;
    
    @Parameter(label="build in virtual memory")
    private boolean virtual = true;
    
    //OUTPUT PARAMETERS
	@Parameter(label="Molecule Archive", type = ItemIO.OUTPUT)
	private MoleculeArchive archive;
    
    @Override
	public void run() {				
		
		String name = table.getName() + ".yama";
		
		if (moleculeArchiveService.contains(name)) {
			uiService.showDialog("The MoleculeArchive " + name + " has already been created and is open.", MessageType.ERROR_MESSAGE, OptionType.DEFAULT_OPTION);
			return;
		}
		
		if (table.get("molecule") == null) {
			uiService.showDialog("The table given doesn't have a molecule column. It must have a molecule column in order to generate the Molecule Archive.", MessageType.ERROR_MESSAGE, OptionType.DEFAULT_OPTION);
			return;
		}
		
		LogBuilder builder = new LogBuilder();
		
		String log = builder.buildTitleBlock("Building MoleculeArchive from Table");

		builder.addParameter("From Table", table.getName());
		builder.addParameter("Virtual", String.valueOf(virtual));
		builder.addParameter("Ouput Archive Name", name);
		
		archive = new MoleculeArchive(name, table, resultsTableService, moleculeArchiveService, virtual);

		builder.addParameter("Molecules addeded", String.valueOf(archive.getNumberOfMolecules()));
		log += builder.buildParameterList();
		log += builder.endBlock(true);
		
		//Make sure the output archive has the correct name
		getInfo().getMutableOutput("archive", MoleculeArchive.class).setLabel(archive.getName());
        
        archive.addLogMessage(log);
        
        logService.info(log);
	}
    
    public MoleculeArchive getArchive() {
    	return archive;
    }
    
    public void setTable(SDMMResultsTable table) {
    	this.table = table;
    }
    
    public SDMMResultsTable getTable() {
    	return table;
    }
    
    public void setVirtual(boolean virtual) {
    	this.virtual = virtual;
    }
    
    public boolean getVirtual() {
    	return virtual;
    }
}