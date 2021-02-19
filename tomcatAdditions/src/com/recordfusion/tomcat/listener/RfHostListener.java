package com.recordfusion.tomcat.listener;

import java.io.File;


import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.log4j.Logger;
/**
 * HostListener
 * <h3>Description<.h3>
 * Host listener implements Tomcat/Catalina LifecycleListener, configured in Tomcat server.xml 
 * to listen to Host lifecycle events:
 * <pre>
    <Host name="localhost"  appBase="webapps" unpackWARs="true" autoDeploy="true" >
	<!-- MXB: Recordfusion Lifecycle Listener, used to validate/update deployed webapps on startup -->
	<!-- You may supply appBase="none" logDir="none" to disable respective directory handling      -->
	<!-- logAge=nn enables automatic log directory purge, deleting log files older than nn days    -->
	<!-- accAge=n  specifies separate 'age' for accelerated purge of *stdout*.log, *stderr*.log    -->
	<Listener className="com.recordfusion.tomcat.listener.RfHostListener" appBase="webapps" logDir="logs" logAge="20" />
 * </pre>
 * On startup, listener invokes RfDeployManager, making sure that all deployed web applications (webapps/*.war)
 * expansions (deployments) are up-to-date. Any obsolete expasions are deleted, forcing Tomcat to re-deploy.
 * <br/>
 * Also on startup, listener invokes RfLogManager, which deletes dummy log files created by Tomcat startup
 * prior to log4j initialization, and (optionally) schedules automatic purging of (log) files older
 * than prescribed number of days.
 * 
 * @author Mbrunecky
 *
 */
public class RfHostListener implements LifecycleListener {
	protected Logger log = Logger.getLogger(RfHostListener.class);
	
	private String home    = "unknown";
	private String appBase = "webapps";
	private String logDir  = "logs";
	private String logAge  = null;		// max log age days
	private String accAge  = null;      // accelerated log age days
	private int firstTime  = 0;
	
	
	public void setAppBase(String base) { this.appBase = base; }
	public void setLogDir (String logs) { this.logDir  = logs; }
	public void setLogAge (String age)  { this.logAge  = age;  }
	public void setAccAge (String age)  { this.accAge  = age;  }
	
	public String getHome() 	{ return home; }
	public String getAppBase() 	{ return appBase; }
	public String getLogDir()   { return logDir;  }
	public String getLogAge()   { return logAge == null || logAge.trim().length() == 0 ? null : logAge; }  
	public String getAccAge()   { return accAge == null || accAge.trim().length() == 0 ? null : accAge; }  
	
	public RfHostListener()  {
		home = System.getProperty("catalina.home");
	}

	// @Overrides
	public void lifecycleEvent(LifecycleEvent lev) {
		try {
			LifecycleState state = lev.getLifecycle().getState();
			// Log event, unless it's periodic - repeating such as 'STARTED' repeated every 10 sec.
			if (!"periodic".equals(lev.getType())) {
				log.debug("Host LIFECYCLE event type=" + lev.getType() + " state=" + lev.getLifecycle().getStateName() + " (" + state + ")");
			}
			switch(state) {
			// MXB: This is what I think is the lifecycle event order / flow
			case NEW: 			break;
			case INITIALIZING:	break;
			case INITIALIZED:   log.info("Host LIFECYCLE event type=" + lev.getType() + " state=" + lev.getLifecycle().getStateName() + " (" + state + ")");
								onInitialized(); break;
			case STARTING_PREP: break; 					// follows INITIALIZED
			case STARTING:		break;	
			case STARTED:		break;
			case MUST_STOP:		break;
			case STOPPING_PREP:	break;
			case STOPPED:		break;
			case STOPPING:		break;
			case MUST_DESTROY:	break;
			case DESTROYING:	break;
			case DESTROYED:		break;
			case FAILED: 		break;
			default:  			break;
			}
		}
		catch (Throwable t) {
			if (lev == null) {
				log.error("Lifecycle event null", t);
			} else {
				log.error("Lifecycle event type=" + lev.getType() + " state=" + lev.getLifecycle().getStateName() + " failed", t);
			}
		}
		
	}
	
	
	protected void onInitialized() {
		if (firstTime++ == 0) {
			log.info("RF Tomcat Additions on catalina.home='" +  getHome() + "'");
	
			File appFile = new File(new File(getHome()), getAppBase());
			
			if (appFile.getName().toLowerCase().indexOf("none") >= 0) {
				log.info("Skipping WebApp checks, appBase = " + getAppBase());
			} else if (appFile.exists()) {
				log.info("Using appBase "   + appFile.getAbsolutePath());
				new RfDeployManager().checkWebApps(appFile);
			} else {
				log.warn("Missing appBase " + appFile.getAbsolutePath());
			}
	
		
			File logDir = new File(new File(getHome()), getLogDir());
			if (logDir.getName().toLowerCase().indexOf("none") >= 0) {
				log.info("Skipping log directory handling, logDir = " + getLogDir());
			} else if (logDir.exists()) {
				log.info("Using logDir "   + logDir.getAbsolutePath());
				new RfLogManager().initLogDir(logDir, getLogAge(), getAccAge());
			} else {
				log.info("Missing logDir " + logDir.getAbsolutePath());
			}
		}
	}

}
