package uk.gov.defra.reach.upload.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * Implementation of {@link HandlerExceptionResolver} with the sole purpose of logging the exception and then allowing the other resolvers to deal
 * with the exception
 */
@Component
@Slf4j
public class LoggingHandlerExceptionResolver implements HandlerExceptionResolver, Ordered {

  @Override
  public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    log.error(ex.getMessage(), ex);
    return null;
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

}
