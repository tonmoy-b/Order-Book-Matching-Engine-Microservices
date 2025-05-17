FROM ubuntu:latest
LABEL authors="tonmo"

ENTRYPOINT ["top", "-b"]