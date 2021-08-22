package cc.banco;


import java.util.ArrayList;
import java.util.HashMap;
import es.upm.aedlib.indexedlist.ArrayIndexedList;
import es.upm.babel.cclib.*;

public class BancoMonitor implements Banco {
	private HashMap<String, Integer> cuentas; 			// Estructura de datos con las cuentas de los usuarios
	private Monitor mutex;								// Mutex
    private ArrayList<PetAler> monitSaldos;	// Monitor que maneja una bajada en los saldos
   

	ArrayIndexedList <PetTransferencia> transcola = new ArrayIndexedList<PetTransferencia>();	// Estructura de datos que almacena la lista de transferencias solicitadas

	public BancoMonitor() {		// Constructor de la clase
		/*   Inicializacion de datos   */
		cuentas = new HashMap<String, Integer>();
		mutex = new Monitor();
		monitSaldos = new ArrayList<PetAler>();


	}

	/**
	 * Clase que almacena los datos de la petición de la transferencia
	 * 
	*/
	public class PetTransferencia{
		public String origen;	// Cuenta de origen
		public String destino;	// Cuenta de destino
		public int valor;		// Valor de la transaccion
		public Monitor.Cond c;	// Condicion de la peticion

		public PetTransferencia(String origen, String destino, int valor){ //Constructoe
			/*			Inserccion de datos			*/
			this.origen=origen;
			this.destino=destino;
			this.valor=valor;
			/*			Inicializacion de variables			*/
			this.c = mutex.newCond();
		}
	}

	public class PetAler{
		public String cuenta;	// Cuenta de origen
		public int valor;		// Valor de la transaccion
		public Monitor.Cond c;	// Condicion de la peticion

		public PetAler(String cuenta, int valor){ //Constructoe
			/*			Inserccion de datos			*/
			this.cuenta=cuenta;
			this.valor=valor;
			/*			Inicializacion de variables			*/
			this.c = mutex.newCond();
		}
	}

	/** 
	 *	Se desbloquea en caso de que se haya realizado una transaccion 
	 *	y cumpla los requisitos para desbloquearse
	 *
	 *  @param c Cuenta a desbloquear
	 */
	/*private void desbloqueo_generico_saldos(String c) {
		if(monitSaldos.containsKey(c)){				// Comprobamos que la cuenta dada este en la estructura de condiciones de alertas de cuentas
			if(monitSaldos.get(c).waiting() > 0){	// Comprobamos que hay al menos un signal pendiente
				monitSaldos.get(c).signal();		// Realizamos el signal de la cuenta
			}
		}
	}*/

	/** 
	 *	Se desbloquea en caso de que se haya realizado una transaccion 
	 *	y cumpla los requisitos para desbloquearse
	 *
	 */
	private void desbloqueo_transferencias() { 
		Boolean hayAlerta = false;
		int p = monitSaldos.size();
		/*if(monitSaldos.containsKey(c)){				// Comprobamos que la cuenta dada este en la estructura de condiciones de alertas de cuentas
			if(monitSaldos.get(c).waiting() > 0){	// Comprobamos que hay al menos un signal pendiente
				monitSaldos.get(c).signal();		// Realizamos el signal de la cuenta
				return;
			}
		}*/
		for(int i = 0; i < p && !hayAlerta; i++){	// Recorremos la estructura hasta el final o el desbloqueo
			PetAler pet = monitSaldos.get(i);// Creamos una variable temporal que almacenara la peticion en esa iteracion 
				if(cuentas.containsKey(pet.cuenta)) {
					if(cuentas.get(pet.cuenta) < pet.valor){ 	// Si hay dinero suficiente se le hace un signal
						pet.c.signal();							// Realizamos el signal
						monitSaldos.remove(pet);					// Eliminamos la peticion de la estructura
						hayAlerta = true;						// Habilitamos la salida del bucle
					}
				}
		}
		if(hayAlerta) {
			return;
		}

		HashMap<String, Integer> cuentasLeidas = new HashMap<String, Integer>();
		int n = transcola.size();	// Tomamos el tamano de la estructura de las peticiones
		Boolean signaled = false;	// Creamos una variable de control de salida del bucle
		for(int i = 0; i < n && !signaled; i++){	// Recorremos la estructura hasta el final o el desbloqueo
			PetTransferencia pet = transcola.get(i);// Creamos una variable temporal que almacenara la peticion en esa iteracion 
			if(!cuentasLeidas.containsKey(pet.origen)) {
				if(cuentas.containsKey(pet.origen)&&cuentas.containsKey(pet.destino)) {
					if(cuentas.get(pet.origen) >= pet.valor){ 	// Si hay dinero suficiente se le hace un signal
						pet.c.signal();							// Realizamos el signal
						transcola.remove(pet);					// Eliminamos la peticion de la estructura
						signaled = true;						// Habilitamos la salida del bucle
					}
				}
				cuentasLeidas.put(pet.origen, null);
			}
		}
	}

