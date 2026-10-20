FROM nginx:1.31.6-alpine@sha256:df221db836e1754089190208cee7eeda94f233197056426eda74a43ab1abeac2

COPY prism-apm-ui/dist/ /usr/share/nginx/html/
COPY prism-apm-ui/docker/nginx.conf /etc/nginx/templates/default.conf.template

ENV UI_SERVER_URL=http://host.docker.internal:8080

EXPOSE 80 8066
