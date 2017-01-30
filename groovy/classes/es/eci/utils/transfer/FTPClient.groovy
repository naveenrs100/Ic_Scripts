package es.eci.utils.transfer

import es.eci.utils.Stopwatch
import es.eci.utils.StreamGobbler
import es.eci.utils.TmpDir
import es.eci.utils.base.Loggable


/**
 * Esta clase implementa un cliente FTP muy sencillo, apoyándose en la línea 
 * de comandos.  Se implementa la funcionalidad tanto para Windows como para
 * Unix/Linux.  La estrategia es escribir un script shell con el formato
 * adecuado al SO host y lanzarlo a través de la utilidad ftp nativa.
 */
class FTPClient extends Loggable {
	
	//-----------------------------------------------------------------
	// Propiedades del cliente
	
	// Información de conexión
	private String user
	private String password
	private String address
	// Buffer para construir el script
	private StringBuilder sb
	
	// Lista de pares File, String ruta de destino
	private def copies = []
	
	// Recuento del tráfico acumulado
	private long bytes = 0
	
	//-----------------------------------------------------------------
	// Métodos del cliente
	
	/** 
	 * Construye el cliente FTP con la información necesaria para la conexión.
	 * @param user Usuario FTP de la máquina remota.
	 * @param password Contraseña del usuario FTP en la máquina remota.
	 * @param address URL de la máquina remota.
	 */
	public FTPClient(String user, String password, String address) {
		this.user = user
		this.password = password
		this.address = address
		
		sb = new StringBuilder()		
	}
	
	/**
	 * Copia un fichero a la ruta remota indicada.  Esta copia se acumula pero
	 * no se realiza físicamente (ni se establece conexión alguna) hasta que
	 * se dispara el método flush().
	 * @param f Fichero en la máquina local.
	 * @param path Ruta de destino en la máquina remota.
	 */
	public void copy(File f, String path) {
		copies << [ f, path ]
		bytes += f.size()
	}
	
	/**
	 * Lanza el comando acumulado, conectando con la máquina destino.
	 */
	public void flush() {
		if (copies.size() > 0) {
			// Establecer la conexión
			def crlf = System.getProperty("line.separator")
			// closure que escribe una línea al fichero
			def write = { cadena ->
				sb.append(cadena)
				sb.append(crlf)
			}
			
			TmpDir.tmp { File dir ->
				File f = new File(dir.getCanonicalPath() + System.getProperty("file.separator") + "script")
				def comando = []
				boolean win32 = System.properties['os.name'].toLowerCase().contains('windows') 
				if (win32) {
					comando = ["cmd", "/C", "ftp", "-s:${f.canonicalPath}", "-in", address]
				}
				else {
					comando = ["sh", f.getCanonicalPath()]
					write("ftp -in $address<<FIN_FTP")
				}
				write("user $user $password")
				write("binary")
				// Añadir las copias
				copies.each { copia ->
					File file = (File) copia[0]
					File parent = file.getParentFile()
					String ruta = (String) copia[1]
					String[] componentesRuta = ruta.split("/")
					StringBuilder sb = new StringBuilder()
					for (int i = 0; i < componentesRuta.length; i++) {
						if (componentesRuta[i].trim().compareTo("") != 0) {
							sb.append("/") 
							sb.append(componentesRuta[i])
							write("mkdir \"${sb}\"")
						}
					}
					write("lcd \"${parent.canonicalPath}\"")
					write("cd \"$ruta\"")
					write("put \"${file.name}\"")
				}
				write("bye")
				if (!win32) {
					write("FIN_FTP")
				}
				f.text = sb.toString()
				def KILOBYTE = 1024
				long kbytes = (bytes / KILOBYTE)
				log("Conectando a $address para escribir ${copies.size()} ficheros (${kbytes} kb)...")
				long time = Stopwatch.watch {
					Process p = comando.execute(null, dir)
					StreamGobbler cout = new StreamGobbler(p.getInputStream(), true)
					StreamGobbler cerr = new StreamGobbler(p.getErrorStream(), true)
					cout.start()
					cerr.start()
					p.waitFor()
					log cerr.getOut()
				}
				long kbps = kbytes / (time / 1000)
				log("Copia completada en $time mseg. -> Velocidad estimada: ${kbps} kbps")				
			}
			// Reseteo de los comandos
			copies = []
		}
	}
}