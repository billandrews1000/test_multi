package com.recordfusion.tomcat.listener;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

/**
 * <h3>RfLogManager</h3>
 * Log manager is called by Tomcat 'HostListener'. It is used to delete 'dummy' log files
 * created by Tomcat start (prior to log initialization), and (optionally) to purge old
 * log files.
 * <br/>
 * Old log files are defined by (server.xml) configured logAge={days}, and identified as
 * log directory files having pattern 'log' anywhere in the file name.
 * Special files (see ACCEL_LOGS) may be subject to 'accelerated' purging, using accAge
 * (accelerated files age).
 * Undefined (or 0) oldAge disables old log purging, undefined (or 0) accAge treats
 * accelerated files as other log files.
 * Log purge is performed by a timer thread during the hour 0-1 of the day.
 *
 * @author Mbrunecky
 *
 */

public class RfLogManager {
	protected Logger log = Logger.getLogger(RfLogManager.class);
	
	// Dummy logs (created priro to log4j intialization) using format <logname>.yyy-mm-DD.log
	protected DateFormat LOG_DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");
	protected DateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	private static final String[] DUMMY_LOGS = { "catalina", "localhost", "juli", "commons-daemon" };
	private static final String[] ACCEL_LOGS = { "stdout", "stderr" };
	private static final int      DUMMY_KEEP  = 1024; // keep dummy file IF it is in excess of... bytes
	
    private static Timer timer = new Timer(true); // as daemon thread - may be interrupted by System.exit()

	private File    logDir;
	private int     maxAgeDays = 0;
	private int     accAgeDays = 0;

	
	
	
	public void initLogDir(File logDir, String logAge, String accAge) {
		this.logDir = logDir;
		if (!logDir.exists()) {
			log.error("Invalid log directory '" + logDir.getAbsolutePath() + "' does not exists!");
			return;
		}
		
		try {
			maxAgeDays = (logAge == null || logAge.trim().length() == 0) ? 0 : Integer.parseInt(logAge);
			if (maxAgeDays > 0) {
				log.info("Using logAge " + logAge + " days");
			} else {
				log.info("No logAge specified, log directory purge NOT scheduled");
			}
		} catch (Throwable t) {
			log.error("Invalid log file maxAge (days), assuming 0", t);
			maxAgeDays = 0;
		}
		try {
			accAgeDays = Math.min(maxAgeDays, (accAge == null || accAge.trim().length() == 0) ? 0 : Integer.parseInt(accAge));
			if (accAgeDays != maxAgeDays && accAgeDays != 0) {
				log.info("Using accAge " + accAge + " days");
			}
		} catch (Throwable t) {
			log.error("Invalid log file accAge (days), assuming 0", t);
			accAgeDays = 0;
		}
		
		log.info("Processing log directory '" + logDir.getAbsolutePath() + "'");
		dropDummyLogs();
		
		if (maxAgeDays > 0) {
			purgeAgedLogs();
			
			log.info("Scheduling purge task deleting aged log files");
	        // To keep it simple, I want to run once an hour, Purgers skips repeated runs on the same day
	        timer.schedule(new Purger(), 3600*1000, 3600*1000);
		}
	}
	
	protected class Purger extends TimerTask {
		private int lastPurgeDay;
		
		public void Purger() {
			lastPurgeDay = getDayOfYear();
		}
        public synchronized void run() {
            // Run at elevated priority, preventing delay due to busy "others"
        	try {
	            Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 1);
	            int currPurgeDay = getDayOfYear();
	            if (lastPurgeDay < currPurgeDay) {
	            	lastPurgeDay = currPurgeDay;
					log.info("Checking for logs aged over logAge=" + maxAgeDays + (accAgeDays != 0 && accAgeDays != maxAgeDays ? " accAge=" + accAgeDays : ""));
					purgeAgedLogs();
				} 
        	} catch (Throwable t) {
        		log.error("Exception deleting aged logs", t);
        	}
        }
        
        protected int getDayOfYear() {
			Calendar cal = Calendar.getInstance();
			cal.setTime(new Date());
			return cal.get(Calendar.DAY_OF_YEAR);
        }
    }
	
	protected void dropDummyLogs() {
		try {
			String dateSuffix = '.' + LOG_DATE_FMT.format(new Date()) + ".log";
			for (String logName: DUMMY_LOGS) {
				File logFile = new File(logDir, logName + dateSuffix);
				if ( logFile.exists() ) {
					if (logFile.length() < DUMMY_KEEP) {
						if (logFile.delete()) {
							log.info("Deleted dummy log file '" + logFile.getName() + "'");
						} else {
							logFile.deleteOnExit();
							log.warn("Marking on-exit delete of dummy log file '" + logFile.getName() + "'");
						}
					} else {
						log.info("Keeping dummy log file '" + logFile.getName() + "', non-empty size " + logFile.length() + " bytes");
					}
				}
			}
		} catch (Throwable t) {
			log.error("Failed to delete dummy log file(s)", t);
		}

	}
	
	
	protected void purgeAgedLogs() {
		try {
			for (File file: logDir.listFiles()) {
				String lower = file.getName().toLowerCase();
				if (lower.indexOf("log") >= 0) {
					long now = System.currentTimeMillis();
					long old = now - 24*60*60*1000*((isAccelFile(lower) && accAgeDays != 0) ? accAgeDays : maxAgeDays);
					if (file.lastModified() < old) {
						if (file.delete()) {
							log.info("Dropped aged log file '" + file.getName() + "' last modified " + DATE_FMT.format(new Date(file.lastModified())));
						} else {
							log.info("Failed to droppd aged log file '" + file.getName() + "' last modified " + DATE_FMT.format(new Date(file.lastModified())));
						}
					}
				}
			}
		} catch (Throwable t) {
			log.error("Exception while purging aged logs", t);
		}
	}
	
	protected boolean isAccelFile(String name) {
		for (String accell: ACCEL_LOGS) {
			if (name.indexOf(accell) >= 0) {
				return true;
			}
		}
		return false;
	}
}
