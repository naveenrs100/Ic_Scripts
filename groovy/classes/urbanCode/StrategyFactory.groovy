package urbanCode

/**
 * Esta factoría debe discernir la estrategia necesaria a aplicar en la generación
 * de la instantánea de Urban Code.
 */
class StrategyFactory {

	//----------------------------------------------------------------------------
	// Métodos de la clase
	
	// No queremos que se instancie la factoría estática
	private StrategyFactory() {}
	
	/**
	 * Obtiene la estrategia adecuada a la construcción del componente
	 * @param folder Directorio que contiene el código fuente
	 * @param trivial Si es cierto, fuerza la devolución de una estrategia trivial
	 * @return Objeto estrategia apropiado para componer el descriptor de
	 * instantánea según se decida consultando el código.
	 */
	public static Strategy getStrategy(File folder, Boolean trivial) {
		Strategy estrategia = new StrategyTrivial(folder);
		if (!trivial) {
			// **********************Validar tipo de estrategia a aplicar e instanciar la estrategia que corresponda******************
			// Si el fichero base contiene algún fichero pom.xml aplica estrategia JAVA, si encuentra build.gradle aplica estrategia gradle
			if (new File(folder.getCanonicalPath() + System.getProperty("file.separator") + "pom.xml").exists()){
				//Se instancia la estrategia JAVA
				System.out.println("Estrategia JAVA");
				estrategia = new StrategyJava();
			}else {
				def isGradle = { 
					folder.eachFileRecurse {
						System.out.println(it) 
						if (it.toString().endsWith("build.gradle"))
							return true
					}
					return false
				}
				
				if (isGradle){
					//Se instancia la estrategia REGISTRO UNICO (De momento la estrategia con gradle solo se aplica para REGISTRO UNICO)
					System.out.println("Estrategia REGISTRO UNICO");
					estrategia = new StrategyRegistroUnico();
				}else{
					System.out.println("No se ha encontrado ninguna estrategia válida");
				}
			}	
		}
		return estrategia;
	}
}
