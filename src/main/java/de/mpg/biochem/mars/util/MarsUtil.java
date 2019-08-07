package de.mpg.biochem.mars.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.function.Predicate;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.format.DataFormatDetector;
import com.fasterxml.jackson.core.format.DataFormatMatcher;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;

import de.mpg.biochem.mars.molecule.MoleculeArchive;

public class MarsUtil {
  	
	/**
	 * Used to bypass unknown Json Objects with JacksonJson streaming interface.
	 * 
	 * @param jParser
	 * @throws IOException
	 */
  	public static void passThroughUnknownObjects(JsonParser jParser) throws IOException {
      	while (jParser.nextToken() != JsonToken.END_OBJECT) {
      		if (jParser.getCurrentToken() == JsonToken.START_OBJECT)
      			passThroughUnknownObjects(jParser);
      	}
  	}
  	
	public static String getArchiveTypeFromYama(File file) throws JsonParseException, IOException {
		InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
		
		//Here we automatically detect the format of the JSON file
		//Can be JSON text or Smile encoded binary file...
		JsonFactory jsonF = new JsonFactory();
		SmileFactory smileF = new SmileFactory(); 
		DataFormatDetector det = new DataFormatDetector(new JsonFactory[] { jsonF, smileF });
	    DataFormatMatcher match = det.findFormat(inputStream);
	    JsonParser jParser = match.createParserWithMatch();
	    
	    String archiveType = "de.mpg.biochem.mars.molecule.SingleMoleculeArchive";
	    
		jParser.nextToken();
		jParser.nextToken();
		if ("MoleculeArchiveProperties".equals(jParser.getCurrentName())) {
			jParser.nextToken();
			while (jParser.nextToken() != JsonToken.END_OBJECT) {
			    String fieldname = jParser.getCurrentName();
			    	
			    if ("ArchiveType".equals(fieldname)) {
			    	jParser.nextToken();
			    	archiveType = jParser.getText();
			    	break;
			    }
			}
		} else {
			System.out.println("The file " + file.getName() + " doesn't have a MoleculeArchiveProperties field. Is this a proper yama file?");
			return null;
		}
		
		jParser.close();
		inputStream.close();
		
		return archiveType;
	}
	
	public static String getArchiveTypeFromStore(File file) throws JsonParseException, IOException {
		InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
		
		//Here we automatically detect the format of the JSON file
		//Can be JSON text or Smile encoded binary file...
		JsonFactory jsonF = new JsonFactory();
		SmileFactory smileF = new SmileFactory(); 
		DataFormatDetector det = new DataFormatDetector(new JsonFactory[] { jsonF, smileF });
	    DataFormatMatcher match = det.findFormat(inputStream);
	    JsonParser jParser = match.createParserWithMatch();
	    
	    String archiveType = "de.mpg.biochem.mars.molecule.SingleMoleculeArchive";
	    
		while (jParser.nextToken() != JsonToken.END_OBJECT) {
		    String fieldname = jParser.getCurrentName();
		    	
		    if ("ArchiveType".equals(fieldname)) {
		    	jParser.nextToken();
		    	archiveType = jParser.getText();
		    	break;
		    }
		}
		
		jParser.close();
		inputStream.close();
		
		return archiveType;
	}
	
	public static MoleculeArchive<?,?,?> createMoleculeArchive(String archiveType) {
		try {
			Class<?> clazz = Class.forName(archiveType);
			Constructor<?> constructor = clazz.getConstructor(String.class);
			return (MoleculeArchive<?,?,?>)constructor.newInstance("archive");
		} catch (ClassNotFoundException e) {
			System.err.println(archiveType + " type not found. Is the class in the classpath?");
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static MoleculeArchive<?,?,?> createMoleculeArchive(String archiveType, File file) {
		try {
			Class<?> clazz = Class.forName(archiveType);
			Constructor<?> constructor = clazz.getConstructor(File.class);
			return (MoleculeArchive<?,?,?>)constructor.newInstance(file);
		} catch (ClassNotFoundException e) {
			System.err.println(archiveType + " type not found. Is the class in the classpath?");
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
  	
  	public static <T, E extends Exception> Predicate<T> catchConsumerException(
  		ThrowingConsumer<T, E> throwingConsumer, Class<E> exceptionType) {
	  
	    return i -> {
	        try {
	            throwingConsumer.accept(i);
	            return true;
	        } catch (Exception e) {
	            try {
	                E ex = exceptionType.cast(e);
	                System.err.println("Exception: " + ex.getMessage());
	            } catch (ClassCastException cex) {
	                throw new RuntimeException(e);
	            }
	            return false;
	        }
	    };
	}
  	
  	@FunctionalInterface
  	public interface ThrowingConsumer<T, E extends Exception> {
  	    void accept(T t) throws E;
  	}
}