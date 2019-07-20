package de.mpg.biochem.mars.molecule;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

import de.mpg.biochem.mars.table.MarsResultsTable;

public class SingleMoleculeArchive extends AbstractMoleculeArchive<SingleMolecule, SDMMImageMetadata, SingleMoleculeArchiveProperties> {
	
	public SingleMoleculeArchive(String name) {
		super(name);
	}
	
	public SingleMoleculeArchive(File file) throws IOException, JsonParseException {
		super(file);
	}
	
	public SingleMoleculeArchive(String name, MarsResultsTable table, MoleculeArchiveService moleculeArchiveService) {
		super(name, table, moleculeArchiveService);
	}
	
	public SingleMoleculeArchive(String name, File file, MoleculeArchiveService moleculeArchiveService) throws JsonParseException, IOException {
		super(name, file, moleculeArchiveService);
	}
	
	protected SingleMoleculeArchiveProperties createProperties() {
		return new SingleMoleculeArchiveProperties();
	}
	
	protected SingleMoleculeArchiveProperties createProperties(JsonParser jParser) throws IOException {
		return new SingleMoleculeArchiveProperties(jParser);
	}
	
	protected SDMMImageMetadata createImageMetadata(JsonParser jParser) throws IOException {
		return new SDMMImageMetadata(jParser);
	}
	
	protected SDMMImageMetadata createImageMetadata(String metaUID) {
		return new SDMMImageMetadata(metaUID);
	}
	
	protected SingleMolecule createMolecule() {
		return new SingleMolecule();
	}
	
	protected SingleMolecule createMolecule(JsonParser jParser) throws IOException {
		return new SingleMolecule(jParser);
	}
	
	protected SingleMolecule createMolecule(String UID) {
		return new SingleMolecule(UID);
	}
	
	protected SingleMolecule createMolecule(String UID, MarsResultsTable table) {
		return new SingleMolecule(UID, table);
	}
	
}
