FROM nginx:alpine

# 删除默认 index.html
RUN rm /etc/nginx/conf.d/default.conf

# 复制自定义配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 复制前端构建产物
COPY dist/ /usr/share/nginx/html/

EXPOSE 80