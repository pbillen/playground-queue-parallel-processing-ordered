import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Test;

import java.util.Random;

public class C2_ActiveMQ_IT {
    private static final String HOST = System.getProperty("host");
    private static final int    PORT = Integer.parseInt(System.getProperty("port"));

    @Test
    public void test() throws Exception {
        final DefaultCamelContext context  = new DefaultCamelContext();
        final Verifier            verifier = new Verifier();
        final Random              random   = new Random();

        context.setName("c2_activemq");
        context.getShutdownStrategy().setTimeout(1);
        context.addComponent("activemq", ActiveMQComponent.activeMQComponent("tcp://" + HOST + ":" + PORT));
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                onException(Exception.class)
                        .process(exchange -> verifier.trigger());
                from("timer://message?period=100")
                        .process(exchange -> send(random.nextInt(3)));
                from("direct:message")
                        .to("activemq:queue:q");
                from("activemq:queue:q?concurrentConsumers=1&acknowledgementModeName=CLIENT_ACKNOWLEDGE&destination.consumer.exclusive=true")
                        .loadBalance().sticky(header(Headers.GROUP))
                        .to("seda:q1?blockWhenFull=true", "seda:q2?blockWhenFull=true", "seda:q3?blockWhenFull=true", "seda:q4?blockWhenFull=true", "seda:q5?blockWhenFull=true", "seda:q6?blockWhenFull=true", "seda:q7?blockWhenFull=true", "seda:q8?blockWhenFull=true", "seda:q9?blockWhenFull=true", "seda:q10?blockWhenFull=true");
                from("seda:q1", "seda:q2", "seda:q3", "seda:q4", "seda:q5", "seda:q6", "seda:q7", "seda:q8", "seda:q9", "seda:q10")
                        .to("log:c2_activemq?showAll=true&multiline=true")
                        .process(exchange -> verifier.verify(exchange.getIn().getHeader(Headers.GROUP, Integer.class)));
            }

            private void send(final int group) {
                context.createFluentProducerTemplate()
                        .withHeader(Headers.GROUP, group)
                        //.withHeader(JmsConstants.JMS_DELIVERY_MODE, DeliveryMode.NON_PERSISTENT)
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
