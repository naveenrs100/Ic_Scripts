package ssh

import java.security.KeyStore

import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory

import org.apache.http.client.CredentialsProvider
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClientBuilder

import es.eci.utils.NexusHelper
import es.eci.utils.TmpDir
import es.eci.utils.ZipHelper
import es.eci.utils.base.Loggable
import es.eci.utils.pom.MavenCoordinates

/**
 * Esta clase agrupa funcionalidad para permitir el acceso http a servicios
 * REST seguros.  En particular, permite configurar un cliente http de apache
 * para acceder a una dirección https partiendo de las coordenadas maven de un
 * keystore presente en el nexus corporativo.
 */
class SecureRESTClientHelper extends Loggable {

	//-------------------------------------------------------------
	// Propiedades de la clase
	
	/** URL de Nexus: preferentemente, la del repo public */
	private String urlNexus = "http://nexus.elcorteingles.int/content/groups/public/";
	// Cacheo del keystore
	private byte[] binaryCache = null;
	
	//-------------------------------------------------------------
	// Métodos de la clase
	
	/**
	 * Aplica el keystore indicado para devolver un cliente HTTPS de apache.
	 * @param keystoreInfo Información del keystore (fichero, password)
	 * @param keystoreCoords Coordenadas maven del keystore
	 * @param provider Credenciales BASIC
	 * @return Cliente HTTP inicializado adecuadamente para atacar la url HTTPS
	 * con el keystore correcto
	 */
	public HttpClient createSecureHttpClient(
			KeystoreInformation keystoreInfo,
			MavenCoordinates keystore,
			CredentialsProvider provider = null) {
		HttpClientBuilder builder = HttpClientBuilder.create();
		if (provider != null) {
			builder = builder.setDefaultCredentialsProvider(provider);
		}
		return builder.setSslcontext(getContext(keystoreInfo, keystore)).build();
	}
	
	/**
	 * Si necesitamos acceder a un https, solicitamos un contexto SSL.
	 * @param keystoreInfo Información del keystore (fichero, password)
	 * @param keystoreCoords Coordenadas maven del keystore
	 * @return Objeto inicializado con el contexto SSL (certificados).
	 */
	private SSLContext getContext(
			KeystoreInformation keystoreInfo, 
			MavenCoordinates keystoreCoords) {
		SSLContext theContext = null;
		InputStream is = null;
		KeyStore keystore  = KeyStore.getInstance("jks");
		char[] pwd = keystoreInfo.getPassword().toCharArray();
		// Guarda cacheado el keystore si le es posible
		if (binaryCache == null) {
			TmpDir.tmp { File dir ->
				log "Descargando el keystore..."
				// Si no lo tenía cacheado, lo saca de Nexus
				NexusHelper helper = new NexusHelper(urlNexus);
				helper.initLogger(this);
				helper.download(keystoreCoords,	dir);
				File[] files = dir.listFiles();
				files.each { File file ->
					if (file.name.toLowerCase().endsWith(".zip")) {
						log "Descomprimiendo el keystore..."
						ZipHelper.unzipFile(file, dir);
					}
				}
				File keystoreFile = new File(dir, keystoreInfo.getFilename());	
				log "Cargando el keystore $keystoreFile..."			
				binaryCache = keystoreFile.getBytes();
			}
		}
		is = new ByteArrayInputStream(binaryCache);
		try {
			keystore.load(is, pwd);
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(
			TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(keystore);
			TrustManager[] tm = tmf.getTrustManagers();
			
			KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(
					KeyManagerFactory.getDefaultAlgorithm());
			kmfactory.init(keystore, pwd);
			KeyManager[] km = kmfactory.getKeyManagers();
			
			theContext = SSLContext.getInstance("TLS");
			theContext.init(km, tm, null);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			is.close();
		}
		return theContext;
	}

	/** 
	 * Informa una determinada URL de Nexus
	 * @param urlNexus URL de Nexus para utilizar en lugar de la de por defecto
	 */
	public void setUrlNexus(String urlNexus) {
		this.urlNexus = urlNexus;
	}
	
	
	
}
