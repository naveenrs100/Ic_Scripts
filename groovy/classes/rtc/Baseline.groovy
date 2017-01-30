package rtc

class Baseline {
	public String name
	public String number
	public String identifier
	public String description
	public String user
	public String month
	public String day
	public String year
	public String hour
	public String typeHour
	
	@Override
	public String toString() {
		def value
		if (name != null)
			value = this.number + "" +  this.identifier + this.name + this.description + this.user
		else
			value = this.number + "" +  this.identifier + this.description + this.user
		return value
	}
}