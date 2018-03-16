package jira

import groovy.json.JsonOutput;

import java.util.Iterator;
import java.util.prefs.Base64;

import atlassian.AtlassianClient;
import base.BaseTest

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject
import com.google.gson.JsonParser;

import org.junit.Test
import org.junit.Assert

class ITestJiraProjectAreas extends BaseTest {
	
	@Test
	public void listProjectRepos() {
		AtlassianClient bbClient = 
			new AtlassianClient(
				urlJira, 
				AtlassianClient.JIRA_CERTIFICATE,
				jiraKeystoreVersion,
				nexusURL, 
				jiraUser, 
				jiraPass)
		
		String responseProjects = bbClient.get("project", "2")
		JsonArray mainJsonProjects = new JsonParser().parse(responseProjects).getAsJsonArray()
		
		Iterator<JsonElement> projectElement = mainJsonProjects.iterator()
		while (projectElement.hasNext()) {
			JsonObject projectJson = (JsonObject) projectElement.next()
			System.out.println("Project id: " + projectJson.get("id").getAsString() )
			
			// Obtener esquema de permisos por id de proyecto.
			String responseScheme = bbClient.get("project/" + projectJson.get("id").getAsString() +
				"/permissionscheme", "2")
			JsonObject schemeObject = new JsonParser().parse(responseScheme).getAsJsonObject()
			System.out.println(" --> Scheme id: " + schemeObject.get("id").getAsString() )
			// ---
			// Obtener esquema de permisos
			HashMap<String,String> paramsScheme = new HashMap<String, String>()
			paramsScheme.put("expand", "all")
			String responsePermissions = bbClient.get("permissionscheme/" + schemeObject.get("id").getAsString(),
				 "2", paramsScheme)
			JsonObject mainSchemePermissions = new JsonParser().parse(responsePermissions).getAsJsonObject()
			
			JsonArray mainSchemePermArray = mainSchemePermissions.get("permissions").getAsJsonArray()
			
			Iterator<JsonElement> schemePermElement = mainSchemePermArray.iterator()
			while (schemePermElement.hasNext()) {
				JsonObject schemePermJson = (JsonObject) schemePermElement.next()
				JsonObject holderJson = schemePermJson.get("holder")
				
				if ( holderJson.get("type").getAsString().equals("user") ) {
					JsonObject userJson = holderJson.get("user")
					System.out.println("    --> User id: " + userJson.get("name").getAsString())
					System.out.println("    --> User email: " + userJson.get("emailAddress").getAsString())
					System.out.println("    --> User name: " + userJson.get("displayName").getAsString())
				}
				
				// Obtener grupo y esquema de grupo
				if ( holderJson.get("type").getAsString().equals("group") ) {
					JsonObject groupJson = holderJson.get("group")
					System.out.println("    --> Group name: " + groupJson.get("name").getAsString())
					
					HashMap<String,String> paramsGroup = new HashMap<String, String>()
					paramsGroup.put("groupname", groupJson.get("name").getAsString())
					paramsGroup.put("expand", "users")
					String responseGroup = bbClient.get("group", "2", paramsGroup)
					
					JsonObject mainGroup = new JsonParser().parse(responseGroup).getAsJsonObject()
					JsonObject usersGroup = mainGroup.get("users").getAsJsonObject()
					JsonArray usersGroupArray = usersGroup.get("items").getAsJsonArray()
					
					Iterator<JsonElement> usersGroupElement = usersGroupArray.iterator()
					while (usersGroupElement.hasNext()) {
						JsonObject userJsonFromGroup = (JsonObject) usersGroupElement.next()
						System.out.println("       --> User id: " + userJsonFromGroup.get("name").getAsString())
						System.out.println("       --> User email: " + userJsonFromGroup.get("emailAddress").getAsString())
						System.out.println("       --> User name: " + userJsonFromGroup.get("displayName").getAsString())						
					}
				}
				
			}
			
			//Gson gson = new GsonBuilder().setPrettyPrinting().create()
			//System.out.println(gson.toJson(responseUsers))
			
			System.out.println("Project name: " + projectJson.get("name").getAsString() )
			System.out.println("-------------------------------------------------------")
		}
		
		// Pretty print
		// Gson gson = new GsonBuilder().setPrettyPrinting().create()			
		// System.out.println(gson.toJson(mainJson))
		
	}
	
}
