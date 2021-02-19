package com.recordfusion.tomcat.listener;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.log4j.Logger;

/**
 * Class used to manage Tomcat deployed web-apps. 
 * Since Tomcat (even with autoDeploy="true") does NOT check/update/redeploy web-apps from
 * their *.war files (and hence we could miss updates applied while Tomcat was down),
 * this code invoked from Tomcat 'HostListener':
 * <ul>
 * <li> - checks if the deployed application (expanded .war) matches the .war content </li>
 * <li> - on any mismatch deletes (or renames) deployed application causing 'on_demand' (re)deploy </li>
 * </ul>
 * @author Mbrunecky
 *
 */
public class RfDeployManager {
	protected Logger log = Logger.getLogger(RfDeployManager.class);
	protected DateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	
	 public void checkWebApps(File webAppDir) {
		 for (File warFile: webAppDir.listFiles()) {
			 if (warFile.getName().toLowerCase().endsWith(".war")) {
				 try {
				 String warName = warFile.getName();
				 String appPath = warName.substring(0, warName.length()-4);
				 File   appRoot = new File(warFile.getParent(), appPath);
				 
				 if (!appRoot.exists()) {
					 log.info("WebApp '" + warName + "' has not been deployed yet");
				 } else if (!appIsUpToDate(warFile, appRoot)) {
					 log.warn("WebApp '" + warName + "' is out of sync with '" + appPath + "'");
					 removeWebApp(appRoot);
				 } else {
					 log.info("WebApp '" + warName + "' has up-to-date expansion '" + appPath + "'");
				 }
				 } catch (Throwable t) {
					 log.error("Failure handling deployment of WebApp '" + warFile + "'", t);
				 }
			 }
		 }		 
	 }
	
				
	 boolean  appIsUpToDate(File warFile, File appRoot) {
		try {
			JarFile war = new JarFile(warFile);
//			Turns ut that checking the manifest does not provide anything usuefull			
//			Manifest manifest = war.getManifest();
//			Attributes man_attrs = manifest.getMainAttributes();
			
			// Go thru WAR entries, one by one, and check corresponding expansions for presence/size/time
			Enumeration<JarEntry> entries = war.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
//				Attributes attrs = entry.getAttributes();
//				String path = "" + (attrs == null ? "" : attrs.get(Attributes.Name.CLASS_PATH));
				String name = entry.getName();
				long size = entry.getSize();
				long time = entry.getTime();
				
				File appFile = new File(appRoot, name);
				if ( appFile.exists() ) {
					if (!appFile.isDirectory()) {
						if (Math.abs(size - appFile.length()) > 1) {
							log.info("MISMATCH: WebApp file '" + appFile.getAbsolutePath() + "' file size " + appFile.length() + 
									" differs from war " + size + " (" + (size - appFile.length()) + ")");
							return false;
						}
						if (Math.abs(time - appFile.lastModified()) > 1) {
							log.info("MISMATCH: WebApp file '" + appFile.getAbsolutePath() + "' file time " + DATE_FMT.format(new Date(appFile.lastModified())) + 
									" differs from war " + DATE_FMT.format(new Date(time)) + " (" + (time - appFile.lastModified())/1000 + "sec)");
							return false;
						}
					}
					
					
				} else {
					log.warn("MISMATCH: WebApp file '" + appFile.getAbsolutePath() + "' is missing, war path '" + name + "'");
					return false;
				}
				// log.info("App file '" + appFile.getAbsolutePath() + "' size=" + size + " time=" + DATE_FMT.format(new Date(time)) + " is OK");
				
			}
			return true;

		} catch (Exception e) {
			log.error("Failure processing WebApp '" + warFile + "', " + e);
			return false;
		}
		
	 }

	 
	 protected void removeWebApp(File webAppRoot) {
		 if (webAppRoot.exists()) {
			 File tmpName = new File(webAppRoot.getParent(), webAppRoot.getName() + "_" + System.currentTimeMillis());
			 if ( webAppRoot.renameTo(tmpName)) {
				 if (deleteHierarchy(tmpName)) {
					 log.info("REMOVED deployed WebApp '" +  webAppRoot.getName() + "'");
				 } else {
					 log.warn("FAILED to remove WebApp '" + webAppRoot.getName() + "', ' renamed to " + tmpName.getName());
				 }
			 } else {
				 if (deleteHierarchy(webAppRoot)) {
					 log.info("REMOVED deployed WebApp '" +  webAppRoot.getName() + "'");
				 } else {
					 log.error("FAILED to remove WebApp '" + webAppRoot.getName() + "'");
				 }
			}
		 }
	 }
	 
     protected boolean deleteHierarchy(File file) {
        if (file.isDirectory()) {
            String[] children = file.list();
            for (int i=0; i<children.length; i++) {
            	File child = new File(file, children[i]);
            	if (!deleteHierarchy(child)) {
            		return false;
            	}
            }
        }
        // Now use delete() since empty.
        return file.delete();
    }
	 
	 
}
