/*-
 * #%L
 * Molecule Archive Suite (Mars) - core data storage and processing algorithms.
 * %%
 * Copyright (C) 2018 - 2020 Karl Duderstadt
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
package de.mpg.biochem.mars.molecule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import de.mpg.biochem.mars.kcp.commands.KCPCommand;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.table.MarsTable;
import de.mpg.biochem.mars.util.MarsUtil.ThrowingConsumer;

/**
 * Abstract superclass for molecule records. Molecule records act as the storage location for 
 * molecule properties. Molecule records are stored in {@link MoleculeArchive}s to allow 
 * for fast and efficient retrieval and optimal organization. 
 * <p>
 * Molecule records are designed to allow for storage of many different kinds
 * of single-molecule time-series data. They contain a primary {@link MarsTable} 
 * with molecule properties typically with each row containing features of a given time point. 
 * This may include position or intensity information. To facilitate efficient and reproducible 
 * processing molecule records may also contain calculated parameters, tags, notes, and 
 * kinetic change point segment {@link MarsTable}s generated by {@link KCPCommand}. 
 * Molecule records are assigned a random UID string at the time of creation derived from 
 * a base58 encoded UUID for readability. This serves as their primary identifier within 
 * {@link MoleculeArchive}s and for a range of transformations and merging operations. 
 * Molecule records also have a UID string for corresponding {@link MarsMetadata} records, 
 * which contain the experiments metadata information from. 
 * </p>
 * <p>
 * Molecule records can be saved to Json for storage when done processing. They are then either 
 * stored as an array within MoleculeArchive .yama files or as individual sml or json files within 
 * .yama.store directories.
 * </p>
 * @author Karl Duderstadt
 */
public abstract class AbstractMolecule extends AbstractMarsRecord implements Molecule {

	private String metadataUID;
	private int channel = -1;
	private int image = -1;
	private MarsTable table = new MarsTable();
	/**
	 * Segments tables resulting from change point fitting:
	 * xColumn is at index 0
	 * yColumn is at index 1
	 * region is at index 2
	 */
	private LinkedHashMap<ArrayList<String>, MarsTable> segmentTables = new LinkedHashMap<>();
	
	/**
	 * Constructor for creating an empty Molecule record. 
	 */
	public AbstractMolecule() {
		super();
	}
	
	/**
	 * Constructor for loading a Molecule record from a file. Typically,
	 * used when streaming records into memory when loading a {@link AbstractMoleculeArchive}
	 * or when a record is retrieved from the virtual store. 
	 * 
	 * @param jParser A JsonParser at the start of the molecule record Json
	 * for loading the molecule record from a file.
	 * @throws IOException Thrown if unable to read Json from the JsonParser stream.
	 */
	public AbstractMolecule(JsonParser jParser) throws IOException {
		super();
		fromJSON(jParser);
	}
	
	/**
	 * Constructor for creating an empty Molecule record with the
	 * specified UID. 
	 * 
	 * @param UID The unique identifier for this Molecule record.
	 */
	public AbstractMolecule(String UID) {
		super(UID);
	}

