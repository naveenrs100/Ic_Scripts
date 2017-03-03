package es.eci.utils

class Retries {
	
	/**
	 * Retries a Closure a number of times until it
	 * doesn't throw Exception. Devuelve un booleano
	 * que determina si se han agotado los intentos con error.
	 * @param int times Number of times to retry.
	 * @param int miliseconds Number of milliseconds to wait between each iteration.
	 * @param Closure c Closure to retry.
	 * @return boolean 
	 */
	static void retry(int times, int miliseconds, Closure c) {
		def goOn = true;
		for(int i=0; i < times; i++) {						
			try {
				if(goOn == true) {
					c(i);
					goOn = false;
				}
			} catch (Exception e) {
				goOn = true;
				Thread.sleep(miliseconds);
				if(i == (times -1)) {
					throw new Exception(e);
				}
			}
		}
	}
}
