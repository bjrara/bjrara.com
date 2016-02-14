title: Nginx 502 Bad Gateway
date: 2016-02-14 16:19:01
categories: nginx
---

> All rights reserved by bjrara. If you have any question or feedback, please contact bjrara@sina.com.

Troubleshooting Nginx 502 is great great ... great pain. Not only because there're very few sources that explain 'what causes nginx 502' but the question itself has rarely been asked (relatively), finding different roots which lead to the same phenomenon are just like digging deep into the soil for small fragments, and piecing them together without a picture in your mind.

*The following is discussed under the prerequiste that nginx is used as a proxy server. It doesn't cover the topic of 502 error when nginx is used for other purposes.*

Nginx 502 sc description:

* The request has been successfully recieved by nginx.
* The error occurred during the process when nginx is sending the request to or getting the response from the upstream server.

Root causes:

* Upstream servers are all marked down, in which case nginx could not find an available bankend to handle requests.

    Solution: at least one server should be up and healthy.

* Request/response header size is too big. The header size calculates the size of headers and the size of cookie.

    Solution: modify **proxy_buffer_size** directive. The value defaults to **4k|8k**, depending on the platform.

    When changing **proxy_buffer_size**, two other directives are affected: **proxy_busy_buffers_size** & **proxy_buffers**.

    Restriction is as follow:

    `proxy_buffer_size <= proxy_busy_buffers_size <= proxy_buffers(n-1) * size`

    FYI: Response headers are always buffered by nginx even **proxy_buffering** directive is set to off.

* Request/response headers contains invalid header.

    Solution: if the so called 'invalid header' is expected, modify **ignore_invalid_headers** or **underscores_in_headers** directive.

* Request/response body size is too large.

    Solution: modify **client_max_body_size** directive. The value defaults to **1m**.

* Connection reset by upstream server.

    Most often, the upstream server is one to blame. However, if nginx uses keepalive connections to talk to upstream server, the upstream host server resets the connection without the client being notified occasionally. For more information, check the answer: [Apache HttpClient Interim Error: NoHttpResponseException](http://stackoverflow.com/questions/10558791/apache-httpclient-interim-error-nohttpresponseexception/10600762#10600762)

    Solution:

    case 1 null.

    case 2 null if you use nginx; modify **keepalive_timeout** directive if you use tengine.

    For instance, IIS server configures connection idle timeout to 120s by default, set a number < 120s to keepalive_timeout would solve the problem.

* The server hosting nginx reports tcp timewait overflow.

    Solution: optimize tcp system configuration.