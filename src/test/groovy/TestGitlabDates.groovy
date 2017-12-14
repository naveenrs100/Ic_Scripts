import git.GitUtils

import org.junit.Assert
import org.junit.Test


class TestGitlabDates {

	@Test
	public void testParseDate() {
		def date1 = "2017-01-24T15:04:50.000Z"
		def date2 = "2017-07-14T15:51:35.000+02:00"
		
		def parseDate1 = GitUtils.parseDate(date1)
		Assert.assertNotNull(parseDate1)
		println parseDate1
		def parseDate2 = GitUtils.parseDate(date2)
		Assert.assertNotNull(parseDate2)
		println parseDate2
	}
	
	public void testMostRecentDate() {
		Date date1 = GitUtils.parseDate("2017-01-24T15:04:50.000Z");
		Date date2 = GitUtils.parseDate("2017-01-25T15:04:50.000Z");
		Date date3 = GitUtils.parseDate("2017-01-26T15:04:50.000Z");
		
		Assert.assertEquals(date2, GitUtils.mostRecentDate(date1, date2));
		Assert.assertEquals(date3, GitUtils.mostRecentDate(date1, date3));
		Assert.assertEquals(date3, GitUtils.mostRecentDate(date2, date3));
		Assert.assertEquals(date3, GitUtils.mostRecentDate(date1, date2, date3));
		Assert.assertEquals(date3, GitUtils.mostRecentDate(null, date1, null, date3));
		Assert.assertEquals(date2, GitUtils.mostRecentDate(null, date1, null, date2, null));
		
		Assert.assertNull(GitUtils.mostRecentDate(null, null, null));
	}
}
