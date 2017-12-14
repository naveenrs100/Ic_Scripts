import java.util.regex.Pattern
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import org.apache.commons.io.filefilter.AbstractFileFilter
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter
import org.apache.commons.io.filefilter.NotFileFilter
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;

File dir = new File("C:/Users/dcastro.jimenez/Desktop/testDir");

Iterator<File> files = FileUtils.iterateFiles(new File("C:/Users/dcastro.jimenez/Desktop/testDir"), null, true);

for(Iterator<File> iter = files.iterator(); iter.hasNext();) {
	File file = iter.next();
	//System.out.println(file.getCanonicalPath());
}

IOFileFilter filtertmp = new NotFileFilter(new NameFileFilter("ÑUÑUÑUÑ"));
IOFileFilter filter1 = new NotFileFilter(new NameFileFilter("target"));
IOFileFilter filter2 = new NotFileFilter(new RegexFileFilter("\\..*"));
IOFileFilter finalFilter = FileFilterUtils.and(filter1,filter2);
List<File> files2 =  (List<File>)FileUtils.listFiles(dir, filtertmp, finalFilter);
 for (File file : files2) {
  System.out.println(file.getCanonicalPath());
}



