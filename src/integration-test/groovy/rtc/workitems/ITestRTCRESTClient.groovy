package rtc.workitems

import org.junit.Assert
import org.junit.Before;
import org.junit.Test

import rtc.RTCClient
import base.BaseTest
import es.eci.utils.Stopwatch
import groovy.xml.XmlUtil

class ITestRTCRESTClient extends BaseTest {

	private RTCClient client = null;
	
	@Before
	public void init() {
		client = new RTCClient(
			url, 
			user, 
			password, 
			rtcKeystoreVersion, 
			nexusURL);
		client.initLogger { println it }
	}
	
	@Test
	public void testServicesCatalog() {
		def node = null;
		long millis = Stopwatch.watch {
			String ret = null;
			ret = client.get("rootservices")
			Assert.assertEquals(200, client.getLastHttpStatus())
			node = new XmlParser().parseText(ret)
		}
		println "========================================="
		println "CONSULTA DE CATÁLOGO DE SERVICIOS"
		println XmlUtil.serialize(node)
		println "========================================="
		println "Consulta a RTC realizada en $millis mseg."
	}
	
	@Test
	public void testWorkItemInfo() {
		def node = null;
		long millis = Stopwatch.watch {
			String ret = null;
			ret = client.get("rpt/repository/workitem", 
				["fields":"workitem/workItem[id=1803]/(id|summary|state/*|type/*)"])
			Assert.assertEquals(200, client.getLastHttpStatus())
			node = new XmlParser().parseText(ret)
		}
		println "========================================="
		println "CONSULTA DE WORKITEM"
		println XmlUtil.serialize(node)
		println "========================================="
		println "Consulta a RTC realizada en $millis mseg."
	}
	
	@Test
	public void testReleaseInfo() {
		def node = null;
		long millis = Stopwatch.watch {
			String ret = null;
			ret = client.get("rpt/repository/workitem", 
				["fields":"workitem/workItem[id=13138]/(id|summary|state/*|type/*)"])
			Assert.assertEquals(200, client.getLastHttpStatus())
			node = new XmlParser().parseText(ret)
		}
		println "========================================="
		println "CONSULTA DE WORKITEM"
		println XmlUtil.serialize(node)
		println "========================================="
		println "Consulta a RTC realizada en $millis mseg."
	}
	
		@Test
	public void testReleaseAttributeInfo() {
		def node = null;
		long millis = Stopwatch.watch {
			String ret = null;
			ret = client.get("rpt/repository/workitem", 
				//["fields":"workitem/workItem[id=13141]/(customAttributes/*)"])
				["fields":"workitem/workItem[id=13141]/*/*)"])
			Assert.assertEquals(200, client.getLastHttpStatus())
			node = new XmlParser().parseText(ret)
			String remedy = "";
			node.workItem[0].mediumStringExtensions.each { customAttribute ->
				if ("CRQ_Remedy".equals(customAttribute.key[0].text())) {
					remedy = customAttribute.value[0].text()
				}
			}
			Assert.assertEquals("123456", remedy)
		}
		println "========================================="
		println "CONSULTA COMPLETA DE WORKITEM"
		println XmlUtil.serialize(node)
		println "========================================="
		println "Consulta a RTC realizada en $millis mseg."
	}
	
	@Test
	public void testProjectAreas() {
		def node = null;
		long millis = Stopwatch.watch {
			String ret = null;
			ret = client.get("process/project-areas")
			Assert.assertEquals(200, client.getLastHttpStatus())
			
			// 1803
			//ret = client.get("rpt/repository/workitem?fields=workitem/workItem[id=13122]/(id|summary|type/name)")
			//ret = client.get("rpt/repository/workitem", 
			//	["fields":"workitem/workItem[id=1803]/(id|summary|type/name)"])
			
			//ret = client.get("rpt/repository/workitem", 
			//	["fields":"workitem/workItem[id=1803]/(id|summary|state/name|type/name)"])
			
			//ret = client.get("rpt/repository/foundation", 
			//	["fields":"foundation/teamArea/(name|teamMembers/userId|teamMembers/name)"])
			
			//ret = client.get("rpt/repository/foundation", 
			//	["fields":"foundation/projectArea/(name|allTeamAreas/teamMembers/userId|teamMembers/userId)"])
			
			node = new XmlParser().parseText(ret)
		}
		println "========================================="
		println "CONSULTA DE PROJECT AREAS"
		println XmlUtil.serialize(node)
		println "========================================="
		println "Consulta a RTC realizada en $millis mseg."
	}
}
