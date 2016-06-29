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

如果你显式进行 socket 绑定，那么绑定 0 端口理论上是可行的（0 端口等于“任意端口”）。但实际上由于 socket 不可能绑定到系统所有已存在端口，所以还是需要选择指定端口进行绑定，通常端口的选择范围由系统事先定义预留。与此类似的还有 source addr 的 wildcard，IPv4 的 ``0.0.0.0`` 和 IPv6 的 ``::`` 都代表“任意地址”。不同之处在于， socket 绑定“任意地址”具备可操作性，“任意地址”表示所有本地接口的 src IP addr。但是，socket 绑定本地 IP 地址和建立连接这两件事无法同时发生，因此如果 socket 连接不是立马创建的话，那么依照目标地址和路由表内容，系统需事先选择一个 src IP addr。这种 socket 绑定就不是绑定“任意地址”了，而是与特定 src IP addr 进行的绑定。

默认情况下，任意两个 socket 不会绑定到同一组 src addr 与 src port 地址。其实只要 src port 不同，src addr 就是无关的。

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


SO_REUSEADDR 不仅仅作用与 wildcard addr，该配置之所以在服务器程序中被广泛运用还有一个非常重要的原因。在解释第二个原因之前，首先，先简单介绍 TCP 协议的运作机制。

成功调用 send() 函数后，socket 会生成一个 send buffer，此时请求数据是否已发送尚不可知，这一阶段仅表示数据已添加到 send buffer。对 UDP socket，即便数据没有即刻发送出去，所等待的时间通常也非常短。但对 TCP socket 而言，从数据加入缓冲区到真正发送的延时相对会长很多。所以，当你关闭了一个 TCP socket，send buffer 中可能依然存在没有发送的缓冲数据，但你的程序在调用完 send() 就视作数据已经发送了。如果此时 TCP 程序立即关闭 socket，就会导致数据丢失，而你的程序却对此毫无感知。我们常说 TCP 是可靠协议，但在丢失数据这件事上，TCP 就显得没有那么可靠。这也是为什么，当你关闭一个包含需要发送但尚未发送数据的 socket 时，它的状态会被置为 **TIME_WAIT**。socket 会等待所有 buffer 中的数据发送完毕后才执行关闭，或等到 timewait 超时，强制关闭。

