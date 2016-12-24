# weixin-sdk-demo

使用spring boot，基于weixin-sdk开发的web应用。

##如何使用？

### 企业号：
    - 修改 wx-qy-settings-test.xml 文件, 根据您的情况修改corpId, corpSecret相关信息。
    - 修改application.properties 中默认的agent配置
	    - agent.default.token=xxxxxxxxx
	    - agent.default.aesKey=xxxxxxxxxxxxxxxxx

### 公众号：
	- 修改wx-mp-settings-test.xml 文件, 根据您的情况修改appId, appSecret相关信息。

### 开发平台：
	- 修改wx-open-settings-test.xml 文件, 根据您的情况修改appId, appSecret相关信息。
	
### 微信支付：
	- 修改wx-pay-settings-test.xml 文件, 根据您的情况修改appId, mchID, key, certPath, certPass相关信息。

### 小程序：
	- 修改wx-app-settings-test.xml 文件, 根据您的情况修改appId, appSecret相关信息。
	
### 修改应用服务器端口:
	- 修改文件：application.properties

### 启动：
    - com.riversoft.weixin.demo.mp.Application
    - com.riversoft.weixin.demo.qydev.Application        
    - com.riversoft.weixin.demo.open.Application
    - com.riversoft.weixin.demo.pay.Application
    - com.riversoft.weixin.demo.app.Application


