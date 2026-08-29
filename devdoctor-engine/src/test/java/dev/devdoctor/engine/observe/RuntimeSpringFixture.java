package dev.devdoctor.engine.observe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class RuntimeSpringFixture {
    public static void main(String[] arguments) {
        SpringApplication application = new SpringApplication(RuntimeSpringFixture.class);
        application.setLogStartupInfo(false);
        application.run("--server.address=127.0.0.1", "--server.port=" + arguments[0],
                "--spring.main.banner-mode=off", "--logging.level.root=ERROR");
        System.out.println("READY");
        System.out.flush();
    }

    @RestController
    static class OutcomeController {
        @GetMapping("/ok") String ok() { return "ok"; }
        @GetMapping("/fail") ResponseEntity<String> fail() {
            return ResponseEntity.internalServerError().body("failed");
        }
    }
}
