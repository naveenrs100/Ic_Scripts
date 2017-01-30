package es.eci.ic.version

class Version {
	public Version(groupId){
		this.groupId = groupId
	}
	def version 
	def groupId
	
	public String toString(){
		return "{version: ${version}, groupId: ${groupId}}"
	}
}
