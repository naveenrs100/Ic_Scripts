import org.junit.Assert
import org.junit.Test

import es.eci.utils.StringUtil

class TestStringUtil {

	
	@Test
	public void testNormalizeBlank() {
		Assert.assertEquals("a-b-c", StringUtil.normalize("a b c"));
	}
	
	@Test
	public void testNormalizeScore() {
		Assert.assertEquals("a-b-c-d", StringUtil.normalize("a - b - c - d"));
	}
	
	@Test
	public void testNormalizeOddScore() {
		Assert.assertEquals("a-b-c-d", StringUtil.normalize("a- b- c -d"));
	}
	
	@Test
	public void testNormalizeTrivial() {
		Assert.assertEquals("abcd", StringUtil.normalize("abcd"));
	}
	
	@Test
	public void testNormalizeTrivialScores() {
		Assert.assertEquals("a-b-c-d", StringUtil.normalize("a-b-c-d"));
	}
	
	@Test
	public void testNormalizeParenthesis() {
		Assert.assertEquals("a-b-c-d", StringUtil.normalize("a ( b c (d)"));
	}
}
