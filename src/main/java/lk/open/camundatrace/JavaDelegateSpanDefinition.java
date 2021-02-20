package lk.open.camundatrace;

import java.util.Map;

import brave.Span;
import brave.Tracing;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JavaDelegateSpanDefinition {


    public static final String EXECUTION_ID = "execution-id";
    public static final String TRACE_ID = "Trace_Id";

    /**
     * Intercept call of {@link JavaDelegate} execution and wrapping it in corresponding span;
     * Java delegates are atomic;
     * JavaDelegate may produce messages (different ways);œœ
     * Base sleuth context for such messages -  JavaDelegate execution span;
     */
    public static void javaDelegateSpan(ProceedingJoinPoint pjp, Tracing tracing) throws Throwable {
        //assert context configured properly
        if (tracing == null) {
            pjp.proceed();
            return;
        }

        //LTW-around aspects can obtain values only in this way
        DelegateExecution execution = (DelegateExecution) pjp.getArgs()[0];
        Map<String, String> tracingContextSerialized =
            CamundaSlueuthContextInjectingAspect.extractSerializedContext(
                execution.getVariable(CamundaSlueuthContextInjectingAspect.X_SLEUTH_TRACE_CONTEXT));
        if (tracingContextSerialized.isEmpty()) {
            pjp.proceed();
            return;
        }

        Span parentSpan = CamundaSlueuthContextInjectingAspect.restoreTracingContext(tracingContextSerialized);
        tracing.tracer().withSpanInScope(parentSpan);
        String targetClassName = pjp.getTarget().getClass().getSimpleName();

        //wrap Java delegate execution with new span (JD = JavaDelegate)
        Span span = tracing.tracer().nextSpan().name("JD:" + targetClassName);
        tracing.tracer().withSpanInScope(span);
        span.start();
        pjp.proceed();
        span.finish();
    }

    /**
     * Intercept call of {@link JavaDelegate} execution and wrapping it in corresponding span;
     * Java delegates are atomic;
     * JavaDelegate may produce messages (different ways);œœ
     * Base sleuth context for such messages -  JavaDelegate execution span;
     */
    public static void restExternalSpan(ProceedingJoinPoint pjp, Tracing tracing, RuntimeService runtimeService)
        throws Throwable {
        // assert context configured properly
        if (tracing == null) {
            pjp.proceed();
            return;
        }

        //LTW-around aspects can obtain values only in this way
        Map<String, String> variables = (Map<String, String>) pjp.getArgs()[2];

        Map<String, String> tracingContextSerialized =
            CamundaSlueuthContextInjectingAspect
                .extractSerializedContext(runtimeService.getVariable(variables.get(EXECUTION_ID),
                                                                     CamundaSlueuthContextInjectingAspect.X_SLEUTH_TRACE_CONTEXT));
        if (tracingContextSerialized.isEmpty()) {
            pjp.proceed();
            return;
        }

        Span parentSpan = CamundaSlueuthContextInjectingAspect.restoreTracingContext(tracingContextSerialized);
        tracing.tracer().withSpanInScope(parentSpan);

        //wrap Java delegate execution with new span (JD = JavaDelegate)
        Span span = tracing.tracer().nextSpan().name("external-service-topic : " + variables.get("topic"))
                           .tag("trace-id", (String) runtimeService.getVariable(variables.get(EXECUTION_ID), TRACE_ID))
                           .tag((String) runtimeService.getVariable(variables.get(EXECUTION_ID), TRACE_ID), TRACE_ID);
        tracing.tracer().withSpanInScope(span);
        span.start();
        pjp.proceed();
        span.finish();
    }

}
