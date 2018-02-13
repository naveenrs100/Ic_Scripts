package rtc.workitems

import org.junit.Before;

import base.BaseTest

class BaseWorkItemTest extends BaseTest {

	protected WorkItemHelper helper = null;
	
	@Before
	public void init() {
		helper = new WorkItemHelper(
			url, 
			user, 
			password, 
			rtcKeystoreVersion, 
			nexusURL);
		helper.initLogger { println it }		
	}
	
}
