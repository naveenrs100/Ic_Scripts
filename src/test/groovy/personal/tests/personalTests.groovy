List<String> array = ["A","B","C","D"];

def f = new File("myFile.txt")
def ls = System.getProperty("line.separator");

for(int i = 0; i < array.size(); i++) {
	if(i != (array.size() -1)) {
		f.append(array[i] + ls)
	} else {
		f.append(array[i])
	}
}

