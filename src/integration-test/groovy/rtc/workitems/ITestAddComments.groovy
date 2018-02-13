package rtc.workitems

import org.junit.Test

class ITestAddComments extends BaseWorkItemTest {
	
	@Test
	public void addComment() {
		helper.addComment("13138", "comentario de prueba - " + new Date().getTime())
	}
}
