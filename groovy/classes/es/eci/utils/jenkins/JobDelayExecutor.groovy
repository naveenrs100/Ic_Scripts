package es.eci.utils.jenkins

import es.eci.utils.StringUtil

import javax.xml.bind.DatatypeConverter

/**
 * Clase de utilidad para lanzar procesos de Jenkins planificados en el tiempo.
 */
class JobDelayExecutor {
	private String url
	private String delay
	private String user
	private String password

	private Map params

	private Integer startHour
	private Integer startMinute
	private Integer totalSeconds
	
	/**
	 * Constructor.
	 * @param urlValue URL del api rest para llamar a la ejecución del proceso en Jenkins.
	 * @param delayValue Hora y minuto en que se ejecutará el proceso (Formato HH:MM).
	 * @param userValue Usuario con permisos de ejecución del proceso en Jenkins.
	 * @param passwordValue Contraseña del usuario que ejecutará el proceso en Jenkins.
	 * @param params Parámetros adicionales que se pasarán si el proceso llamado en Jenkins es parametrizado. 
	 */
	JobDelayExecutor(
			String urlValue, 
			String delayValue, 
			String userValue, 
			String passwordValue, 
			Map params) {
		url = urlValue
		delay = delayValue
		user = userValue
		password = new String(passwordValue.toCharArray())
		this.params = params
		fetchStartTime()
		totalSeconds = getSeconds()
	}
	
	/**
	 * Método que lanza la petición http.
	 */
	void sentRequest() {
		InputStream response = null
		OutputStream request = null

		HttpURLConnection connection = null

		String charset = java.nio.charset.StandardCharsets.ISO_8859_1.name()

		try {
			String authStr = DatatypeConverter.printBase64Binary("${user}:${password}".getBytes(charset))
		
			connection = (HttpURLConnection) new URL(url).openConnection()
			connection.setDoOutput(true)
			connection.setRequestMethod('POST')
			connection.setRequestProperty('Content-type', "application/x-www-form-urlencoded;charset=${charset}")
			connection.setRequestProperty('Authorization', "Basic ${authStr}")
			
			connection.connect()

			StringBuilder stb = new StringBuilder()
			stb.append("&delay=${totalSeconds}sec")
			
			if (params != null) {
				params.keySet().each {key ->
					stb.append("&${key}=${URLEncoder.encode(params[key], charset)}")
				}
			}

			String query = stb.toString()

			println("Parámetros de entrada para el proceso llamados: ${query}")

			request = connection.getOutputStream()
			request.write(query.getBytes(charset))
			request.flush()

			println("Código de respuesta: ${connection.getResponseCode()}")
			println("Respuesta del servidor: ${connection.getResponseMessage()}")
		} catch (MalformedURLException malformedURLException) {
			malformedURLException.printStackTrace()
		} catch (ProtocolException protocolException) {
			protocolException.printStackTrace()
		} catch (IOException ioException) {
			ioException.printStackTrace()
		} finally {
			try {
				request.close()
			} catch (IOException ioException) {
				ioException.printStackTrace()
			}
		}
	}
	
	/**
	 * Captura el tiempo actual del sistema
	 */
	private void fetchStartTime() {
		Calendar localTime = Calendar.getInstance()
		localTime.setTime(new Date())

		startHour = Integer.valueOf(localTime.get(Calendar.HOUR_OF_DAY))
		startMinute = Integer.valueOf(localTime.get(Calendar.MINUTE))
	}

	/**
	 * Devuelve la diferencia en segundos entre el tiempo actual del sistema y el tiempo programado para lanzar el proceso de Jenkins.
	 * El formato aceptado para el tiempo programado es HH:MM (24 horas).
	 * 
	 * @return Tiempo en segundos a contar desde la hora actual hasta el lanzamiento.
	 */
	private Integer getSeconds() {
		// Si el parámetro "delay" llega nulo, llega como $delay, o vacío, devuelve 0 segundos
		if (StringUtil.isNull(delay) || delay.startsWith('$') || delay.equals('')) return Integer.valueOf(0)
		
		Integer finalHour = 0
		Integer finalMinute = 0

		Integer totalHours = 0
		Integer totalMinutes = 0

		def timeFormat = /[0-2][0-9]:[0-5][0-9]/

		def result = delay ==~ timeFormat
		println "¿Formato correcto? ${result}"
		
		if (!result) throw new Exception('ERROR - El formato no es correcto')
        
		def arrayData = delay.split(':')
		finalHour = Integer.valueOf(arrayData[0])
		finalMinute = Integer.valueOf(arrayData[1])

		println "Hora de inicio para el cálculo: H ${startHour} M ${startMinute}"
		println "Hora de lanzamiento del proceso: H ${finalHour} M ${finalMinute}"
		
		if (finalHour > 23) throw new Exception('ERROR - La hora programada no puede ser mayor que 23')
		
		if (startHour == finalHour) {
			// Si la hora actual del sistema y la hora programada coinciden, no se permite que el minuto actual del sistema sea mayor que el minuto programado
			if (startMinute > finalMinute) {
				throw new Exception('ERROR - La hora programada es igual a la hora actual del sistema pero el minuto de lanzamiento ya ha pasado')
			} else {
				totalMinutes = finalMinute - startMinute
			}
		} else {
			if (startMinute > 0) {
				totalMinutes += 60 - startMinute
		
				if (startHour == 23) {
					startHour = 0
				} else {
					startHour++
				}
			}
			
			if (finalMinute > 0) totalMinutes += finalMinute
			
			if (startHour > finalHour) {
				totalHours = (24 - startHour) + finalHour
			} else {
				totalHours = finalHour - startHour
			}
			
			def tempHours = (totalMinutes / 60).toBigInteger()
			def tempMinutes = totalMinutes % 60
		
			if (tempHours >= 1) {
				totalHours += tempHours
				totalMinutes -= (tempHours * 60)
			}
		}
		
		println "Total horas: ${totalHours}"
		println "Total minutos: ${totalMinutes}"
		
		println "Total segundos: ${((totalMinutes * 60) + ((totalHours * 60) * 60))} sec"
		
		return ((totalMinutes * 60) + ((totalHours * 60) * 60))
	}
}
