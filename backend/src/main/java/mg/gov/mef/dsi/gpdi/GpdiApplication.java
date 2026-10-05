package mg.gov.mef.dsi.gpdi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GpdiApplication {

        public static void main(String[] args) {
                // spring.devtools.restart.enabled=false dans application.properties ne suffit pas : le
                // RestartClassLoader de devtools s'initialise avant que les proprietes ne soient lues,
                // ce qui produisait des ClassNotFoundException aleatoires sur des classes pourtant
                // presentes (classpath fige au demarrage, incoherent avec les recompilations de l'IDE).
                // Positionner la propriete systeme ici, avant SpringApplication.run, la desactive reellement.
                System.setProperty("spring.devtools.restart.enabled", "false");
                SpringApplication.run(GpdiApplication.class, args);
        }

}