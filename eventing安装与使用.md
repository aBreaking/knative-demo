# Eventing简述

Knative Eventing充当架构不同部分之间的`glue（粘合剂）`，使得你应用架构不同部分之间可以轻松进行通信，并具备较好的容错（fault-tolerant）等特性。一些使用场景例子如下：

* 创建和响应Kubernetes API事件；
* 创建一个图像处理地管道（pipeline）；
* 一些AI边缘地场景；

所以说，一些简单实现，甚至特别复杂的事件驱动场景，都可以考虑用Knative Eventing来实现。

对于knative的组件来说，目前可以先了解最基本的几个组件：

* Sources：事件源，向 Broker 发出事件的 Kubernetes 自定义资源；
* Brokers：事件中心，用于发送事件；
* Trigger：触发器，对broker里的事件进行过滤，也可以配置事件所需的属性；
* Sinks：事件最终到达的目的地。

其关系图如下：

![image-20211116151452805](https://blog.abreaking.com/upload/2021/11/r1sd62c1imjeqrkrv72b71bphl.png)



> knative的服务即可以作为事件的source也可以作为事件的sink，理由很简单，比如你可能想从broker来消费某些事件，或者将修改后的事件发送回到broker里，如同在一些管道（pipeline）用例中一样。

# Eventing的安装

之前的文章已经介绍过了serving的安装。如果没有安装serving，需要先安装serving，见：[Knative1.0版本初探——serving、istio的安装与函数部署](https://blog.abreaking.com/article/156)

eventing的安装同样不太难，参考官网的安装过程：[Knative Install Eventing with YAML](https://knative.dev/docs/install/eventing/install-eventing-with-yaml/)。

同样的，需要访问k8s的镜像库，你可能得需要找个[梯子](https://www.baidu.com)或者[自己制作镜像]([使用阿里云镜像服务来代理k8s镜像 - 知乎 (zhihu.com)](https://zhuanlan.zhihu.com/p/429685829))。

只是官网给的broker安装描述可能有点不够清楚，我补充了下。

## 基本安装

安装所需的CRDs以及核心组件：

```shell
$ wget https://github.com/knative/eventing/releases/download/knative-v1.0.0/eventing-crds.yaml
$ kubectl apply -f eventing-crds.yaml
$ wget https://github.com/knative/eventing/releases/download/knative-v1.0.0/eventing-core.yaml
$ kubectl apply -f eventing-core.yaml
```

此时就可以看到eventing主要组件的pod信息，如下：

```shell
$ kubectl get pods -n knative-eventing
NAME                                   READY   STATUS    RESTARTS   AGE
eventing-controller-7995d654c7-qg895   1/1     Running   0          2m18s
eventing-webhook-fff97b47c-8hmt8       1/1     Running   0          2m17s
```



## 安装Channel

Channels是Kubernetes Custom Resources，可以看作就是消息通道，类似消息队列（MQ）的模式。它定义了一个单一的事件转发和持久层。消息实现可以通过Kubernetes Custom Resource提供Channel的实现，支持不同的技术，比如kafaka、google cloud Pub/Sub channel、In_Memory、NATS Channel等等。

本地学习环境，我们只需要安装最简单的In-Memory的Channel即可。

```shell
$ wget https://github.com/knative/eventing/releases/download/knative-v1.0.0/in-memory-channel.yaml
$ kubectl apply -f in-memory-channel.yaml
```



## 安装Broker 

作为事件的中心（hub），事件被发送到Broker的入口（Ingress），然后被发送到对该事件感兴趣的任何订阅者。

在本地学习环境，我们选择一个内存的broker来进行安装：

```shell
$ wget https://github.com/knative/eventing/releases/download/knative-v1.0.0/mt-channel-broker.yaml
$ kubectl apply -f mt-channel-broker.yaml
```

然后，手动创建一个基于内存的broker，如下，文件名`imc-broker.yaml `

```yaml
apiVersion: eventing.knative.dev/v1
kind: Broker
metadata:
  annotations:
    eventing.knative.dev/broker.class: MTChannelBasedBroker
  name: example-broker
  namespace: default
spec:
  config:
    apiVersion: v1
    kind: ConfigMap
    name: config-br-default-channel
    namespace: knative-eventing

```

而后：

```shell
$ kubectl apply -f imc-broker.yaml
```



> 官网也提供其他broker的安装，比如kafka，但是前提是你得需要先安装kakfa的channel。



## 安装验证

到这里，eventing的基本组件就安装完毕了。此时，我们可以通过相关命令来验证我们安装是否成功。

* 验证eventing的组件安装情况：

```shell
$ kubectl get pod -n knative-eventing
NAME                                    READY   STATUS    RESTARTS      AGE
eventing-controller-6f6c9bb95c-lgblm    1/1     Running   4 (19m ago)   43h
eventing-webhook-59bd9f7dc9-hmmcf       1/1     Running   6 (19m ago)   43h
imc-controller-7976d976f8-t7p2d         1/1     Running   6 (19m ago)   43h
imc-dispatcher-74fdc75c4d-2czqw         1/1     Running   7 (15m ago)   43h
mt-broker-controller-6bcdfdf495-47lxx   1/1     Running   2 (19m ago)   26h
mt-broker-filter-58c4658887-5qdjq       1/1     Running   3 (19m ago)   26h
mt-broker-ingress-59b454bc59-pdqwr      1/1     Running   3 (19m ago)   26h

```

*imc就是之前安装channel，状态不为Running，基本是镜像拉取不到的问题*



* 验证Broker

```
root@kube-PC:~/knative# kubectl get broker 
NAME             URL                                                                               AGE   READY   REASON
example-broker   http://broker-ingress.knative-eventing.svc.cluster.local/default/example-broker   26h   True 
```

此时我们就可以看到一个正常状态的broker，broker名为`example-broker`。

# 使用

## CloudEvents Player

官方提供了一个简单的有可视化页面的CloudEvents Player，可以学习Knative Eventing的一些核心概念。其体系结构图如下：

![image-20211117164345812](https://blog.abreaking.com/upload/2021/11/ro9gh4d170gh1rcqlp6jab61h2.png)

创建一个CloudEvents Player服务，创建一个yml，名为`cloudevents-player.yaml`，内容如下：

```yml
apiVersion: serving.knative.dev/v1
kind: Service
metadata:
  name: cloudevents-player
spec:
  template:
    metadata:
      annotations:
        autoscaling.knative.dev/minScale: "1"
    spec:
      containers:
        - image: ruromero/cloudevents-player:latest
          env:
            - name: BROKER_URL
              value: http://broker-ingress.knative-eventing.svc.cluster.local/default/example-broker
```

而后执行命令：

```shell
$ kubectl apply -f cloudevents-player.yaml
service.serving.knative.dev/cloudevents-player created
```

然后就通过如下命令可以看到该服务：

```
$ kubectl get ksvc
NAME                 URL                                                         LATESTCREATED              LATESTREADY                READY   REASON
cloudevents-player   http://cloudevents-player.default.10.103.151.219.sslip.io   cloudevents-player-00001   cloudevents-player-00001   True    

```

如上，我们看到了一个url，直接在浏览器中打开该url，效果如下：

![image-20211117164904989](https://blog.abreaking.com/upload/2021/11/pvhblbdnkci6vor58bdd3gn3bb.png)

随便输入，你会发现`status`总是`>`箭头符号。它表示我们指定一个事件源，但是没有被接收。

如果被接收呢？很明显，需要指定一个触发器`Trigger`。

## Trigger

创建一个简单的Trigger，先创建一个yaml文件，名为`ce-trigger.yaml`，内容如下：

```yaml
apiVersion: eventing.knative.dev/v1
kind: Trigger
metadata:
  name: cloudevents-trigger
  annotations:
    knative-eventing-injection: enabled
spec:
  broker: example-broker
  subscriber:
    ref:
      apiVersion: serving.knative.dev/v1
      kind: Service
      name: cloudevents-player
```

而后创建：

```shell
$ kubectl apply -f ce-trigger.yaml
trigger.eventing.knative.dev/cloudevents-trigger created
```

也可以验证一下：

```shell
$ kubectl get trigger
NAME                  BROKER           SUBSCRIBER_URI                                        AGE   READY   REASON
cloudevents-trigger   example-broker   http://cloudevents-player.default.svc.cluster.local   26h   True    

```



然后，再次进入cloudevents-player页面，刷新一下，重新创建一个event，你会发现多了一条。

![image-20211117165617343](https://blog.abreaking.com/upload/2021/11/pj5f1fsoauhlcoo10otlimltdu.png)



Status为`√`表示事件已经被正常的接收了。

>所以有人将这种模式称之为`Event-Driven Architecture`（事件驱动架构），它可以用来在Kubernetes上创建`FaaS as a Service`（ 函数即服务）。



# 参考资料

[https://skyao.io/learning-knative/introduction/](https://skyao.io/learning-knative/introduction/)

[https://knative.dev/docs/install/eventing/install-eventing-with-yaml/](https://knative.dev/docs/install/eventing/install-eventing-with-yaml/)

[https://knative.dev/docs/getting-started/getting-started-eventing/](https://knative.dev/docs/getting-started/getting-started-eventing/)