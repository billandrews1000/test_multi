package com.recordfusion.tomcat.valve;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
 
import javax.servlet.ServletException;
 
import org.apache.catalina.Session;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;

 
/**
* 
* Valve to force creation and setting of new Session ID. Based on session attribute "ChangeSessionID" having a value of true.
* 
*/
public class RfChangeSessionIdValve extends ValveBase {
 
	private String parameter = "ChangeSessionID";
	private String value = "true";
	private String eventServiceName = "TerminateSessionEvent";
	private String eventServiceAction = "CHANGED_SESSION_ID";
	private String sessionTimeout = "SetSessionTimeout";
	private String currentUserName = "CurrentUserName";
 
	public String getParameter() {
		return parameter;
	}
 
	public void setParameter(String parameter) {
		this.parameter = parameter;
	}
 
	public String getValue() {
		return value;
	}
 
	public void setValue(String value) {
		this.value = value;
	}
	
	public String getEventServiceName() {
		return eventServiceName;
	}
 
	public void setEventServiceName(String eventServiceName) {
		this.eventServiceName = eventServiceName;
	}
	
	public String getEventServiceAction() {
		return eventServiceAction;
	}
 
	public void setEventServiceAction(String eventServiceAction) {
		this.eventServiceAction = eventServiceAction;
	}

 
	public void invoke(Request request, Response response) throws IOException, ServletException {
		Log logger = container.getLogger();

		try {
			// get the old session
			Session oldSession = request.getSessionInternal();
			if (getParameter() != null && getParameter().length() > 0 && getValue() != null &&  getValue().length() > 0) {
				Boolean attributeValue = (Boolean) oldSession.getSession().getAttribute(getParameter());
				if (attributeValue != null &&  getValue().equals(String.valueOf(attributeValue))) {
					// immediately remove from old session
					oldSession.getSession().removeAttribute(getParameter());
					
		            int timeout = 0;
					if (oldSession.getSession().getAttribute(sessionTimeout) != null) {
						try {
							timeout = (Integer) oldSession.getSession().getAttribute(sessionTimeout);
						}
						catch (Exception e) {
							if (logger != null)
								logger.error(getClass().getName() + " - Failed to get session timeout value: " + oldSession.getSession().getAttribute(sessionTimeout), e);
						}
						// immediately remove from old session
						oldSession.getSession().removeAttribute(sessionTimeout);
					}
					
					String userLogin = "unknown username";
					if (oldSession.getSession().getAttribute(currentUserName) != null) {
						userLogin = (String) oldSession.getSession().getAttribute(currentUserName);
						// immediately remove from old session
						oldSession.getSession().removeAttribute(currentUserName);
					}
		            
					if (logger != null)
						logger.info(getClass().getName() + " - Old Session ID: " + oldSession.getId());
		      
					// save the old session's attributes
					Map<String, Object> oldSessionAttributes = new HashMap<String, Object>();
					Enumeration<String> names = oldSession.getSession().getAttributeNames();
					while (names.hasMoreElements()) {
						String name = (String) names.nextElement();
						oldSessionAttributes.put(name, oldSession.getSession().getAttribute(name));
					}

					if (getEventServiceName() != null && getEventServiceName().length() > 0 && getEventServiceAction() != null &&  getEventServiceAction().length() > 0) {
						// need to set event service name in old session before invalidating, so that we retained the session context (see SessionListener.sessionDestroyed)
						oldSession.getSession().setAttribute(getEventServiceName(), getEventServiceAction());
			
						// invalidate the old session and clear the session id
						request.getSession().invalidate();
						request.setRequestedSessionId(null);
			 
						// create a new session and set the new session id in the request
						Session newSession = request.getSessionInternal();
						request.setRequestedSessionId(newSession.getId());
						
						if (logger != null)
							logger.info(getClass().getName() + " - New Session ID: " + newSession.getId());
						
						// copy old session attributes to the new session
						for (String name : oldSessionAttributes.keySet()) {
							newSession.getSession().setAttribute(name, oldSessionAttributes.get(name));
						}
			
						// update session with new session id
						newSession.getSession().setAttribute("SessionID", newSession.getId());
						
			            if (timeout > 0) {
			            	newSession.getSession().setMaxInactiveInterval(timeout);
			            	if (logger != null)
			            		logger.info(getClass().getName() + " - Setting session timeout for: " + userLogin + " to " + (timeout)  + " seconds.");
			            }
			            else {
			            	if (logger != null)
			            		logger.info(getClass().getName() + " - Setting session timeout for: " + userLogin + " to " + request.getSession().getMaxInactiveInterval()  + " seconds.");
			            }
					}
				}
			}
		}
		catch (Exception e) {
			if (logger != null)
				logger.error(getClass().getName() + "Unable to create new change session id.", e);
		}
		finally {
			if (getNext() != null)
				getNext().invoke(request, response);
		}
	}
}