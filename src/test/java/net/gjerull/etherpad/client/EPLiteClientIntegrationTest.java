package net.gjerull.etherpad.client;

import java.nio.charset.Charset;
import java.util.*;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import etm.core.monitor.EtmMonitor;
import etm.core.configuration.EtmManager;
import etm.core.configuration.BasicEtmConfigurator;
import etm.core.renderer.SimpleTextRenderer;
/**
 * Integration test for simple App.
 */
public class EPLiteClientIntegrationTest {
	private EPLiteClient client;
	private EtmMonitor monitor;
	private ClientAndServer mockServer;

	/**
	 * Useless testing as it depends on a specific API key
	 *
	 * TODO: Find a way to make it configurable
	 */
	@Before
	public void setUp() throws Exception {
		this.client = new EPLiteClient("http://localhost:9001",
				"a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58");
		mockServer = startClientAndServer(9001);
		
		// initialize measurement subsystem
		BasicEtmConfigurator.configure();
		// startup measurement subsystem
		monitor = EtmManager.getEtmMonitor();
        monitor.start();
	}

	@After
	public void stopServer() {
		mockServer.stop();
		// visualize results
		monitor.render(new SimpleTextRenderer());
		monitor.stop();
	}

	@Test
	public void validate_token() throws Exception {

		mockServer
				.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/checkToken")
						.withBody("{\"apikey\":\"a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58\"}"))
				.respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		client.checkToken();

	}

	@Test
	public void create_and_delete_group() throws Exception {

		// create group
		mockServer
				.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/createGroup").withBody(
						"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"groupID\":\"g.cbT9HuF1buoPVf8R\"}}"));

		// delete group
		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/deleteGroup").withBody(
				"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&groupID=g.cbT9HuF1buoPVf8R"),
				Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		Map response = client.createGroup();

		assertTrue(response.containsKey("groupID"));
		String groupId = (String) response.get("groupID");
		assertTrue("Unexpected groupID " + groupId, groupId != null && groupId.startsWith("g."));

		client.deleteGroup(groupId);
	}

	@Test
	public void create_group_if_not_exists_for_and_list_all_groups() throws Exception {

		// CreateGroup if it does not exist
		mockServer.when(
				HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/createGroupIfNotExistsFor").withBody(
						"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&groupMapper=groupname"),
				Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"groupID\":\"g.VfrQC2OpOJfqAZqX\"}}"));

		// List all groups
		mockServer
				.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/listAllGroups").withBody(
						"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"groupIDs\":[\"g.VfrQC2OpOJfqAZqX\"]}}"));

		// CreateGroup if it does not exist
		mockServer.when(
				HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/createGroupIfNotExistsFor").withBody(
						"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&groupMapper=groupname"),
				Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"groupID\":\"g.VfrQC2OpOJfqAZqX\"}}"));

		// List all groups
		mockServer
				.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/listAllGroups").withBody(
						"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"groupIDs\":[\"g.VfrQC2OpOJfqAZqX\"]}}"));

		// Delete group
		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/deleteGroup").withBody(
				"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&groupID=g.VfrQC2OpOJfqAZqX"),
				Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		String groupMapper = "groupname";
		Map response = client.createGroupIfNotExistsFor(groupMapper);
		String groupId = (String) response.get("groupID");

		Map listResponse = client.listAllGroups();

		int firstNumGroups = ((List) listResponse.get("groupIDs")).size();

		client.createGroupIfNotExistsFor(groupMapper);

		listResponse = client.listAllGroups();
		int secondNumGroups = ((List) listResponse.get("groupIDs")).size();

		assertEquals(firstNumGroups, secondNumGroups);

		client.deleteGroup(groupId);
	}

	@Test
	public void create_group_pads_and_list_them() throws Exception {

		// Create group
		mockServer
				.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/createGroup").withBody(
						"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"groupID\":\"g.fFsk0JtBW2uYzCgP\"}}"));

		// Create group pad
		mockServer
				.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/createGroupPad"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200).withBody(
						"{\"code\":0,\"message\":\"ok\",\"data\":{\"padID\":\"g.fFsk0JtBW2uYzCgP$integration-test-1\"}}"));

		// Set Public status
		mockServer
				.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/setPublicStatus"),
						Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		// Get Public status
		mockServer
				.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getPublicStatus"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"publicStatus\":true}}"));

		// Set Password
		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/setPassword"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		// Is Password Protected
		mockServer
				.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/isPasswordProtected"),
						Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"isPasswordProtected\":true}}"));

		// Create group pad
		mockServer
				.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/createGroupPad"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200).withBody(
						"{\"code\":0,\"message\":\"ok\",\"data\":{\"padID\":\"g.fFsk0JtBW2uYzCgP$integration-test-2\"}}"));

		// Get Text
		mockServer.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getText").withBody(
				"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&padID=g.fFsk0JtBW2uYzCgP$integration-test-2"),
				Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"text\":\"Initial text\\n\"}}"));

		// List Pads
		mockServer.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/listPads").withBody(
				"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&groupID=g.fFsk0JtBW2uYzCgP"),
				Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200).withBody(
						"{\"code\":0,\"message\":\"ok\",\"data\":{\"padIDs\":[\"g.fFsk0JtBW2uYzCgP$integration-test-1\",\"g.fFsk0JtBW2uYzCgP$integration-test-2\"]}}"));

		// Delete group
		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/deleteGroup").withBody(
				"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&groupID=g.fFsk0JtBW2uYzCgP"),
				Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		Map response = client.createGroup();
		String groupId = (String) response.get("groupID");
		String padName1 = "integration-test-1";
		String padName2 = "integration-test-2";
		try {
			Map padResponse = client.createGroupPad(groupId, padName1);
			assertTrue(padResponse.containsKey("padID"));
			String padId1 = (String) padResponse.get("padID");

			client.setPublicStatus(padId1, true);
			boolean publicStatus = (boolean) client.getPublicStatus(padId1).get("publicStatus");
			assertTrue(publicStatus);

			client.setPassword(padId1, "integration");
			boolean passwordProtected = (boolean) client.isPasswordProtected(padId1).get("isPasswordProtected");
			assertTrue(passwordProtected);

			padResponse = client.createGroupPad(groupId, padName2, "Initial text");
			assertTrue(padResponse.containsKey("padID"));

			String padId = (String) padResponse.get("padID");
			String initialText = (String) client.getText(padId).get("text");
			assertEquals("Initial text\n", initialText);

			Map padListResponse = client.listPads(groupId);

			assertTrue(padListResponse.containsKey("padIDs"));
			List padIds = (List) padListResponse.get("padIDs");

			assertEquals(2, padIds.size());
		} finally {
			client.deleteGroup(groupId);
		}
	}

	@Test
	public void create_author() throws Exception {

		// Create author
		mockServer
				.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/createAuthor").withBody(
						"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"authorID\":\"a.IVPPuqccEHxn0GgC\"}}"));

		// Create author with name's parameter
		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/createAuthor").withBody(
				"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&name=integration-author"),
				Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"authorID\":\"a.UBa5OXyDTRdHvFhM\"}}"));

		// Get author
		mockServer.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getAuthorName").withBody(
				"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&authorID=a.UBa5OXyDTRdHvFhM"),
				Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":\"integration-author\"}"));

		Map authorResponse = client.createAuthor();
		String authorId = (String) authorResponse.get("authorID");
		assertTrue(authorId != null && !authorId.isEmpty());

		authorResponse = client.createAuthor("integration-author");
		authorId = (String) authorResponse.get("authorID");

		String authorName = client.getAuthorName(authorId);
		assertEquals("integration-author", authorName);
	}

	@Test
	public void create_author_with_author_mapper() throws Exception {
		String authorMapper = "username";

		// Create author 1 with mapper
		mockServer.when(
				HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/createAuthorIfNotExistsFor").withBody(
						"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&name=integration-author-1&authorMapper=username"),
				Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"authorID\":\"a.KNGwfi1E8i6VDk0z\"}}"));

		// Get Author 1 Name
		mockServer.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getAuthorName").withBody(
				"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&authorID=a.KNGwfi1E8i6VDk0z"),
				Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":\"integration-author-1\"}"));

		// Create author 2 with mapper
		mockServer.when(
				HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/createAuthorIfNotExistsFor").withBody(
						"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&name=integration-author-2&authorMapper=username"),
				Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"authorID\":\"a.KNGwfi1E8i6VDk0z\"}}"));

		// Get Author 2 Name
		mockServer.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getAuthorName").withBody(
				"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&authorID=a.KNGwfi1E8i6VDk0z"),
				Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":\"integration-author-2\"}"));

		// Create third author
		mockServer.when(
				HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/createAuthorIfNotExistsFor").withBody(
						"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&authorMapper=username"),
				Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"authorID\":\"a.KNGwfi1E8i6VDk0z\"}}"));

		// Get third author
		mockServer.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getAuthorName").withBody(
				"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&authorID=a.KNGwfi1E8i6VDk0z"),
				Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":\"integration-author-2\"}"));

		Map authorResponse = client.createAuthorIfNotExistsFor(authorMapper, "integration-author-1");
		String firstAuthorId = (String) authorResponse.get("authorID");
		assertTrue(firstAuthorId != null && !firstAuthorId.isEmpty());

		String firstAuthorName = client.getAuthorName(firstAuthorId);

		authorResponse = client.createAuthorIfNotExistsFor(authorMapper, "integration-author-2");
		String secondAuthorId = (String) authorResponse.get("authorID");
		assertEquals(firstAuthorId, secondAuthorId);

		String secondAuthorName = client.getAuthorName(secondAuthorId);

		assertNotEquals(firstAuthorName, secondAuthorName);

		authorResponse = client.createAuthorIfNotExistsFor(authorMapper);
		String thirdAuthorId = (String) authorResponse.get("authorID");
		assertEquals(secondAuthorId, thirdAuthorId);
		String thirdAuthorName = client.getAuthorName(thirdAuthorId);

		assertEquals(secondAuthorName, thirdAuthorName);
	}

	@Test
	public void create_pads_and_list_them() throws InterruptedException {
		String pad1 = "integration-test-pad-1";
		String pad2 = "integration-test-pad-2";

		// Create Pad 1
		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/createPad").withBody(
				"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&padID=integration-test-pad-1"),
				Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		// Create Pad 2
		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/createPad").withBody(
				"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&padID=integration-test-pad-2"),
				Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		// List all pads
		mockServer
				.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/listAllPads")
						.withBody("{\"apikey\":\"a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58\"}"))
				.respond(HttpResponse.response().withStatusCode(200).withBody(
						"{\"code\":0,\"message\":\"ok\",\"data\":{\"padIDs\":[\"AWs4MT3urd\",\"g.6ZShGOpcy95NoDal$integration-test-1\",\"g.6ZShGOpcy95NoDal$integration-test-2\","
								+ "\"g.EYwrwPrhdpNC3WXK$integration-test-1\",\"g.EYwrwPrhdpNC3WXK$integration-test-2\",\"g.fFQaUjAK6xf7DUEt$integration-test-1\",\"g.fFQaUjAK6xf7DUEt$integration-test-2\""
								+ ",\"g.l8wOzPrBUV83k4dL$integration-test-1\",\"g.l8wOzPrBUV83k4dL$integration-test-2\",\"g.m7pvksN5EDJ38sFI$integration-test-1\",\"g.m7pvksN5EDJ38sFI$integration-test-2\""
								+ ",\"g.uysHW6BKmtE3xoHe$integration-test-1\",\"g.uysHW6BKmtE3xoHe$integration-test-2\",\"g.zpfUKVtvXneLJgO1$integration-test-1\",\"g.zpfUKVtvXneLJgO1$integration-test-2\""
								+ ",\"hmOc7zyeiZ\",\"integration-move-pad-move\",\"integration-test-pad\",\"integration-test-pad-1\",\"integration-test-pad-2\",\"integration-test-pad-copy\",\"sergio\"]}}"));

		// Delete pad 1
		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/deletePad").withBody(
				"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&padID=integration-test-pad-1"),
				Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		// Delete pad 2
		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/deletePad").withBody(
				"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&padID=integration-test-pad-2"),
				Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		client.createPad(pad1);
		client.createPad(pad2);
		Thread.sleep(100);
		List padIDs = (List) client.listAllPads().get("padIDs");
		client.deletePad(pad1);
		client.deletePad(pad2);

		assertTrue(String.format("Size was %d", padIDs.size()), padIDs.size() >= 2);
		assertTrue(padIDs.contains(pad1));
		assertTrue(padIDs.contains(pad2));
	}

	@Test
	public void create_and_delete_session() throws Exception {
		String authorMapper = "username";
		String groupMapper = "groupname";

		// Create group if it does not exist
		mockServer.when(
				HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/createGroupIfNotExistsFor").withBody(
						"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&groupMapper=groupname"),
				Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"groupID\":\"g.VfrQC2OpOJfqAZqX\"}}"));

		// Create author
		mockServer.when(
				HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/createAuthorIfNotExistsFor").withBody(
						"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&name=integration-author-1&authorMapper=username"),
				Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"authorID\":\"a.KNGwfi1E8i6VDk0z\"}}"));

		Calendar oneYearFromNow = Calendar.getInstance();
		oneYearFromNow.add(Calendar.YEAR, 1);
		Date sessionValidUntil = oneYearFromNow.getTime();
		long validUntil = sessionValidUntil.getTime() / 1000L;

		// Create session with sessionDuration
		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/createSession")
		// .withBody("apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&"
		// +
		// "groupID=g.VfrQC2OpOJfqAZqX&authorID=a.KNGwfi1E8i6VDk0z&sessionDuration=8"),
				, Times.exactly(1)).respond(HttpResponse.response().withStatusCode(200).withBody(
						"{\"code\":0,\"message\":\"ok\",\"data\":{\"sessionID\":\"s.a8c2d3d51838e11775d11cec7361b785\"}}"));

		// Create session with sessionValidUntil
		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/createSession")
		// .withBody("apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&"
		// +
		// "groupID=g.VfrQC2OpOJfqAZqX&authorID=a.KNGwfi1E8i6VDk0z&validUntil="+validUntil),
				, Times.exactly(1)).respond(HttpResponse.response().withStatusCode(200).withBody(
						"{\"code\":0,\"message\":\"ok\",\"data\":{\"sessionID\":\"s.a9c2d3d51838e11775d11cec7361b785\"}}"));

		// Get session 2 info
		mockServer
				.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getSessionInfo")
						.withBody("apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&"
								+ "sessionID=s.a9c2d3d51838e11775d11cec7361b785"),
						Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200).withBody(
						"{\"code\":0,\"message\":\"ok\",\"data\":{\"groupID\":\"g.VfrQC2OpOJfqAZqX\",\"authorID\":\"a.KNGwfi1E8i6VDk0z\"}}"));

		// List Sessions of group
		mockServer
				.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/listSessionsOfGroup")
						.withBody("apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&"
								+ "groupID=g.VfrQC2OpOJfqAZqX"),
						Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200).withBody(
						"{\"code\":0,\"message\":\"ok\",\"data\":{\"s.a8c2d3d51838e11775d11cec7361b785\":{\"groupID\":\"g.VfrQC2OpOJfqAZqX\",\"authorID\":\"a.KNGwfi1E8i6VDk0z\",\"validUntil\":1541892402},"
								+ "\"s.a9c2d3d51838e11775d11cec7361b785\":{\"groupID\":\"g.VfrQC2OpOJfqAZqX\",\"authorID\":\"a.KNGwfi1E8i6VDk0z\",\"validUntil\":"
								+ validUntil + "}}}"));

		// List Sessions of user
		mockServer
				.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/listSessionsOfAuthor")
						.withBody("apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&"
								+ "authorID=a.KNGwfi1E8i6VDk0z"),
						Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200).withBody(
						"{\"code\":0,\"message\":\"ok\",\"data\":{\"s.a8c2d3d51838e11775d11cec7361b785\":{\"groupID\":\"g.VfrQC2OpOJfqAZqX\",\"authorID\":\"a.KNGwfi1E8i6VDk0z\",\"validUntil\":1541892402},"
								+ "\"s.a9c2d3d51838e11775d11cec7361b785\":{\"groupID\":\"g.VfrQC2OpOJfqAZqX\",\"authorID\":\"a.KNGwfi1E8i6VDk0z\",\"validUntil\":"
								+ validUntil + "}}}"));

		// Delete session 1
		mockServer
				.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/deleteSession")
						.withBody("apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&"
								+ "sessionID=s.a8c2d3d51838e11775d11cec7361b785"),
						Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		// Delete session 2
		mockServer
				.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/deleteSession")
						.withBody("apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&"
								+ "sessionID=s.a9c2d3d51838e11775d11cec7361b785"),
						Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		Map groupResponse = client.createGroupIfNotExistsFor(groupMapper);
		String groupId = (String) groupResponse.get("groupID");
		Map authorResponse = client.createAuthorIfNotExistsFor(authorMapper, "integration-author-1");
		String authorId = (String) authorResponse.get("authorID");

		int sessionDuration = 8;
		Map sessionResponse = client.createSession(groupId, authorId, sessionDuration);
		String firstSessionId = (String) sessionResponse.get("sessionID");

		sessionResponse = client.createSession(groupId, authorId, sessionValidUntil);
		String secondSessionId = (String) sessionResponse.get("sessionID");

		try {
			assertNotEquals(firstSessionId, secondSessionId);

			Map sessionInfo = client.getSessionInfo(secondSessionId);
			assertEquals(groupId, sessionInfo.get("groupID"));
			assertEquals(authorId, sessionInfo.get("authorID"));
			// assertEquals(sessionValidUntil.getTime() / 1000L, (long)
			// sessionInfo.get("validUntil"));

			Map sessionsOfGroup = client.listSessionsOfGroup(groupId);
			sessionInfo = (Map) sessionsOfGroup.get(firstSessionId);
			assertEquals(groupId, sessionInfo.get("groupID"));
			sessionInfo = (Map) sessionsOfGroup.get(secondSessionId);
			assertEquals(groupId, sessionInfo.get("groupID"));

			Map sessionsOfAuthor = client.listSessionsOfAuthor(authorId);
			sessionInfo = (Map) sessionsOfAuthor.get(firstSessionId);
			assertEquals(authorId, sessionInfo.get("authorID"));
			sessionInfo = (Map) sessionsOfAuthor.get(secondSessionId);
			assertEquals(authorId, sessionInfo.get("authorID"));
		} finally {
			client.deleteSession(firstSessionId);
			client.deleteSession(secondSessionId);
		}

	}

	@Test
	public void create_pad_move_and_copy() throws Exception {
		String padID = "integration-test-pad";
		String copyPadId = "integration-test-pad-copy";
		String movePadId = "integration-move-pad-move";
		String keep = "should be kept";
		String change = "should be changed";

		// Create Pad
		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/createPad")
		// .withBody("apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&padID=integration-test-pad"
		// + "&keep=should be kept"),
				, Times.exactly(1)).respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		// Copy Pad
		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/copyPad")
		// .withBody("apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&padID=integration-test-pad&"
		// + "copyPadId=integration-test-pad-copy"),
				, Times.exactly(1)).respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		// Get text
		mockServer.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getText")
		// .withBody("apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&movePadId=integration-test-move&"
		// + "change=should be changed"),
				, Times.exactly(1)).respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"text\":\"should be kept\\n\"}}"));

		// Copy Pad
		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/copyPad")
		// .withBody("apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&padID=integration-test-pad&"
		// + "movePadId=integration-test-pad-move"),
				, Times.exactly(1)).respond(HttpResponse.response().withStatusCode(200).withBody(
						"{\"code\":0,\"message\":\"ok\",\"data\":{\"padID\":\"integration-move-pad-move\"}}"));

		// Get text
		mockServer.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getText")
		// .withBody("apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&movePadId=integration-test-move&"
		// + "change=should be changed"),
				, Times.exactly(1)).respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"text\":\"should be kept\\n\"}}"));

		// Set text
		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/setText")
		// .withBody("apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&movePadId=integration-test-move&"
		// + "change=should be changed"),
				, Times.exactly(1)).respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		// Copy Pad
		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/copyPad")
		// .withBody("apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&padID=integration-test-pad&"
		// + "copyPadId=integration-test-pad-copy"),
				, Times.exactly(1)).respond(HttpResponse.response().withStatusCode(200).withBody(
						"{\"code\":0,\"message\":\"ok\",\"data\":{\"padID\":\"integration-test-pad-copy\"}}"));

		// Get text
		mockServer.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getText")
		// .withBody("apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&movePadId=integration-test-move&"
		// + "change=should be changed"),
				, Times.exactly(1)).respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"text\":\"should be kept\\n\"}}"));

		// Move pad
		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/movePad")
		// .withBody("apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&movePadId=integration-test-move&"
		// + "change=should be changed"),
				, Times.exactly(1)).respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		// Get text
		mockServer.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getText")
		// .withBody("apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&movePadId=integration-test-move&"
		// + "change=should be changed"),
				, Times.exactly(1)).respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"text\":\"should be kept\\n\"}}"));

		// Delete pad copy
		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/deletePad")
		// .withBody(
		// "apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&copyPadId=integration-test-pad-copy"),
				, Times.exactly(1)).respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		// Delete pad
		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/deletePad").withBody(
				"apikey=a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58&padID=integration-test-pad"),
				Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(200)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		client.createPad(padID, keep);

		client.copyPad(padID, copyPadId);
		String copyPadText = (String) client.getText(copyPadId).get("text");
		client.movePad(padID, movePadId);
		String movePadText = (String) client.getText(movePadId).get("text");

		client.setText(movePadId, change);
		client.copyPad(movePadId, copyPadId, true);
		String copyPadTextForce = (String) client.getText(copyPadId).get("text");
		client.movePad(movePadId, copyPadId, true);
		String movePadTextForce = (String) client.getText(copyPadId).get("text");

		client.deletePad(copyPadId);
		client.deletePad(padID);

		assertEquals(keep + "\n", copyPadText);
		assertEquals(keep + "\n", movePadText);

		// assertEquals(change + "\n", copyPadTextForce);
		// assertEquals(change + "\n", movePadTextForce);
	}

	@Test
	public void create_pad_set_and_get_content() {

		String padID = "integration-test-pad";

		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/createPad"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/setText"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		mockServer.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getText"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201).withBody(
						"{\"code\":0,\"message\":\"ok\",\"data\":{\"text\":\"gå å gjør et ærend\\n\"}}",
						Charset.forName("UTF-8")));

		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/setHTML"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		mockServer.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getHTML"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201).withBody(
						"{\"code\":0,\"message\":\"ok\",\"data\":{\"html\":\"<!DOCTYPE HTML><html><body>g&#229; og gj&#248;re et &#230;rend igjen<br><br></body></html>\"}}"));

		mockServer.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getHTML"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201).withBody(
						"{\"code\":0,\"message\":\"ok\",\"data\":{\"html\":\"<!DOCTYPE HTML><html><body><br></body></html>\"}}"));

		mockServer.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getText"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"text\":\"\\n\"}}"));

		mockServer
				.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getRevisionsCount"),
						Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"revisions\":3}}"));

		mockServer
				.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getRevisionChangeset"),
						Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201).withBody(
						"{\"code\":0,\"message\":\"ok\",\"data\":\"Z:1>r|1+r$gå og gjøre et ærend igjen\n\"}",
						Charset.forName("UTF-8")));

		mockServer
				.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getRevisionChangeset"),
						Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":\"Z:j<i|1-j|1+1$\\n\"}"));

		mockServer
				.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/createDiffHTML"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201).withBody(
						"{\"code\":0,\"message\":\"ok\",\"data\":{\"html\":\"<style>\\n.removed {text-decoration: line-through; -ms-filter:\'progid:DXImageTransform.Microsoft.Alpha(Opacity=80)\'; filter: alpha(opacity=80); opacity: 0.8; }\\n</style><span class=\\\"removed\\\">g&#229; &#229; gj&#248;r et &#230;rend</span><br><br>\",\"authors\":[\"\"]}}"));

		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/appendText"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		mockServer.when(HttpRequest.request().withPath("/api/1.2.13/getText"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201).withBody(
						"{\"code\":0,\"message\":\"ok\",\"data\":{\"text\":\"gå og gjøre et ærend igjen\\nlagt til nå\\n\"}}",
						Charset.forName("UTF-8")));

		mockServer
				.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getAttributePool"),
						Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201).withBody(
						"{\"code\":0,\"message\":\"ok\",\"data\":{\"pool\":{\"numToAttrib\":{\"0\":[\"author\",\"\"],\"1\":[\"removed\",\"true\"]},\"attribToNum\":{\"author,\":0,\"removed,true\":1},\"nextNum\":2}}}"));

		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/saveRevision"), Times.exactly(2))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		mockServer
				.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getSavedRevisionsCount"),
						Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"savedRevisions\":2}}"));

		mockServer
				.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/listSavedRevisions"),
						Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"savedRevisions\":[2,4]}}"));

		mockServer.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/padUsersCount"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"padUsersCount\":0}}"));

		mockServer.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/padUsers"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"padUsers\":[]}}"));

		mockServer.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getReadOnlyID"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201).withBody(
						"{\"code\":0,\"message\":\"ok\",\"data\":{\"readOnlyID\":\"r.f93654178b11b8d40ee35e5b4b343a68\"}}"));

		mockServer.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getPadID"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"padID\":\"integration-test-pad\"}}"));

		mockServer
				.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/listAuthorsOfPad"),
						Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"authorIDs\":[]}}"));

		mockServer.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getLastEdited"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"lastEdited\":1541863731098}}"));

		mockServer
				.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/sendClientsMessage"),
						Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/deletePad"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		client.createPad(padID);
		try {
			client.setText(padID, "gå å gjør et ærend");
			String text = (String) client.getText(padID).get("text");
			// assertEquals("gå å gjør et ærend\n", text);

			client.setHTML(padID, "<!DOCTYPE HTML><html><body><p>gå og gjøre et ærend igjen</p></body></html>");
			String html = (String) client.getHTML(padID).get("html");
			assertTrue(html, html.contains("g&#229; og gj&#248;re et &#230;rend igjen<br><br>"));

			html = (String) client.getHTML(padID, 2).get("html");
			assertEquals("<!DOCTYPE HTML><html><body><br></body></html>", html);
			text = (String) client.getText(padID, 2).get("text");
			assertEquals("\n", text);

			long revisionCount = (long) client.getRevisionsCount(padID).get("revisions");
			assertEquals(3L, revisionCount);

			String revisionChangeset = client.getRevisionChangeset(padID);
			// assertTrue(revisionChangeset, revisionChangeset.contains("gå og gjøre et
			// ærend igjen"));

			revisionChangeset = client.getRevisionChangeset(padID, 2);
			assertTrue(revisionChangeset, revisionChangeset.contains("|1-j|1+1$\n"));

			// String diffHTML = (String) client.createDiffHTML(padID, 1, 2).get("html");
			// assertTrue(diffHTML, diffHTML.contains("<span class=\"removed\">g&#229;
			// &#229; gj&#248;r et &#230;rend</span>"));

			client.appendText(padID, "lagt til nå");
			text = (String) client.getText(padID).get("text");
			// assertEquals("gå og gjøre et ærend igjen\nlagt til nå\n", text);

			Map attributePool = (Map) client.getAttributePool(padID).get("pool");
			assertTrue(attributePool.containsKey("attribToNum"));
			assertTrue(attributePool.containsKey("nextNum"));
			assertTrue(attributePool.containsKey("numToAttrib"));

			client.saveRevision(padID);
			client.saveRevision(padID, 2);

			long savedRevisionCount = (long) client.getSavedRevisionsCount(padID).get("savedRevisions");
			assertEquals(2L, savedRevisionCount);

			List savedRevisions = (List) client.listSavedRevisions(padID).get("savedRevisions");
			assertEquals(2, savedRevisions.size());
			assertEquals(2L, savedRevisions.get(0));
			assertEquals(4L, savedRevisions.get(1));

			long padUsersCount = (long) client.padUsersCount(padID).get("padUsersCount");
			assertEquals(0, padUsersCount);

			List padUsers = (List) client.padUsers(padID).get("padUsers");
			assertEquals(0, padUsers.size());

			String readOnlyId = (String) client.getReadOnlyID(padID).get("readOnlyID");
			String padIdFromROId = (String) client.getPadID(readOnlyId).get("padID");
			assertEquals(padID, padIdFromROId);

			List authorsOfPad = (List) client.listAuthorsOfPad(padID).get("authorIDs");
			assertEquals(0, authorsOfPad.size());

			long lastEditedTimeStamp = (long) client.getLastEdited(padID).get("lastEdited");
			Calendar lastEdited = Calendar.getInstance();
			lastEdited.setTimeInMillis(lastEditedTimeStamp);
			Calendar now = Calendar.getInstance();
			assertTrue(lastEdited.before(now));

			client.sendClientsMessage(padID, "test message");
		} finally {
			client.deletePad(padID);
		}

	}

	@Test
	public void create_pad_and_chat_about_it() {

		String padID = "integration-test-pad-1";
		String user1 = "user1";
		String user2 = "user2";

		mockServer
				.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/createAuthorIfNotExistsFor"),
						Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"authorID\":\"a.UQLumMigmWReonxg\"}}"));

		mockServer
				.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/createAuthorIfNotExistsFor"),
						Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"authorID\":\"a.OeyYMMVD6KYHJ0oe\"}}"));

		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/createPad"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		mockServer
				.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/appendChatMessage"),
						Times.exactly(3))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		mockServer.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getChatHead"), Times.exactly(3))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"chatHead\":2}}"));

		mockServer
				.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getChatHistory"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201).withBody(
						"{\"code\":0,\"message\":\"ok\",\"data\":{\"messages\":[{\"text\":\"hi from user1\",\"userId\":\"a.UQLumMigmWReonxg\",\"time\":1541863985955,\"userName\":\"integration-author-1\"},{\"text\":\"hi from user2\",\"userId\":\"a.OeyYMMVD6KYHJ0oe\",\"time\":1541863985,\"userName\":\"integration-author-2\"},{\"text\":\"gå å gjør et ærend\",\"userId\":\"a.UQLumMigmWReonxg\",\"time\":1541863985,\"userName\":\"integration-author-1\"}]}}",
						Charset.forName("UTF-8")));

		mockServer
				.when(HttpRequest.request().withMethod("GET").withPath("/api/1.2.13/getChatHistory"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201).withBody(
						"{\"code\":0,\"message\":\"ok\",\"data\":{\"messages\":[{\"text\":\"hi from user1\",\"userId\":\"a.UQLumMigmWReonxg\",\"time\":1541863985955,\"userName\":\"integration-author-1\"},{\"text\":\"hi from user2\",\"userId\":\"a.OeyYMMVD6KYHJ0oe\",\"time\":1541863985,\"userName\":\"integration-author-2\"}]}}"));

		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/api/1.2.13/deletePad"), Times.exactly(1))
				.respond(HttpResponse.response().withStatusCode(201)
						.withBody("{\"code\":0,\"message\":\"ok\",\"data\":null}"));

		Map response = client.createAuthorIfNotExistsFor(user1, "integration-author-1");
		String author1Id = (String) response.get("authorID");
		response = client.createAuthorIfNotExistsFor(user2, "integration-author-2");
		String author2Id = (String) response.get("authorID");

		client.createPad(padID);
		try {
			client.appendChatMessage(padID, "hi from user1", author1Id);
			client.appendChatMessage(padID, "hi from user2", author2Id, System.currentTimeMillis() / 1000L);
			// client.appendChatMessage(padID, "gå å gjør et ærend", author1Id,
			// System.currentTimeMillis() / 1000L);
			response = client.getChatHead(padID);
			long chatHead = (long) response.get("chatHead");
			assertEquals(2, chatHead);

			response = client.getChatHistory(padID);
			List chatHistory = (List) response.get("messages");
			assertEquals(3, chatHistory.size());
			// assertEquals("gå å gjør et ærend", ((Map) chatHistory.get(2)).get("text"));

			response = client.getChatHistory(padID, 0, 1);
			chatHistory = (List) response.get("messages");
			assertEquals(2, chatHistory.size());
			assertEquals("hi from user2", ((Map) chatHistory.get(1)).get("text"));
		} finally {
			client.deletePad(padID);
		}

	}

}
