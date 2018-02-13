package rtc.workitems

import org.junit.Assert;
import org.junit.Test;

class TestWorkItemWorkFlow {
	
	@Test
	public void testFromNew() {
		List<WIState> states = 
			new ReleaseWorkFlow().
				getActionsUntilClosed("com.eci.team.workitem.releaseWorkflow.state.s1");
		println "Desde nueva -> " 
		states.each { println "\t$it" }
		Assert.assertEquals(5, states.size())
	}
	
	@Test
	public void testFromDesign() {
		List<WIState> states = 
			new ReleaseWorkFlow().
				getActionsUntilClosed("com.eci.team.workitem.releaseWorkflow.state.s2");
		println "Desde diseño -> " 
		states.each { println "\t$it" }
		Assert.assertEquals(4, states.size())
	}
	
	@Test
	public void testFromDevelopment() {
		List<WIState> states = 
			new ReleaseWorkFlow().
				getActionsUntilClosed("com.eci.team.workitem.releaseWorkflow.state.s10");
		println "Desde desarrollo -> "
		states.each { println "\t$it" }
		
		Assert.assertEquals(3, states.size())
	}
	
	@Test
	public void testFromPre() {
		List<WIState> states = 
			new ReleaseWorkFlow().
				getActionsUntilClosed("com.eci.team.workitem.releaseWorkflow.state.s3");
		println "Desde UAT/Pre -> "
		states.each { println "\t$it" }
		Assert.assertEquals(2, states.size())
	}
	
	@Test
	public void testFromReadyPro() {
		List<WIState> states = 
			new ReleaseWorkFlow().
				getActionsUntilClosed("com.eci.team.workitem.releaseWorkflow.state.s8");
		println "Desde listo PRO -> "
		states.each { println "\t$it" }
		Assert.assertEquals(1, states.size())
	}
	
	@Test
	public void testFromNFT_Optional() {
		List<WIState> states = 
			new ReleaseWorkFlow().
				getActionsUntilClosed("com.eci.team.workitem.releaseWorkflow.state.s7");
		println "Desde NFT/Integración -> "
		states.each { println "\t$it" }
		Assert.assertEquals(2, states.size())
	}
	
	@Test
	public void testFromPilot_Optional() {
		List<WIState> states = 
			new ReleaseWorkFlow().
				getActionsUntilClosed("com.eci.team.workitem.releaseWorkflow.state.s9");
		println "Desde piloto -> "
		states.each { println "\t$it" }
		Assert.assertEquals(1, states.size())
	}
}
