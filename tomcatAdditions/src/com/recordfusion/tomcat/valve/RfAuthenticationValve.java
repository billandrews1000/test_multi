package com.recordfusion.tomcat.valve;


import java.io.IOException;
import java.security.Principal;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

import org.apache.catalina.Realm;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.tomcat.util.codec.binary.Base64;

/**
 * <h3>Description: </h3>
 * Solution to PROBLEM that in Tomcat, the FORM and BASIC authorization methods can not co-exist.
 * However, our Monitor app needs to use the BASIC method for the PUT (refresh) requests, and
 * the FORM method for the UI (which is considerably 'safer')
 * <p>
 * This Tomcat Valve is uses Authorization header (if present) in incoming request to authenticate
 * using the BASIC method, otherwise the 'web.xml' specified (FORM) authentication method takes place.
 * There is no reason to use this class when 'web.xml' prescribes BASIC authentication only.
 * </p>
 * You can use this valve by adding 
 * <pre>
 *  <Valve className="com.recordfusion.tomcat.valve.RfAuthenticationValve" allow="refresh" />
 * </pre>
 * tag either to the server.xml file (applies globally) or in the META-INF/context.xml file of your WebApp
 * (as used in app/monitor - applies to that webapp only). The attribute 'allow' specifies RegEx pattern(s)
 * to use as filtering, only allowing BASIC authentication for requests having specified pattern in the URI.
 * For example, allow='refresh' will ONLY allow BASIC authentication for requests having pattern 'refresh'
 * as a part of the url (/monitor/refresh).
 * <p>
 * BEWARE that this valve is shipped as rf_tomcat_valves.jar, which must be placed in the Tomcat /lib directory. 
 * Unfortunately, it can NOT be shipped as a part of the webapp only.
 * </p>
 * <p>Copyright: Copyright (c) 2020 RecordFusion</p>
 * @author MBrunecky
 */
public class RfAuthenticationValve extends ValveBase {
    private static final String BASIC_PREFIX = "basic ";

    private String encoding = "UTF-8";
    // This SUCKS. Prior to Java 8 the only Base64 decoder is available from org.apache.tomcat.util.codec (tomcat-coyote.jar)
    private Base64 decoder = new Base64();
    
    private Pattern pattern = null;

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Attribute allow specifies Regex pattern(s) that must be matched in the URI to allow BASIC authentication.
     * For example allow="refresh" allowing to use BASIC authentication for any URI with a string 'refresh' in it.
     * Default value is null (allow ALL requests).
     * @param allow
     */
    public void setAllow(String allow) {
    	try {
    		this.pattern = (allow == null || allow.length() == 0) ? null : Pattern.compile(allow);
    		if (containerLog != null) {
    			containerLog.info(getClass().getName() + " using attribute allow='" + getAllow() + "'");
    		}
    	} catch (Throwable t) {
    		if (containerLog != null) {
        		containerLog.error(getClass().getName() + " attribute allow='" + allow + "' is INVALID");
    		}
    	}
    }
    
    public String getAllow() {
    	return pattern == null ? null : pattern.toString();
     }
    
    
    public void invoke(Request request, Response response) throws IOException, ServletException {
        Principal principal = request.getUserPrincipal();
        Realm realm = getContainer().getRealm();
        
        if (principal != null) {
            if (containerLog.isDebugEnabled()) {
                super.containerLog.debug("Already authenticated as: " + principal.getName());
            }
        } else if (realm == null) {
            if (containerLog.isDebugEnabled()) {
                containerLog.debug("No realm configured");
            }
        } else if (pattern == null || pattern.matcher(request.getRequestURI()).find()) {
            String auth = request.getHeader("authorization");
            if (auth != null) {
                if (auth.toLowerCase().startsWith(BASIC_PREFIX)) {
                    auth = auth.substring(BASIC_PREFIX.length());
                    byte[] bytes = decoder.decode(auth); 
                    auth = new String(bytes, encoding);
                    int ix = auth.indexOf(':');
                    if (ix >= 0) {
                        String username = auth.substring(0, ix);
                        String password = auth.substring(ix+1);
                        principal = realm.authenticate(username, password);
                        if (principal == null) {
                            containerLog.warn("Could not authenticate " + username + " from " + request.getRemoteAddr());
                        } else {
                            // containerLog.info("Authenticated as " + principal.getName());
                            request.setAuthType("BASIC");
                            request.setUserPrincipal(principal);
                        }
                    }
                }
            }
        }
        getNext().invoke(request, response);
    }
}

