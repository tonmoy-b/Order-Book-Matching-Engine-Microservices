

#check if auto-topic creation is enabled
./kafka-console-producer.sh --bootstrap-server localhost:9092 --topic test-topic
#enter values,
#a message like: [2025-05-20 09:08:12,466] WARN [Producer clientId=console-producer]
# The metadata response from the cluster reported a recoverable issue with correlation id 6 :
# {test-topic=UNKNOWN_TOPIC_OR_PARTITION} (org.apache.kafka.clients.NetworkClient)
#may be given

#check list of topics
./kafka-topics.sh --list --bootstrap-server localhost:9092

#check if messages placed in topic earlier are present
./kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test-topic --from-beginning
