package es.eci.utils

class StringUtil {

	def static cleanHTML(cadena){
		if (cadena!=null){
			cadena = cadena.replaceAll("<","&lt;")
			cadena = cadena.replaceAll(">","&gt;")
			cadena = cadena.replaceAll("&","&amp;")
			cadena = cadena.replaceAll("\"","&quot;")
		}
		return cadena
	}

	def static cleanBlank(cadena){
		if (cadena!=null){
			cadena = cadena.replaceAll(" - ","_")
			cadena = cadena.replaceAll(" ","_")
			cadena = cadena.replaceAll("/","-")
		}
		return cadena
	}

	def static clean(cadena){
		if (cadena!=null){
			cadena = cadena.replaceAll("/","-")
		}
		return cadena
	}
}