import java.nio.file.FileAlreadyExistsException

import org.junit.Assert
import org.junit.Test

import es.eci.utils.TmpDir
import es.eci.utils.ZipHelper

class TestZipHelper {

					private static final String FILE_NAME = "source"

	private static final String FILE_CONTENT = "aaa"

	
	@Test
	public void testTempFile() {
		TmpDir.tmp  { File tempDirSource ->
			TmpDir.tmp { File tempDirTarget ->
				File f = new File(tempDirSource, FILE_NAME);
				f.text = FILE_CONTENT;
				File temp = ZipHelper.addDirToArchive(tempDirSource);
				ZipHelper.unzipFile(temp, tempDirTarget);
				File targetFile = new File(tempDirTarget, FILE_NAME)
				Assert.assertTrue(targetFile.exists());
				Assert.assertEquals(targetFile.text, FILE_CONTENT);
			}
		}
	}
	
	
	@Test
	public void testDesignatedFile() {
		TmpDir.tmp  { File tempDirSource ->
			TmpDir.tmp { File intermediateDir ->
				File zipFile = new File(intermediateDir, "zipFile")
				TmpDir.tmp { File tempDirTarget ->
					File f = new File(tempDirSource, FILE_NAME);
					f.text = FILE_CONTENT;
					ZipHelper.addDirToArchive(tempDirSource, zipFile);
					ZipHelper.unzipFile(zipFile, tempDirTarget);
					File targetFile = new File(tempDirTarget, FILE_NAME)
					Assert.assertTrue(targetFile.exists());
					Assert.assertEquals(targetFile.text, FILE_CONTENT);
				}
			}
		}
	}
}
