package com.example.service;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ai.client.AiClient;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@SpringBootApplication
public class ServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceApplication.class, args);
    }

}

@Controller
@ResponseBody
class AiController {

    private final AiClient aiClient;

    AiController(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    record Response(String message) {
    }

    @GetMapping("/story")
    Response response() {
        return new Response(this.aiClient.generate(
                """
                            tell me a story about BBQ in the lovely city 
                            of Dallas, TX in the style of Dr. Seuss
                        """));
    }

    @GetMapping("/jokes")
    Response ai() {
        return new Response(
                this.aiClient.generate("tell me a joke about BBQ in Dallas, TX")
        );
    }
}


@Controller
@ResponseBody
class CustomerHttpController {

    private final CustomerRepository repository;

    private final ObservationRegistry registry;

    CustomerHttpController(CustomerRepository repository, ObservationRegistry registry) {
        this.repository = repository;
        this.registry = registry;
    }


    @GetMapping("/customers/{name}")
    Collection<Customer> customersByName(@PathVariable String name) {
        Assert.state(Character.isUpperCase(name.charAt(0)),
                "the name must start with an uppercase character");
        return Observation
                .createNotStarted("by-name", this.registry) // metric
                .observe(() -> this.repository.findByName(name)); // trace

    }

    @GetMapping("/customers")
    Collection<Customer> customers() {
        return this.repository.findAll();
    }
}


class BRAOP implements BeanRegistrationAotProcessor {

    @Override
    public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
        return (generationContext, beanRegistrationCode) -> {
            System.out.println("running aot contribution for " +
                               registeredBean.getBeanName() +'.');
        };
    }


}


@Configuration
class InfraConfiguration {

    @Bean
    BRAOP braop() {
        return new BRAOP();
    }

    @Bean
    static BFPP bfpp() {
        return new BFPP();
    }

    @Bean
    static BPP bpp() {
        return new BPP();
    }
}

class BFPP implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(
            ConfigurableListableBeanFactory bf)
            throws BeansException {


    }
}

class BPP implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(
            Object bean, String beanName) throws BeansException {


        return bean;
    }
}


@ControllerAdvice
class ErrorHandlingControllerAdvice {

    @ExceptionHandler
    ProblemDetail handle(IllegalStateException ise, HttpServletRequest request) {
        request.getHeaderNames().asIterator().forEachRemaining(System.out::println);
        var pd = ProblemDetail
                .forStatus(HttpStatus.BAD_REQUEST.value());
        pd.setDetail(ise.getLocalizedMessage());
        return pd;

    }
}

interface CustomerRepository extends ListCrudRepository<Customer, Integer> {
    Collection<Customer> findByName(String name);
}

// loom mom, no Lombok!
record Customer(@Id Integer id, String name) {
}
