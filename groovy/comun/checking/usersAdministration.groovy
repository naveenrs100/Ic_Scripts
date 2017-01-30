import es.eci.utils.Stopwatch;
import groovy.net.xmlrpc.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue

/**
 * Este script actualiza los permisos sobre modelos de 
 * todos los usuarios de la base de datos de checking
 * 
 * argumentos
 * 0 -> urlChecking
 * 1 -> usuarioChecking
 * 2 -> pwdChecking
 * 
 */

def checkingServer = args[0]
def usrChecking = args[1]
def pwdChecking = args[2]

// Definición de las reglas para los modelos

def userRules = 'models:+/J2EE|+/J2SE|+/COBOL|+/ATG|+/C|+/CPP'


// procy para usuarios y reglas

def proxyLogin = new XMLRPCServerProxy(checkingServer+ "/xmlrpc/login");
def proxyUsers  = new XMLRPCServerProxy(checkingServer+ "/xmlrpc/userRole");

// cadena de login 

String login = proxyLogin.login.login(usrChecking, pwdChecking);
println login

// Blocking Queue para los usuarios
BlockingQueue<String> queue = new LinkedBlockingQueue<String>();

// numero de ejecutores
final int NUMBER_OF_EXECUTORS=5;

// clase worker 

class CheckingTask implements Runnable {
	private CountDownLatch latch;
	private XMLRPCServerProxy proxyUsers;
	private String userRules;
	private LinkedBlockingQueue user;
	private String login="";

	public CheckingTask(CountDownLatch latch,XMLRPCServerProxy proxyUsers,LinkedBlockingQueue userqueue,String login,String userRules) {
		this.latch = latch;
		this.proxyUsers = proxyUsers;
		this.user=userqueue;
		this.login=login;
		this.userRules=userRules;
	}

	
	// se lanza el update y se decrementa el contador del CountDown
	public void run() {

		proxyUsers.userRole.updateUser(login, user.take(), 
			['rules':userRules, 'password':'12345678'])
		latch.countDown();
	}
}


try {
	
	println("reading user list")
	def userlist=null

	long millis = Stopwatch.watch {
		userlist=proxyUsers.userRole.getUsers(login)
	}

	println("user list read !!! -> ${millis} msec.")

	def actualUsers = [];
	
	// insertamos en la cola todo los usuarios
	userlist.each {
		// excluir los administradores
		if (!it.roles.contains("ROLE_ADMIN")) {
			queue.put("${it.user}");
			actualUsers << it.user;
		}
		else {
			println "Excluido ${it.user}"
		}
	}

	// lanzamos todas las tareas en el threadpool
	long millis2 = Stopwatch.watch {

		// contador de usuarios actualizados
		CountDownLatch latch = new CountDownLatch(actualUsers.size());

		// seleccionamos el numero de ejecutores correspondiente al numero de hilos arrancados
		ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_EXECUTORS);
		
		// por cada usuario creamos una tarea 
		actualUsers.each {
			String user = it;
			executor.execute(new CheckingTask(latch,proxyUsers,queue,login,userRules));
		}

		// avisamos que hemos acabado de crear tareas y esperamos que termine el trabajo
		executor.shutdown();
		latch.await();

	}

	println("done !!! -> ${millis2} msec total time")
}
finally {
	println proxyLogin.login.logoff(login);
}