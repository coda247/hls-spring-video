package com.joejoe2.video.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class ImageConfig {
  @Value("${server.stream.prefix:https://snailnode.dhive.org/cdn/images/}")
  private String prefix;
}
