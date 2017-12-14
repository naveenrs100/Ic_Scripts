package jenkins

// Este script calcula un timestamp en el formato yyyyMMddHHmm y lo deja en una variable timestamp

import hudson.model.*
import jenkins.model.*
import java.beans.XMLDecoder;
import java.util.Date;
import java.text.DateFormat
import java.text.SimpleDateFormat

import es.eci.utils.ParamsHelper

String ret = null;

Date d = new Date()
DateFormat df = new SimpleDateFormat("yyyyMMddHHmm")

ret = df.format(d)


// Devolver el resultado
if (ret != null) {
	ParamsHelper.addParams(build, ["timestamp":ret])
}