package rtc.workitems

import org.junit.Assert
import org.junit.Test

class ITestRecoverWorkItem extends BaseWorkItemTest {
	
	@Test
	public void recoverWorkItemInfo() {
		WorkItemBean bean = helper.getWorkItem(1803);
		Assert.assertEquals("1803", bean.getId());
		Assert.assertEquals("1", bean.getStateId());
		Assert.assertEquals("task", bean.getTypeId());
	}
	
	@Test
	public void recoverReleaseInfo() {
		WorkItemBean bean = helper.getWorkItem(13141);
		Assert.assertEquals("13141", bean.getId());
		Assert.assertEquals(WorkItemHelper.RTC_WI_TYPE_RELEASE, bean.getTypeId());
		helper.getDiscussion(bean);
		println bean.getDiscussion();
		println bean.getAttributes();
		Assert.assertEquals("123456", bean.attributes["CRQ_Remedy"])
	}
}
