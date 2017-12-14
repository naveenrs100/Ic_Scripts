File brokerComposFile = new File("C:/Users/dcastro.jimenez/Desktop/brokerComponents.txt");
File brokerComposFileConfigs = new File("C:/Users/dcastro.jimenez/Desktop/brokerComponentsConfigs.txt");
brokerComposFileConfigs.createNewFile();

brokerComposFile.eachLine { String line ->
	if(line.endsWith(" - Config")) {
		brokerComposFileConfigs.append("${line}\n");
	}	
}