package hazelcast.platform.labs.machineshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "hazelcast.platform.solutions")
public class MachineStatusService {

    public static void main(String[] args) {
        SpringApplication.run(MachineStatusService.class, args);
    }
}
