package com.recordfusion.tomcat.listener;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class WebAppInitializerTest {
	private static final String HOME_DIR = "C:/Work/tomcatAdditions";
	private static final String APPS_DIR = "C:/Programs/tomcat7/webapps";
	private static final String TEST_APP = "knowledge";

	static {
        try {
            Properties props = new Properties();
			props.load(new FileInputStream(HOME_DIR + "/log4j.properties"));
	        PropertyConfigurator.configure(props);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testDeployment() {
		RfDeployManager init = new RfDeployManager();
		init.checkWebApps(new File(APPS_DIR));
	}
	
	public void XXX_testExpansionIsUpToDate() {
		File warFile = new File(APPS_DIR + "/" + TEST_APP + ".war");
		File appFile = new File(APPS_DIR + "/" + TEST_APP);
		
		if (warFile.exists()) {
			RfDeployManager init = new RfDeployManager();
			boolean isOK =  init.appIsUpToDate(warFile, appFile );
		}
		fail("Not yet implemented");
	}

}
