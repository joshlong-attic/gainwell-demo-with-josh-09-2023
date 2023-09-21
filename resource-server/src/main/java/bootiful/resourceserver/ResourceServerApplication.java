package bootiful.resourceserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class ResourceServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResourceServerApplication.class, args);
    }

}

@Controller
@ResponseBody
class MeController {

    @GetMapping("/me")
    Map<String, String> me(Principal principal) {

        if (principal instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            var jwt = (Jwt) jwtAuthenticationToken.getPrincipal();
            jwt.getClaims().forEach((k, v) -> System.out.println(k + '=' + v));
        }
        return Map.of("name", principal.getName());
    }
}

@Controller
@ResponseBody
class CustomerHttpController {

    @GetMapping("/customers")
    Collection<Customer> customers() {
        return List.of(
                new Customer(1, "Josh"),
                new Customer(2, "Jane")
        );
    }
}

record Customer(Integer id, String name) {
}
