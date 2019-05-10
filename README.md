# ModuleCaller

用于android组件化开发的依赖注入库，解决模块互调的问题，降低模块之间的耦合性。

### 下载aar文件

点击[下载](https://github.com/ldw825/ModuleCaller/blob/master/release/ModuleCaller.aar)最新版aar

### 使用方法

1.项目集成

   可以使用本地aar库或者gradle依赖

   a)使用aar库

  在项目的根目录下新建libs目录，拷入ModuleCaller.aar
  在项目的build.gradle文件中添加以下代码：
  ```
  allprojects {
      repositories { 
          flatDir{
              dirs'libs'
          }
      }
  }
  ```
在需要引用ModuleCaller的模块的build.gradle文件中，增加以下代码：
```
repositories {
    flatDir {
        dirs '../libs'
    }
}

dependencies {
    implementation(name: '../libs/ModuleCaller', ext: 'aar')
}
```
b)使用gradle依赖

在项目的build.gradle文件中添加以下代码：
```
allprojects {
    repositories {
        maven {
            url 'https://jitpack.io'
        }
    }
}
```
在需要引用ModuleCaller的模块的build.gradle文件中，增加以下代码：
```
dependencies {
    implementation 'com.github.ldw825:ModuleCaller:Tag'
}
```
其中Tag写版本名称，如”1.0.1“

app模块需要依赖其他模块，在build.gradle中添加：
```
implementation project(':module1')
implementation project(':module2')
implementation project(':module3')
......
```
2.代码使用

最好是每个模块新建一个类，专门用于定义向外提供的接口，但这不是必须的。

在需要定义外部接口的类上面添加注解：
```
@ModuleClass(module = "xxx")
```
其中”xxx“指模块的名称，为任意字符串，不过最好与本模块的名称保持一致(非必须)。

在该类的接口方法前添加注解，如：
```
@ModuleMethod 
public void jumpToPaymentActivity(Context context, Intent intent) {
    intent.setComponent(new ComponentName(context, PaymentActivity.class));
    context.startActivity(intent);
}
```
同步调用：
```
ModuleCaller.getInstance().action("module2.jumpToPaymentActivity")
    .params(mContext, intent).call();
```
异步调用：
```
ModuleCaller.getInstance().action("module1.getValueAsync").callback(new ModuleCaller.Callback() { 
    @Override
    public void onCallSuccess(String action, Object result) {
        Log.d(TAG, "onCallSuccess, action=" + action + ", result=" + result);
    }

    @Override
    public void onCallFailed(String action, String message) {
        Log.d(TAG, "onCallFailed, action=" + action + ", message=" + message);
    }
}).call();
```
异步调用后回调：
```
ModuleCaller.getInstance().onCallSuccess( resultObj);
```
更多用法请参考demo
