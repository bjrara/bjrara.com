title: Reuse port and reuse address
date: 2016-06-27 21:58:29
tags:
---
Statement
---

This article is simply *a translation script* of [Mecki](http://stackoverflow.com/users/15809/mecki)'s answer of [Socket options SO_REUSEADDR and SO_REUSEPORT](http://stackoverflow.com/questions/14388706/socket-options-so-reuseaddr-and-so-reuseport-how-do-they-differ-do-they-mean-t).

[Reference]
http://stackoverflow.com/questions/14388706/socket-options-so-reuseaddr-and-so-reuseport-how-do-they-differ-do-they-mean-t

在阅读接下来的内容之前，首先需要知道，一个 TCP/UDP 连接主要由以下几个部分组成：
```java
<协议>,<源地址>,<源端口>,<目标地址>,<目标端口>
<protocol>, <src addr>, <src port>, <dest addr>, <dest port>
```
这五个要素构成一个唯一、可识别的连接。即，对于任意两个连接，他们的五个元素的值都不可能完全相同。

创建 socket 时，调用 socket() 函数设定 socket protocol 值，bind() 函数设定 src addr 和 src port 值，connect() 设定 dest addr 和 dest port 值。由于 UDP 是无连接协议，所以 UDP sockets 可以在没有建立连接的情况下使用，当然 UDP sockets 也允许建立连接，这有利于一些特定场景下的程序设计与运行。在无连接模式下，如果 UDP sockets 没有事先进行显式绑定的话，通过第一次数据传输，系统会自动进行绑定，以免 sockets 无法接收响应数据。同理，没有绑定的 TCP socket 也会在建立连接前自动进行绑定。

如果你显式进行 socket 绑定，那么绑定到 0 端口是有可能的（0 端口等于“任意端口”）。但事实上，由于 socket 无法绑定到系统所有已存在端口，所以还是需要选择指定端口进行绑定，通常端口的选择范围由系统事先定制预留。与此类似的还有 source addr 的 wildcard，IPv4 的 ``0.0.0.0`` 和 IPv6 的 ``::`` 都代表“任意地址”。不同之处在于， socket 可以绑定到任何一个地址，换句话说，可以绑定到任一本地接口的 src IP addr。但是，socket 无法在绑定本地 IP 地址的同时建立连接，因此如果 socket 连接不是立马创建的话，那么依照目标地址和路由表内容，系统需事先选择一个 src IP addr。这种 socket 绑定就不是任意绑定了，而是与特定 src IP addr 进行的绑定。

默认情况下，任意两个 socket 不会绑定到同一个 src addr 与 src port 的组合上。其实只要 src port 是不同的，src addr 可视为无关。

> 设：绑定 socketA 到 A:X，绑定 socketB 到 B:Y，A、B 是 address，X、Y 是 port。
当 X!=Y，两个绑定均为成功；
当 X==Y，若 A!=B，绑定成功，若 A==B 绑定失败。

例如，socketA 属于 FTP server 程序并绑定到 192.169.0.1:21，socketB 属于另外一个 FTP server 程序并绑定到 10.0.0.1:21，这是合法的。但是如果其中一个被绑定到了“任意地址”，如：0.0.0.0:21，那么这就意味着这个绑定是绑定到了所有已存在的本地地址，其他绑定无法再占用 21 端口——因为 0.0.0.0 是与所有已存在的本地 IP 地址冲突的。

### BSD
#### SO_REUSEADDR
在 socket 绑定前开启 SO_REUSEADDR，只要 socket 没有和另外一个 socket 绑定具有**完全相同**的 src addr 和 src port，则绑定成功。那这和之前提到的绑定规则有什么不同呢？

关键在于**完全相同**这一概念，SO_REUSEADDR 改变了“任意IP地址”（wildcard addresses）的匹配规则。

当没有开启 SO_REUSEADDR 时，绑定 socketA 到 0.0.0.0:21，再绑定 socketB 到 192.168.0.1:21 会失败（报错信息 EADDRINUSE)——因为 0.0.0.0 表示的是“任意地址”，所有本地 IP 地址都视作已被占用，其中包含 192.168.0.1。而开启 SO_REUSEADDR，socketB 的绑定就会成功，因为 0.0.0.0 和 192.168.0.1 不再被认为是**完全相同**，0.0.0.0 是所有本地地址的 wildcard 地址，而 192.168.0.1 则会被视作是另外一个特定的本地地址。

以上所说的所有内容，都是无所谓绑定的先后顺序的，不开启 SO_REUSEADDR，socketA、B 的绑定总有一个一定会失败，开启 SO_REUSEADDR，socketA、B 的绑定一定都是成功。

```
SO_REUSEADDR       socketA        socketB       Result
---------------------------------------------------------------------
  ON/OFF       192.168.0.1:21   192.168.0.1:21    Error (EADDRINUSE)
  ON/OFF       192.168.0.1:21      10.0.0.1:21    OK
  ON/OFF          10.0.0.1:21   192.168.0.1:21    OK
   OFF             0.0.0.0:21   192.168.1.0:21    Error (EADDRINUSE)
   OFF         192.168.1.0:21       0.0.0.0:21    Error (EADDRINUSE)
   ON              0.0.0.0:21   192.168.1.0:21    OK
   ON          192.168.1.0:21       0.0.0.0:21    OK
  ON/OFF           0.0.0.0:21       0.0.0.0:21    Error (EADDRINUSE)
```

上述图表假设 socketA 已完成与所分配地址的绑定，然后基于 SO_REUSEADDR 开关尝试进行 socketB 绑定，Result 列表示绑定结果。当第一列 SO_REUSEADDR 的值为 ON/OFF，则表示最终结果不受 SO_REUSEADDR 开关影响。


SO_REUSEADDR 不仅仅作用与 wildcard addr，该配置之所以在服务器程序被广泛运用还有一个非常重要的原因。在解释第二个原因之前，先来看看 TCP 协议的运作机制。

成功调用 send() 函数后，socket 会生成一个 send buffer，此时并不代表请求数据已经被成功发送出去，仅表示数据已经加入到 send buffer。对 UDP socket，即便数据没有即可发送出去，所等待的时间通常也非常短。但对 TCP socket 而言，从数据加入缓存到真正发送出去的延时相对会长很多。所以，当你关闭了一个 TCP socket，send buffer 中可能依然存在没有发送出去的缓存数据，但你的程序在调用 send() 后就认为数据已经发送了。如果 TCP 程序此时立即关闭 socket，所有的数据就会丢失，而你的程序对此毫无感知。我们常说 TCP 是可靠协议，但在丢失数据这件事上，TCP 显得没有那么可靠。这就是为什么，当你关闭一个包含需要发送但尚未发送数据的 socket 时，它的状态会被置为 **TIME_WAIT**。socket 会等待所有 buffer 中的数据发送完毕后才执行关闭，或等到 timewait 超时，强制关闭。

无论 socket 中是否包含尚未发送的数据，内核等待关闭 socket 的那段时间，被称为 Linger Time。Linger Time 在大部分系统中都是全局可配的，默认时间相对较长（大部分系统设置均为2分钟）。针对单个 socket 也可通过 SO_LINGER 进行配置。