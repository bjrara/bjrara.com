title: Spring circular reference
date: 2015-11-02 19:57:36
categories: lovely java
---

问题表现：

我在项目里定义了一个 AOP 类（以下面为例 SomeAspect），并在其中引入一个包含循环依赖的资源 circularService。
为了让 Spring 发现并管理这个切面类，我们通过 Annotation 将其声明为一个 **具备 AspectJ 风格** 的 Spring component 资源。如，

```java
@Component
@Aspect
public class SomeAspect {

    // This resource contains injection that has circular depencies for example.
    @Resource
    private CircularService circularService;

    @Around("execution(* com.bjrara.test..*Repository.*(..)) || " +
            "execution(* com.bjrara.test..*Service.*(..))")
    public Object validate(ProceedingJoinPoint point) throws Throwable {
    	return point.proceed();
    }
}

```
在 spring 的 properties 文件中：

```properties
<context:component-scan base-package="com.bjrara.test"/>
<!-- use cglib -->
<aop:aspectj-autoproxy proxy-target-class="true"/>
```

启动服务器 Spring 报出 [BeanCurrentlyInCreationException] 的错误。

所谓 BeanCurrentlyInCreationException，主要是由于 Spring 在创建 bean 的时候，发现了直接/间接的相互引用，而 Spring 却不知道该先创建哪个造成。比如A引用了 B，B引用了C，C引用了A。

Spring 官方文档对这个问题的解释大致如下：

* 如果使用的是 Setter injection，no prob，Spring 会处理这个问题，对象被正常创建；
* 如果使用的是 Constructor injection，sorry，这个问题必须打破循环才能解决。

为什么会出现这样的差别？那就需要明白 Spring 是如何进行对象创建的。

Spring 创建 bean 主要通过两种途径，一种是 JDK dynamic proxies，另一种是使用 cglib。无论使用哪一种方式(无非一个使用 interface proxy，一个使用 subclass proxy)，Spring 在创建 bean 前，都会对 config 内容进行分析校验，会发现诸如对 non-existent beans 的引用，或者 circular dependencies 的问题，并尽可能延迟 set property 和处理引用问题的时间。当对象都创建完毕后，直到真正调用对象时，Spring 会再对空引用抛出异常。

// Reference [3.4.1.3 Dependency resolution process](http://docs.spring.io/spring/docs/3.0.x/spring-framework-reference/html/beans.html#beans-factory-collaborators)
> You can generally trust Spring to do the right thing. It detects configuration problems, such as references to non-existent beans and circular dependencies, at container load-time. Spring sets properties and resolves dependencies as late as possible, when the bean is actually created. This means that a Spring container which has loaded correctly can later generate an exception when you request an object if there is a problem creating that object or one of its dependencies.

在进行 Setter injection 的时候，Bean 已经被成功创建，因此循环引用不会造成 BeanCreation 失败。而通过 Constructor injection，我们预设了某个 Bean 被创建的前提必须依赖另外一个 Bean 已经被创建，因此出现之前所说的“Spring 不知道该先创建哪个”的问题。

于是，我核对了一遍代码，确认并未使用 Constructor injection (Jersey @Resource 全部基于 Setter injection)，那么为什么还会报出 BeanCurrentlyInCreationException！法克...

由于 Resource 本身并不会导致这个问题，所以我开始从 AspectJ 着手排查问题，我起初以为 AspectJ 的创建可能是一种 constructor injection，但是，根据 Spring 的文档：

> @AspectJ refers to a style of declaring aspects as regular Java classes annotated with Java 5 annotations. The @AspectJ style was introduced by the AspectJ project as part of the AspectJ 5 release. Spring 2.0 interprets the same annotations as AspectJ 5, using a library supplied by AspectJ for pointcut parsing and matching. The AOP runtime is still pure Spring AOP though, and there is no dependency on the AspectJ compiler or weaver.


注意，**@AspectJ 只是一种注入风格，使用 @AspectJ 并不会使用 aspectj 进行织入**，那么问题来了，为什么明明只是一种风格，却会导致 Spring 创建 bean 时注入失败呢？

原因在于：aspect 的 scope 为 singleton 模式。对于普通的 resource 资源，只有在需要的时候才会被创建出来，而对于 singleton 资源，在 Spring container 创建之初就会被创建出来。这里可能会发生类似 constructor injection 循环依赖导致创建 bean 失败的问题。

[Reference](http://docs.spring.io/spring/docs/current/spring-framework-reference/html/aop.html#aop-instantiation-models)
> By default there will be a single instance of each aspect within the application context. AspectJ calls this the singleton instantiation model.

[Reference](http://docs.spring.io/spring/docs/current/spring-framework-reference/html/beans.html#beans-dependency-resolution)
> Beans that are singleton-scoped and set to be pre-instantiated (the default) are created when the container is created. Otherwise, the bean is created only when it is requested.

为了让 aop bean 能正常运作，需要做一层跳转：

在配置文件中将 SomeAspect 显式声明为 aop 对象（class 文件中 Annotation 的声明依旧包含了 @Component 和 @Aspect）：
```properties
<context:component-scan base-package="com.ctrip.zeus"/>
<!-- use cglib -->
<aop:aspectj-autoproxy proxy-target-class="true"/>
<aop:config>
    <aop:aspect id="someAspect" ref="someAspect"/>
</aop:config>
```

使用 aop:config 将 someAspect 这个普通的 spring java bean 通过 someAspect 转化成一个 aspect 对象。以此，someAspect 的 field injection 与其他 bean injection 就不存在任何区别。
> The bean backing the aspect ("someAspect" in this case) can of course be configured and dependency injected just like any other Spring bean.

p.s. 如果要使用 aspectj 织入方式的话请参考 [10.8.3 Configuring AspectJ aspects using Spring IoC](http://docs.spring.io/spring/docs/current/spring-framework-reference/html/aop.html#aop-aj-configure)