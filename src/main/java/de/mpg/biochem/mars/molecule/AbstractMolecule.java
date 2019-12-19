/*******************************************************************************
 * Copyright (C) 2019, Duderstadt Lab
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
package de.mpg.biochem.mars.molecule;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import de.mpg.biochem.mars.kcp.commands.KCPCommand;
import de.mpg.biochem.mars.table.MarsTable;
import de.mpg.biochem.mars.util.MarsUtil;

/**
 * Abstract superclass for Molecule records. Molecule records act as the storage location for 
 * molecule properties. Molecule records are stored in {@link MoleculeArchive}s to allow 
 * for fast and efficient retrieval and optimal organization. 
 * <p>
 * Molecule records are designed to allow for storage of many different kinds
 * of single-molecule time-series data. They contain a primary {@link MarsTable} 
 * (or DataTable) with molecule properties typically as a function of time/slice of a video. 
 * This may include position or intensity information. To facilitate efficient and reproducible 
 * processing Molecule records may also contain calculated parameters, tags, notes, and 
 * kinetic change point segment {@link MarsTable}s generated by {@link KCPCommand}. 
 * Molecule records are assigned a random UID string at the time of creation derived from 
 * a base58 encoded UUID for readability. This serves as their primary identifier within 
 * {@link MoleculeArchive}s and for a range of transformations and merging operations. 
 * Molecule records also have a UID string for corresponding {@link SdmmImageMetadata} records, 
 * which contain information about the imaging settings, the timing of frames etc.. 
 * during data collection. 
 * </p>
 * <p>
 * Molecule records can be saved to JSON for storage when done processing. They are then either 
 * stored as an array within MoleculeArchive .yama files or as individual json files within 
 * .yama.store directories.
 * </p>
 * @author Karl Duderstadt
 */
public abstract class AbstractMolecule extends AbstractMarsRecord implements Molecule {
	//UID of ImageMetadata associated wit this molecule.
	protected String imageMetadataUID;
	
	//Segments tables resulting from change point fitting
	//ArrayList has two items:
	//XColumn is at index 0
	//YColumn is at index 1
	//RegionName is at index 2
	protected LinkedHashMap<ArrayList<String>, MarsTable> segmentTables;
	
	/**
	 * Constructor for creating an empty Molecule record. 
	 */
	public AbstractMolecule() {
		super();
		segmentTables = new LinkedHashMap<>();
	}
	
	/**
	 * Constructor for loading a Molecule record from a file. Typically,
	 * used when streaming records into memory when loading a {@link AbstractMoleculeArchive}
	 * or when a record is retrieved from the virtual store. 
	 * 
	 * @param jParser A JsonParser at the start of the molecule record json
	 * for loading the molecule record from a file.
	 * @throws IOException Thrown if unable to read Json from the JsonParser stream.
	 */
	public AbstractMolecule(JsonParser jParser) throws IOException {
		super();
		segmentTables = new LinkedHashMap<>();
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
		segmentTables = new LinkedHashMap<>();
	}

	/**
	 * Constructor for creating a new Molecule record with the
	 * specified UID and the {@link MarsTable} given
	 * as the DataTable. 
	 * 
	 * @param UID The unique identifier for this Molecule record.
	 * @param dataTable The {@link MarsTable} to use for 
	 * initialization.
	 */
	public AbstractMolecule(String UID, MarsTable dataTable) {
		super(UID, dataTable);
		segmentTables = new LinkedHashMap<>();
	}
	
	@Override
	protected void createIOMaps() {
		super.createIOMaps();
		
		//Add to output map
		outputMap.put("metaUID", MarsUtil.catchConsumerException(jGenerator -> {
			if (imageMetadataUID != null)
				jGenerator.writeStringField("ImageMetadataUID", imageMetadataUID);
	 	}, IOException.class));
		outputMap.put("SegmentTables", MarsUtil.catchConsumerException(jGenerator -> {
			if (segmentTables.size() > 0) {
				jGenerator.writeArrayFieldStart("SegmentTables");
				for (ArrayList<String> tableColumnNames :segmentTables.keySet()) {
					if (segmentTables.get(tableColumnNames).size() > 0) {
						jGenerator.writeStartObject();
						
						jGenerator.writeStringField("xColumnName", tableColumnNames.get(0));
						jGenerator.writeStringField("yColumnName", tableColumnNames.get(1));
						jGenerator.writeStringField("RegionName", tableColumnNames.get(2));
						
						jGenerator.writeFieldName("Table");
						segmentTables.get(tableColumnNames).toJSON(jGenerator);
						
						jGenerator.writeEndObject();
					}
				}
				jGenerator.writeEndArray();
			}
	 	}, IOException.class));
		
		//Add to input map
		inputMap.put("ImageMetadataUID", MarsUtil.catchConsumerException(jParser -> {
	    	imageMetadataUID = jParser.getText();
		}, IOException.class));
		inputMap.put("ImageMetaDataUID", MarsUtil.catchConsumerException(jParser -> {
	    	imageMetadataUID = jParser.getText();
		}, IOException.class));
		inputMap.put("SegmentTables", MarsUtil.catchConsumerException(jParser -> {
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
		}, IOException.class));
	}
	
