package git

import org.junit.Assert
import org.junit.Test

import atlassian.AtlassianClient;
import base.BaseTest
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

class ITestBitBucketRepos extends BaseTest {
	
	@Test
	public void testRepos() {
		AtlassianClient bbClient = 
			new AtlassianClient(
					urlBitbucket, 
					AtlassianClient.BITBUCKET_CERTIFICATE,
					bitbucketKeystoreVersion, 
					nexusURL,
					bitbucketUser, 
					bitbucketPass);
		bbClient.initLogger({println it})
		
		String projectName = "GrupoPruebaRelease";
		
		HashMap<String,String> params = new HashMap<String, String>();
		params.put("name",projectName);
		
		String response = bbClient.get("projects", "1.0", params);
		println JsonOutput.prettyPrint(response);
		
		def responseJson = new JsonSlurper().parseText(response);
				
		String key = responseJson.values[0].get("key");
		String name = responseJson.values[0].get("name");
		
		Assert.assertEquals("PROYEC", key);
		Assert.assertEquals("GrupoPruebaRelease", name);
		
		
	}
	
	@Test
	public void listProjectRepos() {
		AtlassianClient atlClient = 
			new AtlassianClient(
				urlBitbucket, 
				AtlassianClient.BITBUCKET_CERTIFICATE,
				bitbucketKeystoreVersion, 
				nexusURL,
				bitbucketUser, 
				bitbucketPass);
		atlClient.initLogger({println it});
		
		String projectName = "GrupoPruebaRelease";
		
		HashMap<String,String> params = new HashMap<String, String>();
		params.put("name",projectName);
		
		String response = atlClient.get("projects", "1.0", params);
		def responseJson = new JsonSlurper().parseText(response);
		
		String key = responseJson.values[0].get("key")
		
		String reposResponse = atlClient.get("projects/${key}/repos", "1.0", null);
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