	/**
	 * Constructor for creating a new Molecule record with the
	 * specified UID and the {@link MarsTable} given
	 * as the DataTable. 
	 * 
	 * @param UID The unique identifier for this Molecule record.
	 * @param table The {@link MarsTable} to use for 
	 * initialization.
	 */
	public AbstractMolecule(String UID, MarsTable table) {
		super(UID);
		setTable(table);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void createIOMaps() {
		super.createIOMaps();
		
		setJsonField("table", 
			jGenerator -> {
					if (table.getColumnCount() > 0) {
						jGenerator.writeFieldName("table");
						table.toJSON(jGenerator);
					}
				}, 
			jParser -> table.fromJSON(jParser));	
				
		setJsonField("metadataUID", 
			jGenerator -> {
					if (metadataUID != null)
						jGenerator.writeStringField("metadataUID", metadataUID);
			 	}, 
			jParser -> metadataUID = jParser.getText());
		
		setJsonField("image", 
				jGenerator -> jGenerator.writeNumberField("image", image), 
				jParser -> image = jParser.getIntValue());
		
		setJsonField("channel", 
				jGenerator -> jGenerator.writeNumberField("channel", channel), 
				jParser -> channel = jParser.getIntValue());
			 	
		setJsonField("segmentTables", 
			jGenerator -> {
					if (segmentTables.size() > 0) {
						jGenerator.writeArrayFieldStart("segmentTables");
						for (ArrayList<String> tableColumnNames :segmentTables.keySet()) {
							if (segmentTables.get(tableColumnNames).size() > 0) {
								jGenerator.writeStartObject();
								
								jGenerator.writeStringField("xColumn", tableColumnNames.get(0));
								jGenerator.writeStringField("yColumn", tableColumnNames.get(1));
								jGenerator.writeStringField("region", tableColumnNames.get(2));
								
								jGenerator.writeFieldName("table");
								segmentTables.get(tableColumnNames).toJSON(jGenerator);
								
								jGenerator.writeEndObject();
							}
						}
						jGenerator.writeEndArray();
					}
			 	}, 
			jParser -> {
		    	while (jParser.nextToken() != JsonToken.END_ARRAY) {
			    	while (jParser.nextToken() != JsonToken.END_OBJECT) {
			    		String xColumn = "";
			    		String yColumn = "";
			    		String region = "";
			    		
					    ArrayList<String> tableColumnNames = new ArrayList<String>();
			    	
			    		//Needed for backwards compatibility when reverse order was used...
					    if ("xColumn".equals(jParser.getCurrentName())) {
					    	jParser.nextToken();
					    	xColumn = jParser.getText();
					    	
					    	//Then move past the field and next name
						    jParser.nextToken();
						    jParser.nextToken();
					    	yColumn = jParser.getText();
					    } else if ("yColumn".equals(jParser.getCurrentName())) {
					    	jParser.nextToken();
					    	yColumn = jParser.getText();
					    	
					    	//Then move past the field and next name
						    jParser.nextToken();
						    jParser.nextToken();
					    	xColumn = jParser.getText();
					    } 
					    
					    tableColumnNames.add(xColumn);
				    	tableColumnNames.add(yColumn);
					    
				    	//Move to next field
				    	jParser.nextToken();
				    	
				    	if ("region".equals(jParser.getCurrentName())) {
				    		jParser.nextToken();
				    		region = jParser.getText();
				    		tableColumnNames.add(region);
				    		
				    		//Move to table field
				    		jParser.nextToken();
				    	} else {
				    		//Must not have a region name
				    		tableColumnNames.add("");
				    	}
				    	
				    	MarsTable segmentTable = new MarsTable(yColumn + " vs " + xColumn + " - " + region);
				    	
				    	segmentTable.fromJSON(jParser);
				    	
				    	segmentTables.put(tableColumnNames, segmentTable);
			    	}
		    	}
			});
		
		/*
		 * 
		 * The fields below are needed for backwards compatibility.
		 * 
		 * Please remove for a future release.
		 * 
		 */
		
		setJsonField("DataTable", null, 
				jParser -> table.fromJSON(jParser));
		
		setJsonField("MetadataUID", null, 
				jParser -> metadataUID = jParser.getText());
		
		setJsonField("ImageMetadataUID", null, 
				jParser -> metadataUID = jParser.getText());
		
		setJsonField("ImageMetaDataUID", null, 
				jParser -> metadataUID = jParser.getText());
		
		setJsonField("SegmentTables", null, 
				jParser -> {
			    	while (jParser.nextToken() != JsonToken.END_ARRAY) {
				    	while (jParser.nextToken() != JsonToken.END_OBJECT) {
				    		String xColumnName = "";
				    		String yColumnName = "";
				    		String regionName = "";
				    		
						    ArrayList<String> tableColumnNames = new ArrayList<String>();
				    	
				    		//Needed for backwards compatibility when reverse order was used...
						    if ("xColumnName".equals(jParser.getCurrentName())) {
						    	jParser.nextToken();
						    	xColumnName = jParser.getText();
						    	
						    	//Then move past the field and next name
							    jParser.nextToken();
							    jParser.nextToken();
						    	yColumnName = jParser.getText();
						    } else if ("yColumnName".equals(jParser.getCurrentName())) {
						    	jParser.nextToken();
						    	yColumnName = jParser.getText();
						    	
						    	//Then move past the field and next name
							    jParser.nextToken();
							    jParser.nextToken();
						    	xColumnName = jParser.getText();
						    } 
						    
						    tableColumnNames.add(xColumnName);
					    	tableColumnNames.add(yColumnName);
						    
					    	//Move to next field
					    	jParser.nextToken();
					    	
					    	if ("RegionName".equals(jParser.getCurrentName())) {
					    		jParser.nextToken();
					    		regionName = jParser.getText();
					    		tableColumnNames.add(regionName);
					    		
					    		//Move to table field
					    		jParser.nextToken();
					    	} else {
					    		//Must not have a region name
					    		tableColumnNames.add("");
					    	}
					    	
					    	MarsTable segmenttable = new MarsTable(yColumnName + " vs " + xColumnName + " - " + regionName);
					    	
					    	segmenttable.fromJSON(jParser);
					    	
					    	segmentTables.put(tableColumnNames, segmenttable);
				    	}
			    	}
				});
	}
	
	/**
	 * Set the image index for this molecule. Format is 0 to 
	 * (image count) - 1. Set to -1 for no image index.
	 * 
	 * @param image Integer value for the image index.
	 */
	public void setImage(int image) {
		if (image > -1) {
			this.image = image;
			//if (parent != null) {
			//	parent.properties().addImage(image);
			//}
		} else
			channel = -1;
	}
	
	/**
	 * Get the image index for this molecule. If the molecule does
	 * not have image information this parameter will be set to
	 * -1. Format is 0 to (image count) - 1.
	 * 
	 * @return The image integer or -1 if not set.
	 */
	public int getImage() {		
		return image;
	}
	
	/**
	 * Set the channel for this molecule. Format is 0 to 
	 * (channel count) - 1. Set to -1 for no channel.
	 * 
	 * @param channel Integer value for the channel.
	 */
	public void setChannel(int channel) {
		if (channel > -1) {
			this.channel = channel;
			if (parent != null) {
				parent.properties().addChannel(channel);
			}
		} else
			channel = -1;
	}
	
	/**
	 * Get the channel for this molecules. If the molecule does
	 * not have channel information this parameter will be set to
	 * -1. Format is 0 to (channel count) - 1.
	 * 
	 * @return The channel integer or -1 if not set.
	 */
	public int getChannel() {		
		return channel;
	}
	
	/**
	 * Get the {@link MarsTable} holding the primary data for
	 * this record.
	 * 
	 * @return Table for this record.
	 */
	public MarsTable getTable() {
		return table;
	}
	
	@Deprecated
	public MarsTable getDataTable() {
		return table;
	}
	
	@Deprecated
	public void setDataTable(MarsTable table) {
		this.table.clear();
		this.table = table;
	}
	
	/**
	 * Set the {@link MarsTable} holding the primary data for
	 * this record. Usually this is tracking or intensity 
	 * as a function of time.
	 * 
	 * @param table The {@link MarsTable} to add or update in the 
	 * record.
	 */
	public void setTable(MarsTable table) {
		this.table.clear();
		this.table = table;
	}
	
	/**
	 * Set the UID of the {@link MarsMetadata} record associated with
	 * this molecule. The {@link MarsMetadata} contains information about
	 * the data collection (Timing of frames, colors, collection date, etc...)
	 * 
	 * @param metadataUID The new MarsMetadata UID to set.
	 */
	public void setMetadataUID(String metadataUID) {
		this.metadataUID = metadataUID;
	}
	
	/**
	 * Get the UID of the {@link MarsMetadata} record associated with
	 * this molecule. The {@link MarsMetadata} contains information about
	 * the data collection (Timing of frames, colors, collection date, etc...)
	 * 
	 * @return Return UID string of the metadata record associated with this molecule.
	 */
	public String getMetadataUID() {
		return metadataUID;
	}
		
	/**
	 * Add or update a segments table ({@link MarsTable}) generated 
	 * using the x column and y column names. The {@link KCPCommand} performs
	 * kinetic change point analysis generating segments to fit regions
	 * of a trace. The information about these segments is added using
	 * this method.
	 * 
	 * @param xColumn The name of the column used for x for KCP analysis.
	 * @param yColumn The name of the column used for y for KCP analysis.
	 * @param segmentsTable The {@link MarsTable} to add that contains the 
	 * segments.
	 */
	public void putSegmentsTable(String xColumn, String yColumn, MarsTable segmentsTable) {
		ArrayList<String> tableColumnNames = new ArrayList<String>();
		tableColumnNames.add(xColumn);
		tableColumnNames.add(yColumn);
		tableColumnNames.add("");
		segmentTables.put(tableColumnNames, segmentsTable);
	}
	
	/**
	 * Add or update a segments table ({@link MarsTable}) generated 
	 * using the x column, y column and region names. The {@link KCPCommand} performs
	 * kinetic change point analysis generating segments to fit regions
	 * of a trace. The information about these segments is added using
	 * this method.
	 * 
	 * @param xColumn The name of the column used for x for KCP analysis.
	 * @param yColumn The name of the column used for y for KCP analysis.
	 * @param region The name of the region used for analysis.
	 * @param segmentsTable The {@link MarsTable} to add that contains the 
	 * segments.
	 */
	public void putSegmentsTable(String xColumn, String yColumn, String region, MarsTable segmentsTable) {
		ArrayList<String> tableColumnNames = new ArrayList<String>();
		tableColumnNames.add(xColumn);
		tableColumnNames.add(yColumn);
		tableColumnNames.add(region);
		segmentTables.put(tableColumnNames, segmentsTable);
	}
	
	/**
	 * Retrieve a segments table ({@link MarsTable}) generated 
	 * using xColumn and yColumn.
	 * 
	 * @param xColumn The name of the x column used for analysis.
	 * @param yColumn The name of the y column used for analysis.
	 * @return The segments table.
	 */	
	public MarsTable getSegmentsTable(String xColumn, String yColumn) {
		ArrayList<String> tableColumnNames = new ArrayList<String>();
		tableColumnNames.add(xColumn);
		tableColumnNames.add(yColumn);
		tableColumnNames.add("");
		return segmentTables.get(tableColumnNames);
	}
	
	/**
	 * Retrieve a Segments table ({@link MarsTable}) generated 
	 * using xColum, yColumn and region names.
	 * 
	 * @param xColumn The name of the x column used for analysis.
	 * @param yColumn The name of the y column used for analysis.
	 * @param region The name of the region used for analysis.
	 * @return The segments table generated using the columns specified.
	 */	
	public MarsTable getSegmentsTable(String xColumn, String yColumn, String region) {
		ArrayList<String> tableColumnNames = new ArrayList<String>();
		tableColumnNames.add(xColumn);
		tableColumnNames.add(yColumn);
		tableColumnNames.add(region);
		return segmentTables.get(tableColumnNames);
	}
	
	/**
	 * Check if record has a segments table ({@link MarsTable}) generated 
	 * using xColumn and yColumn.
	 * 
	 * @param xColumn The name of the x column used for analysis.
	 * @param yColumn The name of the y column used for analysis.
	 * @return Returns true if the table exists and false if not.
	 */	
	public boolean hasSegmentsTable(String xColumn, String yColumn) {
		ArrayList<String> tableColumnNames = new ArrayList<String>();
		tableColumnNames.add(xColumn);
		tableColumnNames.add(yColumn);
		tableColumnNames.add("");
		return segmentTables.containsKey(tableColumnNames);
	}
	
	/**
	 * Check if record has a Segments table ({@link MarsTable}) generated 
	 * using xColumn, yColumn and region names.
	 * 
	 * @param xColumn The name of the x column used for analysis.
	 * @param yColumn The name of the y column used for analysis.
	 * @param region The name of the region used for analysis.
	 * @return Returns true if the table exists and false if not.
	 */	
	public boolean hasSegmentsTable(String xColumn, String yColumn, String region) {
		ArrayList<String> tableColumnNames = new ArrayList<String>();
		tableColumnNames.add(xColumn);
		tableColumnNames.add(yColumn);
		tableColumnNames.add(region);
		return segmentTables.containsKey(tableColumnNames);
	}
	
	/**
	 * Retrieve a segments table ({@link MarsTable}) generated 
	 * using x column, y column and region names provided in index positions 0, 1 and 
	 * 2 of an ArrayList, respectively.
	 * 
	 * @param tableColumnNames The list of x column, y column and region names.
	 * @return The MarsTable generated using the columns specified.
	 */	
	public MarsTable getSegmentsTable(ArrayList<String> tableColumnNames) {
		if (tableColumnNames.size() < 3)
			tableColumnNames.add("");
		return segmentTables.get(tableColumnNames);
	}
	
	/**
	 * Remove the Segments table ({@link MarsTable}) generated 
	 * using x column, y column and region names provided.
	 * 
	 * @param tableColumnNames List of the x column, y column, and region names of the segment table to remove.
	 */
	public void removeSegmentsTable(ArrayList<String> tableColumnNames) {
		segmentTables.remove(tableColumnNames);
	}
	
	/**
	 * Remove the Segments table ({@link MarsTable}) generated 
	 * using xColumn and yColumn.
	 * 
	 * @param xColumn The name of the x column used for analysis.
	 * @param yColumn The name of the y column used for analysis.
	 */
	public void removeSegmentsTable(String xColumn, String yColumn) {
		ArrayList<String> tableColumnNames = new ArrayList<String>();
		tableColumnNames.add(xColumn);
		tableColumnNames.add(yColumn);
		tableColumnNames.add("");
		segmentTables.remove(tableColumnNames);
	}
	
	/**
	 * Remove the segments table ({@link MarsTable}) generated 
	 * using x column, y column, and region names provided.
	 * 
	 * @param xColumn The name of the x column used for analysis.
	 * @param yColumn The name of the y column used for analysis.
	 * @param region The name of the region used for analysis.
	 */
	public void removeSegmentsTable(String xColumn, String yColumn, String region) {
		ArrayList<String> tableColumnNames = new ArrayList<String>();
		tableColumnNames.add(xColumn);
		tableColumnNames.add(yColumn);
		tableColumnNames.add(region);
		segmentTables.remove(tableColumnNames);
	}
	
	/**
	 * Get the set of segment table names as lists of x and y column names.
	 * 
	 * @return The set of ArrayLists holding the x and y column and region names at
	 * index positions 0, 1 and 2, respectively.
	 */
	public Set<ArrayList<String>> getSegmentsTableNames() {
		return segmentTables.keySet();
	}
}
