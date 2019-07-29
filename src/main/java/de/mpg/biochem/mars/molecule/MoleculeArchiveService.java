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
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
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
package de.mpg.biochem.mars.molecule;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.scif.services.FormatService;

import org.scijava.app.StatusService;
import org.scijava.display.DisplayService;
import org.scijava.event.EventHandler;
import org.scijava.event.EventService;
import org.scijava.event.SciJavaEvent;
import org.scijava.log.LogService;
import org.scijava.object.ObjectService;
import org.scijava.plugin.AbstractPTService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.script.ScriptService;
import org.scijava.service.Service;
import org.scijava.ui.UIService;

import net.imagej.ImageJService;

@SuppressWarnings({ "rawtypes", "unchecked" })
@Plugin(type = Service.class)
public class MoleculeArchiveService extends AbstractPTService<MoleculeArchiveService> implements ImageJService {
		
    @Parameter
    private UIService uiService;
    
    @Parameter
    private LogService logService;
    
    @Parameter
    private EventService eventService;
    
    @Parameter
    private PrefService prefService;
    
    @Parameter
	private FormatService formatService;
    
    @Parameter
    private StatusService statusService;
    
    @Parameter
    private ScriptService scriptService;
    
    @Parameter
    private DisplayService displayService;
    
    @Parameter
	private ObjectService objectService;
	
	@Override
	public void initialize() {				
		scriptService.addAlias(MoleculeArchive.class);
		scriptService.addAlias(MoleculeArchiveService.class);
	}
	
	public void addArchive(MoleculeArchive archive) {
		objectService.addObject(archive);
	}
	
	public void removeArchive(String title) {
		objectService.removeObject(getArchive(title));
	}
	
	public void removeArchive(MoleculeArchive archive) {
		objectService.removeObject(archive);
	}
	
	public boolean rename(String oldName, String newName) {
		List<MoleculeArchive<?,?,?>> archives = getArchives();
		
		if (archives.stream().anyMatch(archive -> archive.getName().equals(oldName))) {
			logService.error("No MoleculeArchive exists with the name " + oldName + ".");
			return false;
		}
		
		if (archives.stream().anyMatch(archive -> archive.getName().equals(newName))) {
			logService.error("A MoleculeArchive is already open with the name " + newName + ". Choose another name.");
			return false;
		} else {
			MoleculeArchive<?,?,?> archive = archives.stream().filter(a -> a.getName().equals(oldName)).findFirst().get();
			archive.setName(newName);
			displayService.getDisplay(oldName).setName(newName);
			return true;
		}
	}

	public ArrayList<String> getColumnNames() {
		Set<String> columnSet = new LinkedHashSet<String>();
		List<MoleculeArchive<?,?,?>> archives = getArchives();
		
		archives.forEach(archive -> columnSet.addAll(archive.getProperties().getColumnSet()));
		
		ArrayList<String> columns = new ArrayList<String>();
		columns.addAll(columnSet);
		
		return columns;
	}
	
	public Set<ArrayList<String>> getSegmentTableNames() {
		Set<ArrayList<String>> segTableNames = new LinkedHashSet<ArrayList<String>>();
		List<MoleculeArchive<?,?,?>> archives = getArchives();
	
		archives.forEach(archive -> segTableNames.addAll(archive.getProperties().getSegmentTableNames()));
		
		return segTableNames;
	}
	
	public ArrayList<String> getArchiveNames() {
		List<MoleculeArchive<?,?,?>> archives = getArchives();
		
		return (ArrayList<String>) archives.stream().map(archive -> archive.getName()).collect(Collectors.toList());
	}
	
	public boolean contains(String key) {
		return getArchives().stream().anyMatch(archive -> archive.getName().equals(key));
	}
	
	public MoleculeArchive<?,?,?> getArchive(String name) {
		return getArchives().stream().filter(a -> a.getName().equals(name)).findFirst().get();
	}
	
	public List<MoleculeArchive<?,?,?>> getArchives() { 
		return (List) objectService.getObjects(MoleculeArchive.class);
	}
	
	public UIService getUIService() {
		return uiService; 
	}
	
	public LogService getLogService() {
		return logService;
	}
	
	public StatusService getStatusService() {
		return statusService;
	}
	
	public PrefService getPrefService() {
		return prefService;
	}
	
	public FormatService getFormatService() {
		return formatService;
	}
	
	@Override
	public Class<MoleculeArchiveService> getPluginType() {
		return MoleculeArchiveService.class;
	}
}
