package net.gjerull.etherpad.client;

import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import com.pholser.junit.quickcheck.Property;

import org.junit.Before;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

@RunWith(JUnitQuickcheck.class)
public class EPLiteConnectionPropertiesTest {

	private static final String API_VERSION = "1.2.12";
	private static final String ENCODING = "UTF-8";

	@Before
	public void setUp() {
		((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("junit-quickcheck.value-reporting"))
				.setLevel(ch.qos.logback.classic.Level.OFF);
	}

	@Property
	public void domain_with_trailing_slash_when_construction_an_api_path(String exampleMethod) throws Exception {

		EPLiteConnection connection = new EPLiteConnection("http://example.com/", "apikey", API_VERSION, ENCODING);
		String apiMethodPath = connection.apiPath(exampleMethod);
		assertEquals("/api/1.2.12/" + exampleMethod, apiMethodPath);
	}

	@Property
	public void method(String apiKey) throws Exception {
		EPLiteClient client;
		EPLiteConnection connection;
		client = new EPLiteClient("http://localhost:9001", apiKey);

		//assertEquals(apiKey, connection.get("apiKey"));
	}
}