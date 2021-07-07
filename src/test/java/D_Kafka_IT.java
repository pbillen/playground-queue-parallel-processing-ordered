import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Random;

public class D_Kafka_IT {
    @Test
    public void test() throws Exception {
        final KafkaContainer      container = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));
        final DefaultCamelContext context   = new DefaultCamelContext();
        final Verifier            verifier  = new Verifier();
        final Random              random    = new Random();

        container.start();
        context.setName("d_kafka");
        context.getShutdownStrategy().setTimeout(1);
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                onException(Exception.class)
                        .process(exchange -> verifier.trigger());
                from("timer://message?period=100")
                        .process(exchange -> send(random.nextInt(3)));
                for (int g = 0; g < 3; ++g) {
                    from("direct:message-T" + g)
                            .to("kafka://T" + g + "?brokers=" + container.getBootstrapServers());
                }
                for (int g = 0; g < 3; ++g) {
                    for (int i = 0; i < 5; ++i) {
                        from("kafka://T" + g + "?brokers=" + container.getBootstrapServers() + "&groupId=G" + g + "&clientId=" + g + "-" + i)
                                .to("log:d_kafka?showAll=true&multiline=true")
                                .process(exchange -> verifier.verify(Integer.parseInt(exchange.getIn().getHeader(KafkaConstants.KEY, String.class))));
                    }
                }
            }

            private void send(final int group) {
                context.createFluentProducerTemplate()
                        .withHeader(KafkaConstants.KEY, Integer.toString(group))
                        .to("direct:message-T" + group)
                        .send();
            }
        });

        try {
            context.start();
            verifier.await();
        } finally {
            context.stop();
            container.stop();
        }
    }
}
