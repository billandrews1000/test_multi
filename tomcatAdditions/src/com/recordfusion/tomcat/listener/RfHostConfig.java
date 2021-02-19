package com.recordfusion.tomcat.listener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.catalina.startup.HostConfig;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import java.io.*;

/**
 * RfHostConfig
 * <h3>Description</h3>
 * RfHostConfg overrides Apache HostConfig.filterAppPaths() allowing to SORT applications 
 * in order prescribed by (webapps directory) file WEBAPPS_ORDER_FILE.
 * To configure this class into Tomcat 7 instance, you must modify the file server.xml,
 * element <Host/> adding attribute hostConfigClass="com.recordfusion.tomcat.listener.RfHostConfig"
 * (this overrides the default org.apache.catalina.startup.HostConfig).
 * <pre>
 *  <Host name="localhost"  appBase="webapps" unpackWARs="true" autoDeploy="true" deployOnStartup="false" hostConfigClass="com.recordfusion.tomcat.listener.RfHostConfig">
 * </pre>
 * <p>
 * The file webapps/webappsOrder.txt is a list of regular expression patterns, for example:
 * <pre>
 * # This file prescribes webapp load order
 * # Webapp directory/war names will be sorted selecting names matching the first Regex pattern below, then the next ...
 * # Note that webapp directory usually contains both webapp expansion (county) and the war file (count.war),
 * # hence the pattern will usually end with .*. Patterns are case-sensitive by default.
 * mon.*
 * knowledge.*
 * </pre>
 * @author Mbrunecky
 *
 */

public class RfHostConfig extends HostConfig {
    private static final Log log = LogFactory.getLog( HostConfig.class );
    public  static final String WEBAPPS_ORDER_FILE = "webappsOrder.txt";
    
    private List<String> sortPatterns = null;

    public RfHostConfig() {
    	log.info("CREATED RfHostConfig augmenting Apache HostConfig and using " + WEBAPPS_ORDER_FILE);
    }
    
    /**
     * Override.
     * Augments the base class filtering functionality by sorting webapp (file/directory) names
     * according to patterns in WEBAPPS_ORDER_FILE.
     */
    
    public String[] filterAppPaths(String[] names) {
    	String[] src = super.filterAppPaths(names);
    	boolean firstTime = sortPatterns == null;
    	
    	
    	// Note: webapps directory listing will contain all already deployed webApps AND their .war (if any)
    	// so you may get both "knowledge" and "knowledge.war" ...
    	if (firstTime) {
	    	for (String entry: src) {
	    		log.info("ORIGINAL webapps entry " + entry);
	    	}
    	}

    	List<String> sorted = new ArrayList<String>(names.length);
    	for (String pattern: getSortPatterns()) {
    		// In the pattern order look for any matching names
    		for (int i=0; i<names.length; ++i) {
    			String name = names[i];
    			if (name != null && !WEBAPPS_ORDER_FILE.equals(name)) {
    				if (Pattern.matches(pattern, name)) {
    					sorted.add(name);
    					names[i] = null;
    				}
    			}
    		}
    	}
     	
    	String[] res = sorted.toArray(new String[sorted.size()]);
       	if (firstTime) {
	    	for (String entry: res) {
	    		log.info("SORTED webapps entry " + entry);
	    	}
    	}
       	
    	return res;
    	
    }
    
    protected List<String> getSortPatterns() {
    	
    	if (sortPatterns == null) {
    		sortPatterns = new ArrayList<String>();
    		
			File webappsOrder = new File(appBase(), WEBAPPS_ORDER_FILE);
			BufferedReader rdr = null;
    		try {
    			log.info("Loading webapps order file " + webappsOrder.getAbsolutePath());
    			rdr = new BufferedReader(new FileReader(webappsOrder));
    			String line;
    			while ((line = rdr.readLine()) != null) {
    				line = line.trim();
    				if (line.length() > 0 && !line.startsWith("#")) {
    					log.info("Webapps order [" + sortPatterns.size() + "]: '" + line + "'");
    					Pattern.compile (line); // test compile to make sure...
    					sortPatterns.add(line);
    				}
    			}
     		} catch (Throwable t) {
     			log.error("Failed loading webapps order file " + webappsOrder.getAbsolutePath() + ": " + t);
    			
    		} finally {
    			if (rdr != null) try {rdr.close(); } catch (Throwable t) {};
    		}
			// Add the final catch-all pattern
			sortPatterns.add(".*");
    	}
    	return sortPatterns;
    }
    
}
