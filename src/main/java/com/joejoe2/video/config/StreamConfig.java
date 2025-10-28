package com.joejoe2.video.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class StreamConfig {
  @Value("${server.stream.prefix:https://cdns.cubeapp.org/api/storage/}")
  private String prefix;
}