	/**
	 * Generate a JSON String representation of the molecule record.
	 * 
	 * @return Return a JSON string representation of the molecule.
	 */
	public String toJSONString() {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();

		JsonFactory jfactory = new JsonFactory();
		JsonGenerator jGenerator;
		try {
			jGenerator = jfactory.createGenerator(stream, JsonEncoding.UTF8);
			toJSON(jGenerator);
			jGenerator.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return stream.toString();
	}
	
	/**
	 * Set the UID of the {@link MarsImageMetadata} record associated with
	 * this molecule. The {@link MarsImageMetadata} contains information about
	 * the data collection (Timing of frames, colors, collection date, etc...)
	 * 
	 * @param imageMetadataUID The new MarsImageMetadata UID to set.
	 */
	public void setImageMetadataUID(String imageMetadataUID) {
		this.imageMetadataUID = imageMetadataUID;
	}
	
	/**
	 * Get the UID of the {@link MarsImageMetadata} record associated with
	 * this molecule. The {@link MarsImageMetadata} contains information about
	 * the data collection (Timing of frames, colors, collection date, etc...)
	 * 
	 * @return Return a JSON string representation of the molecule.
	 */
	public String getImageMetadataUID() {
		return imageMetadataUID;
	}
		
	/**
	 * Add or update a Segments table ({@link MarsTable}) generated 
	 * using the yColumnName and xColumnName. The {@link KCPCommand} performs
	 * kinetic change point analysis generating segments to fit regions
	 * of a trace. The information about these segments is added using
	 * this method.
	 * 
	 * @param xColumnName The name of the column used for x for KCP analysis.
	 * @param yColumnName The name of the column used for y for KCP analysis.
	 * @param segs The {@link MarsTable} to add that contains the 
	 * segments.
	 */
	public void putSegmentsTable(String xColumnName, String yColumnName, MarsTable segs) {
		ArrayList<String> tableColumnNames = new ArrayList<String>();
		tableColumnNames.add(xColumnName);
		tableColumnNames.add(yColumnName);
		tableColumnNames.add("");
		segmentTables.put(tableColumnNames, segs);
	}
	
	/**
	 * Add or update a Segments table ({@link MarsTable}) generated 
	 * using the yColumnName and xColumnName and region. The {@link KCPCommand} performs
	 * kinetic change point analysis generating segments to fit regions
	 * of a trace. The information about these segments is added using
	 * this method.
	 * 
	 * @param xColumnName The name of the column used for x for KCP analysis.
	 * @param yColumnName The name of the column used for y for KCP analysis.
	 * @param regionName the name of the region used for analysis.
	 * @param segs The {@link MarsTable} to add that contains the 
	 * segments.
	 */
	public void putSegmentsTable(String xColumnName, String yColumnName, String regionName, MarsTable segs) {
		ArrayList<String> tableColumnNames = new ArrayList<String>();
		tableColumnNames.add(xColumnName);
		tableColumnNames.add(yColumnName);
		tableColumnNames.add(regionName);
		segmentTables.put(tableColumnNames, segs);
	}
	
	/**
	 * Retrieve a Segments table ({@link MarsTable}) generated 
	 * using xColumnName and yColumnName.
	 * 
	 * @param xColumnName The name of the x column used for analysis.
	 * @param yColumnName The name of the y column used for analysis.
	 * @return The MARSResultsTable generated using the columns specified.
	 */	
	public MarsTable getSegmentsTable(String xColumnName, String yColumnName) {
		ArrayList<String> tableColumnNames = new ArrayList<String>();
		tableColumnNames.add(xColumnName);
		tableColumnNames.add(yColumnName);
		tableColumnNames.add("");
		return segmentTables.get(tableColumnNames);
	}
	
	/**
	 * Retrieve a Segments table ({@link MarsTable}) generated 
	 * using xColumnName and yColumnName and possibly the region name.
	 * 
	 * @param xColumnName The name of the x column used for analysis.
	 * @param yColumnName The name of the y column used for analysis.
	 * @param regionName the name of the region used for analysis.
	 * @return The MARSResultsTable generated using the columns specified.
	 */	
	public MarsTable getSegmentsTable(String xColumnName, String yColumnName, String regionName) {
		ArrayList<String> tableColumnNames = new ArrayList<String>();
		tableColumnNames.add(xColumnName);
		tableColumnNames.add(yColumnName);
		tableColumnNames.add(regionName);
		return segmentTables.get(tableColumnNames);
	}
	
	/**
	 * Check if record has a Segments table ({@link MarsTable}) generated 
	 * using xColumnName and yColumnName.
	 * 
	 * @param xColumnName The name of the x column used for analysis.
	 * @param yColumnName The name of the y column used for analysis.
	 * @return Boolean whether the segment table exists.
	 */	
	public boolean hasSegmentsTable(String xColumnName, String yColumnName) {
		ArrayList<String> tableColumnNames = new ArrayList<String>();
		tableColumnNames.add(xColumnName);
		tableColumnNames.add(yColumnName);
		tableColumnNames.add("");
		return segmentTables.containsKey(tableColumnNames);
	}
	
	/**
	 * Check if record has a Segments table ({@link MarsTable}) generated 
	 * using xColumnName and yColumnName.
	 * 
	 * @param xColumnName The name of the x column used for analysis.
	 * @param yColumnName The name of the y column used for analysis.
	 * @param regionName the name of the region used for analysis.
	 * @return Boolean whether the segment table exists.
	 */	
	public boolean hasSegmentsTable(String xColumnName, String yColumnName, String regionName) {
		ArrayList<String> tableColumnNames = new ArrayList<String>();
		tableColumnNames.add(xColumnName);
		tableColumnNames.add(yColumnName);
		tableColumnNames.add(regionName);
		return segmentTables.containsKey(tableColumnNames);
	}
	
	/**
	 * Retrieve a Segments table ({@link MarsTable}) generated 
	 * using yColumnName and xColumnName provided in index positions 0
	 * and 1 of an ArrayList, respectively.
	 * 
	 * @param tableColumnNames The xColumnName and yColumnName used when
	 * generating the table, provided in index positions 0 and 1 of an 
	 * ArrayList, respectively. Additionally, a region name can be added 
	 * in the 2 position.
	 * @return The MARSResultsTable generated using the columns specified.
	 */	
	public MarsTable getSegmentsTable(ArrayList<String> tableColumnNames) {
		if (tableColumnNames.size() < 3)
			tableColumnNames.add("");
		return segmentTables.get(tableColumnNames);
	}
	
	/**
	 * Remove the Segments table ({@link MarsTable}) generated 
	 * using X Column, Y Column and region.
	 * 
	 * @param tableColumnNames List of the X Column, Y Column, and region of the segment table to remove.
	 */
	public void removeSegmentsTable(ArrayList<String> tableColumnNames) {
		segmentTables.remove(tableColumnNames);
	}
	
	/**
	 * Remove the Segments table ({@link MarsTable}) generated 
	 * using yColumnName and xColumnName.
	 * 
	 * @param xColumnName The name of the x column used for analysis.
	 * @param yColumnName The name of the y column used for analysis.
	 */
	public void removeSegmentsTable(String xColumnName, String yColumnName) {
		ArrayList<String> tableColumnNames = new ArrayList<String>();
		tableColumnNames.add(xColumnName);
		tableColumnNames.add(yColumnName);
		tableColumnNames.add("");
		segmentTables.remove(tableColumnNames);
	}
	
	/**
	 * Remove the Segments table ({@link MarsTable}) generated 
	 * using yColumnName and xColumnName.
	 * 
	 * @param xColumnName The name of the x column used for analysis.
	 * @param yColumnName The name of the y column used for analysis.
	 * @param regionName The name of the region used for analysis.
	 */
	public void removeSegmentsTable(String xColumnName, String yColumnName, String regionName) {
		ArrayList<String> tableColumnNames = new ArrayList<String>();
		tableColumnNames.add(xColumnName);
		tableColumnNames.add(yColumnName);
		tableColumnNames.add(regionName);
		segmentTables.remove(tableColumnNames);
	}
	
	/**
	 * Retrieve a Segments table ({@link MarsTable}) generated 
	 * using yColumnName and xColumnName.
	 * 
	 * @return The Set of ArrayLists holding the x and y column names at
	 * index positions 0 and 1, respectively.
	 */
	public Set<ArrayList<String>> getSegmentTableNames() {
		return segmentTables.keySet();
	}
}
