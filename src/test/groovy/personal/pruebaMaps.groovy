String valor = "MELME.MELMELADA - DESARROLLO- melme -.kk"

println normalize(valor);

private String normalize(String value) {
	value.replaceAll(" - ", "_").replaceAll(" -","_").replaceAll("- ","_").replaceAll("\\.","_").replaceAll(" ","_");
}
