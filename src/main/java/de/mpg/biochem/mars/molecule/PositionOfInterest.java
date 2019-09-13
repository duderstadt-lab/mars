package de.mpg.biochem.mars.molecule;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class PositionOfInterest implements JsonConvertibleRecord {
		String name;
		String column;
		String color;
		double position;
		
		public PositionOfInterest(String name) {
			this.name = name;
			this.position = 0;
			this.color = "rgba(50,50,50,0.2)";
		}
		
		public PositionOfInterest(JsonParser jParser) throws IOException {
			fromJSON(jParser);
		}
		
		public PositionOfInterest(String name, String column, double position, String color) {
			this.name = name;
			this.column = column;
			this.color = color;
			this.position = position;
		}

		@Override
		public void toJSON(JsonGenerator jGenerator) throws IOException {
			jGenerator.writeStartObject();
			jGenerator.writeStringField("name", name);
			jGenerator.writeStringField("column", column);
			jGenerator.writeNumberField("position",position);
			jGenerator.writeStringField("color", color);
			jGenerator.writeEndObject();
		}

		@Override
		public void fromJSON(JsonParser jParser) throws IOException {
			//Then we move through fields
	    	while (jParser.nextToken() != JsonToken.END_OBJECT) {
	    		String fieldname = jParser.getCurrentName();
	    		if ("name".equals(fieldname)) {
	    			jParser.nextToken();
	    			name = jParser.getText();
	    		}
	    		if ("column".equals(fieldname)) {
	    			jParser.nextToken();
	    			column = jParser.getText();
	    		}
	    		if ("position".equals(fieldname)) {
	    			jParser.nextToken();
	    			position = jParser.getDoubleValue();
	    		}
	    		if ("color".equals(fieldname)) {
	    			jParser.nextToken();
	    			color = jParser.getText();
	    		}
	    	}
		}
		
		//Getters and Setters
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public String getColumn() {
			return column;
		}
		
		public void setColumn(String column) {
			this.column = column;
		}
		
		public String getColor() {
			return color;
		}
		
		public void setColor(String color) {
			this.color = color;
		}
		
		public double getPosition() {
			return position;
		}
		
		public void setPosition(double position) {
			this.position = position;
		}
	}