package org.apache.catalina.connector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.catalina.valves.ValveBase;




/**
 * A Tomcat valve that finds the first entry in the "X-FORWARDED-FOR" header that is an external ip
 * and puts that value into the remote addr header. This is to get the "real" ip that the client is 
 * coming from when sent through a proxy like IIS ARR.
 *
 * this is really ugly because it needs to be in org.apache.catalina.connector because there is a 
 * bug that the Request.setRemoteAddr is a nop so I need to set the member variable directly
 * 
 */
public class FixProxyIPValve extends ValveBase {
	private static final String HEADER_X_FORWARDED_FOR = "X-FORWARDED-FOR";
	
    public void invoke(Request request, Response response) throws IOException, ServletException {

    	try {
	        String fwd = request.getHeader(HEADER_X_FORWARDED_FOR);
	        if (fwd != null && fwd.length() > 0) {
	            List<String> ips = split(fwd, ',');
	            
	            for (String ip : ips) {
	            	if (!isLocalIP (ip)) {
	            		int	ind = ip.indexOf(':');
	            		
	            		if (ind > 0)
	            			ip = ip.substring(0, ind);
//	            		System.out.println("Changing remote ip '" + remoteAddr + "' to '" + ip + "'");
	                    request.remoteAddr = ip;
	                    request.remoteHost = ip;
	            		break;
	            	}
	            }
	        }
    	} catch(Throwable t) {
    		System.out.println("Exception setting remote: " + t);
    		t.printStackTrace();
    	}
    	
	    /*
		     Invoke next valve
		*/
		getNext().invoke(request, response);
    }

    protected boolean isLocalIP(String ip) {
    	if (ip.startsWith("10.") || ip.startsWith("192.168"))
    		return true;
    	if (!ip.startsWith("172."))
    		return false;
    	return ip.startsWith("172.16.") || ip.startsWith("172.17.") || ip.startsWith("172.18.") || 
    			ip.startsWith("172.19.") || ip.startsWith("172.20.") || ip.startsWith("172.21.") || 
    			ip.startsWith("172.22.") || ip.startsWith("172.23.") || ip.startsWith("172.24.") || 
    			ip.startsWith("172.25.") || ip.startsWith("172.26.") || ip.startsWith("172.27.") || 
    			ip.startsWith("172.28.") || ip.startsWith("172.29.") || ip.startsWith("172.30.") || 
    			ip.startsWith("172.31.");
    }
    
    protected List<String> split(String src, char sep) {
		ArrayList<String>	ret = new ArrayList<String>();
		int start = 0;
		int cnt = 0;
		int len = src.length();

		while (cnt < len) {
			if (src.charAt(cnt) == sep) {
				ret.add(src.substring(start, cnt));
				start = cnt+1;
			}
			cnt++;
		}
		if (start != cnt) {
			ret.add(src.substring(start, cnt));
		}

		return ret;
    	
    }
    
    
    /**
     * @see org.apache.catalina.valves.ValveBase#getInfo()
     */
    public String getInfo() {
    	return getClass() + "/1.0";
    }
}
