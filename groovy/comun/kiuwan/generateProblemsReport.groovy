
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.streaming.SXSSFWorkbook

@GrabResolver(name='nexusECI', root='http://nexus.elcorteingles.pre/content/groups/public/')
@Grab(group='org.apache.poi', module='poi-ooxml', version='3.10.1')

import es.eci.utils.SystemPropertyBuilder
import groovy.json.JsonSlurper

/**
 * Este script se invoca como Groovy Script simple.
 * 
 * Se ejecuta para informar de qué corrientes de desarrollo no tienen
 * corriente de producción en RTC, y qué repos no tienen rama master o no
 * está inicializada.
 * 
 * Recoge el json de informe de errores y lo convierte en un excel.
 * El formato del json de informe de errores es una lista de maps:
 * 
 *  
 * Un informe como:
 * 
 * 
 <pre>{
 	[
	 	parentWorkspace:..., 
	 	product:TPV PdS2, 
	 	subsystem:CC.CC - TPV Pds2 r22.1, 
	 	component:6A2_UpdateToolkit, 
	 	stream:CC.CC - TPV Pds2 r22.1 - PRODUCCION, 
	 	workspaceRTC:WSR - CC.CC - TPV Pds2 r22.1 - PRODUCCION - KIUWAN, 
	 	jobName:...
 	], 
 	[
	 	parentWorkspace:..., 
	 	product:TPV PdS2, 
	 	subsystem:CC.CC - TPV Pds2 r22.1, 
	 	component:6A2_BloqueoBinarios, 
	 	stream:CC.CC - TPV Pds2 r22.1 - PRODUCCION, 
	 	workspaceRTC:WSR - CC.CC - TPV Pds2 r22.1 - PRODUCCION - KIUWAN, 
	 	jobName:...
 	],
 	[
	 	parentWorkspace:..., 
	 	product:TPV PdS2, 
	 	subsystem:CC.CC - TPV Pds2 r22.1, 
	 	component:6A2_TPV, 
	 	stream:CC.CC - TPV Pds2 r22.1 - PRODUCCION, 
	 	workspaceRTC:WSR - CC.CC - TPV Pds2 r22.1 - PRODUCCION - KIUWAN, 
	 	jobName:...
 	],
 	[
	 	parentWorkspace:..., 
	 	product:OTRO_PRODUCTO, 
	 	subsystem:UN_SUBSISTEMA, 
	 	component:repo11, 
	 	stream:UN_SUBSISTEMA - PRODUCCION, 
	 	workspaceRTC:WSR - UN_SUBSISTEMA - PRODUCCION - KIUWAN, 
	 	jobName:...
 	],
 	[
	 	parentWorkspace:..., 
	 	product:OTRO_PRODUCTO, 
	 	subsystem:UN_SUBSISTEMA, 
	 	component:repo12, 
	 	stream:UN_SUBSISTEMA - PRODUCCION, 
	 	workspaceRTC:WSR - UN_SUBSISTEMA - PRODUCCION - KIUWAN, 
	 	jobName:...
 	], 
 	[
	 	parentWorkspace:..., 
	 	product:OTRO_PRODUCTO, 
	 	subsystem:OTRO_SUBSISTEMA, 
	 	component:repo21, 
	 	stream:OTRO_SUBSISTEMA - PRODUCCION, 
	 	workspaceRTC:WSR - OTRO_SUBSISTEMA - PRODUCCION - KIUWAN, 
	 	jobName:...
 	],
 	[
	 	parentWorkspace:..., 
	 	product:OTRO_PRODUCTO, 
	 	subsystem:OTRO_SUBSISTEMA, 
	 	component:repo23, 
	 	stream:OTRO_SUBSISTEMA - PRODUCCION, 
	 	workspaceRTC:WSR - OTRO_SUBSISTEMA - PRODUCCION - KIUWAN, 
	 	jobName:...
 	],
 	[
	 	parentWorkspace:..., 
	 	product:OTRO_PRODUCTO, 
	 	subsystem:OTRO_SUBSISTEMA, 
	 	component:repo23, 
	 	stream:OTRO_SUBSISTEMA - PRODUCCION, 
	 	workspaceRTC:WSR - OTRO_SUBSISTEMA - PRODUCCION - KIUWAN, 
	 	jobName:...
 	]
 }</pre>
 se traduce a un excel tal que este:
 <pre>
 -----------------------------------------------------------------
 TPV PdS2		CC.CC - TPV Pds2 r22.1	6A2_UpdateToolkit
 TPV PdS2		CC.CC - TPV Pds2 r22.1	6A2_BloqueoBinarios
 TPV PdS2		CC.CC - TPV Pds2 r22.1	6A2_TPV
 OTRO_PRODUCTO	UN_SUBSISTEMA			repo11
 OTRO_PRODUCTO	UN_SUBSISTEMA			repo12
 OTRO_PRODUCTO	OTRO_SUBSISTEMA			repo21
 OTRO_PRODUCTO	OTRO_SUBSISTEMA			repo22
 OTRO_PRODUCTO	OTRO_SUBSISTEMA			repo23
 -----------------------------------------------------------------
 </pre>
 */

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

def params = propertyBuilder.getSystemParameters()

File parentWorkspace = new File(params.get("parentWorkspace").toString())

assert parentWorkspace.exists()
assert parentWorkspace.isDirectory()

File reportFile = new File(parentWorkspace, params.get("fileName"))

assert reportFile.exists()

def report = new JsonSlurper().parseText(reportFile.text)

File destFile = new File(parentWorkspace, params.get("fileName") + ".xlsx")

Workbook book = new SXSSFWorkbook(); 
Sheet sheet = book.createSheet("componentes")

int i = 0;
// Recorrer áreas de proyecto
report.each { Map productMap ->
	println "${productMap['product']} - ${productMap['subsystem']} - ${productMap['component']}"
	Row row = sheet.createRow(i++)
	int column = 0;
	Cell productCell = row.createCell(column++)
	productCell.setCellValue(productMap['product']);
	Cell subsystemCell = row.createCell(column++);
	subsystemCell.setCellValue(productMap['subsystem']);
	Cell componentCell = row.createCell(column++);
	componentCell.setCellValue(productMap['component']);
}

OutputStream os = new FileOutputStream(destFile)
try {
	book.write(os)
}
finally {
	if (os != null) {
		os.close();
	}
}

