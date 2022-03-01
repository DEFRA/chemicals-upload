package uk.gov.defra.reach.upload.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

  @GetMapping(value = "/", produces = MediaType.TEXT_PLAIN_VALUE)
  public String getRoot() {
    return "ok";
  }

}
