FROM rabbitmq:3-alpine
RUN rabbitmq-plugins enable --offline rabbitmq_management
COPY rabbitmq.config /etc/rabbitmq/
COPY definitions.json /etc/rabbitmq/
