package portal.interfase

import rtc.interfase.RTCPermissionsReader
import es.eci.utils.SystemPropertyBuilder

@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.4')

/**
 * Este script debe generar el fichero de permisos a partir del origen de datos.
 * En este caso el origen de datos es RTC.
 * 
 * Parámetros de entrada:
 * 
 *  nexusURL - URL de nexus corporativo
 *  keystoreVersion - Versión del keystore en Nexus, usado para lanzar servicios
 *  	contra endpoints https
 *  rtcURL - URL de RTC
 *  rtcUser - Usuario de RTC
 *  rtcPwd - Password del usuario en RTC
 *  parentWorkspace - Directorio de trabajo
 *  targetFile - [opcional] si no se informa, el fichero se llama permissions.json
 *  
 *  Ejemplo de fichero de salida:
{
  "projectAreas": {
    "id_project_area_1": {
      "id": "la primera área de proyecto",
      "name": "la primera área de proyecto",
      "users": {
        "123456": {
          "eciCode": "123456",
          "name": "foo bar",
          "email": null
        },
        "654321": {
          "eciCode": "654321",
          "name": "bar baz",
          "email": null
        }
      }
    },
    "la segunda área de proyecto": {
      "id": "id_project_area_2",
      "name": "la segunda área de proyecto",
      "users": {
        "123456": {
          "eciCode": "123456",
          "name": "foo bar",
          "email": null
        }
      }
    }
  }
}
 */

SystemPropertyBuilder parameterBuilder = new SystemPropertyBuilder();
RTCPermissionsReader reader = new RTCPermissionsReader();
reader.initLogger { println it }
parameterBuilder.populate(reader);

reader.execute();
