package env;

// FlaskServer.java
import org.python.util.PythonInterpreter;

//TODO Questa classe è da eliminare, ma la lascio come promemoria poiché serve come testimonianza del fatto che il server Flask debba essere manualmente eseguito
// con il comando: " python .\pacman\src\main\java\env\app.py "
// e bisogna necessariamente avere come plugin: "python"
// e digitare all'interno del terminale: "pip install flask"
//
// Il motivo: " Se stai utilizzando Jython come dipendenza Gradle e hai installato Flask tramite pip nel terminale di IntelliJ IDEA,
// potrebbe esserci un problema di compatibilità. Jython non supporta completamente tutte le librerie Python, compreso Flask,
// che è scritto in C e richiede un interprete Python CPython per funzionare correttamente.
// Se hai bisogno di eseguire codice Python con librerie come Flask all'interno di un'applicazione Java,
// potrebbe essere più appropriato utilizzare un approccio basato su processi o servizi. Ad esempio,
// potresti eseguire il tuo server Flask come un servizio separato e comunicare con esso tramite HTTP
//  o qualche altro protocollo di rete nel tuo codice Java."
public class FlaskServer {

    public static void main(String[] args) {
        PythonInterpreter pythonInterpreter = new PythonInterpreter();
        pythonInterpreter.execfile("pacman/src/main/java/env/pre_run.py");  // Esegui il modulo pre_run prima del tuo script principale
        pythonInterpreter.exec("import sys");
        pythonInterpreter.exec("sys.argv = ['app.py']");
        pythonInterpreter.execfile("pacman/src/main/java/env/app.py");  // Specifica il percorso corretto
    }
}
