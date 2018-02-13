package utils

import static org.junit.Assert.assertEquals

import org.junit.Test

import es.eci.utils.UtilUUID


class TestUUID {

	@Test
	public void testGetUUIDFromBase64String() {
		final String base64String = "Ba94oBngEeGIbZVtiUctVQ";
		final UUID uuid = UtilUUID.getUUIDFromBase64String(base64String);
		System.out.println(uuid.toString());
		final String base64String2 = UtilUUID.getBase64String(uuid);
		assertEquals(base64String, base64String2);
	}
	
	@Test
	public void testGetUUIDFromBase64StringWithUnderscore() {
		final String base64String = "_Ba94oBngEeGIbZVtiUctVQ";
		final UUID uuid = UtilUUID.getUUIDFromBase64String(base64String, true);
		System.out.println(uuid.toString());
		final String base64String2 = UtilUUID.getBase64String(uuid);
		assertEquals(base64String, "_" + base64String2);
	}

	@Test
	public void testProjectAreaIdFromJIRA() {
		Long jiraId = 11200l;
		UUID uuid = UtilUUID.getUUIDFromJIRAProject(jiraId);
		System.out.println(jiraId + " --> " + uuid);
		Long longValue = UtilUUID.getJiraProjectFromUUID(uuid);
		assertEquals(jiraId, longValue);
	}
}
