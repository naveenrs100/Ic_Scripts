import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import groovy.json.JsonSlurper;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.hssf.usermodel.HSSFCell
import org.apache.poi.hssf.usermodel.HSSFCellStyle
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.hssf.usermodel.HSSFFont;

import es.eci.utils.SystemPropertyBuilder
import es.eci.utils.excel.HelperGenerateReleaseNotes;

SystemPropertyBuilder parameterBuilder = new SystemPropertyBuilder();
def params = parameterBuilder.getSystemParameters();

def streamTarget = params["streamTarget"]
def component = params["component"]
def lastBaseline = params["lastBaseline"]
def penultimateBaseline = params["penultimateBaseline"]
def SCMTOOLS_HOME = params["SCMTOOLS_HOME"]
def DAEMONS_HOME = params["DAEMONS_HOME"]
def userRTC = params["userRTC"]
def pwdRTC = params["pwdRTC"]
def urlRTC = params["urlRTC"]

File changeSetFile = new File("C:/OpenDevECI/WSECI_NEON/DIC - Scripts/src/test/groovy/personal/changeSet.json");
String changeLogXLS = "C:/Users/dcastro.jimenez/Desktop/ExcelReportTest/changelog.xls";
File parentWorkspace = new File("C:/Users/dcastro.jimenez/Desktop/ExcelReportTest/parentWorkspace");

HelperGenerateReleaseNotes helper = new HelperGenerateReleaseNotes();
//helper.writeExcelReport(changeSetFile.text, changeLogXLS);

helper.generateReleaseNotesRTC(
	"10",
	"GIS - QUVE - RELEASE",
	"DIC - Portal ICQA",
	"JENKINS_RTC",
	"12345678",
	"https://rtc.elcorteingles.int:9443/ccm",
	parentWorkspace,
	"C:/OpenDevECI/scmtools/eclipse",
	"C:/OpenDevECI/scmtools/daemons_home",
	//build,
	"buildInvoker no use",
	"1.2.25.0",
	"1.2.20.0",
	build.works);


