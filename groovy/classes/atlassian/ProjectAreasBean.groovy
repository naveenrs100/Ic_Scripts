package atlassian

import java.util.ArrayList;

class ProjectAreasBean {
	
	HashMap<String, ProjectAreaBean> projectAreas = new HashMap<>()

	/**
	 * @return the projectAreas
	 */
	public HashMap<String, ProjectAreaBean> getProjectAreas() {
		return projectAreas;
	}

	/**
	 * @param projectAreas the projectAreas to set
	 */
	public void setProjectAreas(HashMap<String, ProjectAreaBean> projectAreas) {
		this.projectAreas = projectAreas;
	}


}