	/**
	 * Un cajero pide que se ingrese una determinado valor v a una cuenta c. Si la
	 * cuenta no existe, se crea.
	 * 
	 * @param c nÃºmero de cuenta
	 * @param v valor a ingresar
	 */
	public void ingresar(String c, int v) {
		if (c != null || v>=0) { // Comprobar que los valores de entrada son correctos

			/******* Entrada en el mutex *******/
			mutex.enter(); // Entramos en el mutex
			if (!cuentas.containsKey(c)) { 				// Comprobamos si la cuenta solicitada falta en la estructura de cuentas
				cuentas.put(c, v); 						// Añadimos la cuenta a la estructura
			/*	if(!monitSaldos.containsKey(c)){		// Comprobamos si falta la cuenta en el monitor de alertas
					monitSaldos.put(c,mutex.newCond());	// Añadimos la condicion de alerta de la cuenta a la estructura
				}*/
			}else {									// Si la cuenta ya esta en la estructura
				cuentas.put(c, cuentas.get(c) + v); // Sumamos el valor del ingreso al saldo			 
			}
			
			/** Debloqueos */
			desbloqueo_transferencias();	// Se llama al desbloqueo de transferencias

			mutex.leave(); // Cerramos el mutex
		} 
	}

	/**
	 * Un ordenante pide que se transfiera un determinado valor v desde una cuenta o
	 * a otra cuenta d.
	 * 
	 * @param o nÃºmero de cuenta origen
	 * @param d nÃºmero de cuenta destino
	 * @param v valor a transferir
	 * @throws IllegalArgumentException si o y d son las mismas cuentas
	 *
	 */
	public void transferir(String o, String d, int v) throws IllegalArgumentException {
		if (o != null || d != null || v >= 0) {  // si existen las cuentas y los valores no son negativos

			if (o.equals(d)) { // si el origen y el destino son el mismo sale por excepcion
				throw new IllegalArgumentException("La cuenta origen y destino son las mismas\n");
			}
			/******* Entrada en el mutex *******/
			mutex.enter(); // se abre mutex

			if (!cuentas.containsKey(o) || (!cuentas.containsKey(d)) || cuentas.get(o) < v || hayPeticion(o)) {
				PetTransferencia req = new PetTransferencia(o, d, v);
				transcola.add(transcola.size(), req);
				req.c.await();
			}
			//desbloqueo_generico_saldos(o);		// se comprueba si algun saldo ha cambiado y puede hacer alertas
			cuentas.put(o, cuentas.get(o) - v); // se quita de la origen
			cuentas.put(d, cuentas.get(d) + v); // se da a la destino				
			desbloqueo_transferencias();
			mutex.leave(); 						// se cierra mutex
		}
	}
	
	private boolean hayPeticion(String cuenta) {
		Boolean signaled = false;
		int n = transcola.size();
		for(int i = 0; i < n && !signaled; i++){	// Recorremos la estructura hasta el final o el desbloqueo
			PetTransferencia pet = transcola.get(i);// Creamos una variable temporal que almacenara la peticion en esa iteracion 
			if(pet.origen.equals(cuenta)) {
				signaled = true;
			}
		}
			return signaled;
	}

	/**
	 * Un consultor pide el saldo disponible de una cuenta c.
	 * 
	 * @param c nÃºmero de la cuenta
	 * @return saldo disponible en la cuenta id
	 * @throws IllegalArgumentException si la cuenta c no existe
	 */
	public int disponible(String c) throws IllegalArgumentException {
		if(c==null){																	// Comprobamos que los datos de entrada son correctos
			throw new IllegalArgumentException("[ERROR]: La cuenta <"+c+"> no existe");	// Lanzamos una excepcion
		}

		/******* Entrada en el mutex *******/
		mutex.enter(); 													// Se abre mutex
		if (!cuentas.containsKey(c)) { 									// Comprobamos si la estructura contiene la cuenta
			mutex.leave(); 												// Si no, cerramos el mutex
			throw new IllegalArgumentException("[ERROR]: La cuenta <"+c+"> no existe"); 	//		y lanzamos un error
		}

		int saldo = cuentas.get(c);	// Guardamos el saldo
		mutex.leave();				// Salimos del mutex
		return saldo;				// Devolvemos el saldo
	}

	/**
	 * Un avisador establece una alerta para la cuenta c. La operaciÃ³n termina
	 * cuando el saldo de la cuenta c baja por debajo de m.
	 * 
	 * @param c nÃºmero de la cuenta
	 * @param m saldo mÃ­nimo
	 * @throws IllegalArgumentException si la cuenta c no existe
	 */
	public void alertar(String c, int m) throws IllegalArgumentException {
		if(m>=0 && c!=null){	// Entramos si los datos de entrada son correctos
			/******* Entrada en el mutex *******/
			mutex.enter();	// Entramos en el mutex
			if (!cuentas.containsKey(c)) {													// Comprobamos si la cuenta no está en la estructura de cuentas
				mutex.leave();																// Si no lo esta salimos del mutex
				throw new IllegalArgumentException("[ERROR]: La cuenta <"+c+"> no existe");	// Lanzamos un error	
			} 
			if(m<=cuentas.get(c)){			// Nos mantenemos el bucle hasta que que la cantidad de dinero que haya en la cuenta sea inferior al limite				
				PetAler req = new PetAler(c, m);
				monitSaldos.add(monitSaldos.size(), req);
				req.c.await();
			}
			desbloqueo_transferencias();	// Desbloqueamos la cuenta de la alerta
			mutex.leave();					// Salimos del mutex
		}
	}
}
