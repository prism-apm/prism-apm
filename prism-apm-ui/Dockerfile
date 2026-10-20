FROM nginx:1.31.7-alpine@sha256:892bb2152464f0c5d8972593a5f5afaf8054552580b699d95f8ff4af08c2d33d

COPY prism-apm-ui/dist/ /usr/share/nginx/html/
COPY prism-apm-ui/docker/nginx.conf /etc/nginx/templates/default.conf.template

ENV UI_SERVER_URL=http://host.docker.internal:8080

EXPOSE 80 8066
