import es.eci.utils.Retries;

def closure = { int vez ->
	println("El melme $vez")
	if(vez < 2) {
		throw new Exception("peto");
	}
}

println("empezamos:")

Retries.retry(5, 200, closure);