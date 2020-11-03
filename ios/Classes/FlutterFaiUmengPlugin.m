#import "FlutterFaiUmengPlugin.h"
#import <Flutter/Flutter.h>
#import <UMCommon/UMCommon.h>
#import <UMCommonLog/UMCommonLogHeaders.h>
#import <UMAnalytics/MobClick.h>
#import "UMengRecordTool.h"

@interface FlutterFaiUmengPlugin ()

@property (nonatomic, strong) NSMutableArray *pageList;

@property (nonatomic, strong) NSDictionary *launchOptions;
@property (nonatomic, strong) NSData *deviceToken;
@property (nonatomic, strong) FlutterBasicMessageChannel *messageChannel;

@end

@implementation FlutterFaiUmengPlugin

- (NSMutableArray *)pageList {
    if (_pageList) {
        _pageList = [[NSMutableArray alloc]init];
    }
    return _pageList;
}

+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar> *)registrar {
    [registrar addApplicationDelegate:[FlutterFaiUmengPlugin shareInstance]];
    FlutterBasicMessageChannel *messageChannel = [FlutterBasicMessageChannel messageChannelWithName:@"flutter_and_native_um_100" binaryMessenger:registrar.messenger];
    [FlutterFaiUmengPlugin shareInstance].messageChannel = messageChannel;

    // 接收消息监听
    [messageChannel setMessageHandler:^(id message, FlutterReply callback) {
        NSString *method = message[@"method"];
        if ([method isEqualToString:@"umInit"]) {
            NSLog(@"flutter 调用到了 ios umInit");
            NSString *appkey = message[@"appkey"];
            NSString *pushSecret = message[@"pushSecret"];
            BOOL logEnabled = message[@"logEnabled"];

            NSLog(@"%@", [NSString stringWithFormat:@"umeng 初始化消息【 appkey:%@ pushSecret:%@ logEnabled:%d 】", appkey, pushSecret, logEnabled]);
            //日志
            [UMConfigure setLogEnabled:logEnabled];
            if (logEnabled) {
            //开发者需要显式的调用此函数，日志系统才能工作
                [UMCommonLogManager setUpUMCommonLogManager];
            }
            //渠道设置以及友盟的初始化
            [UMConfigure initWithAppkey:appkey channel:@"App Store"];
            //设置为自动采集页面
            [MobClick setAutoPageEnabled:NO];
            if (callback) {
                callback(@{ @"chnnel": @"APPStore" });
            }
        } else if ([method isEqualToString:@"umPageStart"]) {
            NSString *pageTitle = message[@"pageTitle"];
            if (pageTitle != nil) {
                [UMengRecordTool umengEnterViewWithName:pageTitle];
            }
        } else if ([method isEqualToString:@"umPageEnd"]) {
            NSString *pageTitle = message[@"pageTitle"];
            if (pageTitle != nil) {
                [UMengRecordTool umengOutViewWithName:pageTitle];
            }
        } else if ([method isEqualToString:@"eventClick"]) {
            NSString *eventTitle = message[@"eventTitle"];
            if (eventTitle != nil) {
                [UMengRecordTool umengEventCountWithId:eventTitle];
            }
        } else if ([method isEqualToString:@"umError"]) {
            NSString *errorMessage = message[@"errorMessage"];
            [UMengRecordTool umengErrorMessage:errorMessage];
        } else if ([method isEqualToString:@"pushInit"]) {

        } else if ([method isEqualToString:@"setAlias"]) {
            
        }
    }];
}

- (void)checkDeviceToken {
    if (self.deviceToken) {
        //[UMessage registerDeviceToken:self.deviceToken];
        NSLog(@"umeng注册deviceToken成功！");
        if (self.messageChannel) {
            const unsigned *tokenBytes = (const unsigned *)[self.deviceToken bytes];
            NSString *hexToken = [NSString stringWithFormat:@"%08x%08x%08x%08x%08x%08x%08x%08x",
                                  ntohl(tokenBytes[0]), ntohl(tokenBytes[1]), ntohl(tokenBytes[2]),
                                  ntohl(tokenBytes[3]), ntohl(tokenBytes[4]), ntohl(tokenBytes[5]),
                                  ntohl(tokenBytes[6]), ntohl(tokenBytes[7])];
            [self.messageChannel sendMessage:@{ @"method": @"deviceToken", @"deviceToken": hexToken }];
        }
    } else {
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(2 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
            [self checkDeviceToken];
        });
    }
}

- (void)handleMethodCall:(FlutterMethodCall *)call result:(FlutterResult)result {
    if ([@"getPlatformVersion" isEqualToString:call.method]) {
        result([@"iOS " stringByAppendingString:[[UIDevice currentDevice] systemVersion]]);
    } else {
        result(FlutterMethodNotImplemented);

    }
}

#pragma mark - life cycle

/**
 * Called if this has been registered for `UIApplicationDelegate` callbacks.
 *
 * @return `NO` if this vetoes application launch.
 */
- (BOOL)              application:(UIApplication *)application
    didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    self.launchOptions = launchOptions;
    if ([launchOptions.allKeys containsObject:UIApplicationLaunchOptionsRemoteNotificationKey]) {
        NSDictionary *userInfo = [launchOptions objectForKey:UIApplicationLaunchOptionsRemoteNotificationKey];
        /// 点击通知启动app，将通知信息回调给flutter做进一步处理
        if (self.messageChannel) {
            [self.messageChannel sendMessage:@{ @"method": @"notification", @"userInfo": userInfo ? : @{} }];
        }
    }
    return YES;
}

/**
 * Called if this has been registered for `UIApplicationDelegate` callbacks.
 */
- (void)                                 application:(UIApplication *)application
    didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken {
    self.deviceToken = deviceToken;
    NSLog(@"umeng获取deviceToken成功：%@", deviceToken);
}

/**
 * Called if this has been registered for `UIApplicationDelegate` callbacks.
 *
 * @return `YES` if this handles the request.
 */
- (BOOL)             application:(UIApplication *)application
    didReceiveRemoteNotification:(NSDictionary *)userInfo
          fetchCompletionHandler:(void (^)(UIBackgroundFetchResult result))completionHandler {
    if (self.messageChannel) {
        [self.messageChannel sendMessage:@{ @"method": @"notification", @"userInfo": userInfo ? : @{} }];
    }
    //....TODO
    //过滤掉Push的撤销功能，因为PushSDK内部已经调用的completionHandler(UIBackgroundFetchResultNewData)，
    //防止两次调用completionHandler引起崩溃
    if (![userInfo valueForKeyPath:@"aps.recall"]) {
        completionHandler(UIBackgroundFetchResultNewData);
    }
    return YES;
}


#pragma mark - 单例

static FlutterFaiUmengPlugin *_instance;

+ (instancetype)shareInstance {
    return [[self alloc] init];
}

+ (instancetype)allocWithZone:(struct _NSZone *)zone {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        if (_instance == nil) {
            _instance = [super allocWithZone:zone];
        }
    });
    return _instance;
}

- (id)copy {
    return _instance;
}

- (id)mutableCopy {
    return _instance;
}

@end
