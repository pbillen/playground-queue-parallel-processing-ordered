FROM rabbitmq:3-alpine
RUN rabbitmq-plugins enable --offline rabbitmq_management rabbitmq_consistent_hash_exchange
COPY rabbitmq.config /etc/rabbitmq/
COPY definitions.json /etc/rabbitmq/
