title: Nginx SSL 证书配置与信任
date: 2015-10-20 12:38:01
categories: nginx
---
今天对 Nginx 证书配置进行了在使用反向代理场景下的模拟，发现以下现象：

Nginx 不对 upstream 的证书进行有效性验证；

Nginx 会对请求中的 hostname 信息与自己的 ssl 证书信息进行匹配和验证。

如果在 server 中有以下配置信息：

```bash
ssl on;
ssl_certificate		cert/a.com.crt;
ssl_certifivate_key	cert/a.com.key;

location / {
    # suppose 127.0.0.1:8888 is an https service having a cert with hostname info 'b.com'
    proxy_set_header HOST a.com;
    proxy_pass 127.0.0.1:8888;
}
```

当一个带有 b.com 的请求进来时，Chrome 报出了 NET::ERR_CERT_COMMON_NAME_INVALID 的证书不受信任警告。描述中表示 server 上的证书域名为 a.com，而请求的域名却是 b.com。

将请求域名改为 a.com 了以后，请求正常访问了 upstream service 并返回。

这里提出两个问题：

* 证书不受信任出现在请求响应的哪个环节？
* Nginx 作为反向代理是如何进行证书验证的？

针对问题1：

推荐文章[《Https(SSL/TLS)原理详解》](http://www.fenesky.com/blog/2014/07/19/how-https-works.html)

我们知道 http 协议是对 tcp 协议的一层封装，https 是对 http 的一层封装，添加了 SSL/TLS 协议封装。

这里通过浏览器动作扩展开讲一些模型的细节。

Chrome 通过 DNS 找到域名对应 IP 信息，然后建立两点连接。在 OSI 模型中，从发现 IP 节点到建立连接全部发生于 7 层协议的前三层。然后到了 tcp 所在的第四层-传输层(Transport layer)，用于建立 data segments / datagram 传输通道，tcp 在这基础上保证了可靠性，也是三次握手发生的原因。随后 Chrome 发起 https 请求，对应用层(Application layer)的解释有一个很形象的比喻，就是对某个具体的资源发出请求。例如，针对 http/https 来说，这个资源是某个具体的 url，对 ftp 来说是某个具体的文件等等。好了，由于 Chrome 发起的是一个 https 请求，不同于 http 可直接进行数据交流，需要进行 TLS 验证。TLS 一共有四个步骤(具体请参考推荐文章)，主要包括：**确立加密算法、确认域名以及证书有效性(签名与有效期)**。

分析到这里，“证书不受信任出现在哪一环节”就很清楚了：客户端发送请求后，服务端接收请求前的 TLS 证书验证阶段。

针对问题2：

其实从之前的测试行为就可以发现，Nginx 作为反向代理将一个请求拆分成了两阶段。

第一阶段，客户端向 Nginx 发送 https 请求，Nginx 作为服务端与其进行了一次证书验证。所以当我们在 conf 中包含了 a.com 的证书信息后，发送对 b.com 域名的请求会被处以警告。

第二阶段，Nginx 作为客户端向 upstream 发起 https 请求。你会不会奇怪为什么 Nginx 以 a.com 去访问一个包含 b.com 证书的服务却没有返回警告呢？我怀疑 Nginx 强制忽略了证书警告信息，严格来说是有不安全隐患的。