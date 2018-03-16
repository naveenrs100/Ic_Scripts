package testing

import es.eci.utils.SystemPropertyBuilder;

SystemPropertyBuilder b = new SystemPropertyBuilder();
Map params = b.getSystemParameters()

println params['saludo']
//println params['despedida']