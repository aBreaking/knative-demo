
# Preface

关于knative的介绍，参考：[初识 Knative - 技术教程 (knative-sample.com)](https://knative-sample.com/10-getting-started/10-why-knative/)

11月4日，knative终于发布了其第一个稳定版本：[Knative 1.0](https://knative.dev/blog/articles/knative-1.0/)。这是从2018年7月knative第一次发布时，到现在已经历经3年的时间了。本次发布的主要特点有：

* 支持多个 HTTP 路由层（包括 Istio、Contour、Kourier 和 Ambassador），当然主选应该还是istio；
* 支持多个事件存储层来处理事件的订阅机制，比如Kafka、MQ等等；
* 抽象了一个"duck type"类型，允许处理具有公共字段的Kubernetes资源；
* Knative Build独立了出来，演化成了一个单独的CI/CD项目：Tekon；
* 使用Brokers and Triggers，简化了事件的发布和订阅机制，解耦生产者和消费者；
* 支持将事件组件传送到非 Knative 组件，包括集群外组件或主机上的特定 URL；
* 支持自动配置TLS证书，通过DNS或HTTP01方式；
* 新增Parallel 和 Sequence 组件，用于编写某些复合事件工作流；
* 支持基于并发或 RPS 的水平 Pod 自动缩放；
* 使用 DomainMapping 简化服务的管理和发布 。

更多内容见官方文件：[Knative 1.0 is out! - Knative](https://knative.dev/blog/articles/knative-1.0/)



# 安装

tips：官网上installing描述不够准确，还有helloworld的例子也有坑。我前前后后花了一周左右的时间才搞定，有点无语。

## 安装前准备

### 配置

建议Linux环境进行安装，我本地使用ubuntu。

* cpu: 8核及以上
* 内存：8GB及以上
* 磁盘：40GB及以上
* 网络：能访问外网

### 环境

1. docker

   这个不需多说，容器引擎docker。

   安装比较简单，直接安装官方文档操作即可：[https://docs.docker.com/get-started/](https://docs.docker.com/get-started/)

   tips: 最好再创建一个自己的[docker up](https://hub.docker.com/)，可用于存放个人镜像。

2. k8s

   本地学习建议安装minukube，直接安装k8s难度太大了。

   minikube安装比较简单，参考：[Minikube - Kubernetes本地实验环境-阿里云开发者社区 (aliyun.com)](https://developer.aliyun.com/article/221687)

3. golang

   直接去官网下载最新的golang版本，解压添加环境变量即可。

   官方手册：[https://golang.org/doc/install](https://golang.org/doc/install)

### 其他

由于需要拉取k8s镜像库，国内是访问不了的。办法有：

* 找个梯子，自行百度。
* 自己制作镜像。比较麻烦，使用阿里云容器镜像服务，还得需要手动替换yaml里镜像地址。具体方式可参考：[使用阿里云镜像服务来代理k8s镜像 - 知乎 (zhihu.com)](https://zhuanlan.zhihu.com/p/429685829)



## 安装Knative-serving

官方的手册：[https://knative.dev/docs/install/serving/install-serving-with-yaml/](https://knative.dev/docs/install/serving/install-serving-with-yaml/)

官方的文档有些坑，我后面再说。

安装步骤如下：

### 安装serving的组件

> tips：官网的安装方式是直接：
>
> ```shell
> kubectl apply -f https://github.com/knative/serving/releases/download/knative-v1.0.0/serving-core.yaml
> ```
>
> 我建议你先把url的文件下载到本地，然后再`kubectl apply -f`。这样好处有：1. 方便你安装失败了回退；2. 方便你手动替换里面拉取不到的镜像文件。

1. serving-crds.yaml

   安装crd的资源，该文件只有一些基础配置，没有要拉取的镜像文件

   ```shell
   $ wget https://github.com/knative/serving/releases/download/knative-v1.0.0/serving-crds.yaml
   $ kubectl apply -f serving-crds.yaml
   ```

   

2. serving-core.yaml

   这个文件就是serving的核心组件。

   ```shell
   $ wget https://github.com/knative/serving/releases/download/knative-v1.0.0/serving-core.yaml
   $ kubectl apply -f serving-core.yaml
   ```

   该文件里面有6个组件，也就是要拉取这6个组件的镜像，通过`kubectl get pod -n knative-serving`你可以看到：

   ```shell
   $ kubectl get pod -n knative-serving
   NAME                                     READY   STATUS    RESTARTS       AGE
   activator-68b7698d74-gkgnd               1/1     Running   3 (148m ago)   22h
   autoscaler-6c8884d6ff-b5rkt              1/1     Running   3 (148m ago)   22h
   controller-76cf997d95-c28ft              1/1     Running   3 (148m ago)   22h
   domain-mapping-57fdbf97b-gj96k           1/1     Running   3 (148m ago)   22h
   domainmapping-webhook-579dcb874d-x4zc2   1/1     Running   4 (148m ago)   21h
   webhook-7df8fd847b-c85tx                 1/1     Running   4 (148m ago)   22h
   ```

   > 如果状态STATUS不是Running，那么你得需要通过`kubectl describe ...`命令来找原因。通常来说：ErrImagePull或CrashLoopBackOff等状态，一般是镜像拉取不到的问题。解决方式：比如自己通过阿里云镜像服务来拉取yaml里面的文件，然后制作镜像，再替换掉yaml文件里对应组件的镜像地址。（下同）


### 安装网络层

一般是选择istio作为Knative的服务网格。

> 需要注意：官网的上的这一步并不是安装istio，istio还得需要单独安装，见下面。这一步只是为Knative-serving选择一个网络层。

1. 配置istio。（注：并不是安装istio）

   ```shell
   $ wget https://github.com/knative/net-istio/releases/download/knative-v1.0.0/istio.yaml
   $ kubectl apply -l knative.dev/crd-install=true -f istio.yaml
   $ kubectl apply -f istio.yaml
   ```

2. 安装knative的istio的控制器controller。

   ```shell
   $ https://github.com/knative/net-istio/releases/download/knative-v1.0.0/net-istio.yaml
   $ kubectl apply -f net-istio.yaml
   ```



然后就可以通过如下命令看到istio-ingressgateway的服务是否正常：

```shell
$ kubectl --namespace istio-system get service istio-ingressgateway
NAME                   TYPE           CLUSTER-IP     EXTERNAL-IP                   PORT(S)                AGE
istio-ingressgateway   LoadBalancer   10.98.11.197   10.98.11.197    15021:31993/TCP,80:31426/TCP,443:30168/TCP,31400:30840/TCP,15443:31673/TCP   21h
```

>在minikube上，如果发现istio-ingressgateway的状态为`pengding`或者EXTERNAL-IP内容为`none`。还得需要打开Minikube的负载均衡策略：
>
>在后台或者开一个新命令窗口，执行命令：
>
>```shell
>minikube tunnel
># 或者使用强制命令：
># minikube tunnel --cleanup
>```



正如上面所说，这一步并不是安装istio，你会看到istio-system里的很多pod都是CrashLoopBackOff、pending状态。

```shell
$ kubectl get pod -n istio-system
NAME                                   READY   STATUS             RESTARTS      AGE
istio-ingressgateway-b899b7b79-87sn6   0/1     CrashLoopBackOff   3 (41s ago)   93s
istio-ingressgateway-b899b7b79-dzrrr   0/1     CrashLoopBackOff   3 (41s ago)   93s
istio-ingressgateway-b899b7b79-l85lq   0/1     CrashLoopBackOff   3 (41s ago)   93s
istiod-d845fbcfd-8zk9w                 1/1     Running            0             93s
istiod-d845fbcfd-fsrb4                 1/1     Running            0             78s
istiod-d845fbcfd-qszng                 0/1     Pending            0             78s
```

之前我一直以为到这里就算是将knative-serving安装成功了，但并不是，就是这里的坑。

所以，istio还得需要单独安装。



### 配置DNS（可选）

可以为配置一个DNS，后面我们要部署一个helloworld服务就可以直接使用一个CNAME域名，这样我们访问helloworld服务时直接通过curl就可以了。

当然也可以不配置DNS，但是后面访问helloworld服务时需要给curl带上请求头。（后面再详细说）

如果需要配置，执行如下命令：

```shell
$ wget https://github.com/knative/serving/releases/download/knative-v1.0.0/serving-default-domain.yaml
$ kubectl apply -f serving-default-domain.yaml
```



还可以安装其他插件，可以见官网操作手册，比如我还安装了 HPA。但这些都不是必须的。你可以直接跳过这步。

到这里knative-serving就算基本安装完毕了，此时查看命令空间knative-serving的pod内容如下：

```shell
$ kubectl get pod -n knative-serving
NAME                                     READY   STATUS    RESTARTS        AGE
activator-68b7698d74-gkgnd               1/1     Running   3 (4h20m ago)   24h
autoscaler-6c8884d6ff-b5rkt              1/1     Running   3 (4h20m ago)   24h
autoscaler-hpa-85b46c9646-9k6h9          2/2     Running   2 (4h20m ago)   20h
controller-76cf997d95-c28ft              1/1     Running   3 (4h20m ago)   24h
domain-mapping-57fdbf97b-gj96k           1/1     Running   3 (4h20m ago)   24h
domainmapping-webhook-579dcb874d-x4zc2   1/1     Running   4 (4h20m ago)   23h
net-istio-controller-544874485d-ztb2d    1/1     Running   2 (4h20m ago)   22h
net-istio-webhook-695d588d65-s79d2       2/2     Running   8 (4h18m ago)   22h
webhook-7df8fd847b-c85tx                 1/1     Running   4 (4h20m ago)   24h

```



## 安装istio

istio的下载安装可以直接参考官方的文档：[https://istio.io/latest/docs/setup/getting-started/#download](https://istio.io/latest/docs/setup/getting-started/#download)

针对istio的版本，knative的官方给的建议是`1.9.5`的版本。

我们就使用官方建议的istio版本来进行安装。

### 下载

进入istio的下载页：[https://github.com/istio/istio/releases/tag/1.9.5](https://github.com/istio/istio/releases/tag/1.9.5)

选择合适你主机的版本下载到本地即可。

### 环境变量

然后直接解压，按照正常步骤，配置环境变量。

```shell
$ export ISTIOPATH=/root/istio-1.9.5
$ export PATH=$ISTIOPATH/bin:$PATH
$ istioctl version
client version: 1.9.5
pilot version: 1.9.5
pilot version: 1.9.5
pilot version: 1.9.5
pilot version: 1.10.5
pilot version: 1.10.5
pilot version: 1.10.5
data plane version: 1.10.5 (2 proxies), 1.9.5 (4 proxies)
```



### 安装

本地学习环境，所以我们直接选择demo这个profile来进行安装即可。

```shell
$ istioctl install --set profile=demo -y
# 可能会有一些莫名的报错，不过关系不大，只要能确认以下组件能安装好就ok
✔ Istio core installed
✔ Istiod installed
✔ Egress gateways installed
✔ Ingress gateways installed
✔ Installation complete
```

### 启用 sidecar 

在 knative-serving 系统命名空间上启用 sidecar 容器

```shell
$ kubectl label namespace knative-serving istio-injection=enabled
namespace/knative-serving labeled
```



此时istio就算安装完毕了，此时可以看到命令空间里istio-system的pod状态：

```shell
$ kubectl get pod -n istio-system
NAME                                    READY   STATUS    RESTARTS        AGE
istio-egressgateway-64b4ccccbf-r9d2j    1/1     Running   0               4h27m
istio-ingressgateway-6dc7b4b675-4k2pq   1/1     Running   0               4h27m
istio-ingressgateway-6dc7b4b675-lrtk7   1/1     Running   0               4h27m
istio-ingressgateway-6dc7b4b675-nl6xz   1/1     Running   0               4h27m
istiod-65fbd8c54c-mc9dr                 0/1     Running   0               4h27m
istiod-65fbd8c54c-pd5dq                 0/1     Running   0               4h27m
istiod-65fbd8c54c-wq4dd                 0/1     Running   0               4h27m
istiod-d845fbcfd-dcvs6                  1/1     Running   2 (4h35m ago)   22h
istiod-d845fbcfd-q7t8k                  0/1     Running   0               4h27m
istiod-d845fbcfd-rt49j                  0/1     Running   0               4h27m

```



再确认下命令空间里istio-system的服务状态：

```shell
$ kubectl get svc -n istio-system
NAME                    TYPE           CLUSTER-IP      EXTERNAL-IP                   PORT(S)                                                                      AGE
istio-egressgateway     ClusterIP      10.107.56.203   <none>                        80/TCP,443/TCP,15443/TCP                                                     4h27m
istio-ingressgateway    LoadBalancer   10.98.11.197    10.98.11.197,192.168.80.129   15021:31993/TCP,80:31426/TCP,443:30168/TCP,31400:30840/TCP,15443:31673/TCP   22h
istiod                  ClusterIP      10.97.157.147   <none>                        15010/TCP,15012/TCP,443/TCP,15014/TCP                                        22h
knative-local-gateway   ClusterIP      10.108.95.163   <none>                        80/TCP                                                                       22h

```



# HelloWorld

此时就knative-serrving安装完毕了，然后我们就可以来为Knative部署一个hello world服务。

>ATTENTION！！！这里还有个坑，如果你直接使用官方文档上helloworld-go例子，多半跑不起来，反正我在两台主机上部署过，都没成功，服务状态最后总是revisionMiss。
>
>而且官方demo的镜像大小居然有200多M，除去golang语言本身的包，一个简单的helloworld程序应该不至于那么大吧，所以我严重怀疑官网给的demo有问题。如下：
>
>![image-20211109140430680](https://blog.abreaking.com/upload/2021/11/udtsm62dn8jp4pruemqjoi0m8r.png)



接下来介绍自己编写helloworld的例子。

## 使用现成的

如果你懒得编写，那么也可以使用我做好得helloworld镜像。

```dockerfile
abreaking/helloworld-go:latest
```

然后你就可以跳到 

## 自己编写

使用了我现成的helloworld镜像，你可以跳过本步骤。

我们来编写一个Java工程的spring boot服务来作为demo。

参考：[Hello World - Spring Boot Java - Knative](https://knative.dev/docs/serving/samples/hello-world/helloworld-java-spring/)

### 准备

1. 一个简单的spring boot工程，我的工程名：[knative-demo]()
2. [maven](https://dlcdn.apache.org/maven/maven-3/)环境
3. docker hub，用来上传我们的镜像。

### 构建

1. 可以先在自己的idea上创建一个简单的springboot工程。然后在该spring boot工程里写一个简单的controller。

   ```java
   @RestController
   public class KnativeServingController {
   
       @Value("${TARGET:World}")
       String target;
   
       @RequestMapping("/")
       String hello() {
           return "Hello " + target + "!";
       }
   }
   ```

   写好之后记得自行测试一下，比如启动后，直接访问`localhost:8080`，能够输出"hello world"

   ```shell
   $ curl http://localhost:8080
   Hello World!
   ```

   这样就表示我们编写的代码没问题了。

2. 创建Dockerfile文件，一般是放在工程的根目录下（即knative-demo文件夹下）。

   ```dockerfile
   # Use the official maven/Java 8 image to create a build artifact: https://hub.docker.com/_/maven
   FROM maven:3.5-jdk-8-alpine as builder
   
   # Copy local code to the container image.
   WORKDIR /root/knative/helloworld-java/knative-demo
   COPY pom.xml .
   COPY src ./src
   
   # Build a release artifact.
   RUN mvn package -DskipTests
   
   # Use the Official OpenJDK image for a lean production stage of our multi-stage build.
   # https://hub.docker.com/_/openjdk
   # https://docs.docker.com/develop/develop-images/multistage-build/#use-multi-stage-builds
   FROM openjdk:8-jre-alpine
   
   # Copy the jar to the production image from the builder stage.
   COPY --from=builder /root/knative/helloworld-java/knative-demo/target/knative-demo-*.jar /helloworld.jar
   
   # Run the web service on container startup.
   CMD ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/helloworld.jar"]
   ```

   （注意替换上面你自己工程目录路径）。

3. 构建镜像，推送到你的docker hub

   在上述Dockerfile文件的路径下，执行以下命令来进行build:

   ```shell
   # abreaking是我自己的docker hub用户名，helloworld-java是自定义的镜像名
   docker build -t abreaking/helloworld-java .
   ```

   build时间有点长，完毕之后推送到你自己的docker hub仓库里：

   ```shell
   docker push abreaking/helloworld-java
   ```

   推送成功后，就可以在dockerhub中看到你的镜像

   ![image-20211110094614167](https://blog.abreaking.com/upload/2021/11/5beel2fbnmhqkqqc7jr1n802sa.png)

## 部署应用

我们前面自己编写的demo在knative看来，叫做函数（function），因为knative是一个faas（服务及函数）平台。

然后我们就可以来进行部署，knative在部署应用的过程中，通常会做三件事：

* 创建一个新的并且不可变更的版本；（这个很重要，后面涉及到一些灰度策略）
* 在网络层面，会自动为应用创建好路由route、流量入口ingress、服务service以及负载均衡策略loadblanceer。
* 在k8s层面，会自动为该应用的pod扩容或缩容，如果你的服务没有被调用，会自动缩容到零。

### 编写yaml

创建一个新的文件名`service-java.yaml`，内容如下：

```yaml
apiVersion: serving.knative.dev/v1
kind: Service
metadata:
  name: helloworld-java-spring
  namespace: default
spec:
  template:
    spec:
      containers:
      # 这里就是镜像文件地址
        - image: docker.io/abreaking/helloworld-java
          env:
            - name: TARGET
              value: "World"
```

### 部署

直接使用以下命令来部署该应用

```shell
$ kubectl apply -f service-java.yaml
service.serving.knative.dev/helloworld-java-spring created
```

### 验证

通过如下命令来验证我们部署的hello world：

```shell
$ kubectl get ksvc helloworld-java-spring
NAME                     URL                                                          LATESTCREATED                  LATESTREADY                    READY   REASON
helloworld-java-spring   http://helloworld-java-spring.default.10.105.59.0.sslip.io   helloworld-java-spring-00001   helloworld-java-spring-00001   True    

```

看到状态为true了，那么就表示部署成功了。

> 如果状态一直为revisionMissing，问题的原因可能就是该镜像的本身的问题，比如镜像根本就下不下来，你可以先手动`doucker pull 该镜像名`来验证下。
>
> 如果确认镜像本身没问题，那么你可能需要耐心等一会（最多也就两三分钟吧）。



### 使用

上面我们可以看到该helloworld有一个url，我们直接手动执行该url即可:

```shell
$ curl http://helloworld-java-spring.default.10.105.59.0.sslip.io
Hello World!
```

能输出`Hello World!`那么就代表整个安装部署就算成功了。

>第一次执行该url可能响应有点慢，原因很简单：我前面说过，没有调用时，应用会自动缩容到零。有调用时，就会先拉取代码、部署、启动分配pod等过程，所以第一次启动都会有点慢。





# 一些问题及处理

## istio安装失败，一些组件pending或者crashloopoff

istio的版本还是建议高版本，直接最新即可，官方给的建议是1.9.5版本；

其次，需要注意：istio对内存和cpu的要求比较严格，建议内存8G及以上，CPU8核及以上。

只要版本没问题，内存和CPU的配置达到要求，基本上istio安装没有问题。



## helloworld服务启动后，一直revisionMissing

这个问题有点无解，个人认为原因还是出在demo上，官方给的demo个人感觉还是有问题的，拉取下来居然好几百M。

后来我解决方式是自己制作demo，替换官方给的demo镜像，然后也出现过revisionMission的问题，多等了一会，然后就好了。



## istio-ingressgateway服务一直pending，EXTERNAL-IP为空

这个问题是出在minikube上。先看看你的istio-ingressgateway服务的type是不是LoadBalancer，如果是，还得需要minikube做一个操作：
开启一个新得窗口，运行命令：`minikube tunnel`。 然后就能解决。



# 参考资料

[https://knative.dev/docs/serving/](https://knative.dev/docs/serving/)

[https://istio.io/latest/docs/setup/install/istioctl/](https://istio.io/latest/docs/setup/install/istioctl/)

