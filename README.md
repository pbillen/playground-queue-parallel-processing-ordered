# Problem

Safe dynamic rebalancing of consumers in consumer groups.

All solutions rely on [Apache Camel](http://camel.apache.org) 2.25.4.

# Solutions

## [A] ActiveMQ - Message groups

* Execution: `mvn clean verify -U -Pa-activemq`
* Documentation: <http://activemq.apache.org/message-groups.html>
* Implementation: [A_ActiveMQ_IT.java](src/test/java/A_ActiveMQ_IT.java)

Note that this does not support safe dynamic rebalancing of consumers without external synchronization. We can reset a message group (by setting `JMSXGroupSeq` to `-1`), but we must first make sure that there are no pending messages for that particular group.

## [B] RabbitMQ - Consistent hash exchange

* Execution: `mvn clean verify -U -Pb-rabbitmq`
* Documentation: <https://github.com/rabbitmq/rabbitmq-consistent-hash-exchange>
* Implementation: [B_RabbitMQ_IT.java](src/test/java/B_RabbitMQ_IT.java) & [definitions.json](src/test/docker/b-rabbitmq/definitions.json)

Note that this does not support safe dynamic rebalancing of consumers without external synchronization. We can alter the routing topology by adding/removing a queue, but we must first make sure that there are no pending messages at all.

## [C] RabbitMQ/ActiveMQ & sticky load balancer & in-memory SEDA queues

* Execution: `mvn clean verify -U -Pc1-rabbitmq` & `mvn clean verify -U -Pc2-activemq`
* Documentation: <http://camel.apache.org/load-balancer.html> & <http://camel.apache.org/seda.html>
* Implementation: [C1_RabbitMQ_IT.java](src/test/java/C1_RabbitMQ_IT.java) & [definitions.json](src/test/docker/c1-rabbitmq/definitions.json) / [C2_ActiveMQ_IT.java](src/test/java/C2_ActiveMQ_IT.java)

Note that this does not support safe dynamic rebalancing of consumers.

Also note that messages could be lost after a service crash or restart! A message will be acknowledged once it has been added to the SEDA queue, as the route ends at that point.
In theory, we could alter the implementation to manually acknowledge the message after successful asynchronous execution, but we currently face the following limitations in Apache Camel:

* RabbitMQ:
  * Asynchronous processing is currently not supported.
  * Manual acknowledgment is currently not supported.
* ActiveMQ:
  * Asynchronous transactions are scheduled for Camel 3.0, as documented [here](http://camel.apache.org/jms.html).
  * As an alternative to asynchronous transactions, we could use asynchronous processing (which can be enabled via the `asyncConsumer=true` property, as documented [here](http://camel.apache.org/jms.html)) and the `INDIVIDUAL_MESSAGE` acknowledgment mode, as documented [here](https://activemq.apache.org/artemis/docs/1.0.0/pre-acknowledge.html).
    Unfortunately, the `INDIVIDUAL_MESSAGE` acknowledgment mode is currently not supported, as reported [here](https://issues.apache.org/jira/browse/AMQ-4767).
    * Note that we cannot use the `CLIENT_ACKNOWLEDGE` acknowledgment mode, as Spring's `MessageListenerContainer` acknowledges messages synchronously in the message callback, as documented [here](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jms/listener/AbstractMessageListenerContainer.html).
      See also [here](https://github.com/spring-projects/spring-framework/blob/v5.1.2.RELEASE/spring-jms/src/main/java/org/springframework/jms/listener/AbstractMessageListenerContainer.java#L780) and [here](https://github.com/apache/camel/blob/camel-2.23.0/components/camel-jms/src/main/java/org/apache/camel/component/jms/EndpointMessageListener.java#L127).

## [D] Kafka - Consumer groups

* Execution: `mvn clean verify -U -Pd-kafka`
* Documentation: <https://kafka.apache.org/documentation>
* Implementation: [D_Kafka_IT.java](src/test/java/D_Kafka_IT.java)

Consumer groups are natively supported, which allows dynamic rebalancing of consumers.
