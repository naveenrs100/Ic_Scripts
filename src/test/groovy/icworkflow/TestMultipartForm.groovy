package icworkflow
import org.apache.http.message.BasicHeader;
import org.junit.Test;

import es.eci.utils.MultipartUtility;


class TestMultipartForm {

	//@Test
	public void testMultipartUtility() {
		String url = "http://portalgrupo.elcorteingles.pre/icqaportal/executions/steps?quvetoken={\"sessionKey\":\"055ef85e-5480-477a-bc97-cf36d8256ab0\"}"
		File json = new File("c:/temp/20160614/file.json");
		File zip = new File("c:/temp/20160614/file.zip");
		MultipartUtility utility = new MultipartUtility(url, "UTF-8");
		//utility.addFormField("updateCommand", json.text);
		utility.addFormField("updateCommand", "adfasdfsdf");
		utility.finish();
	}

	//@Test
	public void testMultipartForm() {

		String url = "http://portalgrupo.elcorteingles.pre/icqaportal/executions/steps?quvetoken={\"sessionKey\":\"055ef85e-5480-477a-bc97-cf36d8256ab0\"}"
		//String url = "http://portalgrupo.elcorteingles.pre/icqaportal/executions/"
		File json = new File("c:/temp/20160614/file.json");
		File zip = new File("c:/temp/20160614/file.zip");

		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.setRequestMethod("POST");

		// Cabeceras
		con.setDoOutput(true);

		//String encoded = "Og=="
		//con.setRequestProperty("Authorization", "Basic "+encoded);
		def boundary = Long.toHexString(System.currentTimeMillis());
		con.setRequestProperty("Content-Type",
                "multipart/form-data; boundary=" + boundary);
		con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.157 Safari/537.36");
		
		con.setRequestProperty("Accept", "application/json");
		Writer wr = new PrintWriter(new OutputStreamWriter(con.getOutputStream(), "UTF-8"),
                true);
		def LINE_FEED = "\r\n";
		def write = { String str ->
			println str;
			wr.write(str);
			wr.write(LINE_FEED)
		}
		write("--$boundary")
		
		write("Content-Disposition: form-data; name=\"updateCommand\"")
		//write("Content-Type: text/plain; charset=utf-8")
		write("")
		write(json.text)
		//write("aaa")
		wr.flush();

		// file -> contenido binario del zip
		write("--${boundary}")
		write("Content-Disposition: form-data; name=\"file\"; filename=\"${zip.name}\"")
		write("Content-Type: application/zip, application/octet-stream")
		write("Content-Transfer-Encoding: binary")
		write("")
		wr.flush();
		//wr.write(zip.getBytes())
		con.getOutputStream().write(zip.getBytes())
		con.getOutputStream().flush();
		

		// Final
		write("")
		write("--${boundary}--")

		wr.flush();
		wr.close();
		// Código de retorno
		int responseCode = con.getResponseCode();
		println "\nRespuesta HTTP -> $responseCode"
		// Recoger la respuesta
		BufferedReader input = new BufferedReader(
				new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuilder response = new StringBuilder();

		while ((inputLine = input.readLine()) != null) {
			response.append(inputLine);
		}
		input.close();

		println response.toString();

		if (responseCode != 200) {
			throw new Exception("Server Error: ${responseCode}")
		}
	}
}
