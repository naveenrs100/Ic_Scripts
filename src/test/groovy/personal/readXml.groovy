import es.eci.utils.StringUtil;
import org.apache.poi.hssf.usermodel.HSSFCellStyle
import org.apache.poi.hssf.usermodel.HSSFFont
import org.apache.poi.hssf.usermodel.HSSFSheet
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.IndexedColors


File outputFile = new File("C:/Users/dcastro.jimenez/Desktop/areas.txt");
String xml = new File("C:/Users/dcastro.jimenez/Desktop/full_areas.xml").text;

def root = new XmlParser().parseText(xml);

HSSFWorkbook workbook = new HSSFWorkbook();
HSSFFont defaultFont= workbook.createFont();
defaultFont.setBoldweight(HSSFFont.BOLDWEIGHT_NORMAL);
defaultFont.setFontHeightInPoints((short)10);
defaultFont.setFontName("Arial");
defaultFont.setColor(IndexedColors.BLACK.getIndex());
defaultFont.setItalic(false);

HSSFFont font= workbook.createFont();
font.setFontHeightInPoints((short)10);
font.setFontName("Arial");
font.setColor(IndexedColors.BLACK.getIndex());
font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
font.setItalic(false);

HSSFCellStyle boldStyle = workbook.createCellStyle();
boldStyle.setFont(font);

HSSFCellStyle defaultStyle = workbook.createCellStyle();
defaultStyle.setFont(defaultFont);



root.object.void.each {
	println "" + it.string.text() + ":"
	outputFile.append(it.string.text() + ":\n");
	HSSFSheet sheet = workbook.createSheet(it.string.text());
	ArrayList<String> tmpList = new ArrayList<String>();
	int rowCount = 0;
	it.object.void.each { streamObject ->				
		String normalizedStream = StringUtil.trimStreamName(streamObject.string.text())		
		if(!tmpList.contains(normalizedStream)) {
			Row row = sheet.createRow(rowCount);
			rowCount++;
			Cell cell = row.createCell(0);
			tmpList.add(normalizedStream);
			println "\t" + StringUtil.trimStreamName(streamObject.string.text())
			outputFile.append("\t" + StringUtil.trimStreamName(streamObject.string.text()) + "\n")
			cell.setCellValue(StringUtil.trimStreamName(streamObject.string.text()));			
		}		
	}
	println("\n")
	outputFile.append("\n");
}


try {
	FileOutputStream fileOut = new FileOutputStream("C:/Users/dcastro.jimenez/Desktop/full_Areas.xlsx");
	workbook.write(fileOut);
	fileOut.flush();
	fileOut.close();
	System.out.println("Excel written successfully..");

} catch (FileNotFoundException e) {
	e.printStackTrace();
} catch (IOException e) {
	e.printStackTrace();
}