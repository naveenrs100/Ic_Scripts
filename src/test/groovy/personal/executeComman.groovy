//def jenkins_int_file = "C:/Users/dcastro.jimenez/Desktop/Jobs a Modificar/jenkins_int.json";
def jobstxt_file = "C:/Users/dcastro.jimenez/Desktop/Jobs a Modificar/plantillasRTC.txt";

def json_int = new File("C:/OpenDevECI/WSECI_NEON/DIC - Scripts/src/test/groovy/personal/jsontest.json").text;

def jenkins_cli = "C:/Users/dcastro.jimenez/git/jenkins-cli/bin/jenkins-cli.js";

def jenkins_int_file = new File("jenkins_int.json")
jenkins_int_file.text = json_int;


//def command = "\"C:/Program Files/nodejs/node\" \"" + jenkins_cli + "\" -c \"jenkins_int.json\" -f \""+ jobstxt_file + "\" backup -j";
def command = "\"C:/Program Files/nodejs/node\" \"" + jenkins_cli + "\" -c \"jenkins_int.json\" list -j";
println command;

Process p = Runtime.getRuntime().exec(command);
p.waitFor();
//System.out.println(p.getText());

