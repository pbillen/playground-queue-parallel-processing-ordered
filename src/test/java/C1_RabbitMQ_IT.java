import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Test;

import java.util.Random;

public class C1_RabbitMQ_IT {
    private static final String HOST = System.getProperty("host");
    private static final int    PORT = Integer.parseInt(System.getProperty("port"));

    @Test
    public void test() throws Exception {
        final DefaultCamelContext context  = new DefaultCamelContext();
        final Verifier            verifier = new Verifier();
        final Random              random   = new Random();

        context.setName("c1_rabbitmq");
        context.getShutdownStrategy().setTimeout(1);
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                onException(Exception.class)
                        .process(exchange -> verifier.trigger());
                from("timer://message?period=100")
                        .process(exchange -> send(random.nextInt(3)));
                from("direct:message")
                        .to("rabbitmq://" + HOST + ":" + PORT + "/e?username=test&password=test&declare=false&publisherAcknowledgements=true");
                from("rabbitmq://" + HOST + ":" + PORT + "?username=test&password=test&declare=false&queue=q&autoAck=false&threadPoolSize=1&exclusiveConsumer=true")
                        .loadBalance().sticky(header(Headers.GROUP))
                        .to("seda:q1?blockWhenFull=true", "seda:q2?blockWhenFull=true", "seda:q3?blockWhenFull=true", "seda:q4?blockWhenFull=true", "seda:q5?blockWhenFull=true", "seda:q6?blockWhenFull=true", "seda:q7?blockWhenFull=true", "seda:q8?blockWhenFull=true", "seda:q9?blockWhenFull=true", "seda:q10?blockWhenFull=true");
                from("seda:q1", "seda:q2", "seda:q3", "seda:q4", "seda:q5", "seda:q6", "seda:q7", "seda:q8", "seda:q9", "seda:q10")
                        .to("log:c1_rabbitmq?showAll=true&multiline=true")
                        .process(exchange -> verifier.verify(exchange.getIn().getHeader(Headers.GROUP, Integer.class)));
            }

            private void send(final int group) {
                context.createFluentProducerTemplate()
                        .withHeader(Headers.GROUP, group)
                        //.withHeader(RabbitMQConstants.DELIVERY_MODE, 2)
                        //.withHeader(RabbitMQConstants.REQUEUE, true)
                        .to("direct:message")
                        .send();
            }
        });

        try {
            context.start();
            verifier.await();
        } finally {
            context.stop();
        }
    }
}
