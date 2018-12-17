package net.gjerull.etherpad.client;

import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import etm.core.monitor.EtmMonitor;
import etm.core.monitor.EtmPoint;
import etm.core.configuration.EtmManager;

/**
 * A class for easily executing an HTTP GET request.<br />
 * <br />
 * Example:<br />
 * <br />
 * <code>
 * Request req = new GETRequest(url_object);<br />
 * String resp = req.send();<br />
 * </code>
 */
public class GETRequest implements Request {
	private static final EtmMonitor etmMonitor = EtmManager.getEtmMonitor();
	private final URL url;

    /**
     * Instantiates a new GETRequest.
     * 
     * @param url the URL object
     */
    public GETRequest(URL url) {
        this.url = url;
    }

    /**
     * Sends the request and returns the response.
     * 
     * @return String
     */
    public String send() throws Exception {
        
    	EtmPoint point = etmMonitor.createPoint("GETRequest.send");
    	try {
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			StringBuilder response = new StringBuilder();
			String buffer;
			while ((buffer = in.readLine()) != null) {
				response.append(buffer);
			}
			in.close();
			return response.toString();
		} finally {
			// TODO: handle finally clause
			point.collect();
		}
    }
}
