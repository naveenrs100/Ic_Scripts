import es.eci.utils.Retries;

Retries.retry(10, 250, {
	println("Comenzamos:")
	Random random = new Random();
	def randomNumber = random.nextInt(2)	
	println(randomNumber)
	println(randomNumber % 2)
	if((randomNumber % 2) == 0) {
		throw new Exception("Exception");
	}
});