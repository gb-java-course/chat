### Docker build:
```
docker build . -t may-chat-server
```
### Docker save built image in .tar file:
```
docker save -o target/dist/may-chat-server.tar chat-server
```
### Simple container run:
```
docker run -d --name may-chat-server january-chat-server
```
#### -add port forwarding:
```
-p 8189:8189
```