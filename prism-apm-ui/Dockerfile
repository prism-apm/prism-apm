FROM nginx:alpine

COPY prism-apm-ui/dist/ /usr/share/nginx/html/
COPY prism-apm-ui/docker/nginx.conf /etc/nginx/templates/default.conf.template

ENV UI_SERVER_URL=http://host.docker.internal:8080

EXPOSE 80 8066
