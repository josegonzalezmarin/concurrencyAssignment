package cc.banco;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import org.jcsp.lang.Alternative;
import org.jcsp.lang.AltingChannelInput;
import org.jcsp.lang.Any2OneChannel;
import org.jcsp.lang.CSProcess;
import org.jcsp.lang.Channel;
import org.jcsp.lang.Guard;
import org.jcsp.lang.One2OneChannel;
import org.jcsp.lang.One2OneChannelInt;
import org.jcsp.lang.ProcessManager;

import es.upm.aedlib.indexedlist.ArrayIndexedList;

/**
 * HORROR Peticiones de algo canal y dato de la CPRE?? <- Canal por donde iran
 * los datos
 * 
 * Ejecuccion del servidor Envio la peticon Luego intento enviar los fatos
 * 
 * Crear clase peticion de ingresar, transferir, alerta
 * 
 * No volver a select hasta comprobar todas las peticiones aplazadas.
 * 
 */

public class BancoCSP implements Banco, CSProcess {
    // canales: uno por operación
    // serán peticiones aplazadas
    private Any2OneChannel chIngresar;
    private Any2OneChannel chDisponible;
    private Any2OneChannel chTransferir;
    private Any2OneChannel chAlertar; 

	private HashMap<String, Integer> cuentas; // Estructura de datos con las cuentas de los usuarios
	
	public BancoCSP() {
		this.chIngresar = Channel.any2one();
		this.chAlertar = Channel.any2one();
		this.chDisponible = Channel.any2one();
		this.chTransferir = Channel.any2one();
		cuentas = new HashMap<String, Integer>();
		new ProcessManager(this).start();
	}

	public class PetAler {
		private String cuenta;
		private int valor;
		One2OneChannelInt c;

		public PetAler(String cuenta, int valor) {
			this.cuenta = cuenta;
			this.valor = valor;
			this.c = Channel.one2oneInt();
		}
	}

	public class PetIngr {
		private String cuenta;
		private int valor;
		//One2OneChannel c;

		public PetIngr(String cuenta, int valor) {
			this.cuenta = cuenta;
			this.valor = valor;
			//this.c = Channel.one2one();
		}
	}

	public class PetTrns {
		private String origen;
		private String destino;
		private int valor;
		One2OneChannel c;

		public PetTrns(String origen, String destino, int valor) {
			this.origen = origen;
			this.destino = destino;
			this.valor = valor;
			this.c = Channel.one2one();
		}
	}
	
	public class PetDisp {
		private String cuenta;
		One2OneChannelInt c;
		public PetDisp(String cuenta) {
			this.cuenta = cuenta;
			c = Channel.one2oneInt();
		}
	}

	/**
	 * Un cajero pide que se ingrese una determinado valor v a una cuenta c. Si la
	 * cuenta no existe, se crea.
	 * 
	 * @param c número de cuenta
	 * @param v valor a ingresar
	 */
	public void ingresar(String c, int v) {
		PetIngr pet = new PetIngr(c, v);
		chIngresar.out().write(pet);
		//pet.c.in().read();
		System.out.println("[Ingresar]: Cuenta:<" + c + "> Valor: " + v + "$");
	}

	/**
	 * Un ordenante pide que se transfiera un determinado valor v desde una cuenta o
	 * a otra cuenta d.
	 * 
	 * @param o número de cuenta origen
	 * @param d número de cuenta destino
	 * @param v valor a transferir
	 * @throws IllegalArgumentException si o y d son las mismas cuentas
	 *
	 */
	public void transferir(String o, String d, int v) throws IllegalArgumentException {
		if (o.equals(d)) { // si el origen y el destino son el mismo sale por excepcion
			throw new IllegalArgumentException("La cuenta origen y destino son las mismas\n");
		}
		PetTrns pet = new PetTrns(o, d, v);
		chTransferir.out().write(pet);
		System.out.println("[Transferir]: Esperamos a que se realice la transferencia Origen:<" + o + "> Destino:<" + d + "> Valor: " + v + "$");
		pet.c.in().read();
		System.out.println("[Transferir]: Origen:<" + o + "> Destino:<" + d + "> Valor: " + v + "$");
	}

