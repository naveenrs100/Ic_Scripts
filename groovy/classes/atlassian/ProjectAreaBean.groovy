package atlassian

class ProjectAreaBean {
	
	private String id;
	private String name;
	
	HashMap<String, UsersBean> users = new HashMap<>()

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the users
	 */
	public HashMap<String, UsersBean> getUsers() {
		return users;
	}

	/**
	 * @param users the users to set
	 */
	public void setUsers(HashMap<String, UsersBean> users) {
		this.users = users;
	}


}