无论 socket 中是否包含尚未发送的数据，内核等待关闭 socket 的那段时间，被称为 Linger Time（逗留时间）。Linger Time 在大部分系统中是全局可配的，默认时间相对较长（大部分系统设置为2分钟）。针对单个 socket， 也可通过 socket 选项 SO_LINGER 配置 timeout 时间，甚至完全关闭逗留功能。但是需要指出，优雅地关闭一个 TCP socket 是个较为复杂的过程，其中还包含了来回传送多个数据包（失败会进行包重传），整个过程都局限在 Linger Time 时间内。彻底禁止内核逗留等待，是极为不推荐的做法——这不仅会导致丢失尚在缓冲区里的数据，并且意味着 socket 被强制关闭。如何优雅地关闭一个 TCP 连接已超出本文讨论范围，如果你对此有兴趣，请参考[连接](http://www.freesoft.org/CIE/Course/Section4/11.htm)。即便你真的通过 SO_LINGER 配置禁止 lingering 功能，当你的进程在没有显式关闭 socket 时发生异常终止或僵死，BSD（或其他类似系统）的内核也还是会进入逗留等待。你无法保证 socket 在任何情况下都会彻底关闭，而不逗留在外（linger）。

问题是，系统是如何对待处于 TIME_WAIT 状态的 socket 呢？

如果没有配置 SO_REUSEADDR，处于 TIME_WAIT 状态的 socket 依然被视作绑定在 src addr 和 src port 上，在当前 socket 真正关闭前，任何尝试绑定到同一组 src addr 和 port 上的 socket 都会失败，等待时间与所配置的 Linger Time 大致相仿。所以不要期待在关闭 socket 后，就可以立即重新绑定 src addr。在大部分情况下都会出现问题。但是，如果对正在进行绑定的 socket 配置 SO_REUSEADDR，那么原先绑定在同一组 src addr 和 port 但目前已处于 TIME_WAIT 状态的 socket 就会被忽略（，毕竟它已经在一种“半死亡”状态），这种情况下，是可以完成新 socket 绑定的。值得注意的是，当绑定一个 socket 至一组存在 TIME_WAIT socket 的 src addr 和 port，且恰恰这个 TIME_WAIT socket 还在工作的话，可能会产生一些不可预期的问题。但这不在我们今天的讨论范围内，而且庆幸的是，发生这种问题的概率在实际中非常少见。

最后一点，以上所述的所有内容，只针对当下需要进行绑定的 socket 是否开启了 address reuse 功能，无所谓其他 sockets 有没有进行配置。换句话说，判断绑定是否成功的依据，基于且仅基于等待绑定的这个 socket 是否设定了 SO_REUSEADDR 配置。

#### SO_REUSEPORT
大部分人所期待的 SO_REUSEADDR 其实是 SO_REUSEPORT。SO_REUSEPORT 允许你绑定任意多 sockets 到同一组具有完全相同 src addr 和 src port 的源地址上，前提是**所有**先前已绑定的 sockets 在绑定前都进行了 SO_REUSEPORT 配置。如果第一个绑定到这一组 src addr 和 port 上的 socket 没有开启 SO_REUSEPORT，那么无论其他 sockets 是否配置了 SO_REUSEPORT，在第一个 socket 被释放前，都无法完成绑定。与 SO_REUSEADDR 不同，SO_REUSEPORT 的实现不但校验当前正在进行绑定的 socket 的 SO_REUSEPORT 值，还校验与之有冲突源地址（src addr 和 src port）的 sockets 的 SO_REUSEPORT 值。

SO_REUSEPORT 不等同于 SO_REUSEADDR。如果一个 socket 绑定时没有进行过 SO_REUSEPORT 设置，第二个进入的 socket 即便进行了 SO_REUSEPORT 设置，也无法完成绑定。哪怕先前的那个 socket 已经处于 TIME_WAIT 的半死亡状态，绑定也依然会失败。为了能够将一个 socket 绑定到与其他 sockets 有冲突的同一组源地址，要么新 socket 在绑定前需要配置 SO_REUSEADDR，要么所有 sockets 都需要在绑定前配置 SO_REUSEPORT。当然，在一个 socket 上同时开启两项配置也是可以的。

#### Connect() 返回 EADDRINUSE
我们知道 bind() 有时会返回 EADDRINUSE 错误。但是，你可能会觉得奇怪为什么开启 address reuse 后，connect() 方法也会返回同样的错误？一个远程（remote）地址，简单地说就是 socket 里的连接对象，怎么会已经在使用中了呢？在开启 address reuse 前，多个 sockets 连接同一个远程地址从未报错，怎么现在就有问题了呢？

我在本文一开始就说过，一个连接由五大要素组成。我也说过，这五要素的组合必须唯一，否则系统无法识别。

好，开启 address reuse 后，你可以把两个相同协议的 sockets 绑定到同一组 src addr 和 src port 上。这就意味着，五个元素中已经有三个元素设置了一样的值。如果你现在又尝试把这两个 sockets 连接到同一组目标地址和目标端口（dest addr 和 dest port）上，这就等于你所创建的这两个连接，五个元素的值完全一致。至少对 TCP 连接（UDP 本身就不是真正的连接）而言，系统在获得数据后，是无法识别这些数据到底属于哪个连接的。因此，为使系统能够区分数据归属，至少 dest addr 或 dest port 要有所不同。

所以说，如果你尝试绑定两个相同协议、相同 src addr 和 src port 的 sockets 到相同 dest addr 和 dest port，第二个 socket 就会报 EADDRINUSE 的错误，告诉你另外一个具有相同值的 socket 已完成建立连接。


### FreeBSD/OpenBSD/NetBSD
#### Linux
在 Linux 3.9 版本前，只有 SO_REUSEADDR 配置项。该配置在 Linux 与 BSD 基本保持一致，只有两点重要区分：

一是如果监听 TCP socket（server 端）已经绑定到了一个 wildcard IP 地址和某个特定端口，其他 TCP sockets 就无法绑定到同一端口，无论那些 sockets 是否开启了 SO_REUSEADDR 配置。就算 BSD 系统允许，只要是同一端口，哪怕要绑定的源地址是一个具体的 IP 地址也不行。但是，该限制不会对非监听 TCP sockets（client 端）生效。此外，如果先绑定 TCP socket 到某个具体 IP 地址和端口，然后再绑定另一 socket 到 wildcard 地址和同一端口是可以的。

二是针对 UDP sockets，在 BSD 中 SO_REUSEADDR 与 SO_REUSEPORT 行为完全一致。所以开启 SO_REUSEADDR，两个 UDP sockets 可以绑定到同一组地址和端口。

Linux 3.9 版本后加入 SO_REUSEPORT 配置项。它允许两个（或以上）监听（server 端）或非监听（client 端）TCP/UDP sockets 绑定到同一组地址和端口上，前提就是所有 sockets 在绑定前都开启了 SO_REUSEPORT。为防止端口劫持，有一个特殊限制：所有共享同一组地址和端口的 sockets 所属的进程，也必须归属于同一 user ID。这样一来，用户就无法“偷”另一用户的端口。

除以上几点，内核对 SO_REUSEPORT sockets 也进行了特殊处理，这在其他操作系统中还没见到过。对 UDP sockets，系统会尽量均摊数据报(datagrams)；而对 TCP sockets，系统会尽量均摊请求连接（指通过调用 accept() 接收的请求连接）到所有共享一组地址和端口的 sockets 上。这表示，在其他支持 address reuse 的操作系统中，分配 socket 接收数据报或请求是较为随机的操作，而在 Linux 系统中，尝试优化了分配策略，以保证即便是简单的服务器进程也能通过使用 SO_REUSEPORT 来达到类似负载均衡的效果。