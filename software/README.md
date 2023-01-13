### 简介

AR眼镜能看到手机屏幕上的任何内容。流程是录制手机屏幕，经过JPEG压缩，TCP协议传输到AR眼镜上，然后解压缩显示。

### 通用

- 屏幕尺寸

	AR眼镜的显示屏是240x240正方形，手机屏幕都是长方形，而且分辨率远高于240x240。所以只能截取部分屏幕（仅支持竖屏）：以短边（宽）作为正方形边，裁切手机上半部分的正方形内容，然后等比例缩放到240x240分辨率

- 个人热点

	手机开启热点

### Android

- 热点状态

	使用WifiManager实现

- 悬浮窗口

	申请悬浮窗口权限时，调用startActivityForResult，参数：Settings.ACTION_MANAGE_OVERLAY_PERMISSION。

- 录制屏幕

	使用MediaProjectionManager/MediaProjection/VirtualDisplay/ImageReader结合一个后台的IntentService来实现。
	
	Android的录制机制中，Service线程和屏幕数据线程不是一个，只要在函数`onHandleIntent`中实现截图/裁切/缩放/压缩/TCP发送的逻辑即可，其中的阻塞操作并不会影响屏幕数据（个人理解只是截图的acquireLatestImage一瞬间做了线程同步，确保正确采集到屏幕数据）

- 后台服务

	录制屏幕中有阻塞的TCP网络传输，需要使用Android的IntentService服务（普通的Service不能用耗时的阻塞操作）。

- Min SDK Version = 23

### iOS

- 个人热点

	iOS开启热点时，需要有SIM卡，并且运营商开通了热点权限。所以无SIM卡的手机就无法使用了。
	
	iOS热点不能直接配置，默认是手机名。所以如果要配置成AR眼镜的SSID，需要在`设置`=>`关于手机`=>`名称`进行修改。

	另外，iOS查询热点状态需要特殊扩展，个人账号（非开发者账号）没有权限使用，示例中只是写了代码，并没有验证。

- iOS Development Target = iOS 14.0

- 录制屏幕

	iOS的录制屏幕机制和Android不一样，更多的是类似视频流的机制，无论是否阻塞，屏幕数据流会按照固定速率（例如30帧/秒）持续发送到processSampleBuffer回调函数。如果在processSampleBuffer阻塞了，后续的屏幕数据都还在，就会导致帧的堆积，越来越慢。

	而且iOS网络库提供的NWConnection也是异步的机制，所以录制屏幕就不能用Android的同步阻塞机制，需要用`状态机`+`事件`的异步机制实现。
	