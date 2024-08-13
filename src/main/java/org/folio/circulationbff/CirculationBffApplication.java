package org.folio.circulationbff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class CirculationBffApplication {

  public static void main(String[] args) {
    SpringApplication.run(CirculationBffApplication.class, args);
  }

}