	/**
	 * Un consultor pide el saldo disponible de una cuenta c.
	 * 
	 * @param c número de la cuenta
	 * @return saldo disponible en la cuenta id
	 * @throws IllegalArgumentException si la cuenta c no existe
	 */
	public int disponible(String c) {
		PetDisp pet = new PetDisp(c);
		chDisponible.out().write(pet);
		int solucion = pet.c.in().read();
		if (solucion==-1){
			throw new IllegalArgumentException("La cuenta no existe");
		}
		return solucion;			
	}

	/**
	 * Un avisador establece una alerta para la cuenta c. La operación termina
	 * cuando el saldo de la cuenta c baja por debajo de m.
	 * 
	 * @param c número de la cuenta
	 * @param m saldo mínimo
	 * @throws IllegalArgumentException si la cuenta c no existe
	 */
	public void alertar(String c, int m) {
		PetAler pet = new PetAler(c, m);
		chAlertar.out().write(pet);
		System.out.println("[Alertar]: Esperamos a que se realiza la alerta de Cuenta:<" + c + "> Limite: " + m + "$");
		if (pet.c.in().read()==-1){
			throw new IllegalArgumentException("La cuenta no existe");
		}
		System.out.println("[Alertar]: Cuenta:<" + c + "> Limite: " + m + "$");
	}

