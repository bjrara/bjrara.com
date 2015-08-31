title: Nginx as website keeper
date: 2015-08-31 12:47:38
categories: nginx
---
> copyright by Mengyi Zhou (bjrara) - since 2015
 
This article is a quick guide to build Nginx as your personal website keeper. It introduces Nginx into your website project as a forward proxy, securing your fragile web (services) against potential attacks. We use [Tengine](https://github.com/alibaba/tengine) which is extended from Nginx and has many advanced features free of charge.

References: 
======

		囧思九千 - 通过nginx配置文件抵御攻击 (http://drops.wooyun.org/tips/734) 
        JeremyWei - 利用Proxy Cache使Nginx对静态资源进行缓存 (http://weizhifeng.net/nginx-proxy-cache.html)

Target Readers: 
======
* Lazybones

Prerequisites:
======
* A vps/server running linux/osx system

For linux users, making sure packages in the following are installed:
* gcc-c++
* pcre, pcre-devel
* lua, lua-devel (5.1)

For osx users, making sure packages in the following are installed:
* c++ compiler (installing xcode is recommended)
* pcre
* lua (5.1)


Steps:
======
* Download Tengine-2.1.1 from [Tengine-official-website](http://tengine.taobao.org/download.html)
* Extract the package:
```sh
$ tar zcf Tengine-2.1.1.tar.gz ./
```
* Edit nginx.conf (scroll down to see details)
* Configure nginx with lua module
```sh
$ cd Tengine-2.1.1
$ sudo ./configure --with-http_lua_module
```
* Compile and install (Nginx is installed at /usr/local by default)
```sh
$ sudo make
$ sudo make install
```
* Start, reload, stop Nginx
```sh
$ cd /usr/local/nginx/sbin
$ sudo ./nginx
$ sudo ./nginx -s reload
$ sudo ./nginx -s quit
```

Editing nginx.conf to do the magic:
======
//TODO