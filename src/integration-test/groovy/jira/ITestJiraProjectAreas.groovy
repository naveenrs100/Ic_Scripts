package jira

import atlassian.AtlassianClient;
import base.BaseTest
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import org.junit.Test
import org.junit.Assert

class ITestJiraProjectAreas extends BaseTest {
	
	@Test
	public void listProjectRepos() {
		AtlassianClient bbClient = new AtlassianClient(urlAtlassian, atlassianKeystoreVersion, atlassianUser, atlassianPass)
		bbClient.initLogger({println it});
		
		String projectName = "GrupoPruebaRelease";
		
		HashMap<String,String> params = new HashMap<String, String>();
		params.put("name",projectName);
		
		String response = bbClient.get("projects", "1.0", params);
		def responseJson = new JsonSlurper().parseText(response);
		
		String key = responseJson.values[0].get("key")
		
		String reposResponse = bbClient.get("projects/${key}/repos", "1.0", null);
		def reposJson = new JsonSlurper().parseText(reposResponse);
		
		ArrayList<String> listaRepos = new ArrayList<String>();
		
		reposJson.values.each { value ->
			println value.name;
			listaRepos.add(value.name);			
		}
		
		ArrayList<String> referenceList = new ArrayList<String>();
		referenceList.add("PruebaRelease-App-1");
		referenceList.add("PruebaRelease-App-2");
		referenceList.add("PruebaRelease-Biblioteca-1");
		referenceList.add("PruebaRelease-Biblioteca-2");
		
		Assert.assertEquals(listaRepos, referenceList);		
		
	}
	
}


