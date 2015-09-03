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
* Extract the package, configure (with lua module), compile and install
```sh
$ tar zcf Tengine-2.1.1.tar.gz ./
$ cd Tengine-2.1.1
$ sudo ./configure --with-http_lua_module
$ sudo make
$ sudo make install

// Nginx is installed at /usr/local by default
$ cd /usr/local/nginx/sbin
$ sudo ./nginx
```
Visit http://localhost, you should be able to see the nginx welcome page.

* Edit nginx.conf to do the magic:

*If you don't have the very basic idea of how nginx works using nginx.conf, I recommend you read the [Beginners' Guide](http://nginx.org/en/docs/beginners_guide.html) first before moving on.*

Open the nginx.conf file,
```sh
cd /usr/local/nginx/conf/nginx.conf
```

### Configure your website host as upstream
Let's suppose your Apache host is running on port 8080 with your website deployed.

In your nginx.conf, configure it as an upstream server under **http** context:
```javascript
upstream apache_servers {
    server 127.0.0.1:8080;
}
```

### Configure request entries routing to your backend website
In general, we simply route all the incomming requests to our website.

**Replace all the ${...} with your own values.**

```javascript
server {
    listen       80;
    server_name  localhost ${your_website_hostname};
    location / {
        proxy_pass http://apache_servers;
    }
}
```

### Limitate incomming requests
*Implementation is all reserverd by [the link](http://drops.wooyun.org/tips/734).*

Define limit_req_zone under **http** context:
```javascript
limit_req_zone $cookie_token zone=session_limit:10m rate=30r/s;
```
Description:

*limit_req_zone creates a shared memory zone that will keep states for the given key. In particular, the state stores the current number of excessive requests.*

In this case, a zone named "session_limit" would keep all the requests by key $cookie_token in a 10 megabyte shared memory, and an average request processing rate for this zone cannot exceed 30 request per second.

Under location / , we introduce lua scripts to make more intelligent strategies:

Here, we add a new token with ip address and a random number from 0-999999 encrypted by md5 to visitors' session. Line `limit_req zone=session_limit burst=50 nodelay;` states if the requests rate exceeds the rate configured for **session_limit**, their processing will be terminated such that requests are processed at a defined rate.

```javascript
location / {
	rewrite_by_lua '
    	local random = ngx.var.cookie_random;
        if (random == nil)
        then
        	random = math.random(999999)
        end
        local token = ngx.md5("donthack" .. ngx.var.remote_addr .. random)
        if (ngx.var.cookie_token ~= token)
        then
        	ngx.header["Set-Cookie"] = {"token=" .. token, "random=" .. random}
            return ngx.redirect(ngx.var.scheme .. "://" .. ngx.var.host .. ngx.var.uri)
        end
        ';
        limit_req zone=session_limit burst=50 nodelay;
        proxy_pass http://apache_servers;
}
```

The whole configuration tells nginx if it is a "nice" visitor, routes his/her requests to port 8080 and visits the website; if the user with the same cookie continues requesting the website for more than 30 requests per second, declines all the excessive requests all at once, put him to "bad" visitor list until the rate comes down to the expected 30 r/s and makes him out again.

### Cache static resources
//TODO

You can use the following commands to reload/stop nginx
```sh
$ sudo ./nginx -s reload
$ sudo ./nginx -s quit
```
