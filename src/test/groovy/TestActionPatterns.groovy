import org.junit.Assert;
import org.junit.Test;

import es.eci.utils.jenkins.RTCWorkspaceHelper;


class TestActionPatterns {

	@Test
	public void testActionPatterns() {
		Assert.assertEquals("WSR - mi corriente de prueba - BUILD - IC", 
			RTCWorkspaceHelper.getWorkspaceRTC("build", "mi corriente de prueba"));
		
		Assert.assertEquals("WSR - mi corriente de prueba - DEPLOY - IC", 
			RTCWorkspaceHelper.getWorkspaceRTC("deploy", "mi corriente de prueba"));
		
		Assert.assertEquals("WSR - mi corriente de prueba - RELEASE - IC", 
			RTCWorkspaceHelper.getWorkspaceRTC("release", "mi corriente de prueba"));
		
		
		Assert.assertEquals("WSR - mi corriente de prueba - ADDFIX - IC", 
			RTCWorkspaceHelper.getWorkspaceRTC("addFix", "mi corriente de prueba"));
		
		
		Assert.assertEquals("WSR - mi corriente de prueba - ADDHOTFIX - IC", 
			RTCWorkspaceHelper.getWorkspaceRTC("addHotfix", "mi corriente de prueba"));
	}
}
