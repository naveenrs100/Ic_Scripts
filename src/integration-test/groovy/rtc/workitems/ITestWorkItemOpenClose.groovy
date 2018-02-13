package rtc.workitems

import org.junit.Assert;
import org.junit.Test;

import base.BaseTest

class ITestWorkItemOpenClose extends BaseWorkItemTest {

	@Test
	public void testOpenClose() {
		def WORKITEM = 13141
		WorkItemBean bean = helper.getWorkItem(WORKITEM);
		if (bean.getStateId().equals(WorkItemHelper.RTC_WI_STATE_RELEASE_CLOSED)) {
			// Retrocederlo a Nueva
			println  "[WARNING] Hay que retroceder el estado del workitem"
			helper.sendAction(bean.getId(), "com.eci.team.workitem.releaseWorkflow.action.a15");
			helper.sendAction(bean.getId(), "com.eci.team.workitem.releaseWorkflow.action.a4");
		}
		helper.closeRelease(bean.getId());	
		bean = helper.getWorkItem(WORKITEM);
		Assert.assertEquals(
			WorkItemHelper.RTC_WI_STATE_RELEASE_CLOSED, 
			bean.getStateId());
	}
}
