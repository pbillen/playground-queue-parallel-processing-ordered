import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Test;

import java.util.Random;

public class A_ActiveMQ_IT {
    private static final String HOST = System.getProperty("host");
    private static final int    PORT = Integer.parseInt(System.getProperty("port"));

    @Test
    public void test() throws Exception {
        final DefaultCamelContext context  = new DefaultCamelContext();
        final Verifier            verifier = new Verifier();
        final Random              random   = new Random();

        context.setName("a_activemq");
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
                from("activemq:queue:q?concurrentConsumers=10&acknowledgementModeName=CLIENT_ACKNOWLEDGE")
                        .to("log:a_activemq?showAll=true&multiline=true")
                        .process(exchange -> verifier.verify(exchange.getIn().getHeader(Headers.GROUP, Integer.class)));
            }

            private void send(final int group) {
                context.createFluentProducerTemplate()
                        .withHeader(Headers.GROUP, group)
                        .withHeader(JmsConstants.JMS_X_GROUP_ID, group)
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
