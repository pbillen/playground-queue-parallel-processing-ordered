import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.rabbitmq.RabbitMQConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Test;

import java.util.Random;

public class B_RabbitMQ_IT {
    private static final String HOST = System.getProperty("host");
    private static final int    PORT = Integer.parseInt(System.getProperty("port"));

    @Test
    public void test() throws Exception {
        final DefaultCamelContext context  = new DefaultCamelContext();
        final Verifier            verifier = new Verifier();
        final Random              random   = new Random();

        context.setName("b_rabbitmq");
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
                for (int i = 1; i <= 10; ++i) {
                    from("rabbitmq://" + HOST + ":" + PORT + "?username=test&password=test&declare=false&queue=q" + i + "&autoAck=false&threadPoolSize=1&exclusiveConsumer=true")
                            .to("log:b_rabbitmq?showAll=true&multiline=true")
                            .process(exchange -> verifier.verify(exchange.getIn().getHeader(Headers.GROUP, Integer.class)));
                }
            }

            private void send(final int group) {
                context.createFluentProducerTemplate()
                        .withHeader(Headers.GROUP, group)
                        .withHeader(RabbitMQConstants.ROUTING_KEY, group)
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
