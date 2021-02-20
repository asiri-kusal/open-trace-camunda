package lk.open.camundatrace.config;

import brave.Tracing;
import lk.open.camundatrace.CamundaSlueuthContextInjectingAspect;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnWebApplication
public class EnableCamundaProcessTracingConfig {


    @Bean
    public CamundaSlueuthContextInjectingAspect processSpanDefinitionAspect(RuntimeService runtimeService,
                                                                            Tracing tracing) {
        return new CamundaSlueuthContextInjectingAspect(runtimeService, tracing);
    }

}
