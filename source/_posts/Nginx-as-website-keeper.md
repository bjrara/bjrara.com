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

For linux users, make sure packages in the following are installed:
* gcc-c++
* pcre, pcre-devel
* lua, lua-devel (5.1)

For osx users, make sure packages in the following are installed:
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
*The implementation is all reserverd by [the link](http://drops.wooyun.org/tips/734).*

Define limit_req_zone under **http** context:
```javascript
limit_req_zone $cookie_token zone=session_limit:10m rate=30r/s;
```
Description:

*limit_req_zone creates a shared memory zone that will keep states for the given key. In particular, the state stores the current number of excessive requests.*

In this case, a zone named "session_limit" would keep all the requests by key $cookie_token in a 10 megabyte shared memory, and an average request processing rate for this zone cannot exceed 30 request per second.

Under `location /` , we introduce lua scripts to make more intelligent strategies:

Here, we add a new token with ip address and a random number from 0-999999 encrypted by MD5 to visitors' session. Line `limit_req zone=session_limit burst=50 nodelay;` states if the requests rate exceeds the rate configured for **session_limit**, their processing will be terminated such that requests are processed at a defined rate.

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

Think of requests as guests, and the limitation the law of defining good and evil. The whole configuration works like a keeper who tells nginx whether the guest waiting outside is a nice guy or not. If yes, routes his/her requests to port 8080 and visits the website. The guest with the same cookie requesting the website for more than 30 requests per second will be treated as breaking the law, nginx will refuse all his/her excessive requests all at once, and put him/her into "evil guest" jail until the rate comes down to the expected 30 r/s and cancel the pendalty.

### Cache static resources
> This part works as an optimization to reduce static resource requests to backend service, preparing your website for heavy traffic.

*The implementation is all reserverd by [the link](http://weizhifeng.net/nginx-proxy-cache.html).*

Take facebook as an example, when you open your profile, except a simple request for a php page, your browser is requesting 20 or more css & js files. These static resources remain constant - unchanged for a relatively long time. What's more, most likely these css/js files and other images like icons or banners have been used many times at different pages. The server has to transfer them to users' browsers again and again whenever a page is being opened. Why shall we bother our website for those changeless stuff?

So highly-available (and fast) server as nginx could help us cache static resources and directly respond to resource requests.

Define proxy_cache_path under **http** context:
```javascript
proxy_cache_path /usr/local/nginx/proxy_cache/cache1 levels=1:2 keys_zone=cache1:100m inactive=1d max_size=10g;
```

"It sets the path and other parameters of a cache. Cache data are stored in files.  The levels parameter defines hierarchy levels of a cache." In this case, cache files will look like

/usr/local/nginx/proxy_cache/cache1/c/29/b7f54b2df7773722d382f4809d65029c.

Other parameters define active key information and key expire time. For more information, please read [ngx_http_proxy_module#proxy_cache_path](http://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_cache_path).

Create a new location under **server** context, before or after `location /`. No constraint order is required.

```javascript
location ~ .(jpg|png|gif|css|js|woff)$ {
	proxy_cache cache1;
    proxy_cache_key $host$uri;
    proxy_cache_valid 200 202 304 10m;
    expires 30d;
    proxy_pass http://apache_servers;
}
```

When nginx get requests of static files with extension jpg|png|gif|css|js|woff, it firstly tries to get file with key $host$uri from cache1. When the file doesn't exists, it sends a request to the backend server. If the response status code is either 200/202/304, the file would be saved to cache1 for 10 minutes. Meanwhile, "Expires" header would be added to response header fields with a value equivalent to 30 days.

* Reload/restart Nginx to make configuration take effects:
You can use the following commands to reload/restart nginx
```sh
// reload
$ sudo ./nginx -s reload

// restart
$ sudo ./nginx -s quit
$ sudo ./nginx
```

ENJOY!
===