	/**
	 * Codigo para el servidor.
	 */
	public void run() {
		// nombres simb�licos para las entradas
		final int INGRESAR   = 0;
		final int DISPONIBLE = 1;
		final int TRANSFERIR = 2;
		final int ALERTAR    = 3;
		
		Queue<PetAler> esperaAler = new LinkedList<>();
		ArrayIndexedList<PetTrns> esperaTrns = new ArrayIndexedList<PetTrns>();
		
		// construimos la estructura para recepci�n alternativa
		final Guard[] guards = new AltingChannelInput[4];
		guards[INGRESAR]   = chIngresar.in();
		guards[DISPONIBLE] = chDisponible.in();
		guards[TRANSFERIR] = chTransferir.in();
		guards[ALERTAR]    = chAlertar.in();
		Alternative servicios = new Alternative(guards);

		while (true) {
			// Elegimos un canal a escuchar
			int servicio = servicios.fairSelect();
			// Dependiendo del canal vamos anadiendo las peticiones a ala queue
			switch (servicio) {
			case INGRESAR:
				// recibir petici�n
				// COMPLETAR
				// realizar ingreso
				PetIngr petIngr = (PetIngr) chIngresar.in().read();
				if (!cuentas.containsKey(petIngr.cuenta)) { // Comprobamos si la cuenta solicitada falta en la
					// estructura de cuentas
					cuentas.put(petIngr.cuenta, petIngr.valor); // Añadimos la cuenta a la estructura
					System.out.println("[PetIngresar]: Se crea Cuenta:<" + petIngr.cuenta + "> con Valor: "+ petIngr.valor + "$");
				} else { // Si la cuenta ya esta en la estructura
					cuentas.put(petIngr.cuenta, cuentas.get(petIngr.cuenta) + petIngr.valor); // Sumamos el valor del ingreso al saldo
					System.out.println("[PetIngresar]: Se ingresa en Cuenta:<" + petIngr.cuenta + "> con Valor: "+ petIngr.valor + "$");
				}
				System.out.println("[Choice]: Metemos peticion de ingresar");
				break;
		    case DISPONIBLE: 
				// recibir petici�n
				// COMPLETAD
				// responder
				// COMPLETAD
				//
		    	PetDisp petDisp = (PetDisp) chDisponible.in().read();
				if (cuentas.containsKey(petDisp.cuenta)) { // Comprobamos si la cuenta solicitada falta en la
					// estructura de cuentas
					petDisp.c.out().write(cuentas.get(petDisp.cuenta));
				}else {
					petDisp.c.out().write(-1);
				}
				break;
			case TRANSFERIR:
				// recibir petici�n
				// COMPLETAD
				// encolar petici�n
				// (alternativamente, se puede comprobar CPRE y
				//  solo encolar si estrictamente necesario)
				// 
				//
				//  COMPLETAD
				PetTrns petTrans = (PetTrns) chTransferir.in().read();
				esperaTrns.add(esperaTrns.size(), petTrans);
				System.out.println("[Choice]: Metemos peticion de transferencia");
				break;
			case ALERTAR:
				// recibir petici�n
				// COMPLETAD
				// encolar petici�n
				// (alternativamente, se puede comprobar CPRE y
				//  solo encolar si estrictamente necesario) 
				//
				//
				// COMPLETAD
				PetAler petAler = (PetAler) chAlertar.in().read();
				if (cuentas.containsKey(petAler.cuenta)) {
					esperaAler.add(petAler);
				}else {
					petAler.c.out().write(-1);
				}
				System.out.println("[Choice]: Metemos peticion de alertar");
				break;
			}
			boolean petTratada = true;
			System.out.println("");
			System.out.println("**** Tratamos peticiones ****");
			while (petTratada) {
				System.out.println("-----------------------------");
				petTratada = false;
				for (int i = 0, petsAler = esperaAler.size(); i < petsAler; i++) {
					PetAler petAler = esperaAler.poll();
					if (cuentas.containsKey(petAler.cuenta)) {
						if (cuentas.get(petAler.cuenta) < petAler.valor) {
							System.out.println("[Alert-" + petAler.cuenta + "]: Account <" + petAler.cuenta + "> has less money than -> " + petAler.valor);
							petAler.c.out().write(1);
							petTratada = true;
						} else {
							esperaAler.add(petAler);
						}
					} else {
						esperaAler.add(petAler);
					}
				}
				
				HashMap<String, Integer> cuentasComprobadas = new HashMap<String, Integer>();
				
				for (int i = 0, petsTrans = esperaTrns.size(); i < petsTrans; i++) {
					// CPRE
					PetTrns petTrns = esperaTrns.get(i);				
					if(!cuentasComprobadas.containsKey(petTrns.origen)) {
						cuentasComprobadas.put(petTrns.origen, null);
						if (cuentas.containsKey(petTrns.origen)) {
							if (cuentas.containsKey(petTrns.destino)) {
								if (cuentas.get(petTrns.origen) >= petTrns.valor) {
									cuentas.put(petTrns.origen, cuentas.get(petTrns.origen) - petTrns.valor); // se quita de la origen
									cuentas.put(petTrns.destino, cuentas.get(petTrns.destino) + petTrns.valor); // se da a la destino
									System.out.println("[PetTransferencia]: Se ingresa en Cuenta:<" + petTrns.destino + "> con Valor: "+ petTrns.valor + "$");
									petTrns.c.out().write(null);
									esperaTrns.remove(petTrns);
									i--;
									petsTrans = esperaTrns.size();
									petTratada = true;
								} else {
									System.out.println("[PetTransferencia]: Se espera a que Cuenta:<" + petTrns.origen + "> tenga saldo mayor a: "+ petTrns.valor + "$");
								}
							} else {
								System.out.println("[PetTransferencia]: Se espera a que Cuenta:<" + petTrns.destino + "> sea creada");
							}
						} else {
							System.out.println("[PetTransferencia]: Se espera a que Cuenta:<" + petTrns.origen + "> sea creada");
						}
					}
				}
			}
			System.out.println("**** No mas peticiones ****");
		}
	}
}
