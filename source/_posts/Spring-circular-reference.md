title: Spring circular reference
date: 2015-11-02 19:57:36
categories: lovely java
---
> 我靠，这简直是个大坑。

问题表现：启动服务器，Spring 报出 [BeanCurrentlyInCreationException]

出现这个异常，主要是由于 Spring 在创建 bean 的时候，发现了直接/间接的相互引用，而 Spring 却不知道该先创建哪个造成。比如A引用了 B，B引用了C，C引用了A。

当时碰到这个问题的时候还在想，不是吧，Spring 连这种常见问题都没解决么。么。。么。。

我怀着对 Spring 的无限鄙视去验证这个问题，官方文档大致是这样解释的：

* 如果使用的是 Setter injection，no prob，Spring 会处理这个问题，对象被正常创建；
* 如果使用的是 Constructor injection，sorry，这个问题必须打破循环才能解决。

为什么会出现这样的差别？那就需要明白 Spring 是如何进行对象创建的。

Spring 创建 bean 主要通过两种途径，一种是 JDK dynamic proxies，另一种是使用 cglib。无论使用哪一种方式(无非一个使用 interface proxy，一个使用 subclass proxy)，Spring 在创建 bean 前，都会对 config 内容进行分析校验，会发现诸如对 non-existent beans 的引用，或者 circular dependencies 的问题，并尽可能延迟 set property 和处理引用问题的时间。当对象都创建完毕后，直到真正调用对象时，Spring 会再对空引用抛出异常。

// Reference [3.4.1.3 Dependency resolution process](http://docs.spring.io/spring/docs/3.0.x/spring-framework-reference/html/beans.html#beans-factory-collaborators)
> You can generally trust Spring to do the right thing. It detects configuration problems, such as references to non-existent beans and circular dependencies, at container load-time. Spring sets properties and resolves dependencies as late as possible, when the bean is actually created. This means that a Spring container which has loaded correctly can later generate an exception when you request an object if there is a problem creating that object or one of its dependencies.

在进行 Setter injection 的时候，Bean 已经被成功创建，因此循环引用不会造成 BeanCreation 失败。而通过 Constructor injection，我们预设了某个 Bean 被创建的前提必须依赖另外一个 Bean 已经被创建，因此出现之前所说的“Spring 不知道该先创建哪个”的问题。

这个是最主流的 common cases，然后说下我碰到的问题。

我核对了一遍代码，确认并未使用 Constructor injection (Jersey @Resource 全部基于 Setter injection)，那么为什么还会报出 BeanCurrentlyInCreationException！法克...

没办法，这种情况只能打破循环链一个个节点测，最后发现只有当在 **AspectJ** 中引入循环依赖的时候，才会导致失败异常。这对我真是里程碑式的重大发现...

后来在另外一篇文章里发现了同样的表述，我为什么没有早点发现这篇文章 T^T：

> 普通使用是没有问题，但在切面环境下就会引发该错误，因代理类在使用时可能会引发死循环。尽量产生使用循环引用的。难以避免的情况下，切面pointcut设置避开这两个bean。如果不能避开，则可将其中一个bean设置为延迟加载 lazy-init="true"。

那么问题来了，为什么 AspectJ 会导致在出现 Circular dependency 的时候 Bean Creation 的失败？StackOverflow 上有一个答案：`The aspect is a singleton object and is created outside the Spring container`。结论已经有了，听着还挺靠谱，所以要找到证据：

// Reference [10.8.3 Configuring AspectJ aspects using Spring IoC](http://docs.spring.io/spring/docs/current/spring-framework-reference/html/aop.html#aop-aj-configure)
> The AspectJ runtime itself is responsible for aspect creation, and the means of configuring the AspectJ created aspects via Spring depends on the AspectJ instantiation model (the 'per-xxx' clause) used by the aspect.

//TODO 写困了。。