package cn.Jolyne;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author zhiguo.liu
 */
@ComponentScan(basePackages = {"cn.Jolyne"})
@SpringBootApplication
public class Bootstrap {

    public static void main(String[] args) {
        new SpringApplication(Bootstrap.class).run(args);
    }
}
